# 开发规范文档 (RULE.md)

本文档定义了Spring Cloud微服务项目的开发规范和最佳实践,所有开发人员必须遵守。

---

## 📋 目录

1. [代码规范](#代码规范)
2. [命名规范](#命名规范)
3. [项目结构规范](#项目结构规范)
4. [API设计规范](#api设计规范)
5. [数据库设计规范](#数据库设计规范)
6. [异常处理规范](#异常处理规范)
7. [日志规范](#日志规范)
8. [安全规范](#安全规范)
9. [Git提交规范](#git提交规范)
10. [测试规范](#测试规范)
11. [性能规范](#性能规范)
12. [文档规范](#文档规范)

---

## 代码规范

### 1. 通用规则

- ✅ **编码格式**: UTF-8
- ✅ **缩进**: 4个空格(不使用Tab)
- ✅ **行宽**: 不超过120字符
- ✅ **空行**: 合理使用空行分隔代码块
- ✅ **注释**: 必须使用中文注释

### 2. Java代码规范

#### 类规范

```java
/**
 * 用户服务实现类
 * 提供用户CRUD、认证、权限管理等功能
 *
 * @author CloudTeam
 * @since 2025-01-15
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    // 业务方法
}
```

**规则**:
- 类必须有完整的JavaDoc注释
- 使用`@RequiredArgsConstructor`替代构造函数注入
- 使用`@Slf4j`记录日志
- 依赖注入使用final修饰

#### 方法规范

```java
/**
 * 根据用户ID查询用户信息
 *
 * @param userId 用户ID
 * @return 用户信息VO
 * @throws BusinessException 用户不存在时抛出
 */
@Override
@Transactional(rollbackFor = Exception.class, readOnly = true)
public UserVO getUserById(Long userId) {
    log.info("查询用户信息, userId: {}", userId);

    // 参数校验
    if (userId == null || userId <= 0) {
        throw new BusinessException(ErrorCode.INVALID_PARAM, "用户ID无效");
    }

    // 缓存查询
    UserVO cached = getCachedUser(userId);
    if (cached != null) {
        return cached;
    }

    // 数据库查询
    User user = userMapper.selectById(userId);
    if (user == null) {
        throw new BusinessException(ErrorCode.USER_NOT_FOUND);
    }

    // 转换并缓存
    UserVO userVO = UserConverter.INSTANCE.toVO(user);
    cacheUser(userId, userVO);

    return userVO;
}
```

**规则**:
- 公共方法必须有JavaDoc注释
- 使用占位符记录日志: `log.info("msg: {}", param)`
- 参数校验优先
- 异常必须明确类型并提供有意义的错误信息
- 事务注解必须指定rollbackFor

### 3. 禁止事项

❌ **严禁**:
- 使用`System.out.println()`打印日志
- 硬编码魔法值(使用常量类)
- 捕获异常后不处理(`catch (Exception e) {}`)
- 使用`@Autowired`字段注入(使用构造函数注入)
- 直接返回Entity(使用VO/DTO)
- 在循环中进行数据库操作
- 在事务方法中进行远程调用
- 暴露敏感信息(密码、密钥等)

---

## 命名规范

### 1. 项目命名

- **服务模块**: `xxx-service` (如: user-service, order-service)
- **公共模块**: `xxx-module` (如: common-module, api-module)

### 2. 包命名

```
com.cloud.{service}.{layer}
```

**层级划分**:
- `controller` - 控制器层
- `service` / `service.impl` - 服务层
- `mapper` - 数据访问层
- `domain` / `entity` - 实体类
- `dto` - 数据传输对象
- `vo` - 视图对象
- `converter` - 对象转换器
- `config` - 配置类
- `exception` - 异常类
- `constant` - 常量类
- `enums` - 枚举类
- `utils` - 工具类

### 3. 类命名

| 类型 | 命名规则 | 示例 |
|------|---------|------|
| Controller | XxxController | UserController |
| Service接口 | XxxService | UserService |
| Service实现 | XxxServiceImpl | UserServiceImpl |
| Mapper | XxxMapper | UserMapper |
| Entity | Xxx | User, Order |
| DTO | XxxDTO | UserDTO, CreateUserDTO |
| VO | XxxVO | UserVO, OrderVO |
| Converter | XxxConverter | UserConverter |
| Exception | XxxException | BusinessException |
| Config | XxxConfig | RedisConfig, SecurityConfig |
| Constant | XxxConstant | UserConstant, RedisKeyConstant |

### 4. 方法命名

| 操作类型 | 命名规则 | 示例 |
|---------|---------|------|
| 查询单个 | getXxxById, getXxxByXxx | getUserById |
| 查询列表 | listXxx, getXxxList | listUsers |
| 分页查询 | pageXxx, getXxxPage | pageUsers |
| 新增 | createXxx, addXxx, saveXxx | createUser |
| 修改 | updateXxx, modifyXxx | updateUser |
| 删除 | deleteXxx, removeXxx | deleteUser |
| 统计 | countXxx | countUsers |
| 判断存在 | existsXxx, hasXxx | existsUser |
| 校验 | validateXxx, checkXxx | validateUser |

### 5. 变量命名

- **变量**: camelCase (userId, userName)
- **常量**: UPPER_SNAKE_CASE (MAX_RETRY_COUNT, DEFAULT_PAGE_SIZE)
- **布尔变量**: is/has/can开头 (isActive, hasPermission, canDelete)

---

## 项目结构规范

### 标准服务结构

```
xxx-service/
├── src/main/java/com/cloud/xxx/
│   ├── XxxServiceApplication.java      # 启动类
│   ├── controller/                     # 控制器层
│   │   ├── XxxController.java
│   │   └── XxxFeignController.java     # 内部Feign接口
│   ├── service/                        # 服务层
│   │   ├── XxxService.java
│   │   └── impl/
│   │       └── XxxServiceImpl.java
│   ├── mapper/                         # 数据访问层
│   │   └── XxxMapper.java
│   ├── domain/                         # 领域模型
│   │   ├── entity/                     # 实体类
│   │   │   └── Xxx.java
│   │   ├── dto/                        # 数据传输对象
│   │   │   ├── XxxDTO.java
│   │   │   ├── CreateXxxDTO.java
│   │   │   └── UpdateXxxDTO.java
│   │   └── vo/                         # 视图对象
│   │       └── XxxVO.java
│   ├── converter/                      # 对象转换器
│   │   └── XxxConverter.java
│   ├── config/                         # 配置类
│   │   ├── SecurityConfig.java
│   │   ├── AsyncConfig.java
│   │   └── RocketMQConfig.java
│   ├── exception/                      # 异常类
│   │   └── XxxException.java
│   ├── constant/                       # 常量类
│   │   └── XxxConstant.java
│   └── enums/                          # 枚举类
│       └── XxxEnum.java
├── src/main/resources/
│   ├── application.yml                 # 主配置
│   ├── application-dev.yml             # 开发环境
│   ├── application-prod.yml            # 生产环境
│   ├── mapper/                         # MyBatis映射文件
│   └── logback-spring.xml              # 日志配置
├── src/test/java/                      # 测试代码
├── pom.xml
└── README.md                           # 服务文档
```

---

## API设计规范

### 1. RESTful规范

#### HTTP方法语义

| 方法 | 语义 | 幂等性 | 示例 |
|------|------|-------|------|
| GET | 查询资源 | ✅ | GET /api/users/{id} |
| POST | 创建资源 | ❌ | POST /api/users |
| PUT | 完整更新 | ✅ | PUT /api/users/{id} |
| PATCH | 部分更新 | ❌ | PATCH /api/users/{id}/status |
| DELETE | 删除资源 | ✅ | DELETE /api/users/{id} |

#### URL设计规范

```bash
# ✅ 正确示例
GET    /api/users                  # 查询用户列表
GET    /api/users/{id}            # 查询单个用户
POST   /api/users                 # 创建用户
PUT    /api/users/{id}            # 更新用户
DELETE /api/users/{id}            # 删除用户
GET    /api/users/{id}/orders     # 查询用户的订单

# ❌ 错误示例
GET    /api/getUsers               # 动词不应出现在URL中
POST   /api/user/create           # 重复语义
PUT    /api/updateUser            # 应使用PUT /api/users/{id}
```

### 2. 统一响应格式

#### 成功响应

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "username": "admin"
  },
  "timestamp": 1704067200000,
  "success": true
}
```

#### 错误响应

```json
{
  "code": 40001,
  "message": "用户不存在",
  "data": null,
  "timestamp": 1704067200000,
  "success": false
}
```

#### Result类实现

```java
@Data
public class Result<T> {
    private Integer code;
    private String message;
    private T data;
    private Long timestamp;
    private Boolean success;

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("操作成功");
        result.setData(data);
        result.setTimestamp(System.currentTimeMillis());
        result.setSuccess(true);
        return result;
    }

    public static <T> Result<T> fail(Integer code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        result.setTimestamp(System.currentTimeMillis());
        result.setSuccess(false);
        return result;
    }
}
```

### 3. 分页响应

```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "current": 1,
    "size": 10,
    "total": 100,
    "records": [
      {"id": 1, "username": "user1"},
      {"id": 2, "username": "user2"}
    ]
  }
}
```

### 4. 状态码规范

| 状态码 | 说明 | 使用场景 |
|-------|------|---------|
| 200 | 成功 | 操作成功 |
| 400 | 参数错误 | 请求参数不合法 |
| 401 | 未认证 | 未登录或Token无效 |
| 403 | 无权限 | 没有操作权限 |
| 404 | 资源不存在 | 找不到指定资源 |
| 409 | 冲突 | 资源已存在(如用户名重复) |
| 500 | 服务器错误 | 系统异常 |

**自定义业务错误码** (40001-49999):

```java
public enum ErrorCode {
    // 用户模块 (41xxx)
    USER_NOT_FOUND(41001, "用户不存在"),
    USERNAME_EXISTS(41002, "用户名已存在"),
    INVALID_PASSWORD(41003, "密码错误"),

    // 订单模块 (42xxx)
    ORDER_NOT_FOUND(42001, "订单不存在"),
    ORDER_STATUS_ERROR(42002, "订单状态错误"),

    // 库存模块 (43xxx)
    STOCK_NOT_ENOUGH(43001, "库存不足"),
    STOCK_LOCKED_FAILED(43002, "库存锁定失败");

    private final Integer code;
    private final String message;
}
```

### 5. API文档注解

```java
@RestController
@RequestMapping("/api/users")
@Tag(name = "用户管理", description = "用户CRUD相关接口")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询用户", description = "根据用户ID获取用户详细信息")
    @Parameters({
        @Parameter(name = "id", description = "用户ID", required = true, example = "1")
    })
    public Result<UserVO> getUserById(
            @PathVariable @NotNull @Positive Long id) {
        UserVO user = userService.getUserById(id);
        return Result.success(user);
    }

    @PostMapping
    @Operation(summary = "创建用户", description = "创建新用户")
    public Result<UserVO> createUser(
            @RequestBody @Valid CreateUserDTO dto) {
        UserVO user = userService.createUser(dto);
        return Result.success("用户创建成功", user);
    }
}
```

---

## 数据库设计规范

### 1. 表命名规范

- **表名**: 小写字母+下划线,复数形式 (users, orders, user_addresses)
- **字段名**: 小写字母+下划线 (user_id, created_at)
- **索引名**: idx_{table}_{column} (idx_users_username)
- **唯一索引**: uk_{table}_{column} (uk_users_email)

### 2. 必备字段

所有表必须包含以下字段:

```sql
CREATE TABLE example_table (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',

    -- 业务字段
    ...

    -- 必备字段
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除(0:未删除 1:已删除)',
    version INT DEFAULT 0 COMMENT '乐观锁版本号'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='示例表';
```

### 3. 字段类型规范

| 数据类型 | MySQL类型 | 说明 |
|---------|----------|------|
| 主键ID | BIGINT | 使用雪花算法生成 |
| 字符串 | VARCHAR(n) | 明确长度,最大65535 |
| 长文本 | TEXT | 超过2000字符使用 |
| 整数 | INT, BIGINT | 根据取值范围选择 |
| 小数 | DECIMAL(10,2) | 金额必须用DECIMAL |
| 日期时间 | DATETIME | 不使用TIMESTAMP |
| 布尔 | TINYINT | 0/1表示 |
| 枚举 | VARCHAR(20) | 不使用ENUM类型 |
| JSON | JSON | MySQL 5.7+ |

### 4. 索引规范

- **主键索引**: 每个表必须有主键
- **唯一索引**: 唯一性约束字段(如username, email)
- **普通索引**: 频繁查询字段、外键字段
- **联合索引**: 多字段组合查询,遵循最左前缀原则
- **索引命名**: idx_{table}_{column1}_{column2}

```sql
-- 单列索引
CREATE INDEX idx_users_username ON users(username);

-- 唯一索引
CREATE UNIQUE INDEX uk_users_email ON users(email);

-- 联合索引
CREATE INDEX idx_orders_user_status ON orders(user_id, status, created_at);
```

### 5. 外键与关联

- ❌ **不使用数据库外键约束**(影响性能)
- ✅ 在应用层维护数据一致性
- ✅ 关联字段添加普通索引

---

## 异常处理规范

### 1. 异常分类

#### 业务异常 (BusinessException)

```java
/**
 * 业务异常 - 预期内的业务错误
 * 例如: 用户不存在、库存不足、订单已取消等
 */
public class BusinessException extends RuntimeException {
    private Integer code;
    private String message;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.message = errorCode.getMessage();
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }
}
```

#### 系统异常 (SystemException)

```java
/**
 * 系统异常 - 预期外的系统错误
 * 例如: 数据库连接失败、Redis异常、网络超时等
 */
public class SystemException extends RuntimeException {
    private Integer code;

    public SystemException(String message) {
        super(message);
        this.code = 500;
    }

    public SystemException(String message, Throwable cause) {
        super(message, cause);
        this.code = 500;
    }
}
```

### 2. 全局异常处理

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 业务异常 - WARN级别日志
     */
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * 系统异常 - ERROR级别日志+堆栈
     */
    @ExceptionHandler(SystemException.class)
    public Result<?> handleSystemException(SystemException e) {
        log.error("系统异常: {}", e.getMessage(), e);
        return Result.fail(500, "系统繁忙,请稍后重试");
    }

    /**
     * 参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining(", "));
        log.warn("参数校验失败: {}", message);
        return Result.fail(400, message);
    }

    /**
     * 未知异常 - 兜底处理
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("未知异常: {}", e.getMessage(), e);
        return Result.fail(500, "系统异常");
    }
}
```

### 3. 异常抛出规范

```java
// ✅ 正确: 明确的业务异常
if (user == null) {
    throw new BusinessException(ErrorCode.USER_NOT_FOUND);
}

// ✅ 正确: 携带上下文信息
if (stock < quantity) {
    throw new BusinessException(ErrorCode.STOCK_NOT_ENOUGH,
        String.format("商品ID: %d, 需要: %d, 可用: %d", productId, quantity, stock));
}

// ❌ 错误: 捕获后不处理
try {
    // ...
} catch (Exception e) {
    // 什么都不做
}

// ❌ 错误: 捕获后仅打印日志
try {
    // ...
} catch (Exception e) {
    e.printStackTrace();  // 应该使用log.error
}

// ✅ 正确: 转换为业务异常
try {
    redisTemplate.opsForValue().set(key, value);
} catch (Exception e) {
    log.error("Redis操作失败", e);
    throw new SystemException("缓存写入失败", e);
}
```

---

## 日志规范

### 1. 日志级别

| 级别 | 使用场景 | 示例 |
|------|---------|------|
| ERROR | 系统错误、异常 | 数据库连接失败、第三方API调用失败 |
| WARN | 业务异常、警告 | 用户不存在、库存不足 |
| INFO | 重要业务流程 | 用户登录、订单创建、支付成功 |
| DEBUG | 调试信息 | 方法入参、SQL语句、中间结果 |
| TRACE | 详细追踪 | 几乎不用 |

### 2. 日志格式

```java
// ✅ 使用占位符(性能更好)
log.info("用户登录成功, userId: {}, username: {}", userId, username);

// ❌ 使用字符串拼接
log.info("用户登录成功, userId: " + userId + ", username: " + username);

// ✅ 异常日志包含堆栈
log.error("订单创建失败, orderId: {}", orderId, exception);

// ❌ 仅记录异常消息
log.error("订单创建失败: " + exception.getMessage());
```

### 3. 日志内容规范

```java
@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO createOrder(CreateOrderDTO dto) {
        // 入口日志 - INFO
        log.info("开始创建订单, userId: {}, productId: {}, quantity: {}",
            dto.getUserId(), dto.getProductId(), dto.getQuantity());

        // 关键步骤 - INFO
        log.info("开始扣减库存, productId: {}, quantity: {}",
            dto.getProductId(), dto.getQuantity());
        stockService.deductStock(dto.getProductId(), dto.getQuantity());

        // 业务异常 - WARN
        if (order == null) {
            log.warn("订单创建失败, 库存不足, productId: {}", dto.getProductId());
            throw new BusinessException(ErrorCode.STOCK_NOT_ENOUGH);
        }

        // 系统异常 - ERROR
        try {
            paymentService.createPayment(order.getId());
        } catch (Exception e) {
            log.error("支付创建失败, orderId: {}", order.getId(), e);
            throw new SystemException("支付创建失败", e);
        }

        // 出口日志 - INFO
        log.info("订单创建成功, orderId: {}, orderNo: {}", order.getId(), order.getOrderNo());
        return orderVO;
    }
}
```

### 4. 禁止事项

❌ **严禁**:
- 在循环中打印日志(影响性能)
- 记录敏感信息(密码、身份证、银行卡号)
- 使用`System.out.println()`
- 日志没有上下文信息(只打印"成功"/"失败")
- 在生产环境开启DEBUG级别

---

## 安全规范

### 1. 认证授权

- ✅ 所有API必须经过网关统一认证
- ✅ 使用JWT令牌进行身份验证
- ✅ 敏感操作需要权限校验(`@PreAuthorize`)
- ✅ 令牌设置合理的过期时间(访问令牌2小时,刷新令牌7天)

```java
@PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')")
@DeleteMapping("/{id}")
public Result<Boolean> deleteUser(@PathVariable Long id) {
    // ...
}
```

### 2. 密码安全

- ✅ 使用BCrypt加密存储密码
- ✅ 密码强度要求: 8位以上,包含大小写字母+数字
- ❌ 禁止明文存储密码
- ❌ 禁止在日志中记录密码

```java
// ✅ 密码加密
String encodedPassword = passwordEncoder.encode(rawPassword);

// ✅ 密码验证
boolean matches = passwordEncoder.matches(rawPassword, encodedPassword);
```

### 3. SQL注入防护

- ✅ 使用MyBatis Plus预编译语句
- ✅ 使用`#{param}`而不是`${param}`
- ❌ 禁止拼接SQL

```xml
<!-- ✅ 正确: 使用#{}预编译 -->
<select id="selectByUsername" resultType="User">
    SELECT * FROM users WHERE username = #{username}
</select>

<!-- ❌ 错误: 使用${}直接拼接 -->
<select id="selectByUsername" resultType="User">
    SELECT * FROM users WHERE username = '${username}'
</select>
```

### 4. XSS防护

- ✅ 前端输入进行HTML转义
- ✅ 使用`@Valid`校验参数
- ✅ 网关配置XSS防护响应头

### 5. 敏感数据保护

```java
// ✅ 敏感字段脱敏
public class UserVO {
    private Long id;
    private String username;

    @JsonSerialize(using = SensitiveDataSerializer.class)
    private String phone;  // 138****8000

    @JsonSerialize(using = SensitiveDataSerializer.class)
    private String email;  // u***@example.com

    @JsonIgnore
    private String password;  // 永远不序列化
}
```

---

## Git提交规范

### 1. 分支管理

```
main/master       - 生产环境分支(受保护)
├── develop       - 开发环境分支(受保护)
│   ├── feature/xxx  - 功能分支
│   ├── bugfix/xxx   - Bug修复分支
│   └── hotfix/xxx   - 紧急修复分支
└── release/v1.0.0   - 发布分支
```

### 2. 提交消息格式

```
<type>(<scope>): <subject>

<body>

<footer>
```

#### Type类型

| Type | 说明 | 示例 |
|------|------|------|
| feat | 新功能 | feat(user): 添加用户注册功能 |
| fix | Bug修复 | fix(order): 修复订单金额计算错误 |
| docs | 文档 | docs: 更新API文档 |
| style | 格式 | style: 格式化代码 |
| refactor | 重构 | refactor(cache): 重构缓存模块 |
| perf | 性能优化 | perf(query): 优化用户查询性能 |
| test | 测试 | test(order): 添加订单单元测试 |
| chore | 构建/工具 | chore: 升级Spring Boot版本 |

#### 示例

```bash
feat(order): 添加订单退款功能

- 实现退款申请接口
- 实现商家审核接口
- 添加退款状态流转逻辑
- 集成支付宝退款API

Closes #123
```

### 3. 提交规则

- ✅ 每次提交只做一件事
- ✅ 提交前先pull最新代码
- ✅ 提交前运行测试确保通过
- ✅ 提交消息使用中文或英文
- ❌ 禁止提交敏感信息(.env, credentials)
- ❌ 禁止提交大文件(>5MB)

---

## 测试规范

### 1. 测试分类

- **单元测试**: 测试单个方法/类
- **集成测试**: 测试多个组件协作
- **接口测试**: 测试HTTP API
- **性能测试**: 压测和性能评估

### 2. 单元测试规范

```java
@SpringBootTest
@Transactional
@DisplayName("用户服务测试")
class UserServiceImplTest {

    @Autowired
    private UserService userService;

    @MockBean
    private UserMapper userMapper;

    @Test
    @DisplayName("根据ID查询用户-成功")
    void testGetUserById_Success() {
        // Given - 准备测试数据
        Long userId = 1L;
        User mockUser = new User();
        mockUser.setId(userId);
        mockUser.setUsername("testuser");

        when(userMapper.selectById(userId)).thenReturn(mockUser);

        // When - 执行被测方法
        UserVO result = userService.getUserById(userId);

        // Then - 验证结果
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        verify(userMapper, times(1)).selectById(userId);
    }

    @Test
    @DisplayName("根据ID查询用户-用户不存在")
    void testGetUserById_UserNotFound() {
        // Given
        Long userId = 999L;
        when(userMapper.selectById(userId)).thenReturn(null);

        // When & Then
        assertThrows(BusinessException.class, () -> {
            userService.getUserById(userId);
        });
    }
}
```

### 3. 测试覆盖率

- **核心业务代码**: >80%
- **工具类**: >90%
- **Controller**: >60%

### 4. 测试原则

- ✅ 测试要快速(单个测试<100ms)
- ✅ 测试要独立(不依赖其他测试)
- ✅ 测试要可重复(多次运行结果一致)
- ✅ 使用Given-When-Then结构
- ❌ 测试中不要有硬编码日期/时间

---

## 性能规范

### 1. 数据库优化

- ✅ 合理使用索引
- ✅ 避免SELECT *,只查询需要的字段
- ✅ 分页查询大数据集
- ✅ 批量操作使用batch
- ❌ 禁止在循环中查询数据库
- ❌ 禁止N+1查询

```java
// ✅ 正确: 批量查询
List<Long> userIds = orders.stream()
    .map(Order::getUserId)
    .collect(Collectors.toList());
List<User> users = userMapper.selectBatchIds(userIds);

// ❌ 错误: 循环查询
for (Order order : orders) {
    User user = userMapper.selectById(order.getUserId());  // N+1问题
}
```

### 2. 缓存优化

- ✅ 热点数据使用缓存
- ✅ 设置合理的过期时间
- ✅ 使用多级缓存(Caffeine + Redis)
- ✅ 缓存穿透使用布隆过滤器

### 3. 并发控制

- ✅ 高并发场景使用分布式锁
- ✅ 乐观锁处理并发更新
- ✅ 异步处理非核心业务

```java
// ✅ 分布式锁
@DistributedLock(key = "'stock:' + #productId")
public void deductStock(Long productId, Integer quantity) {
    // 扣减库存
}

// ✅ 乐观锁
@Version
private Integer version;
```

---

## 文档规范

### 1. 代码注释

- ✅ 类必须有JavaDoc注释
- ✅ 公共方法必须有注释
- ✅ 复杂逻辑必须添加行注释
- ❌ 禁止注释掉的代码提交到仓库

### 2. README文档

每个服务必须有README.md,包含:
- 服务概述
- 技术栈
- 核心功能
- 配置说明
- 启动方式
- API接口
- 注意事项

### 3. API文档

- ✅ 使用Swagger/Knife4j注解
- ✅ 提供请求/响应示例
- ✅ 说明参数含义和约束

---

## 附录

### 常用工具

- **代码格式化**: IntelliJ IDEA默认格式化
- **代码检查**: SonarLint插件
- **Git客户端**: SourceTree / GitKraken
- **API测试**: Postman / Apifox
- **数据库工具**: Navicat / DBeaver

### 学习资源

- [Spring官方文档](https://spring.io/projects/spring-boot)
- [MyBatis Plus官方文档](https://baomidou.com/)
- [Alibaba Java开发手册](https://github.com/alibaba/p3c)

---

**最后更新**: 2025-10-16
**维护者**: Cloud Development Team

**Happy Coding!** 🎉
