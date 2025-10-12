# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Spring Cloud microservices architecture project using Spring Boot 3.5.3, Spring Cloud 2025.0.0, and Spring
Cloud Alibaba 2025.0.0.0-preview. It demonstrates enterprise-grade distributed system patterns with OAuth2.1
authentication, service mesh, distributed transactions, and message-driven architecture.

**Tech Stack:** Spring Boot 3.5.3 | Spring Cloud 2025.0.0 | Nacos | OAuth2.1 + JWT | Seata | RocketMQ | Redis | MySQL
9.3.0 | Elasticsearch | MyBatis Plus | Redisson

## Essential Build Commands

### Building the Project

```bash
# Clean and install all modules (skip tests)
mvn clean install -DskipTests

# Parallel build for faster compilation
mvn clean install -DskipTests -T 4

# Build specific module
mvn clean install -pl user-service -am -DskipTests
```

### Running Tests

```bash
# Run all tests
mvn test

# Run tests for specific module
mvn test -pl user-service

# Run integration tests
mvn verify -P integration-test

# Run single test class
mvn test -Dtest=UserServiceTest

# Run single test method
mvn test -Dtest=UserServiceTest#testCreateUserSuccess
```

### Starting Services

Services must be started in this order due to dependencies:

```bash
# 1. Auth service (port 8081)
cd auth-service && mvn spring-boot:run

# 2. Gateway (port 80)
cd gateway && mvn spring-boot:run

# 3. Business services (can be started in parallel)
cd user-service && mvn spring-boot:run     # port 8081
cd order-service && mvn spring-boot:run    # port 8082
cd product-service && mvn spring-boot:run  # port 8083
cd stock-service && mvn spring-boot:run    # port 8084
cd payment-service && mvn spring-boot:run  # port 8085
cd search-service && mvn spring-boot:run   # port 8087
```

**Environment profiles:** Use `-Dspring.profiles.active=dev|test|prod` to activate environments

### Infrastructure Services

```bash
# Start all infrastructure (MySQL, Redis, Nacos, RocketMQ, ES, etc.)
cd docker && docker-compose up -d

# Start specific services
docker-compose up -d mysql redis nacos

# View logs
docker-compose logs -f [service-name]

# Stop all services
docker-compose down
```

**Important:** Nacos depends on MySQL, RocketMQ Broker depends on NameServer

## Architecture Overview

### Module Dependencies

```
common-module (base utilities, config, exceptions)
    ↓
api-module (Feign client definitions)
    ↓
[gateway, auth-service, business services]
```

**Key principle:** `common-module` is a shared dependency for all services and contains cross-cutting concerns.
`api-module` defines inter-service contracts.

### Common Module Structure

Located in `common-module/src/main/java/com/cloud/common/`:

- **annotation/** - Custom annotations (caching, rate limiting, monitoring)
- **aspect/** - AOP aspects (logging, performance tracking, distributed tracing)
- **cache/** - Multi-level cache (Redis + Caffeine) implementation
- **config/** - Shared Spring configurations (Jackson, MyBatis Plus, Redis, ThreadPool, async)
- **constant/** - Application-wide constants
- **domain/** - DTOs, entities, value objects, Result wrapper
- **enums/** - Business enumerations (status codes, error codes)
- **exception/** - Custom exceptions and global exception handlers
- **lock/** - Distributed lock abstractions (Redisson)
- **messaging/** - RocketMQ message producers/consumers base classes
- **monitoring/** - Performance metrics and health checks
- **result/** - Standardized API response structure
- **security/** - OAuth2 utilities, JWT handling, token validation
- **service/** - Base service interfaces (e.g., MinIO file service)
- **threadpool/** - Async thread pool configurations
- **utils/** - Utility classes (JSON, date, validation, encryption)

### API Module Structure

Located in `api-module/src/main/java/com/cloud/api/`:

Contains Feign client interfaces organized by domain (auth, user, order, product, payment, stock). These define
service-to-service communication contracts.

### Authentication & Authorization Flow

1. **Auth Service (8081)** - OAuth2.1 Authorization Server
    - Issues JWT access tokens via `/oauth2/token`
    - Supports password grant and client credentials
    - Provides JWKS endpoint at `/.well-known/jwks.json`
    - Token validation includes blacklist checking

2. **Gateway (80)** - Entry point with JWT validation
    - Validates tokens against JWKS from auth-service
    - Routes requests to backend services
    - Enforces CORS, CSRF, rate limiting, IP filtering
    - Aggregates API docs via Knife4j

3. **Resource Servers** - All business services validate JWT tokens
    - Configure `spring.security.oauth2.resourceserver.jwt.jwk-set-uri`
    - Use `@PreAuthorize` for method-level security

**Getting tokens:**

```bash
curl -X POST "http://localhost:8081/oauth2/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "username=admin" \
  -d "password=admin123" \
  -d "client_id=web-client" \
  -d "client_secret=WebClient@2024#Secure" \
  -d "scope=read write"
```

### Service Communication Patterns

1. **Synchronous:** OpenFeign with circuit breakers (api-module)
2. **Asynchronous:** RocketMQ via Spring Cloud Stream
    - Order events → Payment, Stock, Log services
    - User events → Log service
    - Payment events → Order service

3. **Configuration:** Nacos for service discovery and centralized config
    - Services register at startup
    - Pull configs from `application-common.yml` and service-specific configs

### Distributed Transactions

Seata AT mode for cross-service transactions (configured but implementation varies by use case):

- Order creation → Stock deduction + Payment processing
- Use `@GlobalTransactional` for distributed transaction boundaries

### Caching Strategy

Two-level cache pattern (common-module/cache):

1. **L1 Cache:** Caffeine (local, fast, TTL-based)
2. **L2 Cache:** Redis (distributed, shared across instances)
3. **Cache-aside pattern** with automatic eviction and refresh

### Database Design Conventions

All tables include standard fields:

- `id` - BIGINT PRIMARY KEY AUTO_INCREMENT
- `created_at` - DATETIME DEFAULT CURRENT_TIMESTAMP
- `updated_at` - DATETIME ON UPDATE CURRENT_TIMESTAMP
- `is_deleted` - TINYINT (0=active, 1=deleted) for soft deletes
- `version` - INT for optimistic locking

Use MyBatis Plus for ORM with `BaseMapper<T>` interface.

## Configuration Management

### Nacos Configuration

- **Server:** http://localhost:8848/nacos (username: nacos, password: nacos)
- **Namespace:** public
- **Group:** DEFAULT_GROUP
- **Common config:** `common.yaml` (shared across all services)

### Environment Variables

Key environment variables (set in IDE or docker-compose):

- `NACOS_SERVER_ADDR` - Nacos address (default: localhost:8848)
- `ROCKETMQ_NAME_SERVER` - RocketMQ NameServer (default: 127.0.0.1:39876)
- `SPRING_PROFILES_ACTIVE` - Active profile (dev|test|prod)

### Application Profiles

Each service has:

- `application.yml` - Base config with Nacos connection
- `application-dev.yml` - Development environment
- `application-prod.yml` - Production environment
- `application-rocketmq.yml` - RocketMQ bindings (if applicable)

Gateway additionally has `application-route.yml` for route definitions.

## Code Conventions (from RULE.md)

### Naming Standards

- **Classes:** PascalCase (e.g., `UserServiceImpl`)
- **Methods:** camelCase (e.g., `getUserById`)
- **Constants:** UPPER_SNAKE_CASE (e.g., `MAX_RETRY_COUNT`)
- **Packages:** lowercase with dots (e.g., `com.cloud.user.service`)

### Layer Responsibilities

- **Controller** - HTTP request handling, validation, Result wrapping
    - Use `@RestController`, `@RequestMapping`
    - Annotate with `@Tag`, `@Operation` for API docs
    - Return `Result<T>` wrapper

- **Service** - Business logic, transaction management
    - Use `@Service`, `@Transactional(rollbackFor = Exception.class)`
    - Implement interfaces for better testability

- **Mapper** - Database access with MyBatis Plus
    - Extend `BaseMapper<Entity>`
    - Use `@Mapper` annotation

### Exception Handling

Global exception handlers in each service catch:

- `BusinessException` - Expected business errors (log as WARN)
- `SystemException` - Unexpected system errors (log as ERROR with stack trace)
- `MethodArgumentNotValidException` - Validation errors

Use `Result.fail(code, message)` for error responses.

### Async Processing

Services use custom thread pools (configured in `AsyncConfig`):

```java
@Async("taskExecutor")
public CompletableFuture<T> asyncMethod() { ... }
```

### Logging Standards

- Use `@Slf4j` from Lombok
- Use placeholders: `log.info("User created: {}", userId)`
- Never log sensitive data (passwords, tokens)
- ERROR level requires exception: `log.error("Failed to process", exception)`

## Testing Approach

### Unit Tests

- Use `@SpringBootTest` for integration tests
- Use `@MockBean` for mocking dependencies
- Name pattern: `testMethodName_Scenario` (e.g., `testCreateUser_Success`)
- Use `@DisplayName` for readable test descriptions

### Test Structure (Given-When-Then)

```java
@Test
@DisplayName("创建用户-成功")
void testCreateUser_Success() {
    // Given - setup test data
    UserDTO dto = ...

    // When - execute method under test
    UserVO result = service.createUser(dto);

    // Then - verify results
    assertNotNull(result);
    assertEquals("expected", result.getUsername());
}
```

## API Documentation

- **Gateway aggregated docs:** http://localhost:80/doc.html (Knife4j)
- **Individual service docs:** http://localhost:PORT/doc.html
- **OpenAPI 3.0 specs:** /v3/api-docs endpoints

Services auto-register with gateway for API doc aggregation.

## Docker Network Configuration

All services run in `service_net` bridge network (172.28.0.0/24):

- MySQL: 172.28.0.10:3306
- Redis: 172.28.0.20:6379
- Nacos: 172.28.0.30:8848
- RocketMQ NameServer: 172.28.0.40:39876
- RocketMQ Broker: 172.28.0.50
- Elasticsearch: 172.28.0.90:9200

Data persists to `D:\docker\[service-name]` directories.

## Git Workflow

### Commit Message Format

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Types:** feat, fix, docs, style, refactor, perf, test, chore

**Example:**

```
feat(user): 添加用户注册功能

- 实现用户注册接口
- 添加邮箱验证
- 添加用户名重复检查

Closes #123
```

### Branching Strategy

- `main` - Production-ready code
- `develop` - Integration branch
- `feature/*` - New features
- `bugfix/*` - Bug fixes
- `hotfix/*` - Production hotfixes
- `release/*` - Release preparation

## Monitoring & Operations

- **Actuator endpoints:** `/actuator/health`, `/actuator/metrics`, `/actuator/prometheus`
- **Nacos console:** http://localhost:8848/nacos
- **RocketMQ console:** http://localhost:38082
- **Elasticsearch:** http://localhost:9200
- **Kibana:** http://localhost:5601

## Common Issues & Solutions

### Service startup fails

1. Check if infrastructure is running: `docker-compose ps`
2. Verify Nacos is accessible: `curl http://localhost:8848/nacos/`
3. Check service logs in `[service-name]/logs/`

### Authentication errors

1. Verify auth-service is running on port 8081
2. Check JWKS endpoint: `curl http://localhost:8081/.well-known/jwks.json`
3. Validate token hasn't expired (default 2 hours)

### RocketMQ connection issues

1. Verify NameServer: `docker-compose ps namesrv`
2. Check Broker connectivity: `docker exec -it rmqbroker ping 172.28.0.40`
3. Confirm application.yml has correct `spring.cloud.stream.rocketmq.binder.name-server`

## Working with This Codebase

1. **Before making changes:** Always run tests to ensure baseline functionality
2. **Configuration changes:** Update Nacos configs, not just local files
3. **New dependencies:** Add to parent `pom.xml` `<dependencyManagement>` section
4. **Inter-service calls:** Define Feign clients in `api-module`, not in service modules
5. **Database changes:** Update SQL scripts in `sql/` directory
6. **Security-sensitive changes:** Consult OAuth2 flow diagrams before modifying auth
