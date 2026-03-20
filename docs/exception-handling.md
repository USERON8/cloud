# Exception Handling Design Notes

This document describes the current exception model and handling strategy across service layers and service boundaries.

## Goals

- Unify exception types and error codes to avoid scattered handling and magic strings
- Keep controllers free from `try-catch` and let global `@RestControllerAdvice` handle fallback behavior
- Translate exception semantics explicitly across RPC, MQ, TCC, and Outbox boundaries
- Always include `X-Trace-Id` in response headers, but include `traceId` in the body only for error responses
- Use service-layer AOP as the final fallback while excluding infrastructure-heavy packages such as `infrastructure`, `cache`, `tcc`, `task`, `outbox`, `search.messaging`, `common.messaging`, `payment.service.support`, `gateway.config`, `gateway.controller`, and `gateway.cache`

## Exception Hierarchy

### `BaseException`

Root type for custom exceptions with the following fields:

- `code`: business error code from `ResultCode` or a custom numeric value
- `httpStatus`: recommended HTTP status code
- `category`: `BIZ`, `SYSTEM`, or `REMOTE`
- `alert`: whether operational alerting is required
- `traceId`: captured from MDC when the exception is created

### `BizException`

- Used for business errors such as request validation failures, invalid status transitions, and insufficient stock
- Defaults to `4xx` HTTP status codes through `ResultCode` mapping
- `alert=false`

### `SystemException`

- Used for system failures such as database errors, transaction failures, and unavailable infrastructure
- Defaults to HTTP `500`
- `alert=true`

### `RemoteException`

- Used for remote boundary failures such as RPC or MQ-related remote calls
- Defaults to HTTP `503`
- `alert=true`

### `BusinessException` (Compatibility)

The legacy `BusinessException` type is still retained, but it now extends `BizException` for backward compatibility.

## Error Code Rules

- Use `ResultCode` as the unified error code enum
- Newly introduced representative codes include:
  - `RATE_LIMITED` -> `429`
  - `REMOTE_SERVICE_UNAVAILABLE` -> `8001`
  - `REMOTE_SERVICE_TIMEOUT` -> `8002`
- Centralize HTTP status mapping inside `ResultCodeHttpStatusMapper` instead of scattering it through business code

## Global Exception Handling

### `GlobalExceptionHandler` (`common-web`)

Responsibilities:

- Handle `BizException`, `SystemException`, and `RemoteException`
- Increment the `exception.system` metric when system exceptions occur
- Delegate `traceId` response header writing and error-body enrichment to the shared response enhancement path
- Report exception logs and metrics through `ExceptionReporter`

### `GlobalPermissionExceptionHandler` (`common-security`)

Handles authentication and authorization failures with unified outputs:

- `401`: unauthenticated
- `403`: insufficient permission
- `400`: parameter-shaped authorization errors

## Layered Handling Strategy

### Controller

- Do not write `try-catch`
- Validation failures and authorization failures should throw `BizException` directly or rely on annotation-based validation handling

### Service

- Catch exceptions only when business semantics need to be added:
  - RPC failure -> `RemoteException`
  - Business failure -> `BizException`
  - Other system failure -> `SystemException`
- Use AOP as the final fallback around `@Service` methods to translate unknown exceptions and `DataAccessException`

### DAO / Infrastructure

- Do not catch exceptions by default; let them bubble upward
- Catch only when explicit downgrade or retry behavior is required, and always emit logs or metrics

### RPC Boundary

- Catch `RpcException` and translate it into `RemoteException` or an appropriate `BizException`
- Centralize Dubbo provider and consumer filters in `common-web` SPI registration instead of duplicating configuration in each service

## Typical Scenarios

### Sentinel Gateway Rate Limiting

- Return HTTP `429`
- Always write `X-Trace-Id` to the response header

### MQ Consumption

Rules:

- `BizException`: log and ACK without retry
- `SystemException` / `RemoteException`: throw to trigger retry (NACK)

### Seata TCC

- Exceptions in the `try` phase must bubble upward to trigger Seata rollback
- Exceptions in the `rollback` phase must be swallowed and return `true` to avoid infinite retries

### Outbox Relay

- Send failures must not be thrown upward
- Log the error and increment the `outbox.relay.failure` metric

## Trace ID Rules

- `BaseException` reads `traceId` from MDC when instantiated
- All response headers include `X-Trace-Id`
- Only error response bodies contain the `traceId` field

## Development Guidelines

- New exception types should extend `BizException` or `SystemException`
- RPC boundaries must catch and translate `RpcException`
- Controllers must remain free from `try-catch`
- Service methods should not add fallback `try-catch` unless they are converting semantics explicitly; `ServiceExceptionAspect` remains the shared fallback layer
