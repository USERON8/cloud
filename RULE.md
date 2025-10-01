# 📜 Cloud微服务电商平台 - 开发规范文档

<div align="center">

**文档版本**: v6.0  
**最后更新**: 2025-01-25  
**维护团队**: Cloud Platform Team  

</div>

---

## 📄 文档说明

本文档是Cloud微服务电商平台的官方开发规范，旨在为开发团队提供统一的编码标准、架构设计原则和最佳实践指导。所有参与项目开发的成员都应认真阅读并遵循本规范。

## 🏗️ 架构设计原则

### 1. 微服务设计原则

#### 🎯 服务自治
- **独立部署**: 每个服务可以独立部署和扩容
- **数据隔离**: 每个服务拥有自己的数据库
- **故障隔离**: 单个服务故障不影响整体系统
- **技术异构**: 允许不同服务使用不同技术栈

#### 🔗 服务通信
- **同步通信**: 基于Feign的HTTP/REST通信
- **异步通信**: 基于RocketMQ的消息驱动
- **服务发现**: Nacos动态服务注册与发现
- **负载均衡**: Spring Cloud LoadBalancer

### 2. 分层架构设计

```
┌──────────────────────────────────────────┐
│            表示层 (Controller)              │
├──────────────────────────────────────────┤
│             业务层 (Service)               │
├──────────────────────────────────────────┤
│           数据访问层 (Repository)          │
├──────────────────────────────────────────┤
│          数据存储层 (Database)            │
└──────────────────────────────────────────┘
```

#### 层次职责
- **Controller层**: 接收请求、参数校验、调用Service
- **Service层**: 业务逻辑、事务管理、调用Repository
- **Repository层**: 数据操作、SQL执行
- **Database层**: 数据持久化存储

### 3. 配置分离架构

#### 🔧 配置分层
- **公共配置**: common-module提供配置模板
- **服务配置**: 各服务继承并定制化配置
- **环境配置**: dev/test/prod不同环境配置

#### 🔄 缓存策略
- **L1缓存**: Caffeine本地缓存，毫秒级响应
- **L2缓存**: Redis分布式缓存，数据共享
- **缓存同步**: 基于消息队列的缓存更新

## 📋 技术规范

### 1. 编码规范

#### 📝 命名规范

| 类型 | 规范 | 示例 |
|------|------|------|
| 类名 | PascalCase | `UserService`, `OrderController` |
| 方法名 | camelCase | `getUserById()`, `createOrder()` |
| 变量名 | camelCase | `userId`, `orderStatus` |
| 常量名 | UPPER_SNAKE_CASE | `MAX_RETRY_COUNT`, `DEFAULT_PAGE_SIZE` |
| 包名 | 小写，点分隔 | `com.cloud.user.service` |
| 数据库表 | 小写下划线 | `user_info`, `order_detail` |
| 数据库字段 | 小写下划线 | `create_time`, `update_by` |

#### 💬 注释规范

##### 类JavaDoc示例
```java
/**
 * 用户服务实现类
 * 
 * 提供用户相关的核心业务功能，包括：
 * - 用户注册与登录
 * - 用户信息管理
 * - 用户权限控制
 *
 * @author Cloud Team
 * @version 1.0.0
 * @since 2025-01-25
 */
@Service
@Slf4j
public class UserServiceImpl implements UserService {
```

##### 方法JavaDoc示例
```java
/**
 * 根据用户ID查询用户信息
 *
 * 该方法会先从L1缓存查询，如果未命中则查询L2缓存，
 * 最后再从数据库查询，并更新缓存。
 *
 * @param userId 用户ID，不能为null
 * @return 用户信息DTO
 * @throws UserNotFoundException 当用户不存在时抛出
 * @throws IllegalArgumentException 当userId为null时抛出
 */
@Override
@MultiLevelCacheable(name = "userInfo", key = "#userId")
public UserDTO getUserById(Long userId) {
    // 实现代码
}
```

#### 📁 包结构规范

```
com.cloud.{service}/
├── 🌐 controller/       # REST控制器
│   ├── admin/         # 管理后台API
│   └── api/           # 对外API
├── 💼 service/          # 业务逻辑层
│   ├── impl/          # 业务实现
│   └── feign/         # Feign客户端
├── 🗍️ mapper/           # MyBatis Mapper
├── 📦 domain/           # 领域模型
│   ├── entity/        # 数据库实体
│   ├── dto/           # 数据传输对象
│   ├── vo/            # 视图对象
│   ├── query/         # 查询对象
│   └── enums/         # 枚举类
├── ⚙️ config/           # 配置类
│   ├── cache/         # 缓存配置
│   ├── security/      # 安全配置
│   └── database/      # 数据库配置
├── 📡 messaging/        # 消息处理
│   ├── producer/      # 消息生产者
│   └── consumer/      # 消息消费者
├── 🎯 handler/          # 处理器
│   ├── exception/     # 异常处理
│   └── event/         # 事件处理
├── 🔧 utils/            # 工具类
└── 🔒 interceptor/      # 拦截器
```

