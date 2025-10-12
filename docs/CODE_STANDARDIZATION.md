# 代码标准化指南

本文档定义了Spring Cloud微服务项目的代码规范和标准化实践，确保项目的一致性、可维护性��可读性。

## 目录
- [日志记录标准](#日志记录标准)
- [异常处理标准](#异常处理标准)
- [控制器层标准](#控制器层标准)
- [服务层标准](#服务层标准)
- [API文档标准](#api文档标准)
- [通用编码规范](#通用编码规范)

## 日志记录标准

### 使用LogTemplate工具类

项目提供了统一的日志模板工具类`LogTemplate`，用于标准化日志记录格式。

#### 基本使用方法

```java
import com.cloud.common.utils.LogTemplate;

// 业务操作日志
LogTemplate.logStart("用户创建", "开始创建用户");
LogTemplate.logSuccess("用户创建", "创建成功，ID: 123");
LogTemplate.logFailed("用户创建", "创建失败，用户名已存在");

// 数据操作日志
LogTemplate.logCreate("用户", "ID: 123, 用户名: test");
LogTemplate.logUpdate("用户", "ID: 123, 更新字段: status");
LogTemplate.logDelete("用户", "ID: 123");
LogTemplate.logQuery("用户", "ID: 123");

// 批量操作日志
LogTemplate.logBatchStart("批量创建用户", 100);
LogTemplate.logBatchComplete("批量创建用户", 95, 100);
LogTemplate.logBatchFailed("批量创建用户", 5, 100);

// 分页查询日志
LogTemplate.logPageQuery("用户", 1, 20, 1000);
```

#### 日志级别使用规范

- **ERROR**: 系统错误、异常情况
- **WARN**: 警告信息、业务异常
- **INFO**: 重要业务操作、状态变更
- **DEBUG**: 调试信息、详细执行过程

#### 日志格式规范

```java
// ✅ 推荐格式
LogTemplate.logInfo("操作名称", "详细信息");

// 使用格式化工具
LogTemplate.logInfo("用户操作", LogTemplate.formatDetails("id", userId, "action", "create"));

// ✅ 异常日志
LogTemplate.logException("用户创建", LogTemplate.formatDetails("username", username), exception);
```

## 异常处理标准

### 全局异常处理器

各服务应继承`GlobalExceptionHandler`基类：

```java
@Slf4j
@RestControllerAdvice
@Hidden
public class UserGlobalExceptionHandler extends GlobalExceptionHandler {

    @ExceptionHandler(UserServiceException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Object> handleUserServiceException(UserServiceException e, HttpServletRequest request) {
        LogTemplate.logWarn("用户服务异常", LogTemplate.formatDetails("uri", request.getRequestURI(), "message", e.getMessage()));
        return Result.businessError(e.getMessage());
    }
}
```

### 异常分类和处理

1. **业务异常** (`BusinessException`): 记录WARN级别日志
2. **系���异常** (`SystemException`): 记录ERROR级别日志，包含堆栈信息
3. **参数校验异常**: 记录WARN级别日志，返回详细错误信息
4. **权限异常**: 记录WARN级别日志，返回403状态码

### 自定义异常示例

```java
public class UserServiceException extends BusinessException {

    public static class UserNotFoundException extends UserServiceException {
        public UserNotFoundException(Long userId) {
            super("用户不存在: " + userId);
        }
    }

    public static class UserAlreadyExistsException extends UserServiceException {
        public UserAlreadyExistsException(String username) {
            super("用户已存在: " + username);
        }
    }
}
```

## 控制器层标准

### 标准化控制器

使用`StandardController`基类进行标准化实现：

```java
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@ApiStandard.ServiceTag(serviceName = "用户")
@PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
public class UserController extends StandardController<Long, Long, UserDTO, UserVO> {

    private final UserService userService;

    @Override
    protected StandardService<Long, Long, UserDTO, UserVO> getService() {
        return userService;
    }

    @Override
    protected String getEntityName() {
        return "用户";
    }

    // 业务特定接口
    @PostMapping("/{id}/enable")
    @ApiStandard.BusinessOperations.Enable(entityName = "用户")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Boolean> enableUser(@PathVariable Long id, Authentication authentication) {
        LogTemplate.logProcess("用户启用", LogTemplate.formatDetails("id", id));
        boolean result = userService.enableUser(id);
        LogTemplate.logSuccess("用户启用", LogTemplate.formatDetails("id", id));
        return Result.success("用户启用成功", result);
    }
}
```

### 控制器规范

1. **继承StandardController**: 提供标准CRUD操作
2. **使用ApiStandard注解**: 统一API文档格式
3. **权限控制**: 使用@PreAuthorize注解
4. **参数校验**: 使用@Valid和校验注解
5. **统一返回**: 使用Result包装返回结果

### 请求映射规范

```java
// ✅ RESTful风格
@GetMapping            // 查询列表
@GetMapping("/{id}")    // 查询详情
@PostMapping           // 创建
@PutMapping("/{id}")    // 更新
@PatchMapping("/{id}")  // 部分更新
@DeleteMapping("/{id}") // 删除

// ✅ 批量操作
@GetMapping("/batch")    // 批量查询
@PostMapping("/batch")   // 批量创建
@PutMapping("/batch")    // 批量更新
@DeleteMapping("/batch") // 批量删除
```

## 服务层标准

### 标准化服务

使用`BaseService`基类进行标准化实现：

```java
@Service
@Transactional(readOnly = true)
public class UserServiceImpl extends BaseService<User, Long, UserDTO, UserVO> implements UserService {

    @Override
    protected String getEntityName() {
        return "用户";
    }

    @Override
    protected PageResult<UserVO> doPageQuery(Integer page, Integer size) {
        return executeWithPerformanceMonitor("分页查询用户",
            () -> userMapper.selectPage(new Page<>(page, size), buildQueryWrapper()));
    }

    @Override
    protected UserVO doGetById(Long id) {
        return executeOperation("查询用户",
            () -> userConverter.toVO(userMapper.selectById(id)));
    }

    @Override
    protected Long doCreate(UserDTO dto) {
        return executeWithCacheEvict("创建用户",
            new String[]{"userCache", "userPageCache"},
            "all",
            () -> {
                User user = userConverter.toEntity(dto);
                userMapper.insert(user);
                return user.getId();
            });
    }
}
```

### 服务层规范

1. **继承BaseService**: 使用标准化的操作模板
2. **事务管理**: 正确使用@Transactional注解
3. **缓存策略**: 使用Spring Cache注解
4. **性能监控**: 使用executeWithPerformanceMonitor方法
5. **异常处理**: 抛出业务异常而非系统异常

### 数据访问规范

```java
// ✅ 使用MyBatis Plus
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT * FROM users WHERE username = #{username} AND is_deleted = 0")
    User findByUsername(@Param("username") String username);

    // ✅ 自定义查询方法
    Page<User> selectUserPage(Page<User> page, @Param("query") UserQueryDTO query);
}
```

## API文档标准

### 使用ApiStandard注解

```java
// ✅ 标准CRUD操作
@ApiStandard.CrudOperations.List(entityName = "用户")
@ApiStandard.CrudOperations.Detail(entityName = "用户")
@ApiStandard.CrudOperations.Create(entityName = "用户")
@ApiStandard.CrudOperations.Update(entityName = "用户")
@ApiStandard.CrudOperations.Delete(entityName = "用户")

// ✅ 业务操作
@ApiStandard.BusinessOperations.Enable(entityName = "用户")
@ApiStandard.BusinessOperations.Disable(entityName = "用户")
@ApiStandard.BusinessOperations.Approve(entityName = "用户")

// ✅ 批量操作
@ApiStandard.BatchOperations.BatchCreate(entityName = "用户")
@ApiStandard.BatchOperations.BatchUpdate(entityName = "用户")
@ApiStandard.BatchOperations.BatchDelete(entityName = "用户")
```

### 文档注解规范

```java
@Operation(
    summary = "创建用户",
    description = "创建新的用户账户",
    responses = {
        @ApiResponse(responseCode = "200", description = "创建成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "401", description = "未授权"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    }
)
public Result<Long> createUser(
    @Parameter(description = "用户信息", required = true)
    @Valid @RequestBody UserCreateDTO dto) {
    // 实现逻辑
}
```

## 通用编码规范

### 命名规范

```java
// ✅ 类名：PascalCase
public class UserServiceImpl
public class UserDTO
public class UserVO

// ✅ 方法名：camelCase
public UserDTO getUserById(Long id)
public boolean updateUserStatus(Long id, Integer status)

// ✅ 常量：UPPER_SNAKE_CASE
public static final String DEFAULT_USERNAME = "guest";
public static final int MAX_RETRY_COUNT = 3;
```

### 代码注释规范

```java
/**
 * 用户服务实现类
 * 提供用户相关的业务逻辑处理
 *
 * @author cloud
 * @since 1.0.0
 */
@Slf4j
@Service
public class UserServiceImpl {

    /**
     * 创建用户
     *
     * @param dto 用户信息
     * @return 用户ID
     * @throws BusinessException 当用户名已存在时抛出
     */
    public Long createUser(UserCreateDTO dto) {
        // 实现逻辑
    }
}
```

### 异常处理规范

```java
// ✅ 使用具体异常类型
if (user == null) {
    throw new UserServiceException.UserNotFoundException(userId);
}

// ✅ 使用标准日志记录
LogTemplate.logException("用户创建", LogTemplate.formatDetails("username", username), exception);

// ✅ 避免捕获Exception
try {
    // 业务逻辑
} catch (BusinessException e) {
    throw e; // 重新抛出业务异常
} catch (Exception e) {
    throw new SystemException("系统错误", e); // 包装为系统异常
}
```

### 参数校验规范

```java
// ✅ 使用JSR-303注解
public class UserCreateDTO {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度必须在3-20之间")
    private String username;

    @Email(message = "邮箱格式不正确")
    private String email;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
}
```

## 最佳实践

### 1. 性能优化

```java
// ✅ 使用缓存
@Cacheable(cacheNames = "userCache", key = "#id")
public UserVO getUserById(Long id) {
    return userConverter.toVO(userMapper.selectById(id));
}

// ✅ 批量操作
public int deleteUsers(List<Long> ids) {
    return executeBatchOperation("批量删除用户", ids, this::deleteUser);
}
```

### 2. 安全规范

```java
// ✅ 权限控制
@PreAuthorize("hasRole('ADMIN') or @permissionUtils.isOwner(authentication, #id)")
public Result<UserVO> getUser(@PathVariable Long id, Authentication authentication) {
    // 实现逻辑
}

// ✅ 参数校验
public Result<Void> updateUser(@PathVariable Long id,
                              @Valid @RequestBody UserUpdateDTO dto) {
    // 实现逻辑
}
```

### 3. 错误处理

```java
// ✅ 统一错误响应
@ExceptionHandler(UserNotFoundException.class)
public Result<Void> handleUserNotFound(UserNotFoundException e) {
    LogTemplate.logWarn("用户查询失败", e.getMessage());
    return Result.notFound(e.getMessage());
}
```

## 工具和配置

### IDE配置

- 启用Lombok插件
- 配置代码格式化规则
- 启用静态导入检查

### 构建配置

```xml
<!-- Maven插件配置 -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-checkstyle-plugin</artifactId>
    <version>3.2.0</version>
    <configuration>
        <configLocation>checkstyle.xml</configLocation>
    </configuration>
</plugin>
```

### 代码检查工具

- **Checkstyle**: 代码风格检查
- **SpotBugs**: 潜在bug检查
- **SonarQube**: 代码质量分析

## 总结

遵循本标准化指南可以确保：

1. **代码一致性**: 统一的编码风格和结构
2. **可维护性**: 清晰的日志和异常处理
3. **可读性**: 标准化的API文档和注释
4. **可扩展性**: 模块化的设计模式
5. **团队协作**: 统一的开发规范

所有新代码都应遵循本指南，现有代码应逐步重构以符合标准。