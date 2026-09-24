# BankCore

Aplicación bancaria simulada como proyecto educativo full-stack.

Permite practicar usuarios, cuentas, depósitos, retiros, transferencias internas, sesiones, auditoría, concurrencia e idempotencia con prácticas reales de ingeniería de software.

No utiliza dinero real ni se conecta a sistemas bancarios externos.

## Contenido

- [Stack tecnológico](#stack-tecnológico)
- [Arquitectura](#arquitectura)
- [Reglas principales](#reglas-principales)
- [Inicio rápido](#inicio-rápido)
- [Tests](#tests)
- [Documentación](#documentación)
- [Flujo de trabajo](#flujo-de-trabajo)
- [Aviso y licencia](#aviso-y-licencia)

## Stack tecnológico

- **Backend:** Java 25 LTS, Spring Boot, Spring Security, Spring Data JPA, Liquibase, Maven
- **Base de datos:** PostgreSQL (Docker en local, Supabase en demo)
- **Seguridad:** JWT (15 min) + Refresh Tokens UUID, sesiones persistidas, Argon2
- **Frontend:** React, TypeScript, Vite
- **Tests:** JUnit, Mockito, MockMvc, Testcontainers
- **DevOps:** Docker, Docker Compose, GitHub Actions

## Arquitectura

Monolito modular con principios de Clean Architecture.

El frontend solo habla con el backend por HTTPS/JSON. Toda la lógica de negocio vive en el backend.

```text
React + TypeScript
        |
        | HTTPS / JSON
        v
Java 25 + Spring Boot
        |
        | JPA / Hibernate
        v
PostgreSQL
```

Estructura general:

```text
BankCore/
├── backend/
├── frontend/
├── docs/
└── docker-compose.yml
```

## Reglas principales

- **Moneda:** solo MXN. En Java se usa `BigDecimal`, en PostgreSQL `NUMERIC(15,2)`. Nunca `float` o `double`.
- **Montos:** de $1.00 a $50,000.00 MXN por operación.
- **Cuentas:** máximo 5 cuentas no cerradas por cliente. Estados: `ACTIVA`, `BLOQUEADA`, `CERRADA`.
- **Autenticación:** JWT de acceso + Refresh Token + sesión persistida. Se permiten varias sesiones a la vez.
- **Transferencias:** solo entre cuentas BankCore. Son atómicas: si algo falla, se hace rollback completo.
- **Concurrencia:** bloqueo pesimista (`SELECT ... FOR UPDATE`) con orden fijo de bloqueo para evitar deadlocks.
- **Idempotencia:** las operaciones financieras aceptan cabecera `Idempotency-Key` para evitar duplicados por reintentos, doble clic o timeouts.

## Inicio rápido

### Requisitos

- Git, Java 25, Docker + Docker Compose, Node.js
- No necesitas instalar PostgreSQL manualmente.

### Pasos

1. Clonar y configurar entorno:

```bash
git clone https://github.com/ORGANIZATION/BankCore.git
cd BankCore
cp .env.example .env
```

2. Levantar la base de datos:

```bash
docker compose up -d
docker compose ps
```

3. Ejecutar el backend (aplica las migraciones con Liquibase):

```bash
cd backend
./mvnw spring-boot:run
```

4. Ejecutar el frontend (en otra terminal):

```bash
cd frontend
npm install
npm run dev
```

Nunca subas el archivo `.env` al repositorio.

## Tests

Backend:

```bash
cd backend
./mvnw test
```

Frontend:

```bash
cd frontend
npm test
```

Los tests críticos cubren autenticación, cuentas, precisión monetaria, saldo insuficiente, atomicidad, concurrencia, idempotencia y auditoría. Los tests de backend usan PostgreSQL real con Testcontainers cuando es necesario.

## Documentación

Detalles de producto, requisitos y diseño en `docs/`:

- `PRD.md`: qué producto se quiere construir.
- `SRS.md`: requisitos funcionales y no funcionales.
- `TECHNICAL-DESIGN.md`: cómo se construye el sistema.
- `DATABASE-DESIGN.md`: modelo relacional y concurrencia.

## Flujo de trabajo

Ramas principales: `main` (estable) y `develop` (integración).

Flujo básico: Issue -> rama `feature/...` o `fix/...` -> tests -> Pull Request -> revisión -> `develop` -> `main`.

Evita commits directos a `main`. Usa mensajes descriptivos, por ejemplo: `feat: add user registration`, `fix: prevent concurrent withdrawals`.

