# Auth Service
Version: 1.1.0

Handles OAuth 2.1 authorization code + PKCE flows, JWT issuance, session management, and GitHub OAuth login.

- Service name: `auth-service`
- Port: `8081`
- Dependencies: Redis and Nacos (MySQL is not required in the current dev profile)

## Core Endpoints

- `POST /auth/users/register`: Register a user
- `DELETE /auth/sessions`: Logout the current session
- `GET /auth/tokens/validate`: Validate the current access token
- `GET /oauth2/authorize`: Standard authorization endpoint
- `POST /oauth2/token`: Standard token endpoint for authorization code exchange and refresh token renewal
- `GET /auth/oauth2/github/login-url`: Initialize GitHub login and persist the local authorization request

The legacy `POST /auth/sessions` login endpoint and `POST /auth/tokens/refresh` custom refresh endpoint were removed and are no longer supported.

## Web Login Flow

1. The frontend generates `state`, `code_verifier`, and `code_challenge`.
2. The browser redirects to `GET /oauth2/authorize`.
3. After authorization completes, the browser returns to the frontend `redirect_uri`.
4. The frontend calls `POST /oauth2/token` with `authorization_code` + `code_verifier` to exchange tokens.

GitHub login completes third-party authentication first and then returns to the same local authorization code flow instead of issuing tokens directly.

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
