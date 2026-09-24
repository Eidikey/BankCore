
# BankCore — Database Design

**Motor:** PostgreSQL
**Migraciones:** Liquibase
**Desarrollo:** PostgreSQL en Docker
**Demo/Producción académica:** Supabase PostgreSQL

---

# 1. Objetivo

Este documento define el modelo de datos físico de BankCore.

La base de datos no será únicamente almacenamiento. PostgreSQL funcionará como una segunda capa de integridad mediante:

* Primary Keys.
* Foreign Keys.
* `UNIQUE`.
* `CHECK`.
* Transacciones.
* Locks.
* Índices.
* Restricciones monetarias.

La lógica de negocio principal seguirá viviendo en Java/Spring Boot, pero la base de datos deberá impedir estados financieros evidentemente inválidos.

---

# 2. Convenciones

Se utilizarán:

```text
Tablas:        plural + snake_case
Columnas:      snake_case
PK internas:   UUID
Fechas:        TIMESTAMPTZ
Dinero:        NUMERIC(15,2)
```

Ejemplos:

```text
users
sessions
accounts
transfers
movements
idempotency_keys
audit_events
```

Los UUID internos serán inicialmente:

```text
UUID v4
```

generados desde Java mediante:

```java
UUID.randomUUID()
```

Esto evita depender de extensiones específicas de PostgreSQL.

---

# 3. Modelo general

```text
users
 │
 ├──────────< sessions
 │
 ├──────────< accounts
 │                │
 │                ├──────────< movements
 │                │
 │                ├──────────< transfers (source)
 │                │
 │                └──────────< transfers (destination)
 │
 ├──────────< idempotency_keys
 │
 └──────────< audit_events


transfers
   │
   └──────────< movements
```

Relaciones principales:

```text
User    1:N Sessions
User    1:N Accounts
User    1:N IdempotencyKeys
User    1:N AuditEvents

Account 1:N Movements

Account 1:N Transfers como origen
Account 1:N Transfers como destino

Transfer 1:2 Movements de forma lógica
```

---

# 4. Tabla users

Representa a clientes, empleados y administradores.

```sql
CREATE TABLE users (
    id UUID PRIMARY KEY,

    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,

    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    failed_login_attempts SMALLINT NOT NULL DEFAULT 0,
    locked_until TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT ck_users_role
        CHECK (
            role IN (
                'CLIENT',
                'EMPLOYEE',
                'ADMIN'
            )
        ),

    CONSTRAINT ck_users_status
        CHECK (
            status IN (
                'ACTIVE',
                'DISABLED'
            )
        ),

    CONSTRAINT ck_users_failed_login_attempts
        CHECK (
            failed_login_attempts BETWEEN 0 AND 3
        )
);
```

El email será único ignorando mayúsculas/minúsculas:

```sql
CREATE UNIQUE INDEX uk_users_email_ci
ON users (LOWER(email));
```

## Reglas

`role` corresponde al enum:

```java
public enum Role {
    CLIENT,
    EMPLOYEE,
    ADMIN
}
```

El bloqueo de autenticación:

```text
failed_login_attempts
locked_until
```

es completamente independiente del estado bancario de las cuentas.

Por ejemplo:

```text
Usuario bloqueado temporalmente para login
≠
Cuenta bancaria BLOQUEADA
```

---

# 5. Tabla sessions

Cada login crea una sesión independiente.

BankCore utilizará:

```text
JWT Access Token
TTL = 15 minutos

+

Refresh Token UUID
```

El Refresh Token que recibe el cliente será un UUID aleatorio.

Por seguridad, propongo que PostgreSQL no guarde directamente el token reutilizable, sino:

```text
SHA-256(refreshToken)
```

que produce 64 caracteres hexadecimales.

```sql
CREATE TABLE sessions (
    id UUID PRIMARY KEY,

    user_id UUID NOT NULL,

    refresh_token_hash CHAR(64) NOT NULL,

    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    last_used_at TIMESTAMPTZ,

    user_agent VARCHAR(512),
    ip_address INET,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_sessions_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE RESTRICT,

    CONSTRAINT uk_sessions_refresh_token
        UNIQUE (refresh_token_hash),

    CONSTRAINT ck_sessions_expiration
        CHECK (
            expires_at > created_at
        ),

    CONSTRAINT ck_sessions_revocation
        CHECK (
            revoked_at IS NULL
            OR revoked_at >= created_at
        )
);
```

