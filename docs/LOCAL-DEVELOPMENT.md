# BankCore — Desarrollo local

Guía para levantar el entorno en una máquina nueva (SRS #28, RNF-010).
Referencia de decisiones: `TECHNICAL-DESIGN.md` (#11 ambientes, #44 Docker, #45 variables, #51 deploy).

## 1. Clonar

```bash
git clone git@github.com:Eidikey/BankCore.git
cd BankCore
```

Por HTTPS:

```bash
git clone https://github.com/Eidikey/BankCore.git
cd BankCore
```

Ramas: `main` (estable) y `develop` (integración). Trabaja en `feature/...` o `fix/...` y abre Pull Request contra `develop`.

## 2. `.env`

El backend y Docker Compose se configuran **solo** con variables de entorno (SRS #29).
Nunca hay secretos en el repo ni en `application.yaml`.

```bash
cp .env.example .env
```

| Variable | Uso | Valor local |
|---|---|---|
| `DATABASE_URL` | JDBC que usa el backend | `jdbc:postgresql://localhost:5432/bankcore` |
| `DATABASE_USERNAME` | Usuario DB backend y `POSTGRES_USER` | `bankcore` |
| `DATABASE_PASSWORD` | Password DB backend y `POSTGRES_PASSWORD` | `bankcore` |
| `POSTGRES_DB` | Base creada por el contenedor | `bankcore` |
| `POSTGRES_PORT` | Puerto publicado en tu máquina | `5432` |
| `JWT_SECRET` | Firma de JWT. **Cámbialo** fuera de local | `change-me` |
| `ACCESS_TOKEN_TTL_MINUTES` | Vida del access JWT | `15` |
| `REFRESH_TOKEN_TTL_DAYS` | Vida del refresh token | `30` |

Nunca subas `.env` (está en `.gitignore`).

## 3. Docker (PostgreSQL local)

No instales PostgreSQL manual. Un solo servicio, `postgres:16`, con volumen
persistente `bankcore-pgdata` y healthcheck (`pg_isready`):

```bash
docker compose up -d
docker compose ps        # postgres "healthy"
```

Comandos útiles:

```bash
docker compose logs -f postgres   # ver arranque
docker compose down               # detener (conserva datos)
docker compose down -v            # detener y BORRAR datos
```

Validar el compose sin levantar nada:

```bash
docker compose --env-file .env.example config
```

> Producción/demo usa Supabase PostgreSQL: solo cambia `DATABASE_URL/USERNAME/PASSWORD`.
> La lógica de negocio siempre queda en el backend (TECHNICAL-DESIGN #11).

## 4. Backend

Requisitos: Java 25+, Maven wrapper incluido (no instales Maven).

El backend **no** lee `.env` solo; exporta las variables primero:

```bash
cd backend
set -a; source ../.env; set +a
./mvnw spring-boot:run
```

Al arrancar, Liquibase aplica `db/changelog/db.changelog-master.yaml` y crea las
7 tablas (`users`, `sessions`, `accounts`, `transfers`, `movements`,
`idempotency_keys`, `audit_events`). `ddl-auto: validate` garantiza que el
esquema lo manda Liquibase, nunca Hibernate.

URLs locales (puerto 8080 por defecto):

| Qué | URL |
|---|---|
| API base | `http://localhost:8080/api/v1` |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |

Tests:

```bash
./mvnw test      # unitarios (H2, Liquibase desactivado)
./mvnw verify    # + integración con PostgreSQL real (requiere Docker)
```

## 5. Frontend

> **Pendiente:** el scaffold `frontend/` aún no existe en el repo.
> Stack definido (TECHNICAL-DESIGN #43): React + TypeScript + Vite, SPA que solo
> consume la API REST. Cuando se cree, estos serán los comandos:

```bash
cd frontend
npm install
npm run dev     # Vite, contra http://localhost:8080/api/v1
```

Regla: el frontend solo hace validaciones de UX; las reglas financieras y de
autorización viven en el backend (SRS #26, RF-037).

## 6. Verificación de un entorno nuevo

```bash
git clone git@github.com:Eidikey/BankCore.git && cd BankCore
cp .env.example .env
docker compose up -d
cd backend && set -a; source ../.env; set +a && ./mvnw verify
```

Éxito = `BUILD SUCCESS` con `Tests run: 15/15` unitarios y `2/2` de integración,
y `http://localhost:8080/swagger-ui.html` responde.

## 7. Problemas comunes

| Síntoma | Causa probable | Solución |
|---|---|---|
| `Connection refused` a postgres | Contenedor no healthy | `docker compose ps`; `docker compose logs postgres` |
| Backend: `DATABASE_URL` vacío | Variables no exportadas | `set -a; source ../.env; set +a` antes de `./mvnw` |
| `liquibase ... does not exist` en tests | Normal en unitarios | Los unitarios llevan Liquibase desactivado; los IT lo reactivan |
| Puerto 5432 ocupado | Otro postgres local | Cambia `POSTGRES_PORT` en `.env` y ajusta `DATABASE_URL` |
| `Could not find a valid Docker environment` en `verify` | Daemon apagado | `sudo systemctl start docker` (daemon requerido por Testcontainers) |
