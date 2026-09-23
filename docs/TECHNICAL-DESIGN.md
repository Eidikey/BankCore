# BankCore --- Technical Design

**Versión:** 1.0\
**Proyecto:** BankCore\
**Tipo:** Aplicación web bancaria simulada\
**Backend:** Java 25 LTS + Spring Boot\
**Frontend:** React + TypeScript + Vite\
**Base de datos:** PostgreSQL\
**Proveedor de producción:** Supabase\
**Arquitectura:** Monolito modular con principios Clean/Hexagonal

------------------------------------------------------------------------

# 1. Propósito

Este documento define las decisiones técnicas y la arquitectura de
implementación de BankCore a partir de los requisitos establecidos en el
SRS v1.1.

Mientras que el SRS define **qué debe hacer el sistema**, este documento
define principalmente **cómo será construido**.

El diseño prioriza:

-   Integridad financiera.
-   Seguridad.
-   Mantenibilidad.
-   Testabilidad.
-   Claridad arquitectónica.
-   Aprendizaje de conceptos backend.
-   Facilidad de desarrollo para un equipo de 3 personas.
-   Posibilidad de evolución futura.

------------------------------------------------------------------------

# 2. Decisiones técnicas principales

  -----------------------------------------------------------------------
  Área                                Decisión
  ----------------------------------- -----------------------------------
  Backend                             Java 25 LTS

  Framework                           Spring Boot

  Seguridad                           Spring Security

  Autenticación                       JWT de corta duración + Refresh
                                      Tokens persistidos

  Access Token                        JWT, 15 minutos

  Refresh Token                       UUID opaco persistido en `sessions`

  Password hashing                    Argon2

  Arquitectura                        Monolito modular + principios
                                      Clean/Hexagonal

  Persistencia                        Spring Data JPA + Hibernate

  SQL especializado                   SQL nativo cuando sea necesario

  Base de datos                       PostgreSQL

  Desarrollo DB                       PostgreSQL mediante Docker

  Producción DB                       Supabase PostgreSQL

  Migraciones                         Liquibase

  API                                 REST + JSON

  Versionado API                      `/api/v1`

  Documentación API                   OpenAPI

  Concurrencia                        Transacciones + pessimistic row
                                      locking

  Retry                               `@Retryable` para fallos
                                      transitorios de concurrencia

  Testing                             JUnit + Mockito + Spring Boot
                                      Test + MockMvc + Testcontainers

  Contenedores                        Docker

  Frontend                            React + TypeScript + Vite

  Repositorio                         Monorepo

  CI/CD                               GitHub Actions, fase posterior

  Deploy                              Plataforma gratuita o free tier a
                                      determinar
  -----------------------------------------------------------------------

------------------------------------------------------------------------

# 3. Arquitectura general

BankCore será implementado como un **monolito modular**.

No se utilizarán microservicios en el MVP.

La arquitectura combinará:

-   Organización modular por dominio.
-   Separación de responsabilidades.
-   Principios de Clean Architecture.
-   Principios de arquitectura hexagonal cuando aporten valor.
-   Spring Boot como infraestructura principal.

``` text
Frontend (React + TypeScript + Vite)
                |
              HTTPS
                |
                v
        REST API / Spring Boot
                |
     +----------+-----------+
     |                      |
 Application             Security
 / Use Cases          Spring Security
     |
     v
   Domain
 Business Rules
     |
     v
 Infrastructure
 JPA / Hibernate
 + SQL nativo
     |
     v
 PostgreSQL
 Local / Supabase
```

------------------------------------------------------------------------

# 4. Monolito modular

Aunque BankCore será una única aplicación desplegable, internamente se
dividirá por dominios.

``` text
bankcore/
├── auth/
├── user/
├── account/
├── transfer/
├── movement/
├── audit/
├── idempotency/
└── shared/
```

Cada módulo será responsable de una parte concreta del dominio.

------------------------------------------------------------------------

# 5. Estructura interna de módulos

Una estructura de referencia será:

``` text
account/
├── domain/
│   ├── model/
│   ├── repository/
│   └── service/
├── application/
│   ├── service/
│   ├── usecase/
│   └── dto/
├── infrastructure/
│   └── persistence/
└── presentation/
    └── controller/
```

No todos los módulos deberán replicar carpetas innecesarias. La
estructura debe representar responsabilidades reales.

------------------------------------------------------------------------

# 6. Responsabilidades arquitectónicas

## 6.1 Presentation

Responsable de HTTP:

-   Recibir requests.
-   Validar estructura de entrada.
-   Invocar casos de uso.
-   Convertir resultados a respuestas HTTP.
-   No contener lógica financiera.

## 6.2 Application

Contendrá casos de uso como:

``` text
CreateAccountUseCase
DepositMoneyUseCase
WithdrawMoneyUseCase
TransferMoneyUseCase
CloseAccountUseCase
```

Coordinará reglas, transacciones, servicios de dominio, repositorios e
idempotencia.

## 6.3 Domain

Contendrá reglas centrales del negocio:

``` text
Account
Money
Transfer
Movement
AccountStatus
Role
```

El dominio evitará depender innecesariamente de HTTP, controllers, JWT o
detalles de PostgreSQL.

## 6.4 Infrastructure

Contendrá implementaciones técnicas:

-   Repositorios JPA.
-   SQL nativo.
-   JWT.
-   Persistencia de sesiones.
-   Configuración de PostgreSQL.
-   Configuración de seguridad.

------------------------------------------------------------------------

# 7. Java

BankCore fija oficialmente:

``` text
Java 25 LTS
```

Java 25 fue publicado en septiembre de 2025 y es una versión LTS. En
septiembre de 2026 sigue siendo la LTS actual seleccionada para
BankCore.

No se utilizarán versiones no-LTS únicamente por ser más recientes.

------------------------------------------------------------------------

# 8. Spring Boot

Spring Boot será el framework principal.

Se utilizará para:

-   REST API.
-   Dependency Injection.
-   Configuration.
-   Spring Security.
-   Transactions.
-   Persistence.
-   Validation.
-   Testing.
-   Resilience/retry cuando corresponda.

Dependencias previstas:

``` text
Spring Web
Spring Security
Spring Data JPA
Bean Validation
PostgreSQL Driver
Liquibase
Spring Boot Test
Spring Retry / capacidades de retry de la versión de Spring utilizada
```

------------------------------------------------------------------------

# 9. Persistencia

Se utilizará:

``` text
Spring Data JPA
+
Hibernate
```

JPA será la opción principal, permitiendo SQL nativo cuando una
operación requiera control explícito.

Casos esperados:

-   `SELECT ... FOR UPDATE`.
-   Consultas específicas.
-   Operaciones sensibles a concurrencia.
-   Operaciones donde SQL resulte más claro que la abstracción ORM.

------------------------------------------------------------------------

# 10. Migraciones

Las modificaciones de esquema se gestionarán mediante **Liquibase**.

``` text
db/
└── changelog/
    ├── db.changelog-master.yaml
    ├── 001-create-users.yaml
    ├── 002-create-sessions.yaml
    ├── 003-create-accounts.yaml
    ├── 004-create-transfers.yaml
    ├── 005-create-movements.yaml
    ├── 006-create-idempotency-keys.yaml
    └── 007-create-audit-events.yaml
```

No se dependerá de cambios manuales de esquema en PostgreSQL.

------------------------------------------------------------------------

# 11. Base de datos y ambientes

PostgreSQL será la base de datos principal.

Desarrollo:

``` text
Spring Boot
    |
PostgreSQL Docker
```

Producción/demo:

``` text
Spring Boot
    |
Supabase PostgreSQL
```

Supabase se utilizará como proveedor administrado de PostgreSQL. La
lógica de negocio permanecerá en el backend.

------------------------------------------------------------------------

# 12. Modelo lógico inicial

El modelo incluirá como mínimo:

``` text
users
sessions
accounts
transfers
movements
audit_events
idempotency_keys
```

------------------------------------------------------------------------

# 13. Usuarios y roles

Conceptualmente:

``` text
User
├── id
├── email / username
├── password_hash
├── role
├── status
├── failed_login_attempts
├── locked_until
├── created_at
└── updated_at
```

## 13.1 Enum formal de roles

Para el MVP se utilizará:

``` java
public enum Role {
    CLIENT,
    EMPLOYEE,
    ADMIN
}
```

El rol será persistido como `VARCHAR`.

No se implementará inicialmente una matriz dinámica de permisos, tablas
complejas RBAC ni un sistema de permisos configurable.

Una alternativa aceptable si la evolución del modelo lo requiere será
una tabla intermedia simple `user_roles`, pero no es necesaria para el
MVP con un único rol por usuario.

------------------------------------------------------------------------

# 14. Contraseñas

Se utilizará **Argon2**.

``` text
Password
   |
 Argon2
   |
Password Hash
   |
Database
```

Las contraseñas nunca serán almacenadas en texto plano ni registradas en
logs.

------------------------------------------------------------------------

# 15. Autenticación

Se utilizará:

``` text
Spring Security
+
JWT Access Token
+
Refresh Token persistido
```

La autenticación distinguirá claramente:

-   Access Token: autorización de requests.
-   Refresh Token: renovación de la sesión.
-   Session: registro persistente y revocable.

------------------------------------------------------------------------

# 16. Access Token JWT

El Access Token será un JWT de corta duración.

Duración inicial:

``` text
15 minutos
```

Será enviado mediante:

``` http
Authorization: Bearer <access-token>
```

El JWT contendrá únicamente claims necesarios para
autenticación/autorización.

No contendrá:

-   Contraseñas.
-   Saldos.
-   Información financiera.
-   Refresh Tokens.
-   Secretos.

El Access Token no se almacenará como sesión completa en PostgreSQL.

------------------------------------------------------------------------

# 17. Refresh Tokens y sesiones

Los Refresh Tokens serán identificadores opacos UUID generados
criptográficamente y persistidos mediante la tabla `sessions`.

Conceptualmente:

``` text
Session
├── id
├── user_id
├── refresh_token
├── expires_at
├── revoked_at
├── created_at
└── last_used_at
```

Cada login crea una sesión independiente.

Esto permite:

-   Múltiples sesiones concurrentes.
-   Logout individual.
-   Revocación individual.
-   Expiración.
-   Invalidación del Refresh Token.

El Refresh Token no será un JWT.

Para producción deberá evitarse almacenar un secreto reutilizable
innecesariamente en texto plano; la implementación puede persistir una
representación segura/hash del token manteniendo el UUID opaco entregado
al cliente.

------------------------------------------------------------------------

# 18. Flujo de autenticación

``` text
POST /api/v1/auth/login
        |
Validate credentials
        |
Create session
        |
Generate JWT access token (15 min)
        |
Generate UUID refresh token
        |
Persist session
        |
Return credentials
```

Renovación:

``` text
Refresh Token
      |
Validate session
      |
Check expiration/revocation
      |
Issue new Access Token
```

Logout:

``` text
Refresh Token / Session
      |
Revoke session
```

Un logout no invalidará automáticamente otras sesiones del usuario.

------------------------------------------------------------------------

# 19. Bloqueo de autenticación

Después de tres intentos consecutivos fallidos:

``` text
3 failed attempts
       |
15 minute lock
```

Se utilizarán conceptualmente:

``` text
failed_login_attempts
locked_until
```

Los intentos durante el bloqueo no reiniciarán innecesariamente el
temporizador.

------------------------------------------------------------------------

# 20. Modelo de cuenta

``` text
Account
├── id
├── account_number
├── user_id
├── account_type
├── status
├── balance
├── created_at
└── updated_at
```

El identificador interno será UUID.

El número público:

-   Exactamente 10 dígitos.
-   `VARCHAR`.
-   `UNIQUE`.
-   No será PK.
-   No será mecanismo de seguridad.

------------------------------------------------------------------------

# 21. Estados de cuenta

``` java
public enum AccountStatus {
    ACTIVA,
    BLOQUEADA,
    CERRADA
}
```

