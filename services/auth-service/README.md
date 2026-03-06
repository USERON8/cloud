# Auth Service

负责 OAuth2/JWT 认证、会话管理、Token 刷新和 GitHub OAuth 登录。

- 服务名：`auth-service`
- 端口：`8081`
- 依赖：Redis、Nacos（MySQL 在当前 dev 配置下不是必需）

## 核心接口

- `POST /auth/users/register`：注册
- `POST /auth/sessions`：账号密码登录
- `DELETE /auth/sessions`：登出
- `GET /auth/tokens/validate`：校验 token
- `POST /auth/tokens/refresh`：刷新 token
- `GET /oauth2/authorize`、`POST /oauth2/token`：OAuth2 标准端点
- `GET /auth/oauth2/github/login-url`：获取 GitHub 登录链接

## GitHub 登录配置

需要在环境变量配置：

- `GITHUB_CLIENT_ID`
- `GITHUB_CLIENT_SECRET`
- `GITHUB_REDIRECT_URI`（默认 `http://127.0.0.1:18080/login/oauth2/code/github`）

GitHub OAuth App 的 Callback URL 必须与上面完全一致。

## 本地启动

```bash
mvn -pl auth-service spring-boot:run
```
