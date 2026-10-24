# BankCore — Guía de contribución

## 1. Modelo de ramas

- `main`: estable, solo vía Pull Request desde `develop`.
- `develop`: integración. Todo el trabajo entra aquí por PR.
- `feature/<nombre>`: trabajo individual. Se crean desde `develop` y se fusionan de vuelta por PR.
- Nunca commits directos a `main` ni a `develop`.

## 2. Commits

Formato obligatorio (Conventional Commits, en minúsculas, descriptivo):

```text
feat: add user registration
fix: prevent concurrent withdrawals
docs: update local development guide
chore: bump postgres image
```

Prohibido subir commits tipo `troleado papu`, `ola`, `oxd` o `esta bien bro jeje`
(llegaron a `develop` en Fase 0 y se documentan aquí como ejemplo de lo que no hacer).
Reescribe el historial local (`git rebase -i`, `git commit --amend`) antes de abrir el PR.

## 3. Pull Requests

1. Actualiza tu rama: `git fetch && git merge origin/develop` (resuelve conflictos en local).
2. Verifica en verde: `./mvnw verify` desde `backend/` (requiere Docker).
3. Abre el PR contra `develop` con descripción, criterios verificados y evidencia (salida de tests).
4. Otro integrante revisa y aprueba antes del merge.
5. Tras el merge, elimina la rama `feature/*` local y remota.

## 4. Entorno local (resumen)

Guía completa en [`docs/LOCAL-DEVELOPMENT.md`](docs/LOCAL-DEVELOPMENT.md).

```bash
cp .env.example .env
docker compose up -d                 # postgres:16 en 5432, volumen bankcore-pgdata
cd backend && set -a; source ../.env; set +a && ./mvnw verify
```

- PostgreSQL oficial del proyecto: **16** (dev, Testcontainers y criterio Fase 0 alineados en 16).
- `ErrorResponse` (`{timestamp,status,code,message,path}`) es el formato de error acordado; no migrar a `ProblemDetail` sin decisión del equipo.
- `frontend/` está pendiente (React + TypeScript + Vite cuando se cree el scaffold).

## 5. Sesión 2026-10-06 (Eidikey)

Trabajo realizado en esta sesión, integrado en `develop`:

1. **Manejo global de errores HTTP** — `GlobalExceptionHandler` (`@RestControllerAdvice`), jerarquía `BusinessException` (400/401/403/404/409/422/500), formato `ErrorResponse`; 11 tests MockMvc.
2. **Liquibase** — changelog maestro + 8 archivos (28 changesets, 7 tablas con PK/FK/UNIQUE/CHECK/índices); `ddl-auto: validate`; verificado contra PostgreSQL real.
3. **Testcontainers** — base `AbstractIntegrationTest` (`postgres:16-alpine` compartido) + `DatabaseConnectionIT`; plugin failsafe (`*IT` corre en `verify`).
4. **OpenAPI** — `springdoc-openapi-starter-webmvc-ui:3.1.1`, `OpenApiConfig` (scheme `bearerAuth`), `@Schema` en `ErrorResponse`, patrón `@ApiResponse` por operación; 3 tests.
5. **Docker local** — daemon habilitado al arranque, usuario en grupo `docker`, `docker-compose.yml` (`postgres:16`, healthcheck, volumen persistente verificado con recreación + `psql`).
6. **Documentación** — `docs/LOCAL-DEVELOPMENT.md`, README actualizado, `.env.example` comentado.
7. **Merges** — resueltos conflictos con `origin/develop` (`.env.example`, `.gitignore`, `docker-compose.yml`; se descartó `postgres:18` y `test.yml` basura), rama puesta al día con `origin/Docs`, eliminadas ramas ya fusionadas (`GlobalErrors`, `LiquidBase`, `OpenAPI`, `Testcontainers`, `postgres-docker`, `Docs`).
8. **Decisiones fijadas** — Postgres 16 en todo; `ErrorResponse` custom (no `ProblemDetail`); frontend pendiente; verificación manual de Swagger UI pendiente.