Las reglas serán aplicadas en el dominio/aplicación y reforzadas
mediante constraints cuando sea apropiado.

------------------------------------------------------------------------

# 22. Dinero y balance

Java:

``` java
BigDecimal
```

PostgreSQL:

``` sql
NUMERIC(15,2)
```

No se utilizará `float` ni `double` para dinero.

La cuenta mantendrá `accounts.balance` como saldo actual y `movements`
como historial. El saldo no se recalculará desde todo el historial en
cada consulta.

Reglas:

``` text
1.00 MXN <= amount <= 50,000.00 MXN
```

------------------------------------------------------------------------

# 23. Value Object Money

Se recomienda un Value Object:

``` text
Money
├── amount
└── currency
```

Aunque el MVP opera exclusivamente en MXN, esto centraliza reglas de
precisión y validación.

------------------------------------------------------------------------

# 24. Concurrencia financiera

La estrategia principal será:

``` text
Database Transaction
+
Pessimistic Row Locking
```

mediante operaciones equivalentes a:

``` sql
SELECT ...
FROM accounts
WHERE id = ?
FOR UPDATE;
```

Los locks solamente existirán dentro de transacciones y deberán
mantenerse durante el menor tiempo razonable.

------------------------------------------------------------------------

# 25. Retiros

``` text
BEGIN
  |
Lock account
  |
Validate status
  |
Validate balance
  |
Update balance
  |
Create movement
  |
COMMIT
```

Ante cualquier fallo:

``` text
ROLLBACK
```

Ejemplo concurrente:

``` text
Balance inicial = 1000

A -> retirar 800
B -> retirar 800
```

A obtiene el lock, deja el saldo en 200 y confirma. B obtiene
posteriormente el lock, observa 200 y su operación es rechazada.

------------------------------------------------------------------------

# 26. Transferencias

Una transferencia será una entidad propia:

``` text
Transfer
├── id
├── operation_id
├── source_account_id
├── destination_account_id
├── amount
├── status
├── created_at
└── completed_at
```

Generará dos movimientos:

``` text
Transfer
├── TRANSFER_DEBIT
└── TRANSFER_CREDIT
```

------------------------------------------------------------------------

# 27. Atomicidad de transferencias

``` text
BEGIN
  |
Lock source
  |
Lock destination
  |
Validate
  |
Debit source
  |
Credit destination
  |
Create transfer
  |
Create debit movement
  |
Create credit movement
  |
COMMIT
```

Cualquier error provocará `ROLLBACK`.

------------------------------------------------------------------------

# 28. Orden determinista de locks

Cuando una operación deba bloquear dos cuentas, los locks se adquirirán
en un orden determinista basado en sus IDs internos.

Ejemplo:

``` text
min(accountA.id, accountB.id)
             |
             v
max(accountA.id, accountB.id)
```

El objetivo es reducir el riesgo de deadlocks cuando dos transferencias
concurrentes involucren las mismas cuentas en orden inverso.

------------------------------------------------------------------------

# 29. Política de retry para concurrencia

Los reintentos automáticos se limitarán exclusivamente a **errores
transitorios de concurrencia**.

No se reintentará automáticamente una operación por:

-   Saldo insuficiente.
-   Cuenta bloqueada.
-   Cuenta cerrada.
-   Monto inválido.
-   Falta de autorización.
-   Idempotency Key reutilizada con otro payload.
-   Cualquier otro error de negocio determinista.

Sí podrán reintentarse fallos como:

-   Timeout esperando un pessimistic lock.
-   Deadlock detectado por PostgreSQL.
-   Excepciones transitorias equivalentes traducidas por Spring.
-   Conflictos de concurrencia expresamente clasificados como
    recuperables.

La implementación utilizará `@Retryable` o la capacidad equivalente de
retry de la versión de Spring utilizada.

Política inicial:

``` text
Intento inicial: 1
Reintentos automáticos máximos: 2
Total máximo de intentos: 3
Backoff inicial: 100 ms
Multiplicador: 2
Backoff máximo: 500 ms
Jitter: habilitado
```

Conceptualmente:

``` java
@Retryable(
    includes = {
        CannotAcquireLockException.class,
        PessimisticLockingFailureException.class,
        DeadlockLoserDataAccessException.class
    },
    maxRetries = 2,
    delay = 100,
    multiplier = 2,
    maxDelay = 500,
    jitter = 50
)
```

Las excepciones concretas deberán validarse contra la versión real de
Spring empleada, evitando depender de excepciones obsoletas.

## 29.1 Límite del retry

Cada intento deberá ejecutar una **transacción nueva y completa**.

No se reintentará parcialmente una transacción fallida.

La frontera de retry deberá envolver la frontera transaccional de forma
que:

``` text
Attempt 1
  |
BEGIN
...
ROLLBACK
  |
Attempt 2
  |
BEGIN
...
COMMIT
```

y no:

``` text
BEGIN
  |
retry fragmentos internos
  |
COMMIT
```

## 29.2 Retry + idempotencia

Los reintentos internos del servidor y los reintentos HTTP del cliente
son problemas distintos.

Las operaciones financieras seguirán protegidas mediante
`Idempotency-Key`, de forma que un timeout observado por el cliente no
provoque una segunda operación financiera.

## 29.3 Retry agotado

Si los reintentos por conflicto transitorio se agotan, la operación
completa deberá permanecer sin aplicar y la API devolverá un error
controlado.

Para BankCore se fija:

``` http
409 Conflict
```

con un código de negocio estable, por ejemplo:

``` json
{
  "status": 409,
  "code": "CONCURRENCY_CONFLICT",
  "message": "The operation could not be completed due to a concurrent transaction. Please retry."
}
```

`409 Conflict` se utiliza porque el fallo representa un conflicto
temporal con el estado concurrente del recurso, no una solicitud
sintácticamente inválida.

------------------------------------------------------------------------

# 30. Política HTTP para solicitudes inválidas

Para errores de validación de la solicitud se utilizará:

``` http
400 Bad Request
```

Esto incluye casos como:

-   JSON mal formado.
-   Campos obligatorios ausentes.
-   Tipos/formato inválidos.
-   Monto inválido.
-   Violaciones de validaciones del request.

Por tanto, **400 Bad Request queda fijado como respuesta estándar para
solicitudes inválidas**, mientras que un conflicto de concurrencia
agotado utiliza `409 Conflict`.

Otros códigos previstos:

``` text
200 OK
201 Created
400 Bad Request
401 Unauthorized
403 Forbidden
404 Not Found
409 Conflict
500 Internal Server Error
```

La API evitará `500` para errores de negocio esperados.

------------------------------------------------------------------------

# 31. Movimientos

``` text
Movement
├── id
├── operation_id
├── account_id
├── type
├── amount
├── balance_after
└── created_at
```

Tipos iniciales:

``` text
DEPOSIT
WITHDRAWAL
TRANSFER_DEBIT
TRANSFER_CREDIT
```

------------------------------------------------------------------------

# 32. Operation ID

Cada operación financiera tendrá un identificador único.

Permitirá correlacionar:

``` text
Request
  |
Operation
  ├── Transfer
  ├── Movement(s)
  └── Audit Event
```

------------------------------------------------------------------------

# 33. Idempotencia

Las operaciones financieras susceptibles de reintento aceptarán:

``` http
Idempotency-Key: <unique-key>
```

La idempotencia protege frente a:

-   Doble clic.
-   Reintento HTTP.
-   Timeout del cliente.
-   Reenvío accidental.
-   Solicitudes concurrentes con la misma clave.

------------------------------------------------------------------------

# 34. Tabla formal `idempotency_keys`

La tabla queda definida como:

``` sql
CREATE TABLE idempotency_keys (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    idempotency_key VARCHAR(255) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL,
    response_code INT,
    response_body JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_user_idempotency UNIQUE (user_id, idempotency_key)
);
```

Estados permitidos:

``` text
IN_PROGRESS
COMPLETED
FAILED
```

La restricción:

``` sql
UNIQUE (user_id, idempotency_key)
```

hace que la clave sea única dentro del ámbito del usuario.

