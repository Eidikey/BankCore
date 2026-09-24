# BankCore — Agent Instructions

## Estructura del repositorio (monorepo)

```
BankCore/
├── backend/          # Spring Boot 3.x (Java 25 LTS)
├── frontend/         # React + TypeScript + Vite
├── docs/             # PRD.md, SRS.md, TECHNICAL-DESIGN.md
├── docker-compose.yml
└── README.md
```

## Stack tecnológico clave

- **Backend**: Java 25 LTS, Spring Boot, Spring Security, Spring Data JPA + Hibernate
- **Auth**: JWT Access Token (15 min) + Refresh Token UUID persistido en BD
- **Password hashing**: Argon2
- **DB**: PostgreSQL (desarrollo via Docker, producción via Supabase)
- **Migraciones**: Liquibase (fuente de verdad del esquema)
- **Concurrencia**: Transacciones + `SELECT ... FOR UPDATE` + orden determinista de locks
- **Idempotencia**: Header `Idempotency-Key` + tabla `idempotency_keys` con `UNIQUE(user_id, idempotency_key)`
- **Testing**: JUnit, Mockito, Spring Boot Test, MockMvc, Testcontainers
- **API**: REST + JSON, versionado `/api/v1`, OpenAPI

## Comandos de desarrollo

```bash
# Levantar BD local + backend + frontend
docker compose up

# Solo BD para desarrollo backend
docker compose up postgres

# Backend (desde backend/) - hot reload
./mvnw spring-boot:run

# Frontend (desde frontend/) - dev server
npm run dev

# Tests backend
./mvnw test

# Tests integración (requiere Testcontainers/PostgreSQL)
./mvnw verify

# Migraciones Liquibase (aplicar)
./mvnw liquibase:update

# Generar changelog nueva migración
./mvnw liquibase:diffChangelog
```

## Arquitectura interna (monolito modular)

```
backend/
├── auth/
├── user/
├── account/
├── transfer/
├── movement/
├── audit/
├── idempotency/
└── shared/
```

Cada módulo sigue: `domain/` (reglas), `application/` (casos de uso), `infrastructure/` (JPA, SQL nativo), `presentation/` (controllers).

## Reglas críticas que el backend valida

- Máximo 5 cuentas `ACTIVA` por cliente (cerradas no cuentan)
- Monto: `1.00 <= amount <= 50,000.00 MXN`
- Operaciones solo en cuentas `ACTIVA`
- Saldo suficiente (con `FOR UPDATE` para evitar race conditions)
- Cierre de cuenta solo con saldo `0.00`
- Transferencias atómicas (debitar + acreditar + movimientos en una transacción)
- Locks en orden determinista por `account.id` para evitar deadlocks
- Retry automático SOLO para errores transitorios de concurrencia (max 2 reintentos, backoff 100-500ms)
- Idempotencia en operaciones financieras (depósitos, retiros, transferencias)
- Autorización en backend: `CLIENT` | `EMPLOYEE` | `ADMIN` (enum persistido como VARCHAR)

## Flujo financiero estándar

```
HTTP Request → Auth → Authorization → Input Validation → Idempotency
    → Retry Boundary → Transaction → Lock resources → Business validation
    → Modify balance → Create operation/movements → Commit
    → Store/recover idempotent result → HTTP Response
```

## Variables de entorno requeridas

```env
DATABASE_URL=
DATABASE_USERNAME=
DATABASE_PASSWORD=
JWT_SECRET=
ACCESS_TOKEN_TTL_MINUTES=15
REFRESH_TOKEN_TTL_DAYS=
SUPABASE_URL=
```

Nunca commitear secretos reales. Usar `.env.example` como plantilla.

## Convenciones importantes

- **Entidades JPA NO se exponen directamente** → usar DTOs + mappers
- **Dinero**: `BigDecimal` en Java, `NUMERIC(15,2)` en PostgreSQL (nunca float/double)
- **Número de cuenta**: 10 dígitos, `VARCHAR`, `UNIQUE`, prefijo `10` + 8 aleatorios, NO es PK
- **IDs internos**: UUID
- **Auditoría**: eventos persistentes separados de logs de aplicación
- **Errores HTTP**: 400 (validación), 401, 403, 404, 409 (concurrencia agotada), 500 (inesperados)
- **Formato error**: `{ timestamp, status, code, message, path }` — `code` es identificador estable de negocio

## Pruebas obligatorias (mínimo)

- Concurrencia: 2 retiros concurrentes $800 sobre saldo $1000 → exactamente uno exitoso, balance final $200
- Idempotencia: misma clave 2 veces, misma clave concurrente, misma clave payload distinto
- Transferencias: atómicas, rollback en error, locks orden determinista, deadlocks
- Auth: registro, login, 3 fallos → bloqueo 15 min, expiración bloqueo, sesiones concurrentes

## Documentación de referencia

- `docs/PRD.md` — Qué hace el sistema (reglas de negocio completas)
- `docs/SRS.md` — Requisitos funcionales/no funcionales detallados
- `docs/TECHNICAL-DESIGN.md` — Decisiones técnicas cerradas (TD-001 a TD-012) y arquitectura

## Decisiones cerradas (no cambiar sin justificación)

| Área | Decisión |
|------|----------|
| Java | 25 LTS |
| Framework | Spring Boot |
| Auth | JWT 15min + Refresh Token UUID persistido |
| Password | Argon2 |
| Persistencia | Spring Data JPA + Hibernate + SQL nativo |
| Migraciones | Liquibase |
| Concurrencia | Transacciones + pessimistic locking + orden determinista |
| Retry | Solo fallos transitorios concurrencia (max 3 intentos totales) |
| Idempotencia | Tabla `idempotency_keys` con `UNIQUE(user_id, key)` |
| API | REST + JSON `/api/v1` |
| Frontend | React + TypeScript + Vite |
| Testing | JUnit + Mockito + Spring Boot Test + MockMvc + Testcontainers |