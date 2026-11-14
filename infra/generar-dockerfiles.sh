#!/usr/bin/env bash
# Genera los 10 Dockerfile de los microservicios a partir de una plantilla comun.
# Ejecutar desde la raiz del repositorio: ./infra/generar-dockerfiles.sh
set -euo pipefail

raiz="$(cd "$(dirname "$0")/.." && pwd)"
cd "$raiz"

# servicio:puerto:clase principal
servicios=(
  "cuentas:8081:CuentasApplication"
  "comercios:8082:ComerciosApplication"
  "atractivos:8083:AtractivosApplication"
  "eventos:8084:EventosApplication"
  "reservas:8085:ReservasApplication"
  "pagos:8086:PagosApplication"
  "preferencias:8087:PreferenciasApplication"
  "recomendaciones:8088:RecomendacionesApplication"
  "notificaciones:8089:NotificacionesApplication"
  "indicadores:8090:IndicadoresApplication"
)

# Modulos del multi-modulo: se copian sus pom.xml para resolver dependencias sin el codigo.
modulos=(tamaka-common)
for s in "${servicios[@]}"; do modulos+=("${s%%:*}"); done

poms=""
for m in "${modulos[@]}"; do
  poms+="COPY ${m}/pom.xml ${m}/pom.xml"$'\n'
done

for entrada in "${servicios[@]}"; do
  servicio="${entrada%%:*}"
  resto="${entrada#*:}"
  puerto="${resto%%:*}"
  clase="${resto##*:}"

  cat > "${servicio}/Dockerfile" <<EOF
# Imagen del microservicio "${servicio}". Contexto de construccion: la raiz del repositorio (multi-modulo Maven).
#   docker build -f ${servicio}/Dockerfile -t tamaka-${servicio} .
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
${poms}RUN mvn -B -ntp -DskipTests dependency:go-offline -DexcludeArtifactIds=tamaka-common || true

FROM deps AS build
COPY . .
RUN mvn -B -ntp -DskipTests package -pl ${servicio} -am

FROM eclipse-temurin:21-jre-jammy AS run
# Usuario sin privilegios: el proceso Java nunca corre como root.
RUN groupadd --system --gid 1001 tamaka \\
 && useradd --system --uid 1001 --gid tamaka --home-dir /app --shell /usr/sbin/nologin tamaka \\
 && mkdir -p /app \\
 && chown -R tamaka:tamaka /app
WORKDIR /app
COPY --from=build --chown=tamaka:tamaka /src/${servicio}/target/${servicio}-*.jar /app/app.jar
USER 1001:1001
EXPOSE ${puerto}
# Comprobacion de arranque por TCP (no hay actuator): el contenedor esta healthy cuando el puerto acepta.
HEALTHCHECK --interval=15s --timeout=3s --start-period=120s --retries=5 \\
  CMD bash -c 'exec 3<>/dev/tcp/127.0.0.1/${puerto}' || exit 1
# Opciones por defecto de la JVM (docker-compose las sobrescribe con JAVA_OPTS):
#   -XX:+ExitOnOutOfMemoryError  el contenedor muere y el orquestador lo reinicia
#   -XX:MaxRAMPercentage=70       el heap se ajusta al limite de memoria del contenedor
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70 -XX:+ExitOnOutOfMemoryError -Dfile.encoding=UTF-8"
ENTRYPOINT ["sh", "-c", "exec java \${JAVA_OPTS:-} -jar /app/app.jar"]
EOF
  echo "generado ${servicio}/Dockerfile"
done
