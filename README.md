# TAMAKA — Backend de microservicios

Implementación de los 10 microservicios de `ARQUITECTURA/ARQUITECTURA_MICROSERVICIOS_TAMAKA.md`, organizados por capas.
Java 21 · Spring Boot 4.1.1 · Maven multi-módulo.

## Organización por capas

Cada microservicio sigue el mismo recorrido de una petición:

```
HTTP → Seguridad → Presentación (controller, DTO) → Servicio (interfaz + impl) → Persistencia (domain, repository)
```

| Paquete | Responsabilidad |
|---|---|
| `security` | `SecurityConfig` (rutas, STATELESS, filtro JWT, 401/403). Cuentas añade el login y `UserDetailsService`. |
| `controller` | Solo HTTP: valida con `@Valid`, delega en el servicio, devuelve `ResponseEntity`. Sin repositorios. |
| `dto.request` / `dto.response` | Records. Las entidades nunca salen por la API. |
| `service` | Interfaz de casos de uso + implementación con reglas, transacciones y publicación de eventos. |
| `mapper` | Entity ↔ DTO. Sin reglas. |
| `domain` / `repository` | Entidades con sus transiciones de estado y acceso a datos. |
| `exception` | Excepciones de negocio con nombre propio (base `BusinessException` → 409). |
| `event` | Consumidores de eventos (`ManejadorEvento`): solo traducen el evento y delegan en el servicio. |
| `scheduler` | Jobs `@Scheduled` (timeouts y finalización de reservas) que delegan en el servicio. |

`tamaka-common` contiene solo contratos transversales: `ErrorResponse`, `GlobalExceptionHandler`, la validación JWT RS256
(`JwtService`, `JwtAuthenticationFilter`, handlers 401/403) y el bus de eventos (`com.tamaka.common.event`).
No tiene lógica de dominio.

## Estructura

| Módulo | Puerto | Almacén | Estado |
|---|---|---|---|
| `tamaka-common` | — | — | Librería compartida |
| `cuentas` | 8081 | PostgreSQL (`cuentas`) | Registro, login RS256, roles, suspensión |
| `comercios` | 8082 | PostgreSQL (`comercios`) | Comercios y servicios (JSONB) |
| `atractivos` | 8083 | PostgreSQL (`atractivos`) | Catálogo, estado y alertas |
| `eventos` | 8084 | PostgreSQL (`eventos`) | Publicar, editar, desactivar |
| `reservas` | 8085 | PostgreSQL (`reservas`) | Máquina de estados, timeouts 30/60 min |
| `pagos` | 8086 | PostgreSQL (`pagos`) | Webhook firmado, idempotencia, reembolso |
| `preferencias` | 8087 | MongoDB | Preferencias y favoritos |
| `recomendaciones` | 8088 | Redis | Proyecciones desde eventos |
| `notificaciones` | 8089 | MongoDB | Avisos bilingües es/en |
| `indicadores` | 8090 | PostgreSQL (`indicadores`) | Demanda para Gestores y Operadores |

## Compilar y probar

```bash
mvn -B test
```

Resultado actual (`MAVEN_OPTS=-Xmx768m mvn -B -o test`, ejecutado dos veces seguidas con el mismo resultado):
los 11 módulos compilan y pasan **102 pruebas, 0 fallos** (common 17, cuentas 23, comercios 6, atractivos 2,
eventos 1, reservas 20, pagos 7, preferencias 4, recomendaciones 7, notificaciones 10, indicadores 5). Incluyen pruebas
de servicio, de consumidores de eventos, del outbox (relay, reintentos, FALLIDO) y `@WebMvcTest` de
`AdminReservaController`, `CuentaController` y `AuthenticationController`.
Si la JVM se queda sin memoria, ejecute módulo a módulo: `mvn -B test -pl tamaka-common,reservas`.

## Infraestructura local

Hay dos formas de levantar el backend en local:

| Modo | Comando | Cuándo usarlo |
|---|---|---|
| Todo en contenedores | `docker compose up -d --build` (raíz del repositorio) | Prueba de extremo a extremo. Es el modo recomendado |
| Solo los almacenes | `docker compose --env-file .env -f infra/docker-compose.yml up -d` | Los 10 servicios se ejecutan desde el IDE (`mvn spring-boot:run`) |

