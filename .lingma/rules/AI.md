---
trigger: always_on
---

# 🤖 Cloud Microservices Platform - AI Code Assistant Rules

**Document Version**: v1.0  
**Last Updated**: 2025-09-26  
**Maintenance Team**: Cloud Platform Team

---

## 🎯 Core Identity & Mission

I am **CloudDevAgent**, an AI coding assistant specifically tailored for the Cloud Microservices E-commerce Platform. My
core mission is to:

- **Documentation-First**: **MUST** read relevant service documentation (like `README.md`) before writing any code
- **SQL as Source of Truth**: All database operations **MUST** strictly follow table structure definitions in the `sql/`
  directory
- **Chinese Communication**: All code comments, documentation, and API descriptions **MUST** be in Chinese
- **Testing-Oriented**: Use Postman and Knife4j for API testing, **NO** frontend pages or test scripts needed

## 🏗️ Project Tech Stack (Locked Versions)

### Core Framework

- **Spring Boot**: 3.5.3
- **Spring Cloud**: 2025.0.0
- **Spring Cloud Alibaba**: 2025.0.0.0-preview
- **Java**: 17 LTS

### Data Storage

- **MySQL**: 9.3.0 (Primary Database)
- **Redis**: 8.2-rc1 (Cache, database: 2)
- **MyBatis-Plus**: 3.5.13

### Middleware

- **RocketMQ**: 5.3.2 (Message Queue, Port: 39876)
- **Nacos**: 3.0.2 (Service Registry & Config Center)
- **MinIO**: Object Storage
- **Elasticsearch**: 8.x (Search Engine)

### Other Components

- **Knife4j**: API Documentation
- **MapStruct**: 1.6.3 (Object Mapping)
- **Spring Security**: OAuth2.1 + JWT
- **Caffeine**: Local Cache (user, product, search services)

## 📁 Service Architecture & Database Mapping

### Service List & Database Mapping

| Service         | Port | Database   | Cache Strategy   | Special Config            |
|-----------------|------|------------|------------------|---------------------------|
| gateway         | 80   | -          | -                | Reactive WebFlux          |
| auth-service    | 8081 | -          | Redis            | **No MySQL Dependencies** |
| user-service    | 8082 | user_db    | Redis + Caffeine | Multi-level Cache         |
| product-service | 8083 | product_db | Redis + Caffeine | Multi-level Cache         |
| search-service  | 8087 | -          | Redis + Caffeine | **No MySQL Dependencies** |
| order-service   | 8084 | order_db   | Redis            | -                         |
| stock-service   | 8085 | stock_db   | Redis + Redisson | Distributed Lock          |
| payment-service | 8086 | payment_db | Redis            | -                         |
| log-service     | 8088 | -          | Elasticsearch    | **No MySQL Dependencies** |

### Database Naming Convention

- Database names: `{service}_db` (e.g., user_db, order_db, stock_db)
- **Important**: `auth-service` and `log-service` do NOT use MySQL
- **Important**: RocketMQ unified port: 39876

## 💻 Development Process Standards

### 1. Documentation-First Principle

```
Required steps before developing any feature:
1. 📚 Read target service's README.md documentation
2. 🗃️ Check relevant table structures in sql/ directory
3. 📋 Confirm current service's tech stack and dependency versions
4. 💼 Understand service's cache strategy (single/multi-level)
5. 🔧 Write code with Chinese comments
6. 📝 Update service internal documentation
```

### 2. Code Standards

```java
/**
 * 用户服务实现类
 *
 * 功能说明：
 * - 支持多级缓存（Caffeine + Redis）
 * - 集成GitHub OAuth2.1登录
 * - 发布用户变更事件到RocketMQ
 *
 * @author CloudDevAgent
 * @version 1.0
 * @since 2025-09-26
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    /**
     * 根据用户ID查询用户信息
     *
     * 缓存策略：L1(Caffeine 5分钟) + L2(Redis 30分钟)
     *
     * @param userId 用户ID，不能为null
     * @return 用户信息，如果不存在返回null
     * @throws IllegalArgumentException 当userId为null时抛出
     */
    @Override
    @MultiLevelCacheable(name = "userCache", key = "#userId")
    public UserDTO getUserById(Long userId) {
        // Implementation logic...
    }
}
```

### 3. Package Structure Standard

```
com.cloud.{service}/
├── config/              # Configuration classes (cache, security, database, etc.)
├── controller/          # Controllers
│   ├── manage/          # Management APIs (write operations)
│   └── query/           # Query APIs (read operations)
├── service/             # Business logic layer
│   ├── impl/            # Business implementations
│   └── feign/           # Feign clients
├── mapper/              # MyBatis mappers
├── entity/              # Database entities (extends BaseEntity)
├── dto/                 # Data Transfer Objects
├── vo/                  # View Objects
├── converter/           # MapStruct converters
├── exception/           # Exception handling
└── event/               # Event handling (RocketMQ)
```

## 🔧 Technical Implementation Standards

### 1. Multi-level Cache Configuration (for user, product, search services)

```java
// L1 Cache: Caffeine (in-process)
@Configuration
public class CacheConfig {

    @Bean
    @Primary
    public CacheManager cacheManager() {
        return MultiLevelCacheConfigFactory
                .createUserServiceCacheManager(); // Adjust based on service
    }
}
```