### 2. API设计规范

#### 🌐 RESTful规范

##### URL设计
```
GET    /api/v1/users             # 查询用户列表
GET    /api/v1/users/{id}        # 查询单个用户
POST   /api/v1/users             # 创建用户
PUT    /api/v1/users/{id}        # 更新用户全部信息
PATCH  /api/v1/users/{id}        # 更新用户部分信息
DELETE /api/v1/users/{id}        # 删除用户
```

##### HTTP状态码
| 状态码 | 含义 | 使用场景 |
|--------|------|----------|
| 200 | OK | 请求成功 |
| 201 | Created | 创建成功 |
| 204 | No Content | 删除成功 |
| 400 | Bad Request | 请求参数错误 |
| 401 | Unauthorized | 未认证 |
| 403 | Forbidden | 无权限 |
| 404 | Not Found | 资源不存在 |
| 409 | Conflict | 资源冲突 |
| 500 | Internal Server Error | 服务器错误 |

#### 📦 统一响应格式

##### 成功响应
```java
{
    "code": 200,
    "success": true,
    "message": "success",
    "data": {
        "id": 1,
        "username": "user"
    },
    "timestamp": 1706169600000
}
```

##### 错误响应
```java
{
    "code": 400,
    "success": false,
    "message": "参数验证失败",
    "errors": [
        {
            "field": "username",
            "message": "用户名不能为空"
        }
    ],
    "timestamp": 1706169600000
}
```

##### 分页响应
```java
{
    "code": 200,
    "success": true,
    "message": "success",
    "data": {
        "list": [],
        "total": 100,
        "pageNum": 1,
        "pageSize": 10,
        "pages": 10
    },
    "timestamp": 1706169600000
}
```

### 3. Feign客户端规范

#### 🔄 接口设计原则
- **直接返回数据**: Feign接口方法应直接返回DTO、VO或其他数据对象，不使用Result包装器
- **路径一致性**: Feign接口中的@RequestMapping路径必须与服务端Controller中的路径完全一致
- **职责分离**: 每个服务应创建专门的FeignController来处理外部服务调用，避免与常规API混用

#### 📦 接口定义规范
```java
/**
 * 用户服务Feign客户端
 * 用于服务间调用用户服务的接口
 */
@FeignClient(name = "user-service", path = "/user", contextId = "userFeignClient")
public interface UserFeignClient {
    
    /**
     * 根据用户名查找用户
     *
     * @param username 用户名
     * @return 用户信息
     */
    @GetMapping("/internal/username/{username}")
    UserDTO findByUsername(@PathVariable("username") String username);
    
    /**
     * 保存用户信息
     *
     * @param registerRequest 用户注册信息
     * @return 用户信息
     */
    @PostMapping("/internal/register")
    UserDTO register(@RequestBody RegisterRequestDTO registerRequest);
}
```

#### 🎯 实现方式规范
- **专用控制器**: 每个服务应创建专门的FeignController来实现Feign接口
- **路径匹配**: FeignController中的@RequestMapping路径必须与Feign接口定义完全一致
- **简化处理**: FeignController中应直接调用Service层方法，不添加额外的业务逻辑

#### 📝 注释规范
- **接口注释**: Feign接口必须添加完整的JavaDoc注释，说明接口用途
- **方法注释**: 每个方法必须添加详细的注释，说明功能、参数和返回值
- **参数注释**: 使用@param和@return标准注释格式

#### 📁 包结构规范
```
api-module/
└── com.cloud.api/
    ├── user/
    │   ├── UserFeignClient.java        # 用户服务Feign接口
    │   └── AdminFeignClient.java       # 管理员服务Feign接口
    ├── product/
    │   └── ProductFeignClient.java     # 商品服务Feign接口
    ├── order/
    │   └── OrderFeignClient.java       # 订单服务Feign接口
    ├── payment/
    │   └── PaymentFeignClient.java     # 支付服务Feign接口
    ├── stock/
    │   └── StockFeignClient.java       # 库存服务Feign接口
    └── auth/
        └── AuthFeignClient.java        # 认证服务Feign接口
```

### 4. 配置管理规范

#### 🔧 Redis配置规范
##### 缓存键命名
```
{service}:{type}:{key}
示例: user:info:123
     product:detail:456
     order:status:789
```

##### 缓存过期时间策略
| 数据类型 | L1缓存(Caffeine) | L2缓存(Redis) | 说明 |
|----------|------------------|---------------|------|
| 热点数据 | 5分钟 | 30分钟 | 高频访问数据 |
| 一般数据 | - | 2小时 | 普通业务数据 |
| 静态数据 | 30分钟 | 24小时 | 字典、配置等 |
| 会话数据 | - | 30分钟 | JWT、Session |