Índices:

```sql
CREATE INDEX idx_sessions_user
ON sessions(user_id);

CREATE INDEX idx_sessions_active
ON sessions(user_id, expires_at)
WHERE revoked_at IS NULL;
```

---

# 6. Session ID dentro del JWT

El Access Token tendrá un claim:

```json
{
  "sub": "user-uuid",
  "sid": "session-uuid",
  "role": "CLIENT"
}
```

`sid` corresponde a:

```text
sessions.id
```

Esto permite revocación individual.

Una sesión será válida cuando:

```text
revoked_at IS NULL
AND
expires_at > NOW()
AND
user.status = ACTIVE
```

De esta manera:

```text
PC ─────── Session A
Phone ──── Session B
Laptop ─── Session C
```

Cerrar Session B no elimina A ni C.

---

# 7. Tabla accounts

```sql
CREATE TABLE accounts (
    id UUID PRIMARY KEY,

    user_id UUID NOT NULL,

    account_number VARCHAR(10) NOT NULL,

    account_type VARCHAR(30)
        NOT NULL
        DEFAULT 'CUENTA_CORRIENTE',

    status VARCHAR(20)
        NOT NULL
        DEFAULT 'ACTIVA',

    balance NUMERIC(15,2)
        NOT NULL
        DEFAULT 0.00,

    created_at TIMESTAMPTZ
        NOT NULL
        DEFAULT NOW(),

    updated_at TIMESTAMPTZ
        NOT NULL
        DEFAULT NOW(),

    closed_at TIMESTAMPTZ,

    CONSTRAINT fk_accounts_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE RESTRICT,

    CONSTRAINT uk_accounts_number
        UNIQUE (account_number),

    CONSTRAINT ck_accounts_number
        CHECK (
            account_number ~ '^[0-9]{10}$'
        ),

    CONSTRAINT ck_accounts_type
        CHECK (
            account_type IN (
                'CUENTA_CORRIENTE'
            )
        ),

    CONSTRAINT ck_accounts_status
        CHECK (
            status IN (
                'ACTIVA',
                'BLOQUEADA',
                'CERRADA'
            )
        ),

    CONSTRAINT ck_accounts_balance
        CHECK (
            balance >= 0.00
        ),

    CONSTRAINT ck_accounts_closed_state
        CHECK (
            (
                status = 'CERRADA'
                AND closed_at IS NOT NULL
                AND balance = 0.00
            )
            OR
            (
                status IN ('ACTIVA', 'BLOQUEADA')
                AND closed_at IS NULL
            )
        )
);
```

Índices:

```sql
CREATE INDEX idx_accounts_user
ON accounts(user_id);
```

Y especialmente:

```sql
CREATE INDEX idx_accounts_user_open
ON accounts(user_id)
WHERE status <> 'CERRADA';
```

Este índice será útil para comprobar el límite de cuentas.

---

# 8. Regla de las 5 cuentas

Hay una aclaración importante.

Una cuenta:

```text
ACTIVA       → cuenta
BLOQUEADA    → cuenta
CERRADA      → libera el espacio
```

Por tanto:

```sql
status <> 'CERRADA'
```

es lo que cuenta para el límite de cinco.

Ejemplo:

```text
3 ACTIVA
2 BLOQUEADA
0 CERRADA

= 5 cuentas abiertas
```

No podría crear otra.

Pero:

```text
3 ACTIVA
1 BLOQUEADA
1 CERRADA

= 4 cuentas abiertas
```

sí podría crear una nueva.

---

# 9. Concurrencia al crear cuentas

No basta con hacer:

```sql
SELECT COUNT(*)
```

porque podrían ocurrir dos requests simultáneamente.

Ejemplo:

```text
Actualmente: 4 cuentas

Request A → observa 4
Request B → observa 4

A crea una
B crea una

Resultado incorrecto: 6
```

Por eso la creación de cuenta deberá bloquear primero al usuario:

