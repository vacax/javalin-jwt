# Proyecto Javalin - JWT

Demostración de implementación de JSON Web Tokens (JWT) con Javalin 7, incluyendo autenticación por Bearer Token y por header personalizado (API Key).

## Tecnologías

| Tecnología | Versión | Propósito |
|---|---------|---|
| Java | 25      | Lenguaje de programación |
| Javalin | 7.1.0   | Framework web ligero |
| JJWT | 0.12.6  | Generación y validación de JWT |
| Jackson | 2.17.2  | Serialización/deserialización JSON |
| Datafaker | 2.4.2   | Generación de datos ficticios |
| Gradle | 9.4.1+  | Build tool |

## Arquitectura del Proyecto

```
src/main/java/edu/pucmm/pw/
  Main.java              ← Toda la lógica del servidor (rutas, filtros, handlers)

src/main/resources/public/
  index.html             ← Página principal (interfaz web)
  css/styles.css         ← Estilos de la interfaz
  js/app.js              ← Lógica del frontend (fetch API)
```

### Endpoints

| Método | Ruta | Protección | Descripción |
|---|---|---|---|
| GET | `/` | Ninguna | Página web principal (archivos estáticos) |
| POST | `/login` | Ninguna | Autenticación, retorna JWT |
| GET | `/api/estudiante` | JWT (Bearer Token) | Lista 50 estudiantes aleatorios |
| GET | `/api-token/info` | Header `X-Mi-Token` | Info del acceso autorizado |

### Flujo de autenticación JWT

1. El cliente envía `POST /login` con `username=admin` y `password=admin` (form-data)
2. El servidor valida las credenciales y genera un JWT firmado con HMAC-SHA
3. El JWT incluye: emisor (PUCMM-ECT), usuario, roles, y expiración (3 minutos)
4. El cliente usa el token en peticiones posteriores: `Authorization: Bearer <token>`
5. El filtro `filtroJwt` intercepta las rutas `/api/*` y valida el token

### Flujo de autenticación por X-Mi-Token

1. El cliente envía `GET /api-token/info` con el header `X-Mi-Token: mi-token-secreto-demo-2025`
2. El filtro `filtroMiToken` intercepta las rutas `/api-token/*` y valida el token
3. Si el token coincide, se permite el acceso al recurso

## Requisitos

- Java 25
- Gradle 9.4.1+ (wrapper incluido)

## Compilación y Ejecución

### Vía Gradle (desarrollo local)

```bash
# Ejecutar la aplicación
./gradlew run

# Compilar el fat JAR (shadow JAR)
./gradlew shadowjar

# Ejecutar tests
./gradlew test
```

### Vía Docker Compose (recomendado)

```bash
# Construir y ejecutar
docker compose up --build

# Ejecutar en segundo plano
docker compose up -d --build

# Detener
docker compose down
```

### Vía Docker (sin Compose)

```bash
docker build -t javalin-jwt . && docker run --rm -p 7000:7000 javalin-jwt
```

La aplicación estará disponible en: **http://localhost:7000**

## Pruebas con cURL

### 1. Obtener token JWT (Login)

```bash
curl --location 'http://localhost:7000/login' \
  --form 'username="admin"' \
  --form 'password="admin"'
```

Respuesta:
```json
{
  "token": "eyJhbGciOiJIUzM4NCJ9...",
  "expiresIn": 1710102011865
}
```

### 2. Consultar estudiantes (con JWT)

```bash
curl --location 'http://localhost:7000/api/estudiante' \
  --header 'Authorization: Bearer <TOKEN_OBTENIDO>'
```

### 3. Consultar endpoint con X-Mi-Token

```bash
curl --location 'http://localhost:7000/api-token/info' \
  --header 'X-Mi-Token: mi-token-secreto-demo-2025'
```

Respuesta:
```json
{
  "mensaje": "Acceso autorizado mediante X-Mi-Token",
  "timestamp": 1710102011865,
  "descripcion": "Este endpoint valida el acceso usando un header personalizado..."
}
```

## Interfaz Web

La aplicación incluye una interfaz web accesible en `http://localhost:7000` que permite:

1. **Login** - Enviar credenciales y obtener el JWT
2. **Consultar estudiantes** - Usar el JWT para acceder al endpoint protegido
3. **Probar X-Mi-Token** - Enviar peticiones con el header personalizado
4. **Ver logs** - Historial visual de las peticiones HTTP realizadas

## Notas para Estudiantes

- El **JWT** se transmite firmado pero **no encriptado**. Cualquiera puede leer su contenido (decodificando Base64). La firma solo garantiza que no fue alterado.
- La llave secreta (`LLAVE_SECRETA`) está hardcodeada para fines de demostración. En producción, se debe usar una variable de entorno.
- El token `X-Mi-Token` es un ejemplo de autenticación por API Key estática. Es más simple que JWT pero menos flexible (no contiene claims ni expiración).
- El token JWT expira a los **3 minutos**. Si recibe un error 403 al consultar estudiantes, debe hacer login nuevamente.
