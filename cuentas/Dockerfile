# Imagen de Cuentas. Contexto de construccion: backend/ (raiz del multi-modulo).
#   docker build -f cuentas/Dockerfile -t tamaka-cuentas .
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY . .
# Compila Cuentas y su dependencia tamaka-common, y copia las librerias de ejecucion a /app/lib.
RUN mvn -B -q -pl cuentas -am package dependency:copy-dependencies \
      -DskipTests -DoutputDirectory=/app/lib -DincludeScope=runtime

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/cuentas/target/cuentas-1.0.0-SNAPSHOT.jar /app/app.jar
COPY --from=build /app/lib /app/lib
EXPOSE 8081
ENTRYPOINT ["java", "-cp", "/app/app.jar:/app/lib/*", "com.tamaka.cuentas.CuentasApplication"]
