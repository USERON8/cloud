# Auth Service
Version: 1.1.0

Authorization server responsible for OAuth2 flows, JWT issuance, session logout, and GitHub OAuth login.

- Service name: `auth-service`
- Port: `8081`
- Primary dependencies: Redis, Nacos, `user-service`

## Responsibilities

- Serves the OAuth2 authorization code and token exchange flow.
- Issues short-lived access tokens and refresh tokens.
- Handles user registration and current-session logout.
- Supports GitHub OAuth as a third-party login entry.
- Provides token-governance data that is exposed through `governance-service` on the public admin surface.

## HTTP Surface

- Public auth:
  - `POST /auth/users/register`
  - `GET /auth/oauth2/github/**`
- OAuth2 core:
  - `GET /oauth2/authorize`
  - `POST /oauth2/token`
- Session self-service:
  - `DELETE /auth/sessions`
  - `DELETE /auth/users/{username}/sessions`
  - `GET /auth/tokens/validate`
- Token governance data:
  - `/auth/authorizations/**`
  - `/auth/blacklist-entries/**`
  - `/auth/cleanups/**`

## Runtime Notes

- `gateway` is the normal public entry for browser and app traffic.
- JWT blacklist checks run in fail-closed mode, so access-token TTL must stay short.
- The current GitHub callback default is `http://127.0.0.1:18080/login/oauth2/code/github`.
- Downstream business traffic usually consumes gateway-restored internal identity instead of calling `auth-service` directly on every request.

## Local Run

```bash
mvn -pl auth-service spring-boot:run
```