#### 🗄️ 数据库规范

##### 表设计规范
- **主键**: 使用`id` BIGINT自增
- **时间字段**: `create_time`, `update_time`
- **操作人字段**: `create_by`, `update_by`
- **逻辑删除**: `deleted` TINYINT(1)
- **版本控制**: `version` INT

##### 基础字段示例
```sql
CREATE TABLE `user_info` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `status` TINYINT(1) DEFAULT 1 COMMENT '状态:1-正常,0-禁用',
  `deleted` TINYINT(1) DEFAULT 0 COMMENT '逻辑删除:0-未删除,1-已删除',
  `version` INT DEFAULT 0 COMMENT '乐观锁版本',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` VARCHAR(50) DEFAULT NULL,
  `update_by` VARCHAR(50) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户信息表';
```

## 🔒 安全规范

### 1. 认证授权

#### OAuth2.1标准
- **授权服务器**: auth-service实现
- **资源服务器**: 各业务服务
- **令牌类型**: JWT
- **刷新机制**: RefreshToken
- **PKCE**: 移动端安全增强

#### 权限控制
```java
// 方法级权限
@PreAuthorize("hasRole('ADMIN')")
public void deleteUser(Long userId) {}

// 数据级权限
@PostAuthorize("returnObject.userId == authentication.principal.id")
public UserDTO getUser(Long userId) {}
```

### 2. 数据安全

#### 密码存储
- **加密算法**: BCrypt
- **加密强度**: 10轮
- **密码策略**: 至少8位，包含大小写、数字、特殊字符

#### 敏感数据脱敏
```java
@JsonSerialize(using = SensitiveSerializer.class)
private String phone;  // 138****8888

@JsonSerialize(using = SensitiveSerializer.class)
private String idCard; // 110***********1234
```

### 3. 接口安全

#### 参数验证
```java
@PostMapping("/users")
public Result createUser(@Valid @RequestBody UserCreateDTO dto) {
    // Bean Validation自动验证
}
```

#### 防SQL注入
- 使用MyBatis-Plus参数化查询
- 禁止字符串拼接SQL
- 使用@Param注解传递参数

#### 限流防刷
- **网关限流**: 基于Redis+Lua
- **服务限流**: Sentinel
- **方法限流**: @RateLimiter注解

#### 多级缓存配置
```java
// 用户服务 - 多级缓存
@Configuration
@Primary
public class UserLocalCacheConfig {
    
    @Bean
    @Primary
    public CacheManager cacheManager() {
        return MultiLevelCacheConfigFactory.createUserServiceCacheManager();
    }
}
```

#### 缓存键命名规范
```
{service}:{type}:{key}
例如: user:info:123, product:list:category_1
```

### 4. 自动字段填充规范

#### 标准字段填充
```java
@Bean
@Primary
public MetaObjectHandler metaObjectHandler() {
    return new MetaObjectHandler() {
        @Override
        public void insertFill(MetaObject metaObject) {
            this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, LocalDateTime.now());
            this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
            this.strictInsertFill(metaObject, "createBy", String.class, getCurrentUserId());
        }

        @Override
        public void updateFill(MetaObject metaObject) {
            this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
            this.strictUpdateFill(metaObject, "updateBy", String.class, getCurrentUserId());
        }
    };
}
```

## 🔧 开发工具和环境

### 开发环境要求
- **JDK**: 17 LTS
- **Maven**: 3.8+
- **IDE**: IntelliJ IDEA 2023.3+ (推荐)
- **数据库**: MySQL 8.0+
- **缓存**: Redis 7.0+
- **消息队列**: RocketMQ 5.0+
- **服务注册**: Nacos 2.4.0+

### 代码质量工具
- **静态分析**: SonarQube、SpotBugs、PMD
- **代码风格**: Checkstyle、Alibaba Java Coding Guidelines
- **测试工具**: JUnit 5、Mockito、TestContainers
- **API测试**: Postman、Knife4j

## 📊 性能优化规范

### 缓存策略

#### 多级缓存服务 (user、product、search)
- **L1缓存**: Caffeine本地缓存，热点数据
- **L2缓存**: Redis分布式缓存，共享数据
- **缓存穿透**: 布隆过滤器防护
- **缓存雪崩**: 随机过期时间

#### 单级缓存服务 (其他服务)
- **Redis缓存**: 分布式缓存
- **过期策略**: 根据业务特点设置
- **缓存预热**: 提升性能

### 数据库优化
- **索引优化**: 合理创建和使用索引
- **分页查询**: 避免深度分页
- **批量操作**: 减少数据库交互
- **连接池**: HikariCP参数调优

## 🔒 安全规范

### 认证授权
- **OAuth2.1**: 统一认证授权
- **JWT令牌**: 无状态令牌验证
- **PKCE增强**: 移动端安全
- **权限控制**: 方法级权限验证

### 数据安全
- **密码加密**: BCrypt加密
- **数据脱敏**: 敏感信息保护
- **参数校验**: 严格输入验证
- **SQL注入**: 参数化查询

## 📝 文档规范

### API文档
```java
@Api(tags = "用户管理")
@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    
    @ApiOperation(value = "获取用户信息")
    @GetMapping("/{userId}")
    public Result<UserDTO> getUser(@PathVariable Long userId) {
        return Result.success(userService.getUserById(userId));
    }
}
```

### 代码文档
- **JavaDoc**: 完整的类和方法注释
- **README**: 项目介绍和快速开始
- **配置说明**: 详细的配置参数文档
- **部署指南**: 环境搭建和部署说明

## 🚀 部署规范

### 环境管理
- **开发环境** (dev): 本地开发
- **测试环境** (test): 功能测试
- **预生产环境** (staging): 生产前验证
- **生产环境** (prod): 正式环境

### 容器化部署
```dockerfile
FROM openjdk:17-jre-slim
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

