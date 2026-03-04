# Jenkins CI/CD（容器版）

本项目已提供 Jenkins Pipeline，基于本地镜像 `jenkins/jenkins:jdk17`。

## 1. 启动 Jenkins 容器

```bash
docker compose -f docker/docker-compose.yml up -d jenkins
```

访问地址：`http://127.0.0.1:18088`

## 2. 新建流水线任务

1. 新建 `Pipeline` 类型任务。
2. 在 Pipeline 配置中选择 `Pipeline script from SCM`。
3. 仓库指向当前项目，脚本路径填写：`Jenkinsfile`。

## 3. 流水线阶段说明

- `Checkout`：拉取代码。
- `Build And Test`：执行 `clean test`（`-DskipITs`）。
- `Package`：执行 `package`（`-DskipTests`）。
- `Archive`：归档全部模块 `target/*.jar`。
- `Deploy Local`：默认不执行，勾选构建参数 `RUN_DEPLOY=true` 时执行本地部署脚本。

## 4. 关键实现

- `Jenkinsfile`：流水线定义。
- `scripts/ci/mvnw-local.sh`：自动下载 Maven 到 `.tmp/tools`，不依赖 Jenkins 节点预装 Maven。
- `scripts/ci/deploy-local.sh`：调用 `scripts/dev/start-containers.sh` 与 `scripts/dev/start-services.sh` 做本地部署验证。

## 5. Compose 中 Jenkins 服务

`docker/docker-compose.yml` 已增加 `jenkins` 服务：

- 镜像：`jenkins/jenkins:jdk17`
- 端口：`18088 -> 8080`、`15000 -> 50000`
- 数据卷：`docker/docker-compose/jenkins/home` 持久化 Jenkins 数据
- 工作区挂载：`/workspace/cloud`
- Docker Socket：`/var/run/docker.sock`（用于部署阶段触发 docker）

对应端口变量已写入 `docker/.env`：

- `PORT_JENKINS_HTTP=18088`
- `PORT_JENKINS_AGENT=15000`
