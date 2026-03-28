package edu.pucmm.pw;

import io.javalin.Javalin;
import io.javalin.http.*;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import net.datafaker.Faker;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

/**
 * Clase principal que demuestra la implementación de JWT con Javalin 7.
 *
 * Endpoints públicos:
 * - GET  /           → Página principal (servida desde archivos estáticos)
 * - POST /login      → Autenticación y generación del token JWT
 *
 * Endpoints protegidos por JWT (header Authorization: Bearer <token>):
 * - GET /api/estudiante → Lista de estudiantes generados con Datafaker
 *
 * Endpoint protegido por token personalizado (header X-Mi-Token):
 * - GET /api-token/info → Información del token validado
 */
public class Main {

    // Llave secreta para la firma del JWT. En producción, usar variables de entorno.
    public static final String LLAVE_SECRETA = "ejemplo_de_llave_generada_icc352";

    // Token estático para demostración del endpoint /api-token.
    // En un escenario real, estos tokens se almacenarían en base de datos.
    public static final String MI_TOKEN_ESTATICO = "mi-token-secreto-demo-2025";

    public static void main(String[] args) {

        // En Javalin 7, toda la configuración (rutas, plugins, etc.) va dentro de Javalin.create()
        var app = Javalin.create(config -> {

            // Habilitar archivos estáticos desde /public en el classpath
            config.staticFiles.add("/public");

            // Habilitar CORS para desarrollo local
            config.bundledPlugins.enableCors(cors -> cors.addRule(it -> it.anyHost()));

            // === Rutas públicas ===
            // Endpoint de login - recibe credenciales y retorna JWT
            config.routes.post("/login", Main::login);

            // === Rutas protegidas por JWT (Bearer Token) ===
            config.routes.before("/api/*", Main::filtroJwt);
            config.routes.get("/api/estudiante", Main::listaEstudiantes);

            // === Rutas protegidas por X-Mi-Token (Token personalizado) ===
            config.routes.before("/api-token/*", Main::filtroMiToken);
            config.routes.get("/api-token/info", Main::infoToken);
        });

        // Iniciar el servidor en el puerto 7000
        app.start(7000);
    }

    // ==================== FILTROS ====================

    /**
     * Filtro que valida la presencia y validez de un JWT en el header Authorization.
     * Formato esperado: "Authorization: Bearer <token>"
     *
     * Si el token no está presente → 401 Unauthorized
     * Si el token es inválido o expirado → 403 Forbidden
     */
    private static void filtroJwt(Context ctx) {
        System.out.println("Validando JWT en la petición...");

        // Permitir peticiones OPTIONS (preflight de CORS)
        if (ctx.method() == HandlerType.OPTIONS) {
            return;
        }

        // Verificar que exista el header Authorization con prefijo Bearer
        String headerAutenticacion = ctx.header("Authorization");
        String prefijo = "Bearer";

        if (headerAutenticacion == null || !headerAutenticacion.startsWith(prefijo)) {
            throw new UnauthorizedResponse("Debe autenticarse para acceder al servicio. Envíe el header 'Authorization: Bearer <token>'");
        }

        // Extraer y validar el token JWT
        String tramaJwt = headerAutenticacion.replace(prefijo, "").trim();
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(LLAVE_SECRETA.getBytes()))
                    .build()
                    .parseSignedClaims(tramaJwt)
                    .getPayload();

            System.out.println("JWT válido recibido: " + claims.toString());

