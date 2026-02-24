# My Shop Web

Vue 3 + TypeScript + Axios + Element Plus frontend, with Capacitor support for Web / Android / iOS.

## Setup

```bash
pnpm install
```

## Run in Development

```bash
pnpm dev
```

By default Vite proxies `/api` and `/auth` to `http://127.0.0.1:80`.

You can override the target:

```bash
# .env.development
VITE_DEV_PROXY_TARGET=http://127.0.0.1:8080
```

## Build

```bash
pnpm build
pnpm preview
```

## Capacitor (Android / iOS)

```bash
pnpm build
pnpm dlx cap add android
pnpm dlx cap add ios
pnpm cap:sync
pnpm cap:android
pnpm cap:ios
```

## UI Direction

- Clean and lightweight Apple-style UI
- Desktop side nav + mobile bottom tab nav
- Safe-area support using `env(safe-area-inset-bottom)`