```sql
BEGIN;

SELECT id
FROM users
WHERE id = :user_id
FOR UPDATE;

SELECT COUNT(*)
FROM accounts
WHERE user_id = :user_id
AND status <> 'CERRADA';
```

Entonces:

```text
count >= 5
→ rechazar

count < 5
→ insertar
```

Finalmente:

```sql
COMMIT;
```

Esto serializa la creación de cuentas por cliente.

---

# 10. Número público de cuenta

Formato:

```text
10XXXXXXXX
```

Ejemplo:

```text
1028473916
```

Los primeros dos dígitos:

```text
10
```

identifican conceptualmente BankCore.

Los últimos ocho serán generados aleatoriamente.

La verdadera garantía de unicidad será:

```sql
UNIQUE(account_number)
```

Si ocurre una colisión:

```text
generar
 ↓
INSERT
 ↓
UNIQUE violation
 ↓
generar otro
```

---

# 11. Tabla transfers

Una transferencia representa una operación entre dos cuentas BankCore.

```sql
CREATE TABLE transfers (
    id UUID PRIMARY KEY,

    operation_id UUID NOT NULL,

    source_account_id UUID NOT NULL,
    destination_account_id UUID NOT NULL,

    amount NUMERIC(15,2) NOT NULL,

    status VARCHAR(20)
        NOT NULL
        DEFAULT 'COMPLETED',

    created_at TIMESTAMPTZ
        NOT NULL
        DEFAULT NOW(),

    completed_at TIMESTAMPTZ
        NOT NULL
        DEFAULT NOW(),

    CONSTRAINT fk_transfers_source
        FOREIGN KEY (source_account_id)
        REFERENCES accounts(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_transfers_destination
        FOREIGN KEY (destination_account_id)
        REFERENCES accounts(id)
        ON DELETE RESTRICT,

    CONSTRAINT uk_transfers_operation
        UNIQUE (operation_id),

    CONSTRAINT ck_transfers_different_accounts
        CHECK (
            source_account_id
            <>
            destination_account_id
        ),

    CONSTRAINT ck_transfers_amount
        CHECK (
            amount >= 1.00
            AND amount <= 50000.00
        ),

    CONSTRAINT ck_transfers_status
        CHECK (
            status IN ('COMPLETED')
        )
);
```

Índices:

```sql
CREATE INDEX idx_transfers_source_created
ON transfers(
    source_account_id,
    created_at DESC
);

CREATE INDEX idx_transfers_destination_created
ON transfers(
    destination_account_id,
    created_at DESC
);
```

---

# 12. ¿Por qué solo COMPLETED?

BankCore es síncrono.

Una transferencia ocurre dentro de:

```text
BEGIN
...
COMMIT
```

Si algo falla:

```text
ROLLBACK
```

Por tanto una transferencia financiera incompleta no debería quedar almacenada como si existiera.

Los errores quedan registrados mediante:

```text
idempotency_keys
audit_events
logs
```

mientras que `transfers` representa operaciones financieras confirmadas.

El campo `status` se conserva porque permite evolucionar posteriormente hacia:

```text
PENDING
REVERSED
CANCELLED
```

si BankCore crece.

---

# 13. Tabla movements

Esta es una de las tablas más importantes.

```sql
CREATE TABLE movements (
    id UUID PRIMARY KEY,

    operation_id UUID NOT NULL,

    account_id UUID NOT NULL,

    transfer_id UUID,

    type VARCHAR(30) NOT NULL,

    amount NUMERIC(15,2) NOT NULL,

    balance_after NUMERIC(15,2) NOT NULL,

    created_at TIMESTAMPTZ
        NOT NULL
        DEFAULT NOW(),

    CONSTRAINT fk_movements_account
        FOREIGN KEY (account_id)
        REFERENCES accounts(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_movements_transfer
        FOREIGN KEY (transfer_id)
        REFERENCES transfers(id)
        ON DELETE RESTRICT,

    CONSTRAINT ck_movements_type
        CHECK (
            type IN (
                'DEPOSIT',
                'WITHDRAWAL',
                'TRANSFER_DEBIT',
                'TRANSFER_CREDIT'
            )
        ),

    CONSTRAINT ck_movements_amount
        CHECK (
            amount >= 1.00
            AND amount <= 50000.00
        ),

    CONSTRAINT ck_movements_balance
        CHECK (
            balance_after >= 0.00
        ),

    CONSTRAINT ck_movements_transfer_relation
        CHECK (
            (
                type IN (
                    'TRANSFER_DEBIT',
                    'TRANSFER_CREDIT'
                )
                AND transfer_id IS NOT NULL
            )
            OR
            (
                type IN (
                    'DEPOSIT',
                    'WITHDRAWAL'
                )
                AND transfer_id IS NULL
            )
        ),

    CONSTRAINT uk_movements_operation_account_type
        UNIQUE (
            operation_id,
            account_id,
            type
        )
);
```

