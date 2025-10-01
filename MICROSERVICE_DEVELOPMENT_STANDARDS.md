# 微服务开发标准模板和最佳实践文档

## 📋 目录
1. [概述](#概述)
2. [控制器层架构标准](#控制器层架构标准)
3. [服务层架构标准](#服务层架构标准)
4. [数据访问层标准](#数据访问层标准)
5. [权限控制标准](#权限控制标准)
6. [缓存策略标准](#缓存策略标准)
7. [分布式锁使用标准](#分布式锁使用标准)
8. [异常处理标准](#异常处理标准)
9. [日志记录标准](#日志记录标准)
10. [API设计标准](#api设计标准)
11. [测试标准](#测试标准)
12. [监控和性能优化](#监控和性能优化)

---

## 概述

本文档定义了云平台微服务架构的开发标准和最佳实践，基于 user-service、product-service、order-service、payment-service 和 stock-service 的实践经验总结而成。

### 核心原则
1. **单一职责**: 每个服务、类、方法只做一件事
2. **RESTful设计**: 遵循REST架构风格
3. **统一响应**: 使用标准的Result包装类
4. **安全优先**: 完善的权限控制和数据保护
5. **可观测性**: 详细的日志和监控

---

## 控制器层架构标准

### 控制器分类

每个微服务应该包含两个主要控制器：

#### 1. 主控制器 (例如: UserController)
- **职责**: 提供对外的RESTful API
- **路径规范**: `/resources` (复数形式，如 `/users`, `/products`, `/orders`)
- **权限控制**: 需要完整的权限验证
- **响应格式**: 使用统一的 Result 包装类

#### 2. Feign控制器 (例如: UserFeignController)
- **职责**: 提供内部微服务间调用接口
- **路径规范**: `/feign/resources` (如 `/feign/users`, `/feign/products`)
- **权限控制**: 无需权限验证（通过网络层保护）
- **响应格式**: 使用统一的 Result 包装类

### 控制器模板

```java
@Slf4j
@RestController
@RequestMapping("/resources")
@RequiredArgsConstructor
@Tag(name = "资源服务", description = "资源的RESTful API接口")
public class ResourceController {

    private final ResourceService resourceService;

    /**
     * 分页查询资源
     */
    @PostMapping("/page")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    @Operation(summary = "分页查询资源", description = "根据条件分页查询资源信息")
    public Result<PageResult<ResourceVO>> getResourcesPage(
            @Parameter(description = "分页查询条件") @RequestBody
            @Valid @NotNull(message = "分页查询条件不能为空") ResourcePageDTO pageDTO,
            Authentication authentication) {

        try {
            PageResult<ResourceVO> pageResult = resourceService.pageQuery(pageDTO);
            return Result.success(pageResult);
        } catch (Exception e) {
            log.error("分页查询资源失败", e);
            return Result.error("分页查询资源失败: " + e.getMessage());
        }
    }

    /**
     * 根据ID获取资源详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    @Operation(summary = "获取资源详情", description = "根据ID获取资源详细信息")
    public Result<ResourceDTO> getResourceById(
            @Parameter(description = "资源ID") @PathVariable
            @NotNull(message = "资源ID不能为空")
            @Positive(message = "资源ID必须为正整数") Long id,
            Authentication authentication) {

        try {
            ResourceDTO resource = resourceService.getResourceById(id);
            if (resource == null) {
                return Result.error("资源不存在");
            }
            return Result.success("查询成功", resource);
        } catch (Exception e) {
            log.error("获取资源详情失败，资源ID: {}", id, e);
            return Result.error("获取资源详情失败: " + e.getMessage());
        }
    }

    /**
     * 创建资源
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "创建资源", description = "创建新的资源")
    public Result<ResourceDTO> createResource(
            @Parameter(description = "资源信息") @RequestBody
            @Valid @NotNull(message = "资源信息不能为空") ResourceDTO resourceDTO) {

        try {
            ResourceDTO created = resourceService.createResource(resourceDTO);
            return Result.success("资源创建成功", created);
        } catch (Exception e) {
            log.error("创建资源失败", e);
            return Result.error("创建资源失败: " + e.getMessage());
        }
    }

    /**
     * 更新资源
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "更新资源", description = "更新资源信息")
    public Result<Boolean> updateResource(
            @Parameter(description = "资源ID") @PathVariable Long id,
            @Parameter(description = "资源信息") @RequestBody
            @Valid @NotNull(message = "资源信息不能为空") ResourceDTO resourceDTO,
            Authentication authentication) {

        resourceDTO.setId(id);

        try {
            boolean result = resourceService.updateResource(resourceDTO);
            return Result.success("资源更新成功", result);
        } catch (Exception e) {
            log.error("更新资源失败，资源ID: {}", id, e);
            return Result.error("更新资源失败: " + e.getMessage());
        }
    }

    /**
     * 删除资源
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "删除资源", description = "删除资源")
    public Result<Boolean> deleteResource(
            @Parameter(description = "资源ID") @PathVariable
            @NotNull(message = "资源ID不能为空") Long id) {

        try {
            boolean result = resourceService.deleteResource(id);
            return Result.success("删除成功", result);
        } catch (Exception e) {
            log.error("删除资源失败，资源ID: {}", id, e);
            return Result.error("删除失败: " + e.getMessage());
        }
    }
}
```

### Feign控制器模板

```java
@Slf4j
@RestController
@RequestMapping("/feign/resources")
@RequiredArgsConstructor
@Tag(name = "资源Feign接口", description = "提供内部微服务间调用的资源相关接口")
public class ResourceFeignController {

    private final ResourceService resourceService;

    /**
     * 根据ID获取资源信息（内部调用）
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取资源信息", description = "根据ID获取资源信息（内部调用）")
    public Result<ResourceDTO> getResourceById(
            @Parameter(description = "资源ID") @PathVariable Long id) {

        try {
            log.debug("🔍 Feign调用获取资源信息 - 资源ID: {}", id);
            ResourceDTO resource = resourceService.getResourceById(id);
            
            if (resource == null) {
                log.warn("⚠️ 资源不存在 - 资源ID: {}", id);
                return Result.error("资源不存在");
            }
            
            return Result.success(resource);
        } catch (Exception e) {
            log.error("❌ Feign调用获取资源信息失败 - 资源ID: {}, 错误: {}", id, e.getMessage(), e);
            return Result.error("获取资源信息失败: " + e.getMessage());
        }
    }

    /**
     * 批量获取资源信息（内部调用）
     */
    @PostMapping("/batch")
    @Operation(summary = "批量获取资源信息", description = "批量获取资源信息（内部调用）")
    public Result<List<ResourceDTO>> getResourcesByIds(
            @Parameter(description = "资源ID列表") @RequestBody List<Long> ids) {

        try {
            log.debug("🔍 Feign调用批量获取资源信息 - 数量: {}", ids.size());
            List<ResourceDTO> resources = resourceService.getResourcesByIds(ids);
            
            return Result.success("获取成功", resources);
        } catch (Exception e) {
            log.error("❌ Feign调用批量获取资源信息失败 - 错误: {}", e.getMessage(), e);
            return Result.error("批量获取资源信息失败: " + e.getMessage());
        }
    }
}
```

---

## 服务层架构标准

### 服务接口定义

```java
public interface ResourceService {
    
    // 基础CRUD操作
    ResourceDTO createResource(ResourceDTO resourceDTO);
    boolean updateResource(ResourceDTO resourceDTO);
    ResourceDTO getResourceById(Long id);
    boolean deleteResource(Long id);
    
    // 批量操作
    List<ResourceDTO> getResourcesByIds(Collection<Long> ids);
    boolean deleteResourcesByIds(Collection<Long> ids);
    
    // 分页查询
    PageResult<ResourceVO> pageQuery(ResourcePageDTO pageDTO);
    
    // 业务操作
    // 根据实际业务需求定义
}
```

### 服务实现规范

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceServiceImpl extends ServiceImpl<ResourceMapper, Resource> 
        implements ResourceService {

    private final ResourceMapper resourceMapper;
    private final ResourceConverter resourceConverter;
    private final AsyncLogProducer asyncLogProducer;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CachePut(
            cacheNames = "resourceCache",
            key = "#result.id",
            unless = "#result == null"
    )
    public ResourceDTO createResource(ResourceDTO resourceDTO) {
        if (resourceDTO == null) {
            throw new IllegalArgumentException("资源信息不能为空");
        }

        try {
            log.info("创建资源，名称: {}", resourceDTO.getName());
            Resource resource = resourceConverter.toEntity(resourceDTO);
            boolean saved = save(resource);
            
            if (saved) {
                log.info("资源创建成功，ID: {}", resource.getId());
                
                // 发送异步日志
                sendBusinessLog("CREATE", resource.getId(), "创建资源");
                
                return resourceConverter.toDTO(resource);
            } else {
                throw new BusinessException("创建资源失败");
            }
        } catch (Exception e) {
            log.error("创建资源异常", e);
            throw new BusinessException("创建资源失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = "resourceCache",
            key = "#id",
            unless = "#result == null"
    )
    public ResourceDTO getResourceById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("资源ID不能为空");
        }

        try {
            log.debug("根据ID查找资源: {}", id);
            Resource resource = getById(id);
            if (resource == null) {
                throw EntityNotFoundException.resource(id);
            }
            return resourceConverter.toDTO(resource);
        } catch (Exception e) {
            log.error("根据ID查找资源失败，资源ID: {}", id, e);
            throw new BusinessException("获取资源信息失败", e);
        }
    }

    @Override
    @PreAuthorize("@permissionManager.hasAdminAccess(authentication)")
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(
            cacheNames = "resourceCache",
            key = "#id"
    )
    public boolean deleteResource(Long id) {
        log.info("删除资源，ID：{}", id);

        try {
            Resource resource = getById(id);
            if (resource == null) {
                throw EntityNotFoundException.resource(id);
            }

            boolean result = removeById(id);
            
            if (result) {
                // 发送异步日志
                sendBusinessLog("DELETE", id, "删除资源");
            }

            log.info("资源删除成功，ID：{}", id);
            return result;
        } catch (Exception e) {
            log.error("删除资源失败，ID：{}", id, e);
            throw new BusinessException("删除资源失败", e);
        }
    }

    /**
     * 发送业务日志
     */
    private void sendBusinessLog(String action, Long resourceId, String description) {
        try {
            asyncLogProducer.sendBusinessLogAsync(
                    "resource-service",
                    "RESOURCE_MANAGEMENT",
                    action,
                    description,
                    resourceId.toString(),
                    "RESOURCE",
                    null,
                    null,
                    UserContextUtils.getCurrentUsername() != null ? 
                        UserContextUtils.getCurrentUsername() : "SYSTEM",
                    description
            );
        } catch (Exception e) {
            log.warn("发送业务日志失败", e);
        }
    }
}
```

---

## 数据访问层标准

### Mapper接口定义

```java
@Mapper
public interface ResourceMapper extends BaseMapper<Resource> {
    
    /**
     * 自定义查询方法
     */
    List<Resource> selectByCondition(@Param("condition") ResourceQueryCondition condition);
    
    /**
     * 统计方法
     */
    Long countByStatus(@Param("status") Integer status);
    
    /**
     * 批量更新方法
     */
    int batchUpdateStatus(@Param("ids") Collection<Long> ids, @Param("status") Integer status);
}
```

### XML映射文件规范

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" 
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.cloud.resource.mapper.ResourceMapper">

    <!-- 结果映射 -->
    <resultMap id="BaseResultMap" type="com.cloud.resource.module.entity.Resource">
        <id column="id" property="id" jdbcType="BIGINT"/>
        <result column="name" property="name" jdbcType="VARCHAR"/>
        <result column="status" property="status" jdbcType="INTEGER"/>
        <result column="created_at" property="createdAt" jdbcType="TIMESTAMP"/>
        <result column="updated_at" property="updatedAt" jdbcType="TIMESTAMP"/>
    </resultMap>

    <!-- 基础列 -->
    <sql id="Base_Column_List">
        id, name, status, created_at, updated_at
    </sql>

    <!-- 自定义查询 -->
    <select id="selectByCondition" resultMap="BaseResultMap">
        SELECT <include refid="Base_Column_List"/>
        FROM resource
        <where>
            <if test="condition.name != null and condition.name != ''">
                AND name LIKE CONCAT('%', #{condition.name}, '%')
            </if>
            <if test="condition.status != null">
                AND status = #{condition.status}
            </if>
        </where>
        ORDER BY created_at DESC
    </select>

</mapper>
```

---

## 权限控制标准

### 角色定义
- **ROLE_ADMIN**: 管理员，拥有所有权限
- **ROLE_USER**: 普通用户，拥有基本权限
- **ROLE_MERCHANT**: 商家，拥有商家相关权限

### 权限注解使用

```java
// 1. 只有管理员可以访问
@PreAuthorize("hasRole('ADMIN')")

// 2. 管理员或商家可以访问
@PreAuthorize("hasRole('ADMIN') or hasRole('MERCHANT')")

// 3. 管理员且拥有写权限
@PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')")

// 4. 管理员或资源所有者可以访问
@PreAuthorize("hasRole('ADMIN') or @permissionManager.isResourceOwner(#id, authentication)")

// 5. 自定义权限检查
@PreAuthorize("@permissionManager.hasResourceAccess(#id, authentication)")
```

### 权限管理器示例

```java
@Component
@RequiredArgsConstructor
public class PermissionManager {
    
    private final ResourceService resourceService;
    
    /**
     * 检查用户是否是管理员
     */
    public boolean hasAdminAccess(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(auth -> "ROLE_ADMIN".equals(auth.getAuthority()));
    }
    
    /**
     * 检查用户是否是资源所有者
     */
    public boolean isResourceOwner(Long resourceId, Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        
        ResourceDTO resource = resourceService.getResourceById(resourceId);
        if (resource == null) {
            return false;
        }
        
        String username = authentication.getName();
        return username.equals(resource.getOwnerUsername());
    }
    
    /**
     * 检查用户是否有商家权限
     */
    public boolean hasMerchantAccess(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(auth -> "ROLE_MERCHANT".equals(auth.getAuthority()) 
                        || "ROLE_ADMIN".equals(auth.getAuthority()));
    }
}
```

---

## 缓存策略标准

### 缓存注解使用

```java
// 1. 缓存查询结果
@Cacheable(
        cacheNames = "resourceCache",
        key = "#id",
        unless = "#result == null"
)
public ResourceDTO getResourceById(Long id) {
    // ...
}

// 2. 更新缓存
@CachePut(
        cacheNames = "resourceCache",
        key = "#result.id",
        unless = "#result == null"
)
public ResourceDTO createResource(ResourceDTO resourceDTO) {
    // ...
}

// 3. 删除缓存
@CacheEvict(
        cacheNames = "resourceCache",
        key = "#id"
)
public boolean deleteResource(Long id) {
    // ...
}

// 4. 批量删除缓存
@CacheEvict(
        cacheNames = "resourceCache",
        allEntries = true,
        condition = "#ids != null && !#ids.isEmpty()"
)
public boolean deleteResourcesByIds(Collection<Long> ids) {
    // ...
}

// 5. 条件缓存（批量查询）
@Cacheable(
        cacheNames = "resourceCache",
        key = "'batch:' + #ids.toString()",
        condition = "#ids != null && #ids.size() <= 100",
        unless = "#result == null || #result.isEmpty()"
)
public List<ResourceDTO> getResourcesByIds(Collection<Long> ids) {
    // ...
}
```

### 缓存配置

```yaml
spring:
  cache:
    type: redis
    redis:
      time-to-live: 3600000  # 1小时
      cache-null-values: false
      use-key-prefix: true
      key-prefix: "cloud:"
```

---

## 分布式锁使用标准

### 分布式锁注解

```java
@DistributedLock(
        key = "'resource:operation:' + #resourceId",
        waitTime = 5,
        leaseTime = 30,
        timeUnit = TimeUnit.SECONDS,
        failMessage = "操作获取锁失败，请稍后重试"
)
```

### 锁类型选择

1. **REENTRANT** (可重入锁) - 默认类型
   - 用途: 一般业务操作
   - 特点: 同一线程可重复获取

2. **FAIR** (公平锁)
   - 用途: 秒杀、抢购等需要公平性的场景
   - 特点: 按请求顺序获取锁

3. **READ** (读锁)
   - 用途: 读多写少的场景
   - 特点: 多个读锁可以并发

4. **WRITE** (写锁)
   - 用途: 需要排他访问的写操作
   - 特点: 与其他所有锁互斥

### 失败策略

1. **FAIL_FAST** (快速失败)
   - 用途: 对延迟敏感的操作
   - 行为: 立即返回失败

2. **RETURN_NULL** (返回空)
   - 用途: 可选的查询操作
   - 行为: 返回null而不是抛异常

3. **THROW_EXCEPTION** (抛出异常) - 默认策略
   - 用途: 重要业务操作
   - 行为: 抛出LockAcquireException

### 使用示例

```java
// 1. 普通业务操作
@DistributedLock(
        key = "'resource:update:' + #id",
        waitTime = 5,
        leaseTime = 15,
        timeUnit = TimeUnit.SECONDS
)
public boolean updateResource(Long id, ResourceDTO resourceDTO) {
    // ...
}

// 2. 秒杀场景（公平锁 + 快速失败）
@DistributedLock(
        key = "'seckill:' + #productId",
        lockType = DistributedLock.LockType.FAIR,
        waitTime = 1,
        leaseTime = 3,
        timeUnit = TimeUnit.SECONDS,
        failStrategy = DistributedLock.LockFailStrategy.FAIL_FAST,
        failMessage = "秒杀商品已售罄或系统繁忙"
)
public boolean seckillProduct(Long productId) {
    // ...
}

// 3. 批量操作（写锁）
@DistributedLock(
        key = "'batch:update:' + T(String).join(',', #ids)",
        lockType = DistributedLock.LockType.WRITE,
        waitTime = 10,
        leaseTime = 30,
        timeUnit = TimeUnit.SECONDS
)
public boolean batchUpdate(List<Long> ids) {
    // ...
}
```

---

## 异常处理标准

### 异常类层次结构

```
RuntimeException
└── BusinessException (业务异常基类)
    ├── EntityNotFoundException (实体未找到)
    ├── DuplicateEntityException (实体重复)
    ├── ValidationException (验证异常)
    ├── AuthenticationException (认证异常)
    ├── AuthorizationException (授权异常)
    └── ServiceException (服务异常)
```

### 全局异常处理器

```java
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.error(e.getMessage());
    }

    /**
     * 实体未找到异常
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public Result<?> handleEntityNotFoundException(EntityNotFoundException e) {
        log.warn("实体未找到: {}", e.getMessage());
        return Result.error(e.getMessage());
    }

    /**
     * 参数验证异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("参数验证失败: {}", message);
        return Result.error("参数验证失败: " + message);
    }

    /**
     * 权限异常
     */
    @ExceptionHandler(AccessDeniedException.class)
    public Result<?> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("权限不足: {}", e.getMessage());
        return Result.error("权限不足");
    }

    /**
     * 系统异常
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.error("系统异常，请联系管理员");
    }
}
```

---

## 日志记录标准

### 日志级别使用

- **ERROR**: 错误信息，需要立即处理
- **WARN**: 警告信息，需要关注
- **INFO**: 重要业务信息
- **DEBUG**: 调试信息
- **TRACE**: 详细追踪信息

### 日志记录规范

```java
// 1. 业务操作入口
log.info("📝 创建资源 - 名称: {}, 用户: {}", resourceDTO.getName(), username);

// 2. 业务操作成功
log.info("✅ 资源创建成功 - ID: {}, 名称: {}", resource.getId(), resource.getName());

// 3. 业务操作失败
log.warn("⚠️ 资源创建失败 - 名称: {}, 原因: {}", resourceDTO.getName(), reason);

// 4. 系统错误
log.error("❌ 资源创建异常 - 名称: {}, 错误: {}", resourceDTO.getName(), e.getMessage(), e);

// 5. Feign调用
log.debug("🔍 Feign调用获取资源 - ID: {}", resourceId);

// 6. 分布式锁
log.info("🔒 获取分布式锁 - 资源ID: {}", resourceId);

// 7. 缓存操作
log.debug("💾 从缓存获取资源 - ID: {}", resourceId);
```

### 异步业务日志

```java
asyncLogProducer.sendBusinessLogAsync(
        "resource-service",           // 服务名
        "RESOURCE_MANAGEMENT",        // 模块名
        "CREATE",                     // 操作类型
        "创建资源",                    // 操作描述
        resourceId.toString(),        // 操作对象ID
        "RESOURCE",                   // 对象类型
        null,                         // 操作前数据
        resourceDTO.toString(),       // 操作后数据
        username,                     // 操作人
        "创建新资源"                   // 详细说明
);
```

---

## API设计标准

### RESTful URL设计

```
# 资源操作
GET    /resources          # 获取资源列表（带分页）
POST   /resources/page     # 分页查询（复杂条件）
GET    /resources/{id}     # 获取单个资源
POST   /resources          # 创建资源
PUT    /resources/{id}     # 更新资源
DELETE /resources/{id}     # 删除资源
DELETE /resources          # 批量删除（通过查询参数ids）

# 子资源操作
GET    /resources/{id}/items          # 获取资源的子项
POST   /resources/{id}/items          # 添加子项
DELETE /resources/{id}/items/{itemId} # 删除子项

# 业务操作
POST   /resources/{id}/publish        # 发布资源
POST   /resources/{id}/archive        # 归档资源
POST   /resources/{id}/restore        # 恢复资源

# Feign内部接口
GET    /feign/resources/{id}          # 内部获取资源
POST   /feign/resources/batch         # 内部批量获取
```

### HTTP状态码使用

- **200 OK**: 成功
- **201 Created**: 创建成功
- **204 No Content**: 删除成功
- **400 Bad Request**: 请求参数错误
- **401 Unauthorized**: 未认证
- **403 Forbidden**: 无权限
- **404 Not Found**: 资源不存在
- **500 Internal Server Error**: 服务器错误

### 统一响应格式

```java
public class Result<T> {
    private Integer code;      // 状态码
    private String message;    // 消息
    private T data;           // 数据
    private Long timestamp;   // 时间戳
    
    // 成功响应
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data);
    }
    
    // 失败响应
    public static <T> Result<T> error(String message) {
        return new Result<>(500, message, null);
    }
}
```

---

## 测试标准

### 单元测试

```java
@SpringBootTest
@AutoConfigureMockMvc
class ResourceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ResourceService resourceService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateResource() throws Exception {
        ResourceDTO resourceDTO = new ResourceDTO();
        resourceDTO.setName("Test Resource");

        when(resourceService.createResource(any())).thenReturn(resourceDTO);

        mockMvc.perform(post("/resources")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(resourceDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
```

### 集成测试

```java
@SpringBootTest
@Transactional
class ResourceServiceIntegrationTest {

    @Autowired
    private ResourceService resourceService;

    @Test
    void testResourceLifecycle() {
        // 创建
        ResourceDTO created = resourceService.createResource(resourceDTO);
        assertNotNull(created.getId());

        // 查询
        ResourceDTO found = resourceService.getResourceById(created.getId());
        assertEquals(created.getName(), found.getName());

        // 更新
        found.setName("Updated Name");
        resourceService.updateResource(found);

        // 删除
        resourceService.deleteResource(found.getId());
    }
}
```

---

## 监控和性能优化

### 监控指标

1. **API性能指标**
   - 响应时间
   - 请求成功率
   - 并发量

2. **业务指标**
   - 资源创建数
   - 操作失败率
   - 分布式锁获取成功率

3. **系统指标**
   - CPU使用率
   - 内存使用率
   - 线程池状态

### 性能优化建议

1. **数据库优化**
   - 合理使用索引
   - 避免N+1查询
   - 使用批量操作

2. **缓存优化**
   - 热点数据缓存
   - 合理设置过期时间
   - 避免缓存穿透

3. **并发优化**
   - 使用分布式锁保护关键资源
   - 异步处理非关键业务
   - 合理配置线程池

4. **代码优化**
   - 避免在循环中执行数据库操作
   - 使用Stream API优化集合操作
   - 合理使用对象池

---

## 附录: 完整项目结构

```
service-name/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/cloud/service/
│   │   │       ├── controller/
│   │   │       │   ├── ServiceController.java
│   │   │       │   └── ServiceFeignController.java
│   │   │       ├── service/
│   │   │       │   ├── ServiceService.java
│   │   │       │   └── impl/
│   │   │       │       └── ServiceServiceImpl.java
│   │   │       ├── mapper/
│   │   │       │   └── ServiceMapper.java
│   │   │       ├── module/
│   │   │       │   ├── entity/
│   │   │       │   ├── dto/
│   │   │       │   └── vo/
│   │   │       ├── converter/
│   │   │       ├── exception/
│   │   │       ├── config/
│   │   │       └── ServiceApplication.java
│   │   └── resources/
│   │       ├── mapper/
│   │       ├── application.yml
│   │       └── application-dev.yml
│   └── test/
│       └── java/
│           └── com/cloud/service/
│               ├── controller/
│               └── service/
├── pom.xml
└── README.md
```

---

**文档版本**: v1.0.0
**最后更新**: 2025-10-01
**维护人员**: Architecture Team