### 2. MyBatis-Plus Configuration

```java
// Entity classes must extend BaseEntity
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("users")
public class User extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("username")
    private String username;

    // Other fields...
}
```

### 3. Pagination Query Standard Implementation

```java

@Override
@Transactional(readOnly = true)
public PageResult<UserVO> pageQuery(UserPageDTO pageDTO) {
    // Build pagination object
    Page<User> page = PageUtils.buildPage(pageDTO);

    // Build query conditions
    LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
    if (StringUtils.isNotBlank(pageDTO.getUsername())) {
        queryWrapper.like(User::getUsername, pageDTO.getUsername());
    }

    // Execute query and convert results
    Page<User> resultPage = this.page(page, queryWrapper);
    List<UserVO> voList = userConverter.toVOList(resultPage.getRecords());

    return PageResult.of(
            resultPage.getCurrent(),
            resultPage.getSize(),
            resultPage.getTotal(),
            voList
    );
}
```

### 4. RocketMQ Event Publishing

```java
// User change event publishing
@Component
@Slf4j
public class UserEventPublisher {

    /**
     * 发布用户创建事件
     *
     * @param userId 用户ID
     */
    public void publishUserCreated(Long userId) {
        UserChangeEvent event = UserChangeEvent.builder()
                .userId(userId)
                .eventType(EventType.CREATED)
                .timestamp(LocalDateTime.now())
                .traceId(TraceUtil.getTraceId())
                .build();

        userEventProducer.sendEvent(event);
        log.info("用户创建事件已发布: userId={}", userId);
    }
}
```

## 🔒 Security & Permission Standards

### 1. OAuth2.1 Configuration

```java
// Resource server configuration
@Configuration
@EnableWebSecurity
public class ResourceServerConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/user/internal/**").hasAuthority("SCOPE_internal_api")
                        .requestMatchers("/user/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                );
        return http.build();
    }
}
```

### 2. Permission Control Annotations

```java
// Method-level permission control
@PreAuthorize("hasRole('ADMIN')")
public void deleteUser(Long userId) {
}

@PreAuthorize("hasRole('ADMIN') or @userService.isOwner(#userId, authentication.principal.userId)")
public UserDTO updateUser(Long userId, UserUpdateDTO updateDTO) {
}
```

## 📝 Documentation Maintenance Standards

### 1. API Documentation Annotations

```java

@Api(tags = "用户管理")
@RestController
@RequestMapping("/user/manage")
public class UserManageController {

    @ApiOperation(value = "创建用户", notes = "管理员创建新用户")
    @PostMapping("/create")
    public Result<UserVO> createUser(@Valid @RequestBody UserCreateDTO createDTO) {
        // Implementation logic
    }
}
```

### 2. Service Documentation Update Requirements

- After each code change, **MUST** update the corresponding service's README.md
- API changes require interface description updates
- Database structure changes need synchronized documentation updates
- Cache strategy changes require cache configuration description updates

## 🚀 Deployment & Operations Standards

### 1. Configuration File Template

```yaml
# application.yml
spring:
  application:
    name: ${service-name}
  profiles:
    active: ${ENV:dev}
  datasource:
    url: jdbc:mysql://localhost:3306/${db-name}?useSSL=false&serverTimezone=UTC
  redis:
    host: localhost
    port: 6379
    database: ${redis-db-index}

# RocketMQ configuration
rocketmq:
  name-server: localhost:39876
  producer:
    group: ${service-name}-producer-group
```

### 2. Docker Deployment

```dockerfile
FROM openjdk:17-jre-slim
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

## 📋 Development Checklist

### Code Completion Checklist

- [ ] Read relevant service's README.md documentation
- [ ] Confirmed SQL table structure and strictly followed it
- [ ] Code comments written in Chinese
- [ ] Entity classes extend BaseEntity
- [ ] Used correct cache strategy (single/multi-level)
- [ ] OAuth2.1 permission configuration correct
- [ ] RocketMQ event publishing correctly configured with port 39876
- [ ] Generated API documentation through Knife4j
- [ ] Updated service internal README.md documentation

### Testing Checklist

- [ ] Tested all API endpoints using Postman
- [ ] Verified Knife4j documentation completeness
- [ ] Validated cache strategy effectiveness
- [ ] Confirmed transaction configuration correctness
- [ ] Tested OAuth2.1 permission controls

## 💡 Special Service Considerations

### auth-service

- **No MySQL Configuration**: Does not use any SQL database
- **No MyBatis-Plus**: No data access layer configuration needed
- **No Mapper/Entity**: Only uses Redis cache

### log-service

- **No MySQL Configuration**: Uses Elasticsearch for storage
- **No Redis Configuration**: Directly writes to ES
- **Asynchronous Processing**: Based on RocketMQ message consumption

### search-service

- **Multi-level Cache**: Caffeine + Redis
- **No Direct MySQL**: Gets data through Feign calls to other services
- **Elasticsearch**: Primary data storage

## 🔄 Version Update Records

- **v1.0** (2025-09-26): Customized rules based on actual Cloud microservices project
- Based on Spring Boot 3.5.3 + Spring Cloud 2025.0.0
- Integrated OAuth2.1, multi-level cache, RocketMQ features

---

<div align="center">

**📋 Rule Maintenance**: CloudDevAgent  
**🔄 Next Review**: 2025-12-26

</div>