Índices:

```sql
CREATE INDEX idx_movements_account_created
ON movements(
    account_id,
    created_at DESC
);
```

```sql
CREATE INDEX idx_movements_operation
ON movements(operation_id);
```

```sql
CREATE INDEX idx_movements_transfer
ON movements(transfer_id)
WHERE transfer_id IS NOT NULL;
```

---

# 14. Movimiento de transferencia

Ejemplo:

```text
Cuenta A
$1,000

Cuenta B
$500

Transferencia
$300
```

Se crea:

```text
Transfer

operation_id = XYZ
amount = 300
```

Y dos movements:

```text
A
TRANSFER_DEBIT
amount = 300
balance_after = 700
operation_id = XYZ
```

```text
B
TRANSFER_CREDIT
amount = 300
balance_after = 800
operation_id = XYZ
```

Por tanto:

```text
Transfer 1
   │
   ├── Movement DEBIT
   │
   └── Movement CREDIT
```

---

# 15. Saldo vs movimientos

BankCore utilizará:

```text
accounts.balance
```

como saldo actual.

No hará:

```text
SUM(movements)
```

cada vez que necesite consultar el saldo.

Los movimientos sirven como:

```text
historial
auditoría financiera
trazabilidad
```

Ambos deben permanecer sincronizados dentro de la misma transacción.

---

# 16. Tabla idempotency_keys

Se conserva la estructura que ya aprobamos:

```sql
CREATE TABLE idempotency_keys (
    id UUID PRIMARY KEY,

    user_id UUID NOT NULL
        REFERENCES users(id),

    idempotency_key VARCHAR(255) NOT NULL,

    request_hash VARCHAR(64) NOT NULL,

    status VARCHAR(20) NOT NULL,

    response_code INT,

    response_body JSONB,

    created_at TIMESTAMPTZ
        NOT NULL
        DEFAULT NOW(),

    CONSTRAINT uk_user_idempotency
        UNIQUE (
            user_id,
            idempotency_key
        ),

    CONSTRAINT ck_idempotency_status
        CHECK (
            status IN (
                'IN_PROGRESS',
                'COMPLETED',
                'FAILED'
            )
        ),

    CONSTRAINT ck_idempotency_response_code
        CHECK (
            response_code IS NULL
            OR response_code BETWEEN 100 AND 599
        )
);
```

Índice para mantenimiento:

```sql
CREATE INDEX idx_idempotency_created
ON idempotency_keys(created_at);
```

---

# 17. Request Hash

Será:

```text
SHA-256
```

representado mediante:

```text
64 caracteres hexadecimales
```

El hash deberá incluir una representación canónica de la operación.

Por ejemplo:

```text
HTTP Method
Endpoint / tipo de operación
Cuenta origen
Cuenta destino
Monto
```

Conceptualmente:

```text
POST
/api/v1/transfers
source=123
destination=456
amount=500.00
```

↓

```text
SHA-256
```

↓

```text
request_hash
```

---

# 18. Reutilización incorrecta

Primera solicitud:

```text
Idempotency-Key: ABC123
amount = 500
```

Después:

```text
Idempotency-Key: ABC123
amount = 900
```

La key existe pero:

```text
request_hash A
!=
request_hash B
```

Por tanto se rechaza.

Recomiendo:

```http
409 Conflict
```

para este caso.

---

# 19. Idempotencia y transacciones

La operación financiera seguirá aproximadamente:

```text
@Transactional
     │
     ▼
Insert IdempotencyKey(IN_PROGRESS)
     │
     ▼
Lock account(s)
     │
     ▼
Validate
     │
     ▼
Modify balance(s)
     │
     ▼
Create operation
     │
     ▼
Create Movement(s)
     │
     ▼
Create AuditEvent
     │
     ▼
IdempotencyKey → COMPLETED
     │
     ▼
COMMIT
```

Si ocurre un fallo transitorio:

```text
ROLLBACK
```

también elimina los cambios de idempotencia de ese intento.

---

# 20. Idempotencia + @Retryable

La estructura conceptual deberá ser:

```text
@Retryable
    │
    ├── Attempt 1
    │       │
    │       └── NEW @Transactional
    │
    ├── ROLLBACK
    │
    ├── backoff
    │
    └── Attempt 2
            │
            └── NEW @Transactional
```

No:

```text
@Transactional
    │
    └── retry dentro de la misma
        transacción fallida
```

Cada reintento necesita una **transacción completamente nueva**.

---

# 21. Tabla audit_events

```sql
CREATE TABLE audit_events (
    id UUID PRIMARY KEY,

    actor_user_id UUID,

    event_type VARCHAR(80) NOT NULL,

    target_type VARCHAR(50),
    target_id UUID,

    operation_id UUID,

    metadata JSONB
        NOT NULL
        DEFAULT '{}'::jsonb,

    ip_address INET,
    user_agent VARCHAR(512),

    created_at TIMESTAMPTZ
        NOT NULL
        DEFAULT NOW(),

    CONSTRAINT fk_audit_actor
        FOREIGN KEY (actor_user_id)
        REFERENCES users(id)
        ON DELETE RESTRICT
);
```

Índices:

```sql
CREATE INDEX idx_audit_actor_created
ON audit_events(
    actor_user_id,
    created_at DESC
);
```

```sql
CREATE INDEX idx_audit_event_created
ON audit_events(
    event_type,
    created_at DESC
);
```

```sql
CREATE INDEX idx_audit_operation
ON audit_events(operation_id)
WHERE operation_id IS NOT NULL;
```

---

# 22. Eventos iniciales de auditoría

```text
AUTH_LOGIN_SUCCESS
AUTH_LOGIN_FAILURE
AUTH_TEMP_LOCK
AUTH_LOGOUT

ACCOUNT_CREATED
ACCOUNT_BLOCKED
ACCOUNT_UNBLOCKED
ACCOUNT_CLOSED

DEPOSIT_COMPLETED
WITHDRAWAL_COMPLETED
TRANSFER_COMPLETED

ADMIN_ACTION
```

Se pueden agregar nuevos valores sin modificar la estructura de la tabla.

---

# 23. target_type y target_id

La auditoría puede registrar:

```text
target_type = ACCOUNT
target_id   = account UUID
```

o:

```text
target_type = USER
target_id   = user UUID
```

o:

```text
target_type = TRANSFER
target_id   = transfer UUID
```

No recomiendo crear una Foreign Key para `target_id`.

Es una referencia polimórfica y puede apuntar a distintos tipos de recursos.

---

# 24. Pessimistic locking — retiro

Para un retiro:

```sql
SELECT *
FROM accounts
WHERE id = :account_id
FOR UPDATE;
```

Después:

```text
lock
 ↓
status == ACTIVA?
 ↓
balance >= amount?
 ↓
balance -= amount
 ↓
movement
 ↓
audit
 ↓
commit
```

---

# 25. Pessimistic locking — transferencia

Una transferencia necesita dos locks.

No debemos hacer simplemente:

```text
lock source
lock destination
```

porque dos transferencias inversas podrían provocar:

```text
T1: A → B
T2: B → A
```

T1:

```text
lock A
waiting B
```

T2:

```text
lock B
waiting A
```

Deadlock.

Por eso ambas operaciones adquirirán locks en el mismo orden.

Podemos hacer:

```sql
SELECT *
FROM accounts
WHERE id IN (
    :source_account_id,
    :destination_account_id
)
ORDER BY id
FOR UPDATE;
```

PostgreSQL bloqueará conceptualmente:

```text
UUID menor
 ↓
UUID mayor
```

independientemente de quién sea origen.

---

