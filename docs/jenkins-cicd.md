# Jenkins CI/CD (Local Docker Host)

## Overview

This repository uses a Jenkins Pipeline (`Jenkinsfile`) with these stages:

1. `Checkout`
2. `Environment Check` (`java`, `docker`, `docker compose`)
3. `Backend Unit Test` (Maven)
4. `Backend Package` (Maven)
5. `Frontend Build` (optional)
6. `Archive Artifacts`
7. `Deploy Local` (optional)
8. `Smoke Test` (optional)

On deployment failure, Jenkins triggers `scripts/ci/rollback-local.sh`.

## Jenkins Container Requirements

The Jenkins container is built from:

- `docker/docker-compose/jenkins/Dockerfile`

It includes:

- `docker-ce-cli`
- `docker compose` plugin
- `node` / `npm`

The container uses host Docker via:

- `/var/run/docker.sock` bind mount

## Pipeline Parameters

- `RUN_FRONTEND`: build `my-shop-web`
- `RUN_DEPLOY`: run local deploy
- `RUN_SMOKE`: run smoke checks after deploy (effective only when deploy is enabled)

## Scripts Used

- `scripts/ci/mvnw-local.sh`
- `scripts/ci/deploy-local.sh`
- `scripts/ci/smoke-local.sh`
- `scripts/ci/rollback-local.sh`

## Minimal Jenkins Plugins

- Pipeline
- Git
- JUnit
- Workflow Aggregator

