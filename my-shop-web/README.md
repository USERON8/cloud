# My Shop Web

Vue 3 + TypeScript + Axios + Element Plus 前端，支持 Web / Android / iOS（Capacitor）。

## 本地开发

```bash
pnpm install
pnpm dev
```

默认开发地址：`http://127.0.0.1:5173`，Vite 会把 `/api`、`/auth` 代理到 `http://127.0.0.1:80`。

## 关键环境变量

- `VITE_DEV_PROXY_TARGET`：dev 代理目标（默认 `http://127.0.0.1:80`）
- `VITE_API_BASE_URL`：Axios `baseURL`，生产建议保持空字符串（同源）
- `VITE_GITHUB_AUTHORIZE_URL`：GitHub 登录入口（默认 `/oauth2/authorization/github`）

## 构建与发布到 Nginx

```bash
pnpm build
```

将 `dist/` 内容复制到 `docker/docker-compose/nginx/html/`，然后重启 `nginx` 容器。

## 移动端（Capacitor）

```bash
pnpm cap:sync
pnpm cap:android
pnpm cap:ios
```