# 26. Transferencia completa

Dentro de una única transacción:

```text
BEGIN
 │
 ├── Idempotency check
 │
 ├── Lock both accounts
 │
 ├── Validate ownership
 │
 ├── Validate ACTIVA
 │
 ├── Validate balance
 │
 ├── Debit source
 │
 ├── Credit destination
 │
 ├── Create Transfer
 │
 ├── Create DEBIT Movement
 │
 ├── Create CREDIT Movement
 │
 ├── Audit
 │
 └── Idempotency COMPLETED
 │
COMMIT
```

Si algo falla:

```text
ROLLBACK EVERYTHING
```

---

# 27. No permitir balances negativos

Además de las validaciones Java:

```sql
CONSTRAINT ck_accounts_balance
CHECK (balance >= 0.00)
```

Esto significa que incluso si existiera un bug en el backend:

```sql
UPDATE accounts
SET balance = -500;
```

PostgreSQL lo rechazaría.

---

# 28. Cierre de cuenta

Para cerrar:

```text
status = CERRADA
balance = 0
closed_at != null
```

El constraint:

```sql
ck_accounts_closed_state
```

protege esta condición.

La transición:

```text
CERRADA → ACTIVA
```

debe estar prohibida por la lógica de dominio.

Como hardening posterior podemos añadir un trigger PostgreSQL para impedir reabrir una cuenta incluso mediante SQL directo.

---

# 29. Datos que nunca se eliminan en cascada

No utilizaremos cascade delete entre datos financieros.

Especialmente:

```text
users
  ↓
accounts

accounts
  ↓
movements

accounts
  ↓
transfers

transfers
  ↓
movements
```

No:

```sql
ON DELETE CASCADE
```

sino:

```sql
ON DELETE RESTRICT
```

El historial financiero se preserva.

---

# 30. Datos que pueden limpiarse

Posteriormente podemos aplicar retención a:

```text
sessions expiradas
idempotency_keys antiguas
```

porque no forman parte del ledger financiero.

Pero:

```text
movements
transfers
audit_events
accounts
```

se conservan.

---

# 31. Orden de migraciones Liquibase

La estructura será:

```text
backend/
└── src/
    └── main/
        └── resources/
            └── db/
                └── changelog/
                    ├── db.changelog-master.yaml
                    ├── 001-create-users.yaml
                    ├── 002-create-sessions.yaml
                    ├── 003-create-accounts.yaml
                    ├── 004-create-transfers.yaml
                    ├── 005-create-movements.yaml
                    ├── 006-create-idempotency-keys.yaml
                    ├── 007-create-audit-events.yaml
                    └── 008-create-indexes.yaml
```

`db.changelog-master.yaml` incluirá los demás archivos en orden.

---

# 32. Estrategia Liquibase

Una vez que un changeset haya sido utilizado en un ambiente compartido:

```text
❌ editar migration antigua

✅ crear nueva migration
```

Ejemplo:

```text
001-create-users
002-create-sessions
...
009-add-username-to-users
```

Esto permite que todos tengan el mismo historial de esquema.

---

# 33. Diseño final de entidades

```text
User
├── id
├── email
├── passwordHash
├── role
├── status
├── failedLoginAttempts
├── lockedUntil
├── createdAt
└── updatedAt


Session
├── id
├── userId
├── refreshTokenHash
├── expiresAt
├── revokedAt
├── lastUsedAt
├── userAgent
├── ipAddress
└── createdAt


Account
├── id
├── userId
├── accountNumber
├── accountType
├── status
├── balance
├── createdAt
├── updatedAt
└── closedAt


Transfer
├── id
├── operationId
├── sourceAccountId
├── destinationAccountId
├── amount
├── status
├── createdAt
└── completedAt


Movement
├── id
├── operationId
├── accountId
├── transferId
├── type
├── amount
├── balanceAfter
└── createdAt


IdempotencyKey
├── id
├── userId
├── idempotencyKey
├── requestHash
├── status
├── responseCode
├── responseBody
└── createdAt


AuditEvent
├── id
├── actorUserId
├── eventType
├── targetType
├── targetId
├── operationId
├── metadata
├── ipAddress
├── userAgent
└── createdAt
```