El `docker-compose.yml` de la raíz levanta **todo**: PostgreSQL, MongoDB, Redis, los 10 microservicios y el gateway
nginx. Solo el gateway publica un puerto (`TAMAKA_HTTP_PORT`, 80 por defecto); los almacenes y los servicios quedan en
la red interna.

```bash
./infra/generar-secretos.sh    # crea el .env de la raíz: claves RSA de JWT y secretos aleatorios (no sobrescribe)
docker compose up -d --build
docker compose ps               # esperar a que los 13 contenedores estén healthy
curl http://localhost/api/eventos
```

Para publicar además los puertos de cada microservicio (depuración), añade el `ports` del servicio que
necesites en el `docker-compose.yml` de la raíz, por ejemplo `cuentas: ports: ["8081:8081"]`.

El script `infra/postgres/01-esquemas.sh` crea un esquema y un usuario por servicio (se ejecuta solo la primera vez,
cuando se crea el volumen de PostgreSQL).

## Claves JWT (RS256)

Cuentas firma con la clave privada; todos los servicios validan con la pública. Nunca se versionan.
El flujo completo (quién emite, quién valida y cómo sabe cada servicio quién es el usuario) está en
[Autenticación](#autenticación-cómo-validan-los-usuarios-los-demás-servicios).

```bash
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out jwt-private.pem
openssl pkey -in jwt-private.pem -pubout -out jwt-public.pem

# Valores para las variables de entorno: el cuerpo Base64 del PEM (PKCS#8 la privada, SPKI la pública), sin cabeceras
grep -v -- '-----' jwt-private.pem | tr -d '\n'; echo
grep -v -- '-----' jwt-public.pem | tr -d '\n'; echo
```

Variables de entorno:

| Variable | Servicios | Uso |
|---|---|---|
| `TAMAKA_JWT_PRIVATE_KEY` | cuentas | Firma de tokens (PKCS#8, Base64) |
| `TAMAKA_JWT_PUBLIC_KEY` | todos | Validación (X.509/SPKI, Base64). Sin ella el servicio no arranca. |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | servicios PostgreSQL | Conexión por servicio |
| `MONGO_URI` | preferencias, notificaciones | Conexión MongoDB |
| `REDIS_HOST`, `REDIS_PORT` | recomendaciones | Conexión Redis |
| `COMERCIOS_URL` | reservas | Consulta puntual de servicios |
| `PAGOS_WEBHOOK_SECRET` | pagos | Secreto HMAC de la pasarela (vacío = webhooks rechazados) |
| `TAMAKA_EVENTOS_SECRETO` | todos | Secreto compartido del bus de eventos (vacío = no se aceptan eventos y solo se registran en el log) |
| `TAMAKA_EVENTOS_TRANSPORTE` | todos | `http` (por defecto) o `log` |
| `RESERVAS_URL`, `PAGOS_URL`, `RECOMENDACIONES_URL`, `NOTIFICACIONES_URL`, `INDICADORES_URL` | productores | Base de los consumidores (por defecto `http://localhost:<puerto>`) |
| `TAMAKA_OUTBOX_HABILITADO` | productores | `true` por defecto: eventos por outbox transaccional; `false` vuelve a la entrega directa |

## Autenticación: cómo validan los usuarios los demás servicios

Cuentas es un servicio aparte, pero **ningún otro servicio lo consulta para saber quién es el usuario**. La
identidad viaja en el propio token (JWT **RS256**), de forma que cada microservicio valida al usuario por su cuenta,
sin llamadas de red al servicio de cuentas y sin compartir su base de datos.

```
                      ┌──────────────────────────────────────────────┐
  Cliente ──login──▶ │ Cuentas (único emisor)                        │
                      │  BCrypt + UserDetailsService → cuenta activa  │
                      │  firma JWT RS256 con la CLAVE PRIVADA        │
                      └───────────────┬──────────────────────────────┘
                                      │  { accessToken (15 min), refreshToken (7 días) }
                                      ▼
  Cliente ── Authorization: Bearer <accessToken> ──▶ gateway nginx (solo enruta)
                                      │
        ┌─────────────┬───────────────┼───────────────┬─────────────┐
        ▼             ▼               ▼               ▼             ▼
    Reservas      Pagos        Preferencias     Recomendaciones   Indicadores
        │             │               │               │             │
        └─────────────┴───────────────┴───────────────┴─────────────┘
                     cada uno: JwtAuthenticationFilter (tamaka-common)
                     → verifica la firma con la CLAVE PÚBLICA
                     → arma la identidad en el SecurityContext
                     → @PreAuthorize / reglas de ruta
```

### Quién emite y quién valida

| | Cuentas | Los otros 9 servicios |
|---|---|---|
| Clave que tiene | **privada** (PKCS#8) | solo la **pública** (X.509/SPKI) |
| Qué hace | firma los access tokens | validan la firma y extraen los roles |
| Base de datos | `cuentas` (el único con usuarios y contraseñas) | la suya, sin datos de usuarios |

La clave pública no es un secreto: se reparte a los 10 servicios por variable de entorno (`TAMAKA_JWT_PUBLIC_KEY`)
y se puede rotar sin tocar código. La privada solo la tiene Cuentas (`TAMAKA_JWT_PRIVATE_KEY`).

### El flujo paso a paso

1. **Login** — `POST /api/auth/login` (solo Cuentas). El `AuthenticationManager` compara el password con el hash
   BCrypt (`CuentaUserDetailsService` + `DaoAuthenticationProvider`) y comprueba que la cuenta esté activa.
2. **Emisión** — Cuentas firma un JWT RS256 de 15 minutos:
   - `sub` = id de la cuenta (es lo que usa el resto de servicios: `Long.valueOf(authentication.getName())`),
   - `iss` = `tamaka-cuentas` (obligatorio: un token de otro emisor se rechaza),
   - `roles` = `["TURISTA", "OPERADOR", "GESTOR", "ADMIN"]` según el modo activo,
   - `iat` / `exp`.
   Además entrega un **refresh token opaco** (32 bytes aleatorios) del que solo se guarda el hash SHA-256; rota en
   cada uso y, si se reutiliza uno ya rotado, se revocan todas las sesiones de la cuenta.
3. **Petición** — el cliente manda `Authorization: Bearer <accessToken>`. El gateway nginx **no** valida nada: solo
   enruta y bloquea `/internal/**`.
4. **Validación local** — cada servicio tiene su propia `SecurityConfig` (STATELESS) con el
   `JwtAuthenticationFilter` de `tamaka-common`: lee la cabecera, verifica la firma con la clave pública y el `iss`,
   y si es válido deja la identidad en el `SecurityContextHolder` (`UsernamePasswordAuthenticationToken` con
   `ROLE_<rol>` a partir del claim `roles`). Si el token falta o es inválido, la petición sigue **anónima** y la
   regla de acceso la rechaza con 401 (`RestAuthenticationEntryPoint`); si es válida pero falta el rol, 403
   (`RestAccessDeniedHandler`).
5. **Autorización** — cada endpoint declara su rol: `@PreAuthorize("hasRole('TURISTA')")` o reglas de ruta en la
   `SecurityConfig` (`/api/admin/**` → `hasRole("ADMIN")`).
6. **Identidad** — los servicios toman el id del usuario del token, no de una consulta: `authentication.getName()`
   devuelve el `sub`. Por eso ningún servicio necesita la base de datos de Cuentas ni su API.

### Qué NO pasa por el bus de eventos

`POST /internal/eventos` (bus de eventos) **no** usa JWT: se autentica con el secreto compartido
`TAMAKA_EVENTOS_SECRETO` (cabecera `X-Tamaka-Eventos-Secreto`), cada `SecurityConfig` lo deja fuera del filtro JWT y
el gateway bloquea `/internal/**`. Son dos mecanismos distintos y separados: JWT = identidad de usuario, secreto =
comunicación servicio a servicio.

### Límites del esquema actual (y qué haría falta si molestan)

- **Revocación inmediata**: un access token ya emitido sigue siendo válido hasta que expira (15 min), aunque se
  suspenda la cuenta o se cierre la sesión. Hoy se acepta ese desfase (el evento `CuentaSuspendida` desactiva los
  comercios del operador y el refresh token deja de servir). Para revocación inmediata haría falta una lista negra
  de tokens en Redis (o `jti` + versión de credencial) que el filtro consultara.
- **Rotación de claves**: al cambiar el par RSA, los tokens firmados con la clave vieja dejan de validar; con 15 min
  de vida el impacto es acotado, pero conviene acceptability de las dos claves durante el despliegue.
- **Sin estado por servicio**: cada servicio es stateless y escalable por separado; a cambio, no puede saber si el
  usuario fue borrado o suspendido sin llamar a Cuentas.
- **Alternativas descartadas**: *introspection* (llamada a Cuentas en cada petición: acopla y ralentiza todo),
  tokens opacos con sesión compartida (obliga a un almacén común y a que Cuentas esté disponible),
  o validar el token solo en el gateway (convierte al gateway en un punto único de espera y de caída).
  La firma asimétrica RS256 evita las tres: cada servicio valida localmente, en microsegundos, sin red.

Ejemplo de uso completo:

```bash
# 1. Login (público)
TOKEN=$(curl -s -X POST http://localhost/api/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"turista@ejemplo.com","password":"ClaveSegura123"}' | jq -r .accessToken)

# 2. Cualquier servicio valida ese token por su cuenta
curl -s http://localhost/api/reservas/mias -H "Authorization: Bearer $TOKEN"

# 3. Con el token alterado, todos los servicios responden 401
curl -s http://localhost/api/reservas/mias -H "Authorization: Bearer ${TOKEN}x"
```

Ejemplo para arrancar Cuentas:

```bash
cd cuentas
mvn spring-boot:run
```

## Decisiones de implementación

- **RS256** (ADR-4), no HS256 como en la guía de seguridad.
- **Jar ejecutable**: en Spring Boot 4 el `spring-boot-starter-parent` declara `spring-boot-maven-plugin` **solo** en
  `pluginManagement`, así que `mvn package` no reempaqueta nada por sí solo. Cada servicio lo activa en su `pom.xml`
  para que la imagen pueda arrancar con `java -jar app.jar`. `tamaka-common` no lo lleva: es una librería, no una
  aplicación (un jar "fat" no sirve como dependencia).
- **Sin Lombok**: constructores explícitos, como exige la guía de servicios.
- **Eventos**: los casos de uso publican a través de `EventPublisher`; en desarrollo viajan por HTTP interno
  (sección siguiente). El adaptador SNS/SQS está pendiente pero no requiere cambiar los servicios.
- **Pago tardío** (hallazgo del análisis): si llega `PagoAprobado` sobre una reserva ya cancelada, Reservas no la
  confirma y emite `PagoRequiereRevision`; Pagos reembolsa. Así no queda un cobro sin reserva.
- **Recomendaciones**: Redis guarda proyecciones reconstruibles desde eventos, no fuente de verdad.
- **Jobs con varias réplicas**: los timeouts usan `@Version` para no cancelar dos veces la misma solicitud.

## Bus de eventos

Diseño (en `tamaka-common`, paquete `com.tamaka.common.event`):

```
Servicio ──EventPublisher──▶ OutboxEventPublisher ──INSERT (misma transacción)──▶ outbox_evento
                                RelayOutbox (@Scheduled) ──EntregadorHttp: POST /internal/eventos──▶ otro servicio
                                  ENVIADO | reintento con espera exponencial | FALLIDO tras N intentos o un 4xx
Servicios sin outbox: TransaccionalEventPublisher (tras el commit) → HttpEventPublisher (o LoggingEventPublisher)
otro servicio: ReceptorEventosController → DespachadorEventos (verifica el secreto) → ManejadorEvento (paquete event)
                                                                                      → capa de servicio
```

- **Puerto de salida** `EventPublisher` (sin cambios para los servicios). `EventosConfig` elige el adaptador:
  outbox si `tamaka.eventos.outbox.habilitado=true`; si no, `tamaka.eventos.transporte` (`http` | `log`).
  Para SNS/SQS basta otro `EntregadorEventos` y un listener SQS que invoque `DespachadorEventos`.
- **Outbox transaccional** (`com.tamaka.common.event.outbox`): puerto `OutboxStore` con dos adaptadores:
  `JdbcOutboxStore` (tabla `<esquema>.outbox_evento`, migración `V3__outbox_evento.sql` en cuentas, comercios,
  atractivos, eventos, reservas y pagos; el INSERT comparte la transacción JPA) y `MongoOutboxStore` (colección
  `outbox_evento` en Preferencias). `RelayOutbox` reclama lotes con arriendo (`FOR UPDATE SKIP LOCKED` /
  `findAndModify`, apto para varias réplicas), entrega y marca `ENVIADO`; ante fallos transitorios reintenta con
  espera `espera-base · 2^(n-1)` (tope `espera-maxima`) y tras `max-intentos`, o ante un 4xx, deja `FALLIDO` para
  revisión manual (`SELECT * FROM <esquema>.outbox_evento WHERE estado = 'FALLIDO'`). Sin secreto configurado no
  consume intentos: los eventos esperan `PENDIENTE`.
- **Garantía**: al menos una vez. Cada `DomainEvent` lleva un `id` único (UUID) que se conserva en las reentregas.
- **Entrada**: `POST /internal/eventos` con cabecera `X-Tamaka-Eventos-Secreto`. Cada `SecurityConfig` la deja fuera
  del filtro JWT; **no debe exponerse por el API Gateway**. Respuestas: 202, 401 (secreto), 409 (evento inválido o
  regla de negocio), 5xx (reintentable).
- **Payload**: ids numéricos; montos y fechas como texto (sin pérdida de precisión en JSON). `DomainEvent` ofrece
  lectores tipados (`datoLong`, `datoDecimal`, `datoInstante`, ... y sus variantes `Requerido`).
- **Idempotencia de consumidores**: Reservas (`confirmarPorPago`), Pagos (`registrarReservaAceptada`),
  Indicadores (índice único `tipo + referencia`), Recomendaciones (conjuntos Redis), proyecciones de Notificaciones.
  Los avisos de Notificaciones no son idempotentes por sí mismos: se registra el id del evento en la colección
  `eventos_procesados` (TTL 30 días) y una reentrega se descarta.

Configuración (ya incluida en cada `application.yml`, valores por defecto a `localhost`):

```yaml
tamaka:
  eventos:
    transporte: ${TAMAKA_EVENTOS_TRANSPORTE:http}
    secreto: ${TAMAKA_EVENTOS_SECRETO:}
    timeout: 5s
    reintentos: 3
    suscriptores:
      ReservaAceptada: ${PAGOS_URL:http://localhost:8086}/internal/eventos,${NOTIFICACIONES_URL:http://localhost:8089}/internal/eventos
```

Para el flujo local basta exportar el mismo secreto en todas las terminales: `export TAMAKA_EVENTOS_SECRETO='...'`.

| Evento | Productor | Consumidores |
|---|---|---|
| `CuentaSuspendida` | Cuentas | Comercios (desactiva comercios y servicios del operador) |
| `ServicioCreado`, `ServicioActualizado` | Comercios | Recomendaciones (registra el servicio con la categoría del comercio) |
| `ServicioDesactivado` | Comercios | Reservas (cancela PENDIENTE/ACEPTADA del servicio), Recomendaciones (lo retira) |
| `AtractivoActualizado` | Atractivos | Recomendaciones, Notificaciones (seguidores) |
| `AlertaSeguridadEmitida` | Atractivos | Notificaciones (seguidores) |
| `EventoPublicado`, `EventoDesactivado` | Eventos | Recomendaciones |
| `ReservaSolicitada` | Reservas | Notificaciones, Indicadores |
| `ReservaAceptada` | Reservas | Pagos, Notificaciones |
| `ReservaRechazada` | Reservas | Notificaciones |
| `ReservaCancelada` | Reservas | Pagos (reembolso si estaba pagada), Notificaciones, Indicadores |
| `PagoRequiereRevision` | Reservas | Pagos (reembolso de pago tardío) |
| `PagoAprobado` | Pagos | Reservas (confirma), Notificaciones, Indicadores |
| `PagoRechazado` | Pagos | Notificaciones |
| `PreferenciasActualizadas` | Preferencias | Recomendaciones, Notificaciones (idioma) |
| `FavoritoGuardado`, `FavoritoEliminado` | Preferencias | Notificaciones (seguidores de atractivos) |

Jobs de Reservas: `TimeoutReservasJob` (30/60 min) y `FinalizacionReservasJob` (CONFIRMADA → FINALIZADA cuando la
fecha ya pasó, zona `America/Bogota`, cada `tamaka.reservas.intervalo-finalizacion`).

## Endpoints añadidos

| Servicio | Endpoint | Rol | Notas |
|---|---|---|---|
| Cuentas | `POST /api/auth/login` | público | Devuelve `accessToken` (15 min) + `refreshToken` opaco (7 días, solo se guarda su hash SHA-256) |
| Cuentas | `POST /api/cuentas/me/modo-operador` | TURISTA | Devuelve `{cuenta, token}` con un par nuevo; revoca los refresh tokens anteriores |
| Cuentas | `PATCH /api/cuentas/me/modo` `{"modo":"TURISTA"\|"OPERADOR"}` | autenticado | Alterna el modo; token nuevo. En modo turista el token no lleva `OPERADOR` |
| Cuentas | `GET /api/admin/cuentas`, `GET /api/admin/cuentas/{id}` | ADMIN | |
| Cuentas | `POST /api/auth/refresh` `{"refreshToken":"..."}` | público | Rota el refresh token (el usado queda revocado) y devuelve un par nuevo. Reutilizar uno revocado cierra todas las sesiones de la cuenta. 401 si no es válido |
| Cuentas | `POST /api/auth/logout` `{"refreshToken":"..."}` | público | Revoca el refresh token; siempre 204 |
| Comercios | `GET /api/comercios/{id}` | público | Comercio activo con sus servicios activos |
| Comercios | `PATCH /api/servicios/{id}` | OPERADOR dueño | Actualización parcial; valida el rango de personas |
| Atractivos | `GET /api/atractivos/{id}` | público | Con las 5 alertas más recientes |
| Eventos | `GET /api/eventos/{id}` | público | Los eventos tienen ahora `categoria` (por defecto `GENERAL`) |
| Reservas | `GET /api/reservas/{id}` | TURISTA dueño | |
| Reservas | `GET /api/operador/reservas/{id}` | OPERADOR dueño | |
| Reservas | `PATCH /api/operador/reservas/{id}/cancelar` `{"motivo":"..."}` (opcional) | OPERADOR dueño | Solo ACEPTADA o CONFIRMADA |
| Reservas | `GET /api/admin/reservas?estado=` | ADMIN | |
| Pagos | `GET /api/operador/pagos` | OPERADOR | Solo cobros de sus servicios |
| Pagos | `GET /api/admin/pagos?estado=` | ADMIN | |
| Preferencias | `POST /api/favoritos` con `tipo: COMERCIO` | TURISTA | Nuevo tipo de favorito |

Migraciones nuevas: `pagos/V2__operador_en_pagos.sql`, `eventos/V2__categoria_evento.sql`,
`indicadores/V2__idempotencia_demanda.sql` (elimina duplicados conservando el de menor id antes del índice único;
nunca se había publicado, por eso no hay V3), `cuentas/V2__refresh_token.sql` y `V3__outbox_evento.sql` en los seis
servicios PostgreSQL que publican (Flyway admite el hueco de versión en comercios, atractivos y reservas).

## Despliegue

Todo el backend es portable: imágenes Docker por servicio (`<servicio>/Dockerfile`, contexto en la raíz del
repositorio) y configuración solo por variables de entorno. No depende de Render ni de ningún proveedor.

### Imágenes Docker

Cada `<servicio>/Dockerfile` tiene tres etapas:

| Etapa | Qué hace |
|---|---|
| `deps` | Resuelve las dependencias Maven **solo con los `pom.xml`** (el código se copia después), así que un cambio en el código no vuelve a descargar nada. Las 10 imágenes comparten esta capa: las dependencias se bajan una sola vez para todo el stack. |
| `build` | Compila el módulo y `tamaka-common` (`-am`) y genera el jar ejecutable de Spring Boot (`spring-boot-maven-plugin`, ver *Decisiones de implementación*). |
| `run` | Imagen final: `eclipse-temurin:21-jre-jammy`, usuario **sin privilegios** (uid 1001), `JAVA_OPTS` con `-XX:+ExitOnOutOfMemoryError` (el contenedor muere y el orquestador lo reinicia) y `-XX:MaxRAMPercentage` para que el heap siga el límite de memoria del contenedor. Arranca con `java -jar app.jar`. |

Los Dockerfiles se generan con `infra/generar-dockerfiles.sh` (plantilla común para los 10 servicios: si cambia el
formato, se edita el script y se vuelve a ejecutar).

### Opción A — una VM con Docker Compose (recomendada para presupuesto bajo)

Un solo servidor (AWS Lightsail/EC2, GCP, Azure, DigitalOcean, Hetzner...) con Docker y 4 GB de RAM o más
(10 JVM de 384 MB + PostgreSQL + MongoDB + Redis). El `docker-compose.yml` de la raíz levanta:

- **Almacenes**: PostgreSQL 18 (una base `tamaka`, un esquema y un usuario por servicio vía
  `infra/postgres/01-esquemas.sh`), MongoDB 8 (preferencias y notificaciones), Redis 7 (caché de recomendaciones,
  sin persistencia). Ninguno publica puertos.
- **Los 10 microservicios**, con healthcheck, `depends_on` sobre su almacén y las URLs del bus con nombres de
  servicio Docker (`http://pagos:8086`, ...).
- **Gateway nginx** (`infra/gateway/nginx.conf`): única entrada (`TAMAKA_HTTP_PORT`, 80 por defecto), enruta
  `/api/**` a cada servicio y bloquea `/internal/**`. Para HTTPS ponga delante Caddy/Traefik o el balanceador
  del proveedor.

```bash
./infra/generar-secretos.sh                      # crea .env: claves RSA de JWT y secretos aleatorios (no sobrescribe)
docker compose up -d --build
docker compose ps   # esperar a que todos estén healthy
curl http://localhost/api/eventos
```

**Verificado en local (12/11/2026)** con Docker Desktop (8 CPU, 3,9 GB): los 13 contenedores quedan *healthy* y
`infra/prueba-e2e.mjs` recorre el flujo completo a través del gateway (registro, modo operador, refresh/revocación,
comercio y servicio, reserva, aceptación, pago, webhook firmado, confirmación por evento, notificaciones,
recomendaciones, indicadores, 401/403 y bloqueo de `/internal`):

```bash
PAGOS_WEBHOOK_SECRET=$(grep '^PAGOS_WEBHOOK_SECRET=' .env | cut -d= -f2-) node infra/prueba-e2e.mjs
```

Lecciones del arranque en máquinas pequeñas:

- **Memoria**: con menos de ~5 GB para Docker, baje el límite por servicio: `TAMAKA_MEMORIA_SERVICIO=300m`.
- **CPU**: arrancar las 10 JVM a la vez satura la CPU (Flyway e Hibernate tardan minutos y los healthchecks
  fallan). La JVM ya usa `-XX:TieredStopAtLevel=1` (sobrescribible con `TAMAKA_JAVA_OPTS`); aun así conviene
  arrancar por tandas:
  `docker compose up -d postgres mongo redis`, luego
  `... up -d cuentas comercios atractivos`, `... up -d eventos reservas pagos`,
  `... up -d indicadores preferencias notificaciones`, `... up -d recomendaciones gateway` (~100 s por tanda).
- **Entrega de eventos**: es "al menos una vez". Si un consumidor tarda más que `tamaka.eventos.timeout` (5 s),
  el productor reintenta mientras el primero sigue en curso; `DespachadorEventos` trata el choque con una clave
  única (`DataIntegrityViolationException` / `DuplicateKeyException`) como evento ya aplicado, no como rechazo.
- **MongoDB en Spring Boot 4**: la conexión se configura con `spring.mongodb.uri`; `spring.data.mongodb.uri` se ignora.

Todas las variables están documentadas en `.env.example` (raíz del repositorio). Copias de seguridad: volúmenes `postgres-datos` y
`mongo-datos` (`pg_dump` / `mongodump`). Escalar después: mover PostgreSQL y MongoDB a servicios gestionados cambiando
solo `DB_URL`/`MONGO_URI`.

### Opción B — Render u otro PaaS

Cada microservicio es un *web service* Docker independiente (10 servicios) más: PostgreSQL (puede ser una sola base:
cada servicio crea su esquema con Flyway), Redis/Key Value y **MongoDB externo** (p. ej., MongoDB Atlas; Render no lo
ofrece). `render.yaml` describe hoy los 8 servicios PostgreSQL/Redis; faltan Preferencias y Notificaciones (requieren
`MONGO_URI`). Limitaciones: los planes gratuitos suspenden servicios inactivos (los eventos esperan en el outbox hasta
que despiertan, pero la primera petición tarda) y limitan el número de servicios y bases; 10 servicios pagos suelen
costar más que una VM. Configure en el PaaS las mismas variables de `.env.example` y las URLs públicas o
privadas de los consumidores (`PAGOS_URL`, ...). No exponga `/internal/eventos` fuera de la red privada si el PaaS
la ofrece.

### Desarrollo local

- Todo en contenedores: igual que la opción A.
- Servicios desde el IDE o `mvn spring-boot:run` con solo los almacenes en Docker:
  `docker compose --env-file .env -f infra/docker-compose.yml up -d` (sección *Infraestructura local*), y en cada
  terminal las variables `TAMAKA_JWT_PUBLIC_KEY`, `TAMAKA_EVENTOS_SECRETO`, `DB_PASSWORD`, etc.
  (`./infra/generar-secretos.sh --claves` imprime un par de claves).
  Las URLs del bus apuntan por defecto a `http://localhost:<puerto>`.

## Pendiente (no implementado)

- Arranque real contra PostgreSQL/MongoDB/Redis y `docker compose ... up --build`: **verificado** (13/13 contenedores
  healthy) y `infra/prueba-e2e.mjs` recorre el flujo completo con éxito. Las pruebas son unitarias y de controlador;
  el SQL de `JdbcOutboxStore` y las migraciones no se han probado contra PostgreSQL (pendiente: Testcontainers).
- Adaptador SNS y listener SQS (el bus HTTP cubre desarrollo y Render; el puerto ya permite el cambio).
- Outbox en Preferencias (MongoDB sin transacciones): si el proceso cae entre guardar el documento y el evento, ese
  evento se pierde. Los mensajes `FALLIDO` no tienen todavía un endpoint de reenvío (revisión manual en la base).
- Notificaciones marca el evento antes de crear avisos: si cae justo después, esos avisos se pierden (se prefirió
  no duplicar).
- Orden de eventos no garantizado (p. ej., `ReservaCancelada` antes que `ReservaAceptada` en Pagos).
- Preferencias y Notificaciones aún no están en `render.yaml` (MongoDB): en Render sus entregas fallan y quedan en el log.
- Si cambia la categoría de un comercio, sus servicios conservan la categoría anterior en Recomendaciones hasta la
  siguiente actualización de cada servicio.
- API Gateway, Cloudflare, ALB y despliegue en AWS (infraestructura, no código de estos servicios).
- Integración real con la pasarela (hoy `PasarelaPagosSandbox`) y canales de correo/push.
- Verificación de correo y RNT (Cuentas y Comercios). Un access token ya emitido sigue válido hasta expirar
  (15 min) aunque se cierre la sesión o se cambie de modo.
- Paginación de listados, circuit breaker en la consulta HTTP entre servicios.
- `@WebMvcTest` para el resto de controladores (hoy solo `AdminReservaController` y `CuentaController`) y pruebas
  de integración con Testcontainers.
