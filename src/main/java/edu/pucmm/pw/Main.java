package edu.pucmm.pw;

import io.javalin.Javalin;
import io.javalin.http.*;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import net.datafaker.Faker;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static io.javalin.apibuilder.ApiBuilder.*;

public class Main {

    //Llave secreta para la firma del JWT, tener cuidado con la información.
    public final static String LLAVE_SECRETA = "ejemplo_de_llave_generada_icc352";
    public static void main(String[] args) {
        Javalin app = Javalin.create(config -> {
                    config.router.apiBuilder(() -> {
                        path("/api", () -> {
                            before(Main::filtroJwt);
                            path("/estudiante", () -> {
                                get(Main::listaEstudiantes);
                            });
                            //los demás métodos no están implementados por ser una demostración
                        });
                    });
                })
                .get("/", ctx -> ctx.result("Ejemplo Javalin con JWT"))
                .post("/login", Main::login) //Por Curl o Postman realizar al validación.
                .start(7000);
    }

    /**
     * Filtro para validar que el se cuente con la autenticación de
     * @param ctx
     * @throws Exception
     */
    private static void filtroJwt(Context ctx) throws Exception{
        System.out.println("Analizando que exista el token");

        // Si es del tipo options lo dejo pasar.
        if(ctx.method() == HandlerType.OPTIONS){
            return;
        }

        // Informacion para consultar en la trama.
        String header = "Authorization";
        String prefijo = "Bearer";

        // Mostrando todos los header recibidos.
        Set<String> listaHeader = ctx.headerMap().keySet();
        for(String key : listaHeader){
            System.out.println(String.format("header[%s] = %s", key, ctx.header(key)));
        }

        // Verificando si existe el header de autorizacion.
        String headerAutentificacion = ctx.header(header);
        if(headerAutentificacion ==null || !headerAutentificacion.startsWith(prefijo)){
            throw new UnauthorizedResponse("Debe autenticarse para el servicio");
        }

        //recuperando el token y validando
        String tramaJwt = headerAutentificacion.replace(prefijo, "");
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(LLAVE_SECRETA.getBytes()))
                    .build()
                    .parseSignedClaims(tramaJwt.trim())
                    .getPayload();
            //mostrando la información para demostración.
            System.out.println("Mostrando el JWT recibido: " + claims.toString());
        }catch (ExpiredJwtException | MalformedJwtException | SignatureException e){ //Excepciones comunes
            throw new ForbiddenResponse("Error verificando la trama: "+e.getMessage());
        }

        //En este punto puedo realizar validaciones en función a los permisos del usuario.
        // tener pendiente que el JWT está formado no encriptado.
    }

    /**
     * Handler para validar
     * @param ctx
     * @throws Exception
     */
    private static void login(Context ctx) throws Exception{
        //
        Usuario usuarioObj=null;
        String username = ctx.formParam("username");
        String password = ctx.formParam("password");
        //
        String passwordAdmin = "admin";

        //
        if(username.equals("admin") && password.equals(passwordAdmin)){
            //Información simulando que viene de la base de datos.
            //por simplificar la demostración no implementado.
            usuarioObj = new Usuario("admin","admin",List.of("creacion","listar","actualizar", "eliminar"));
        }else{
            //Retornando con el código de error por si existe algún problema
            ctx.status(HttpStatus.UNAUTHORIZED).result("Debe autenticarse para el servicio");
        }
        //Si la autenticación es valida retornando el token.
        ctx.json(generacionJsonWebToken(usuarioObj));
    }

    /**
     *
     * @param usuario
     * @return
     */
    private static LoginResponse generacionJsonWebToken(Usuario usuario){
        //generando la llave.
        SecretKey secretKey = Keys.hmacShaKeyFor(LLAVE_SECRETA.getBytes());

        //Generando la fecha valida por el momento incluyendo 3 minutos.
        LocalDateTime localDateTime = LocalDateTime.now().plusMinutes(3);
        System.out.println("La fecha actual: "+localDateTime.toString());

        //
        Date fechaExpiracion = Date.from(localDateTime.toInstant(ZoneOffset.ofHours(-4)));
        // creando la trama.
        String jwt = Jwts.builder()
                .setIssuer("PUCMM-ECT")
                .setSubject("Demo JWT")
                .setExpiration(fechaExpiracion)
                .claim("usuario", usuario.nombre())
                .claim("roles", String.join(",", usuario.roles()))
                .signWith(secretKey)
                .compact();
        //
        LoginResponse loginResponse = new LoginResponse(jwt, fechaExpiracion.getTime());

        //
        return loginResponse;
    }

    /**
     *
     * @param ctx
     * @throws Exception
     */
    private static void listaEstudiantes(Context ctx) throws Exception{
        List<Estudiante> lista =  new ArrayList<>();
        Faker faker = new Faker();
        for (int i = 0; i < 50; i++) {
            lista.add(new Estudiante(faker.number().digits(8), faker.name().fullName()));
        }
        ctx.json(lista);
    }

    /**
     * Record para retornar el JWT bajo el estandar.
     * @param token
     * @param expiresIn
     */
    public record LoginResponse(String token,long expiresIn){}

    public record Usuario(String nombre, String password, List<String> roles){}

    /**
     * Registro de prueba de un DTO de un estudiante.
     * @param id
     * @param nombre
     */
    public record Estudiante(String id, String nombre){}
}