Liquibase será la fuente de verdad del esquema; el SQL anterior
representa formalmente el resultado esperado de la migración.

------------------------------------------------------------------------

# 35. Request Hash

`request_hash` será un SHA-256 representado en hexadecimal, por lo que
utilizará:

``` text
VARCHAR(64)
```

El hash se calculará a partir de una representación canónica de los
campos relevantes de la operación.

Ejemplo:

``` text
Idempotency-Key: ABC
amount: 500
```

Si posteriormente se recibe:

``` text
Idempotency-Key: ABC
amount: 900
```

el hash será diferente y la reutilización será rechazada.

------------------------------------------------------------------------

# 36. Flujo de idempotencia

``` text
Request
  |
Read Idempotency-Key
  |
Attempt to register key
  |
+-------------------------------+
|                               |
New key                      Existing key
|                               |
IN_PROGRESS                  Compare hash
|                               |
Process                     +---+---+
|                           |       |
COMPLETED                 same   different
|                           |       |
Store response          previous   reject
                         result
```

La restricción UNIQUE de PostgreSQL será parte de la protección frente a
dos requests concurrentes intentando registrar la misma clave.

------------------------------------------------------------------------

# 37. Estados de idempotencia

## IN_PROGRESS

La operación fue aceptada y está siendo procesada.

## COMPLETED

La operación finalizó exitosamente y la respuesta puede reutilizarse.

## FAILED

La operación terminó en un fallo que el diseño haya determinado
persistir.

La implementación deberá diferenciar fallos definitivos de fallos
transitorios que provocan rollback/retry.

------------------------------------------------------------------------

# 38. API REST

BankCore expondrá:

``` text
REST
+
JSON
+
HTTPS en deployment
```

Todas las rutas tendrán versión:

``` text
/api/v1/...
```

Ejemplos:

``` text
/api/v1/auth/login
/api/v1/auth/refresh
/api/v1/auth/logout
/api/v1/accounts
/api/v1/accounts/{id}
/api/v1/transfers
/api/v1/movements
```

------------------------------------------------------------------------

# 39. DTOs

Las entidades JPA no se expondrán directamente.

``` text
JPA Entity
    |
Mapper
    |
DTO
    |
JSON
```

Esto separará el modelo persistente del contrato público de la API.

------------------------------------------------------------------------

# 40. Formato de errores

Formato base:

``` json
{
  "timestamp": "2026-09-22T20:00:00Z",
  "status": 409,
  "code": "INSUFFICIENT_BALANCE",
  "message": "Insufficient account balance",
  "path": "/api/v1/transfers"
}
```

`code` será un identificador estable de negocio.

------------------------------------------------------------------------

# 41. OpenAPI

La API se documentará mediante OpenAPI.

La documentación deberá reflejar:

-   Endpoints.
-   Authentication.
-   Request DTOs.
-   Response DTOs.
-   Errores.
-   `Idempotency-Key` donde corresponda.

------------------------------------------------------------------------

# 42. Seguridad de API

Las rutas protegidas seguirán:

``` text
JWT
 |
Authentication
 |
Authorization
 |
Resource ownership
 |
Business rules
```

Conocer un UUID o número de cuenta nunca será suficiente para obtener
acceso a un recurso.

------------------------------------------------------------------------

# 43. Frontend

Se utilizará:

``` text
React
+
TypeScript
+
Vite
```

Será una SPA que consumirá exclusivamente la API REST de BankCore.

El frontend será responsable de UI, navegación, formularios,
validaciones de UX, dashboard y presentación de errores.

Las reglas financieras críticas permanecerán en el backend.

------------------------------------------------------------------------

# 44. Docker

Docker se utilizará desde el inicio.

Estructura conceptual:

``` text
docker compose up
       |
 PostgreSQL local
       |
 Spring Boot
       |
 React/Vite
```

El backend podrá ejecutarse directamente durante desarrollo para
conservar hot reload y debugging cómodo.

------------------------------------------------------------------------

# 45. Variables de entorno

Ejemplo:

``` env
DATABASE_URL=
DATABASE_USERNAME=
DATABASE_PASSWORD=

JWT_SECRET=
ACCESS_TOKEN_TTL_MINUTES=15
REFRESH_TOKEN_TTL_DAYS=

SUPABASE_URL=
```

No se almacenarán secretos reales en Git.

Se incluirirá `.env.example`.

------------------------------------------------------------------------

# 46. Testing

Se utilizarán:

``` text
JUnit
Mockito
Spring Boot Test
MockMvc
Testcontainers
```

## Unit tests

Reglas aisladas:

-   Money.
-   Account.
-   Amount validation.
-   Transfer rules.
-   Authorization helpers.

## Integration tests

Interacción real entre:

-   Controllers.
-   Services.
-   Repositories.
-   PostgreSQL.

## Testcontainers

Será especialmente importante para:

-   Constraints.
-   Liquibase.
-   SQL nativo.
-   `FOR UPDATE`.
-   Deadlocks/conflictos.
-   Idempotencia.
-   Concurrencia.

------------------------------------------------------------------------

# 47. Pruebas de concurrencia y retry

Caso obligatorio:

``` text
Balance inicial = 1000

Thread A -> withdraw 800
Thread B -> withdraw 800
```

Resultado:

``` text
Exactly one succeeds
Exactly one fails for insufficient balance
Final balance = 200
```

También se probarán:

-   Lock timeout recuperable.
-   Retry exitoso después de conflicto transitorio.
-   Agotamiento de retries.
-   Rollback completo entre intentos.
-   Transferencias en sentidos opuestos.
-   Orden determinista de locks.
-   Ausencia de movimientos duplicados.

------------------------------------------------------------------------

# 48. Pruebas de idempotencia

Se probarán:

-   Misma clave dos veces.
-   Misma clave concurrentemente.
-   Misma clave con payload diferente.
-   Reintento después de timeout del cliente.
-   Recuperación de respuesta previa.
-   Interacción entre retry interno e idempotencia.

Una clave nunca deberá provocar dos movimientos financieros
equivalentes.

------------------------------------------------------------------------

# 49. Git y repositorio

Se utilizará monorepo:

``` text
BankCore/
├── backend/
├── frontend/
├── docs/
├── docker-compose.yml
└── README.md
```

Ramas de ejemplo:

``` text
main
├── feature/auth
├── feature/accounts
├── feature/transfers
└── feature/frontend-dashboard
```

`main` representará código estable y la integración se realizará
preferentemente mediante Pull Requests.

------------------------------------------------------------------------

# 50. CI/CD

Se incorporará posteriormente con GitHub Actions.

Pipeline objetivo:

``` text
Checkout
  |
Build
  |
Unit Tests
  |
Integration Tests
  |
Package
  |
Deploy
```

------------------------------------------------------------------------

# 51. Deployment

Se priorizará una opción gratuita o free tier adecuada para demostración
académica.

Requisitos:

-   Java 25.
-   Spring Boot.
-   Variables de entorno.
-   HTTPS.
-   Conectividad con Supabase PostgreSQL.
-   Logs.
-   Deploy reproducible.

La plataforma se elegirá cerca de la fase de despliegue para basar la
decisión en los planes gratuitos vigentes en ese momento.

------------------------------------------------------------------------

# 52. Logging y auditoría

Los logs técnicos no deberán contener:

-   Passwords.
-   JWT completos.
-   Refresh Tokens.
-   Secret keys.

Se distinguirá:

``` text
Application Logs
!=
Audit Events
```

La auditoría persistente registrará eventos financieros, administrativos
y de seguridad relevantes.

------------------------------------------------------------------------

# 53. Reglas críticas

El backend protegerá como mínimo:

``` text
Máximo 5 cuentas activas
Monto >= 1 MXN
Monto <= 50,000 MXN
Cuenta ACTIVA para operaciones
Saldo suficiente
Cierre solo con saldo 0
Transferencia atómica
Propiedad/autorización
Idempotencia
Concurrencia
Retry solo ante fallos transitorios
```

------------------------------------------------------------------------

# 54. Principios de diseño

Se aplicarán:

