# Cloud微服务平台 - 开发规范文档

**文档版本**: v5.0 (2025-01-15)  
**项目版本**: 0.0.1-SNAPSHOT  
**维护团队**: Cloud Development Team  
**最新更新**: 配置分离架构重构完成

## 项目概述

Cloud微服务平台是一个基于Spring Boot 3.x + Spring Cloud 2025的现代化企业级微服务架构项目。项目采用最新的技术栈，实现了完整的OAuth2.1标准认证授权、配置分离架构、多级缓存策略、自动字段填充等先进特性，为企业数字化转型提供强有力的技术支撑。

## 🏗️ 架构设计原则

### 1. 配置分离架构
- **服务自治**: 每个服务独立配置，避免强耦合
- **模板化设计**: 公共模块提供配置模板，服务继承并定制
- **按需启用**: 根据业务特点选择合适的配置策略

### 2. 多级缓存策略
- **分层缓存**: L1(Caffeine本地) + L2(Redis分布式)
- **差异化配置**: 不同服务采用不同缓存策略
- **性能优化**: 根据访问模式优化缓存配置

### 3. 自动化管理
- **字段自动填充**: 创建时间、更新时间、操作人自动管理
- **配置工厂模式**: 通过工厂类提供标准化配置
- **类型安全**: 使用@Primary确保配置优先级

## 📋 技术规范

### 1. 代码风格规范

#### 命名规范
- **类名**: 使用PascalCase，如`UserService`、`OrderController`
- **方法名**: 使用camelCase，如`getUserById`、`createOrder`
- **变量名**: 使用camelCase，如`userId`、`orderStatus`
- **常量名**: 使用UPPER_SNAKE_CASE，如`MAX_RETRY_COUNT`
- **包名**: 使用小写，如`com.cloud.user.service`

#### 注释规范
```java
/**
 * 用户服务接口
 * 提供用户管理相关的业务功能
 *
 * @author what's up
 * @date 2025-01-15
 * @since 1.0.0
 */
public interface UserService {
    
    /**
     * 根据用户ID获取用户信息
     *
     * @param userId 用户ID
     * @return 用户信息
     * @throws UserNotFoundException 用户不存在时抛出
     */
    UserDTO getUserById(Long userId);
}
```

#### 包结构规范
```
com.cloud.{service}/
├── controller/          # 控制器层
├── service/            # 业务逻辑层
│   └── impl/          # 业务实现层
├── repository/         # 数据访问层
├── domain/            # 领域模型
│   ├── entity/        # 实体类
│   ├── dto/           # 数据传输对象
│   └── vo/            # 视图对象
├── config/            # 配置类
├── messaging/         # 消息处理
└── exception/         # 异常定义
```

### 2. 配置管理规范

#### 配置分离原则
1. **公共配置** (`common-module`)
   - 仅提供配置模板和工厂类
   - 不创建具体的Bean实例
   - 使用@ConditionalOnMissingBean作为后备配置

2. **服务配置** (各服务模块)
   - 继承公共配置模板
   - 使用@Primary覆盖默认配置
   - 根据业务特点定制配置参数

#### Redis配置规范
```java
@Configuration
@Primary
public class UserRedisConfig extends BaseRedisConfig {
    
    @Override
    protected String getServicePrefix() {
        return "user";
    }
    
    @Override
    protected long getCacheExpireTime(String type) {
        // 根据业务特点设置过期时间
        return switch (type) {
            case "userInfo" -> 1800L;    // 30分钟
            case "userProfile" -> 3600L; // 1小时
            default -> 3600L;
        };
    }
}
```

#### MyBatis Plus配置规范
```java
@Configuration
@Primary
public class UserMybatisPlusConfig {
    
    @Bean
    @Primary
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        return MybatisPlusConfigFactory.createStandardInterceptor(DbType.MYSQL);
    }
    
    @Bean
    @Primary
    public MetaObjectHandler metaObjectHandler() {
        return MybatisPlusConfigFactory.createUserServiceMetaObjectHandler();
    }
}
```

### 3. 缓存使用规范

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

## 📞 联系方式

### 技术支持
- **项目负责人**: what's up
- **技术团队**: Cloud Development Team
- **文档维护**: Development Team

### 问题反馈
- **Bug报告**: Issue跟踪系统
- **功能建议**: 需求管理系统
- **技术讨论**: 技术论坛

---

**文档更新日期**: 2025-01-15  
**下次审查日期**: 2025-04-15
