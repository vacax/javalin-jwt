# Etapa 1: Compilación con Gradle y JDK 25
FROM gradle:9.1-jdk25 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle shadowjar --no-daemon

# Etapa 2: Imagen liviana para ejecución con JRE
FROM eclipse-temurin:25-jre-alpine
EXPOSE 7000
RUN mkdir /app
COPY --from=build /home/gradle/src/build/libs/*.jar /app/app.jar
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/app.jar"]
