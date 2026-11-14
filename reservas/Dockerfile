# Imagen del microservicio "reservas". Contexto de construccion: la raiz del repositorio (multi-modulo Maven).
#   docker build -f reservas/Dockerfile -t tamaka-reservas .
#
# Etapas:
#   deps  : resuelve las dependencias Maven. Las 10 imagenes comparten esta capa (mismos pom.xml y
#           mismos comandos), asi que las dependencias se descargan una sola vez para todo el stack.
#   build : compila el modulo y sus dependencias (tamaka-common) y genera el jar ejecutable.
#   run   : imagen final: JRE minima, usuario sin privilegios y comprobacion de arranque.
FROM maven:3.9-eclipse-temurin-21 AS deps
WORKDIR /src
# Solo los pom.xml: el codigo se copia en la etapa build, para que los cambios en el codigo
# no invaliden la cache de dependencias.
COPY pom.xml ./
COPY tamaka-common/pom.xml tamaka-common/pom.xml
COPY cuentas/pom.xml cuentas/pom.xml
COPY comercios/pom.xml comercios/pom.xml
COPY atractivos/pom.xml atractivos/pom.xml
COPY eventos/pom.xml eventos/pom.xml
COPY reservas/pom.xml reservas/pom.xml
COPY pagos/pom.xml pagos/pom.xml
COPY preferencias/pom.xml preferencias/pom.xml
COPY recomendaciones/pom.xml recomendaciones/pom.xml
COPY notificaciones/pom.xml notificaciones/pom.xml
COPY indicadores/pom.xml indicadores/pom.xml
RUN mvn -B -ntp -DskipTests dependency:go-offline -DexcludeArtifactIds=tamaka-common || true

FROM deps AS build
COPY . .
RUN mvn -B -ntp -DskipTests package -pl reservas -am

FROM eclipse-temurin:21-jre-jammy AS run
# Usuario sin privilegios: el proceso Java nunca corre como root.
RUN groupadd --system --gid 1001 tamaka \
 && useradd --system --uid 1001 --gid tamaka --home-dir /app --shell /usr/sbin/nologin tamaka \
 && mkdir -p /app \
 && chown -R tamaka:tamaka /app
WORKDIR /app
COPY --from=build --chown=tamaka:tamaka /src/reservas/target/reservas-*.jar /app/app.jar
USER 1001:1001
EXPOSE 8085
# Comprobacion de arranque por TCP (no hay actuator): el contenedor esta healthy cuando el puerto acepta.
HEALTHCHECK --interval=15s --timeout=3s --start-period=120s --retries=5 \
  CMD bash -c 'exec 3<>/dev/tcp/127.0.0.1/8085' || exit 1
# Opciones por defecto de la JVM (docker-compose las sobrescribe con JAVA_OPTS):
#   -XX:+ExitOnOutOfMemoryError  el contenedor muere y el orquestador lo reinicia
#   -XX:MaxRAMPercentage=70       el heap se ajusta al limite de memoria del contenedor
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70 -XX:+ExitOnOutOfMemoryError -Dfile.encoding=UTF-8"
ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS:-} -jar /app/app.jar"]