-   Single Responsibility.
-   Separation of Concerns.
-   Dependency Inversion.
-   Fail Safely.
-   Explicit Business Rules.
-   Database as Integrity Layer.

PostgreSQL utilizará:

-   Foreign keys.
-   Unique constraints.
-   Check constraints cuando corresponda.
-   Transactions.
-   Row locks.
-   Índices.

------------------------------------------------------------------------

# 55. Flujo financiero de referencia

``` text
HTTP Request
     |
Authentication
     |
Authorization
     |
Input Validation
     |
Idempotency
     |
Retry Boundary
     |
Transaction
     |
Lock resources
     |
Business validation
     |
Modify balance
     |
Create operation/movements
     |
Commit
     |
Store/recover idempotent result
     |
HTTP Response
```

Los límites de retry, transacción e idempotencia deberán implementarse
deliberadamente para evitar reintentos parciales o duplicación de
efectos.

------------------------------------------------------------------------

# 56. Prioridad de implementación

``` text
1. Project bootstrap
2. PostgreSQL + Docker
3. Liquibase
4. User + Role
5. Sessions
6. Authentication
7. Authorization
8. Accounts
9. Money / balance
10. Movements
11. Deposits
12. Withdrawals
13. Transfers
14. Pessimistic locking
15. Retry policy
16. Idempotency
17. Audit
18. Concurrency tests
19. OpenAPI
20. Frontend integration
21. Deployment
22. CI/CD
```

------------------------------------------------------------------------

# 57. Decisiones cerradas

Quedan formalmente cerradas para Technical Design v1.0:

## TD-001 --- Java

``` text
Java 25 LTS
```

## TD-002 --- Arquitectura

``` text
Monolito modular
+
principios Clean/Hexagonal
```

## TD-003 --- Persistencia

``` text
Spring Data JPA + Hibernate
+
SQL nativo cuando sea necesario
```

## TD-004 --- Migraciones

``` text
Liquibase
```

## TD-005 --- Autenticación

``` text
JWT Access Token de 15 minutos
+
Refresh Token UUID persistido en sessions
```

## TD-006 --- Roles

``` text
Role enum:
CLIENT
EMPLOYEE
ADMIN
```

persistido como `VARCHAR` para el MVP.

## TD-007 --- Concurrencia

``` text
PostgreSQL transactions
+
SELECT ... FOR UPDATE
+
orden determinista de locks
```

## TD-008 --- Retry

Retry automático limitado a errores transitorios de concurrencia, con
máximo 2 reintentos después del intento inicial.

## TD-009 --- Idempotencia

Tabla `idempotency_keys` con unicidad:

``` sql
UNIQUE (user_id, idempotency_key)
```

## TD-010 --- API

``` text
REST + JSON
/api/v1
```

## TD-011 --- Frontend

``` text
React + TypeScript + Vite
```

## TD-012 --- Testing

``` text
JUnit
Mockito
Spring Boot Test
MockMvc
Testcontainers
```

------------------------------------------------------------------------

# 58. Decisiones todavía parametrizables

No bloquean la aprobación del documento:

-   TTL exacto del Refresh Token.
-   Estrategia final de rotación de Refresh Tokens.
-   UUID version concreta para IDs internos.
-   Parámetros exactos de timeout de locks.
-   Ajuste de backoff/retry después de pruebas de carga.
-   Proveedor gratuito de deployment.
-   Detalles visuales del frontend.

Estas decisiones pueden fijarse durante implementación sin alterar la
arquitectura fundamental.

------------------------------------------------------------------------

# 59. Estado del documento

La arquitectura base queda definida como:

``` text
Java 25 LTS
+
Spring Boot
+
Spring Security
+
JWT Access Token (15 min)
+
Refresh Tokens UUID persistidos
+
Argon2
+
JPA / Hibernate
+
SQL nativo cuando sea necesario
+
Liquibase
+
PostgreSQL
+
Docker
+
React + TypeScript + Vite
```

con:

``` text
Monolito modular
+
Clean/Hexagonal principles
+
REST API
+
Transactions
+
Pessimistic locking
+
Controlled retry
+
Idempotency
+
Automated testing
```

