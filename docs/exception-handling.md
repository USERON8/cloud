# 异常处理设计说明

本文档基于当前项目的异常体系与实践进行说明，覆盖服务内分层与服务间边界的处理策略。

## 目标

- 统一异常类型与错误码，避免魔法字符串与分散处理。
- Controller 无 `try-catch`，交由全局 `@RestControllerAdvice` 兜底。
- RPC/MQ/TCC/Outbox 等边界场景显式转换异常语义。
- 响应头统一包含 `X-Trace-Id`，仅错误响应体包含 `traceId`，避免污染成功响应。
- Service 层通过 AOP 兜底收敛异常，排除 `infrastructure`/`cache` 包。

## 异常类体系

### BaseException

所有自定义异常的根，包含以下关键字段：

- `code`：业务码（来自 `ResultCode` 或自定义数值）
- `httpStatus`：建议的 HTTP 状态码
- `category`：`BIZ | SYSTEM | REMOTE`
- `alert`：是否需要告警
- `traceId`：创建异常时从 MDC 读取

### BizException

- 用于业务异常：入参校验、状态不合法、库存不足等。
- HTTP 状态码默认 4xx（由 `ResultCode` 映射）
- `alert=false`

### SystemException

- 用于系统异常：DB 失败、事务异常、基础设施不可用等。
- HTTP 状态码默认 500
- `alert=true`

### RemoteException

- 用于 RPC/MQ 边界的远程调用失败。
- HTTP 状态码默认 503
- `alert=true`

### BusinessException（兼容）

旧的 `BusinessException` 继续保留，但已继承 `BizException`，用于兼容历史调用。

## 错误码规范

- 统一使用 `ResultCode` 作为错误码枚举。
- 典型新增：
  - `RATE_LIMITED`：429
  - `REMOTE_SERVICE_UNAVAILABLE`：18001
  - `REMOTE_SERVICE_TIMEOUT`：18002
- 统一映射策略集中在 `ResultCodeHttpStatusMapper`，避免分散在业务代码。

## 全局异常处理

### GlobalExceptionHandler（common-web）

职责：

- 统一处理 `BizException` / `SystemException` / `RemoteException`
- 系统异常触发指标计数 `exception.system`
- `traceId` 的响应头与错误响应体写入交由统一响应增强处理
- 异常日志与指标统一交由 `ExceptionReporter` 上报

### GlobalPermissionExceptionHandler（common-security）

处理权限与认证相关异常，并统一输出：

- 401：未认证
- 403：权限不足
- 400：参数类权限错误

## 分层处理策略

### Controller

- 不写 `try-catch`。
- 参数校验、权限校验失败直接抛 `BizException` 或交给校验注解处理。

### Service

- 仅在有业务语义时捕获：
  - RPC 调用异常 → `RemoteException`
  - 业务异常 → `BizException`
  - 其他系统异常 → `SystemException`
- AOP 兜底切面拦截 `@Service`（排除 `..infrastructure..`/`..cache..`），统一处理未知异常与 `DataAccessException`

### DAO / Infrastructure

- 默认不捕获异常，向上抛出。
- 降级或重试时允许捕获，但必须记录日志或指标。

### RPC 边界

- 捕获 `RpcException`，统一转换为 `RemoteException` 或对应 `BizException`。
- Dubbo 通过 Provider/Consumer Filter 统一收敛，放在 `common-web` 的 SPI 注册，避免各服务重复配置。

## 典型场景处理

### Sentinel Gateway 限流

- 统一返回 429
- 响应头写入 `X-Trace-Id`

### MQ 消费

约定：

- `BizException`：记录日志，ACK（不重试）
- `SystemException/RemoteException`：抛出触发重试（NACK）

### Seata TCC

- `try` 阶段异常必须向上抛出，触发 Seata rollback。
- `rollback` 阶段异常必须吞掉并返回 `true`，避免无限重试。

### Outbox Relay

- 发送失败不抛出，记录错误日志并递增指标 `outbox.relay.failure`。

## traceId 规范

- `BaseException` 在创建时读取 MDC 中的 `traceId`。
- 所有响应头包含 `X-Trace-Id`。
- 仅错误响应体包含 `traceId` 字段，成功响应体不包含。

## 开发指引

- 新增异常优先继承 `BizException`/`SystemException`。
- RPC 调用必须在边界层捕获 `RpcException` 并转换。
- Controller 内不要写 `try-catch`，确保统一由全局异常处理。
- Service 层不写兜底 `try-catch`，由 `ServiceExceptionAspect` 统一拦截。