---

# 34. Índices finales

## users

```text
LOWER(email) UNIQUE
```

## sessions

```text
user_id
(user_id, expires_at) WHERE revoked_at IS NULL
refresh_token_hash UNIQUE
```

## accounts

```text
user_id
account_number UNIQUE
user_id WHERE status <> CERRADA
```

## transfers

```text
operation_id UNIQUE
(source_account_id, created_at)
(destination_account_id, created_at)
```

## movements

```text
(account_id, created_at)
operation_id
transfer_id
```

## idempotency_keys

```text
(user_id, idempotency_key) UNIQUE
created_at
```

## audit_events

```text
(actor_user_id, created_at)
(event_type, created_at)
operation_id
```

---

# 35. Invariantes críticas

La base y el backend deberán garantizar siempre:

```text
balance >= 0

CERRADA => balance = 0

account_number único

máximo 5 cuentas no cerradas

transfer source != destination

amount >= 1.00

amount <= 50,000.00

transfer = debit + credit atómicos

movement siempre corresponde al saldo confirmado

idempotency key única por usuario

sesión revocada no es válida
```

---

# 36. Tests de integración obligatorios

Estos tests deben ejecutarse contra PostgreSQL mediante Testcontainers.

### Test 1 — Saldo negativo

Intentar:

```text
balance = -100
```

PostgreSQL debe rechazarlo.

### Test 2 — Account Number duplicado

Dos cuentas:

```text
1023456789
1023456789
```

La segunda debe fallar.

### Test 3 — Máximo de cuentas concurrente

Usuario tiene:

```text
4 cuentas
```

Llegan simultáneamente:

```text
Request A → Create Account
Request B → Create Account
```

Resultado:

```text
1 success
1 failure

Total = 5
```

Nunca seis.

### Test 4 — Retiro concurrente

```text
balance = 1000

Thread A → -800
Thread B → -800
```

Resultado:

```text
1 success
1 insufficient balance

balance final = 200
```

### Test 5 — Transferencia inversa concurrente

```text
A → B
B → A
```

Debe comprobarse el orden determinista de locks.

### Test 6 — Idempotencia

```text
Request A
Key = XYZ

Request B
Key = XYZ
```

Resultado:

```text
solo un efecto financiero
```

### Test 7 — Idempotencia concurrente

Los dos requests llegan exactamente al mismo tiempo.

Resultado:

```text
1 operación
1 resultado reutilizado/conflicto controlado
0 duplicados
```

---

# 37. Responsabilidad entre Java y PostgreSQL

No todo debe implementarse en ambos lugares.

## Java/Spring

Responsable principalmente de:

```text
Authorization
Ownership
Business workflows
Account status transitions
Money rules
Transaction orchestration
Idempotency workflow
Retry
```

## PostgreSQL

Última defensa para:

```text
FK
UNIQUE
CHECK
Non-negative balance
Valid account numbers
Valid enums
Valid monetary ranges
Atomicity
Locks
```

---

# 38. División inicial entre el equipo

Para construir esta base:

```text
Alejandro
→ users
→ sessions
→ auth-related persistence


Francisco
→ accounts
→ movements
→ monetary constraints


Leon
→ transfers
→ idempotency_keys
→ audit_events
→ Docker/PostgreSQL
```

Después los tres revisan juntos las migraciones antes de integrarlas a `develop`.

---

# 39. Definition of Done de Database Design

El diseño se considera implementado cuando:

```text
docker compose up -d
```

levanta PostgreSQL y:

```text
./mvnw spring-boot:run
```

hace que Liquibase cree automáticamente:

```text
users
sessions
accounts
transfers
movements
idempotency_keys
audit_events
```

en una base vacía.

Después:

```text
./mvnw test
```

debe ejecutar correctamente los integration tests con Testcontainers.

---

# 40. Estado

Arquitectura de persistencia:

```text
Spring Boot
     │
Spring Data JPA
     │
Hibernate
     │
SQL nativo para operaciones críticas
     │
Liquibase
     │
PostgreSQL
```

Protecciones financieras principales:

```text
Transactions
+
Pessimistic Locks
+
Constraints
+
Idempotency
+
Retry
+
Audit
```
