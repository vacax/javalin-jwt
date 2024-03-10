# Proyecto Javalin - JWT

Demostración de implementación de JWT con Javalin.

## Utiliza:

- Javalin 6.1.3
- JJWT (  io.jsonwebtoken.jjwt-api)
- Datafaker
- Jackson

## Requiere:

- Java 21
- Gradle 8.5

## Compilación y ejecución:

Vía Gradlew:

```
gradlew run
```

o Vía Docker:

````
docker build -t javalin-jwt . && docker run --rm -p 7000:7000 javalin-jwt
````

## Prueba:

Para la generación del token, pueden utilizar el siguiente comand:

```
curl --location 'localhost:7000/login' \
--form 'username="admin"' \
--form 'password="admin"'
```

Salida:

```
{
    "token": "eyJhbGciOiJIUzM4NCJ9.eyJpc3MiOiJQVUNNTS1FQ1QiLCJzdWIiOiJEZW1vIEpXVCIsImV4cCI6MTcxMDEwMjAxMSwidXN1YXJpbyI6ImFkbWluIiwicm9sZXMiOiJjcmVhY2lvbixsaXN0YXIsYWN0dWFsaXphcixlbGltaW5hciJ9._HG2JqEONiXwo3da0wzwtbgIsWYaUiDda2kqtT5cwn_RICDejtgGmC3c2_AEZ37c",
    "expiresIn": 1710102011865
}
```

Para llamar el endpoint controlado por el JWT:

```
curl --location 'localhost:7000/api/estudiante' \
--header 'Authorization: Bearer eyJhbGciOiJIUzM4NCJ9.eyJpc3MiOiJQVUNNTS1FQ1QiLCJzdWIiOiJEZW1vIEpXVCIsImV4cCI6MTcxMDEwMjAxMSwidXN1YXJpbyI6ImFkbWluIiwicm9sZXMiOiJjcmVhY2lvbixsaXN0YXIsYWN0dWFsaXphcixlbGltaW5hciJ9._HG2JqEONiXwo3da0wzwtbgIsWYaUiDda2kqtT5cwn_RICDejtgGmC3c2_AEZ37c'
```

Resultado:

```
[
    {
        "id": "17669651",
        "nombre": "Renda Johnston"
    },
    {
        "id": "76931142",
        "nombre": "Ms. Logan Runolfsson"
    },
    {
        "id": "09825926",
        "nombre": "Marcell Mayer"
    },
    {
        "id": "92792566",
        "nombre": "Carley Wisoky"
    },
    {
        "id": "04640175",
        "nombre": "Odell Schumm"
    }
]
```