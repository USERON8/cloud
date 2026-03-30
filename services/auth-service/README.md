# Auth Service
Version: 1.1.0

Handles OAuth 2.1 authorization code + PKCE flows, JWT issuance, session management, and GitHub OAuth login.

- Service name: `auth-service`
- Port: `8081`
- Primary dependencies: Redis, Nacos, `user-service`

## Responsibilities

- Acts as the project authorization server.
- Issues OAuth2 access tokens and refresh tokens.
- Stores authorization, consent, code, and state data in Redis-backed services.
- Coordinates local principal data with upstream `user-service`.
- Supports browser login through standard OAuth2 authorization-code flow with PKCE.
- Supports GitHub OAuth login as a third-party identity entrance.

## Core Endpoints

- `POST /auth/users/register`: Register a user
- `DELETE /auth/sessions`: Logout the current session
- `GET /auth/tokens/validate`: Validate the current access token
- `GET /oauth2/authorize`: Standard authorization endpoint
- `POST /oauth2/token`: Standard token endpoint for authorization code exchange and refresh token renewal
- `GET /auth/oauth2/github/login-url`: Initialize GitHub login and persist the local authorization request

The legacy `POST /auth/sessions` login endpoint and `POST /auth/tokens/refresh` custom refresh endpoint were removed and are no longer supported.

## Runtime Dependencies

- Redis
  - Authorization, consent, code, and short-lived auth data are stored here.
- Nacos
  - Service discovery and shared configuration.
- `user-service`
  - Principal bootstrap and user lookup.
- `gateway`
  - Public traffic normally enters through gateway first.

## Web Login Flow

1. The frontend generates `state`, `code_verifier`, and `code_challenge`.
2. The browser redirects to `GET /oauth2/authorize`.
3. After authorization completes, the browser returns to the frontend `redirect_uri`.
4. The frontend calls `POST /oauth2/token` with `authorization_code` + `code_verifier` to exchange tokens.

GitHub login completes third-party authentication first and then returns to the same local authorization code flow instead of issuing tokens directly.

## Current Design Notes

- The current implementation is Redis-heavy and does not require a local MySQL dependency in the dev profile.
- This service is already close to the expected short-TTL auth/session cache model.
- Dedicated Redis authorization services are used instead of relying only on generic Spring Cache annotations.
- JWT blacklist validation now defaults to fail-closed, so access-token TTL must stay short.

## Known Findings In This Sync

- Blacklist Redis failures now reject tokens by default instead of temporarily allowing them.
- Default user and internal access-token TTL are reduced to `PT15M`, and startup validation prevents longer access-token TTL when fail-closed mode is enabled.
- If the team later changes this policy, they should treat `app.security.jwt.blacklist-fail-closed` and the access-token TTL settings as one combined control, not separate knobs.

## GitHub Login Configuration

Configure the following environment variables:

- `GITHUB_CLIENT_ID`
- `GITHUB_CLIENT_SECRET`
- `GITHUB_REDIRECT_URI` (default: `http://127.0.0.1:18080/login/oauth2/code/github`)

The GitHub OAuth App callback URL must match the value above exactly.

## Local Run

```bash
mvn -pl auth-service spring-boot:run
```