## 📊 监控规范

### 应用监控
- **业务指标**: 订单量、用户活跃度
- **技术指标**: QPS、响应时间、错误率
- **资源指标**: CPU、内存、磁盘使用率
- **自定义指标**: Micrometer业务指标

### 日志管理
```java
@Slf4j
public class UserService {
    
    public UserDTO createUser(UserCreateDTO createDTO) {
        log.info("开始创建用户, 用户名: {}", createDTO.getUsername());
        try {
            // 业务逻辑
            log.info("用户创建成功, 用户ID: {}", user.getId());
            return userDTO;
        } catch (Exception e) {
            log.error("用户创建失败, 用户名: {}", createDTO.getUsername(), e);
            throw e;
        }
    }
}
```

## 🔄 版本控制规范

### Git工作流
- **master**: 主分支，稳定可发布
- **develop**: 开发分支，集成功能
- **feature/***: 功能分支
- **hotfix/***: 热修复分支
- **release/***: 发布分支

### 提交规范
```
<type>(<scope>): <subject>

feat: 新功能
fix: 修复bug
docs: 文档更新
style: 代码格式
refactor: 代码重构
test: 测试相关
chore: 构建工具
```

## 📋 检查清单

### 开发完成检查
- [ ] 代码符合命名规范
- [ ] 添加完整JavaDoc注释
- [ ] 通过静态代码分析
- [ ] 实现所有需求功能
- [ ] 添加完整单元测试
- [ ] 通过集成测试
- [ ] API文档已更新
- [ ] 配置环境变量
- [ ] 添加健康检查
- [ ] 配置监控指标

## 📊 性能优化规范

### 1. 缓存优化

#### 多级缓存
- **L1缓存**: Caffeine，进程内缓存
- **L2缓存**: Redis，分布式缓存
- **缓存预热**: 启动时加载热点数据
- **缓存更新**: 基于消息队列

### 2. 数据库优化

#### SQL优化
- 避免全表扫描
- 使用EXPLAIN分析执行计划
- 大批量操作使用批处理
- 分页查询使用游标

### 3. JVM调优
```bash
-Xms2G -Xmx2G                    # 堆内存
-XX:+UseG1GC                     # G1垃圾回收器
-XX:MaxGCPauseMillis=200        # 最大GC暂停时间
-XX:+HeapDumpOnOutOfMemoryError # OOM时dump
```

## 📊 监控规范

### 1. 应用监控

#### Actuator端点
- `/actuator/health` - 健康检查
- `/actuator/metrics` - 指标收集
- `/actuator/prometheus` - Prometheus指标

### 2. 日志规范

#### 日志级别
- **ERROR**: 错误日志，需要立即处理
- **WARN**: 警告日志，需要关注
- **INFO**: 业务日志，记录关键操作
- **DEBUG**: 调试日志，开发环境使用

#### 日志格式
```java
log.info("用户登录成功, userId: {}, username: {}, ip: {}", 
         userId, username, ipAddress);
```

### 3. 告警配置
| 指标 | 阈值 | 告警级别 |
|------|------|----------|
| CPU使用率 | >80% | 警告 |
| 内存使用率 | >85% | 警告 |
| 响应时间 | >3s | 警告 |
| 错误率 | >1% | 严重 |

## 📧 联系方式

- **项目维护**: what's up
- **技术团队**: Cloud Platform Team
- **文档网站**: https://docs.cloud-platform.com
- **技术支持**: support@cloud-platform.com

---

<div align="center">

**📆 文档更新**: 2025-01-25  
**🔄 下次审查**: 2025-04-25

</div>
