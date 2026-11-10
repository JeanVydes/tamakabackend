# Imagen de reservas. Contexto de construccion: backend/ (raiz del multi-modulo).
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY . .
RUN mvn -B -q -pl reservas -am package dependency:copy-dependencies       -DskipTests -DoutputDirectory=/app/lib -DincludeScope=runtime

FROM eclipse-temurin:21-jre
WORKDIR /app
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70"
COPY --from=build /src/reservas/target/reservas-1.0.0-SNAPSHOT.jar /app/app.jar
COPY --from=build /app/lib /app/lib
EXPOSE 8085
ENTRYPOINT ["java", "-cp", "/app/app.jar:/app/lib/*", "com.tamaka.reservas.ReservasApplication"]