            // Almacenar claims en el contexto para uso posterior en los handlers
            ctx.attribute("jwt-claims", claims);

        } catch (ExpiredJwtException e) {
            throw new ForbiddenResponse("El token JWT ha expirado: " + e.getMessage());
        } catch (MalformedJwtException | SignatureException e) {
            throw new ForbiddenResponse("Token JWT inválido: " + e.getMessage());
        }
    }

    /**
     * Filtro que valida el acceso mediante un header personalizado "X-Mi-Token".
     * Este es un ejemplo de autenticación por API Key / token estático.
     *
     * Si el header no está presente o el token no coincide → 401 Unauthorized
     */
    private static void filtroMiToken(Context ctx) {
        System.out.println("Validando X-Mi-Token en la petición...");

        // Permitir peticiones OPTIONS (preflight de CORS)
        if (ctx.method() == HandlerType.OPTIONS) {
            return;
        }

        String tokenRecibido = ctx.header("X-Mi-Token");

        if (tokenRecibido == null || tokenRecibido.isBlank()) {
            throw new UnauthorizedResponse("Debe enviar el header 'X-Mi-Token' para acceder a este recurso.");
        }

        if (!tokenRecibido.equals(MI_TOKEN_ESTATICO)) {
            throw new ForbiddenResponse("El token proporcionado en 'X-Mi-Token' no es válido.");
        }

        System.out.println("X-Mi-Token válido recibido.");
    }

    // ==================== HANDLERS ====================

    /**
     * Handler de login. Recibe usuario y contraseña por form params.
     * Si las credenciales son válidas, genera y retorna un JWT.
     *
     * Parámetros (form-data):
     * - username: nombre de usuario (valor válido: "admin")
     * - password: contraseña (valor válido: "admin")
     *
     * Respuesta exitosa: { "token": "...", "expiresIn": 123456789 }
     * Respuesta fallida: 401 Unauthorized
     */
    private static void login(Context ctx) {
        String username = ctx.formParam("username");
        String password = ctx.formParam("password");

        // Validación básica de credenciales (en producción, consultar base de datos)
        if ("admin".equals(username) && "admin".equals(password)) {
            // Simulando información de usuario desde base de datos
            Usuario usuario = new Usuario("admin", "admin", List.of("creacion", "listar", "actualizar", "eliminar"));
            ctx.json(generacionJsonWebToken(usuario));
        } else {
            ctx.status(HttpStatus.UNAUTHORIZED).json(Map.of(
                    "error", "Credenciales inválidas",
                    "mensaje", "Usuario o contraseña incorrectos"
            ));
        }
    }

    /**
     * Handler que retorna una lista de 50 estudiantes generados aleatoriamente con Datafaker.
     * Requiere autenticación JWT válida (filtrado por filtroJwt).
     */
    private static void listaEstudiantes(Context ctx) {
        List<Estudiante> lista = new ArrayList<>();
        Faker faker = new Faker();
        for (int i = 0; i < 50; i++) {
            lista.add(new Estudiante(faker.number().digits(8), faker.name().fullName()));
        }
        ctx.json(lista);
    }

    /**
     * Handler del endpoint /api-token/info.
     * Retorna información confirmando que el acceso fue autorizado.
     * Requiere el header X-Mi-Token con el valor correcto.
     */
    private static void infoToken(Context ctx) {
        ctx.json(Map.of(
                "mensaje", "Acceso autorizado mediante X-Mi-Token",
                "timestamp", System.currentTimeMillis(),
                "descripcion", "Este endpoint valida el acceso usando un header personalizado (X-Mi-Token) en lugar de JWT."
        ));
    }

    // ==================== UTILIDADES ====================

    /**
     * Genera un JSON Web Token firmado con HMAC-SHA para el usuario proporcionado.
     * El token incluye:
     * - Emisor (iss): PUCMM-ECT
     * - Asunto (sub): Demo JWT
     * - Expiración (exp): 3 minutos desde la generación
     * - Claims personalizados: usuario, roles
     */
    private static LoginResponse generacionJsonWebToken(Usuario usuario) {
        SecretKey secretKey = Keys.hmacShaKeyFor(LLAVE_SECRETA.getBytes());

        // Token válido por 3 minutos
        LocalDateTime localDateTime = LocalDateTime.now().plusMinutes(3);
        Date fechaExpiracion = Date.from(localDateTime.toInstant(ZoneOffset.ofHours(-4)));

        String jwt = Jwts.builder()
                .issuer("PUCMM-ECT")
                .subject("Demo JWT")
                .expiration(fechaExpiracion)
                .claim("usuario", usuario.nombre())
                .claim("roles", String.join(",", usuario.roles()))
                .signWith(secretKey)
                .compact();

        return new LoginResponse(jwt, fechaExpiracion.getTime());
    }

    // ==================== RECORDS (DTOs) ====================

    /** Respuesta del login conteniendo el token JWT y su tiempo de expiración en milisegundos. */
    public record LoginResponse(String token, long expiresIn) {}

    /** Representa un usuario del sistema con su nombre, contraseña y lista de roles. */
    public record Usuario(String nombre, String password, List<String> roles) {}

    /** DTO de un estudiante con su matrícula (id) y nombre completo. */
    public record Estudiante(String id, String nombre) {}
}
