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

`tamaka-common` contiene solo contratos transversales: `ErrorResponse`, `GlobalExceptionHandler`, la validación JWT RS256
(`JwtService`, `JwtAuthenticationFilter`, handlers 401/403) y el puerto `EventPublisher`. No tiene lógica de dominio.

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
cd backend
mvn -B test
```

Resultado actual: los 11 módulos compilan y las 26 pruebas unitarias de la capa de servicio pasan.

## Infraestructura local

```bash
cd backend/infra
export POSTGRES_ADMIN_PASSWORD='...'   # elija una clave local
export TAMAKA_DB_PASSWORD='...'        # clave de los usuarios tamaka_<servicio>
docker compose up -d
```

El script `infra/postgres/01-esquemas.sh` crea un esquema y un usuario por servicio.

## Claves JWT (RS256)

Cuentas firma con la clave privada; todos los servicios validan con la pública. Nunca se versionan.

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

Ejemplo para arrancar Cuentas:

```bash
cd backend/cuentas
mvn spring-boot:run
```

## Decisiones de implementación

- **RS256** (ADR-4), no HS256 como en la guía de seguridad.
- **Sin Lombok**: constructores explícitos, como exige la guía de servicios.
- **Eventos**: los casos de uso publican a través de `EventPublisher`. La implementación actual solo registra en el log;
  el adaptador SNS/SQS está pendiente.
- **Pago tardío** (hallazgo del análisis): si llega `PagoAprobado` sobre una reserva ya cancelada, Reservas no la
  confirma y emite `PagoRequiereRevision`; Pagos reembolsa. Así no queda un cobro sin reserva.
- **Recomendaciones**: Redis guarda proyecciones reconstruibles desde eventos, no fuente de verdad.
- **Jobs con varias réplicas**: los timeouts usan `@Version` para no cancelar dos veces la misma solicitud.

## Pendiente (no implementado)

- Arranque real contra PostgreSQL/MongoDB/Redis: no verificado todavía (no había Docker activo). Las pruebas son unitarias.
- Consumidores de eventos (listeners SQS) y adaptador SNS. Los métodos de consumo existen en la capa de servicio.
- Outbox transaccional entre la escritura y la publicación.
- API Gateway, Cloudflare, ALB y despliegue en AWS (infraestructura, no código de estos servicios).
- Integración real con la pasarela (hoy `PasarelaPagosSandbox`) y canales de correo/push.
- Verificación de correo, refresh tokens, cierre de sesión y RNT (Cuentas y Comercios).
- Paginación de listados, circuit breaker en la consulta HTTP entre servicios.
- Pruebas de contrato HTTP con `@WebMvcTest` para la capa de presentación y de integración con Testcontainers.
