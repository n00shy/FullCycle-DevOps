# FullCycle Notes API
Spring Boot REST API for the FullCycle-DevOps project. Uses the existing PostgreSQL RDS; this application does not create infrastructure or another database.

## Requirements
- Java 21
- Maven 3.9+
- PostgreSQL reachable from the runtime

## Environment
Set these in your shell or secret manager. Never commit credentials.
- `DB_URL`: JDBC URL, e.g. `jdbc:postgresql://HOST:5432/DATABASE`
- `DB_USERNAME`
- `DB_PASSWORD`
- Optional initial admin: set all of `APP_BOOTSTRAP_ADMIN_USERNAME`, `APP_BOOTSTRAP_ADMIN_EMAIL`, and `APP_BOOTSTRAP_ADMIN_PASSWORD` only when the users table is empty. Leave all unset otherwise.
- `APP_CORS_ALLOWED_ORIGIN`: defaults to `http://localhost:5173`
- `SESSION_COOKIE_SECURE`: set `true` behind HTTPS; defaults to `false` for local HTTP.

## Run locally
```bash
cd backend
export DB_URL='jdbc:postgresql://HOST:5432/DATABASE'
export DB_USERNAME='YOUR_DB_USER'
read -rsp 'Database password: ' DB_PASSWORD; echo
export DB_PASSWORD
mvn spring-boot:run
```
Flyway applies versioned migrations. Hibernate uses `ddl-auto: validate` and will not alter the schema.

## Endpoints
- `GET /api/auth/csrf`
- `POST /api/auth/login` — JSON `{"username":"...","password":"..."}`; username accepts username or email.
- `POST /api/auth/logout`, `GET /api/auth/me`
- `GET|POST /api/notes`, `GET|PATCH|DELETE /api/notes/{id}`
- `GET|POST /api/admin/users`, `PATCH /api/admin/users/{id}`, `GET /api/admin/audit-events`

Notes are scoped to their owner, including for administrators. Admin role grants user-management and audit access, not automatic access to private note contents.

## Security
- Passwords use BCrypt hashes.
- Session cookie is HttpOnly; CSRF protection is enabled.
- Fetch `/api/auth/csrf` before login and state-changing requests. Send the returned token in the response header named by `headerName` (normally `X-XSRF-TOKEN`) and include browser credentials.
- Do not put database credentials in source control or container images.
- Initial admin creation is opt-in and only occurs when the users table is empty. Remove bootstrap variables after first initialization.
- Audit events never store note content or passwords. Deleting a note preserves its audit event, not its content.

## Scope of this first pass
Backend implementation only. Build, security, and PostgreSQL integration tests must pass before deployment. Dockerfiles are intentionally not included.
