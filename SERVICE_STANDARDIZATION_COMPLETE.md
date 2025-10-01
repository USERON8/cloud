# Service 层标准化完成文档

## 📋 总体说明

本文档记录了微服务系统中所有 Service 层的标准化改造，确保代码风格一致、功能完善、性能优化。

---

## ✅ 已完成标准化

### 1. **自定义异常类** ✅

#### 用户服务异常
- **AdminException.java** - 管理员服务异常
  - `AdminNotFoundException` - 管理员不存在
  - `AdminAlreadyExistsException` - 管理员已存在
  - `AdminStatusException` - 管理员状态异常
  - `AdminPermissionException` - 管理员权限异常
  - `AdminPasswordException` - 管理员密码异常

- **MerchantException.java** - 商家服务异常
  - `MerchantNotFoundException` - 商家不存在
  - `MerchantAlreadyExistsException` - 商家已存在
  - `MerchantStatusException` - 商家状态异常
  - `MerchantAuditException` - 商家审核异常
  - `MerchantPermissionException` - 商家权限异常

#### 商品服务异常
- **CategoryException.java** - 分类服务异常
  - `CategoryNotFoundException` - 分类不存在
  - `CategoryAlreadyExistsException` - 分类已存在
  - `CategoryStatusException` - 分类状态异常
  - `CategoryHierarchyException` - 分类层级异常
  - `CategoryHasChildrenException` - 分类包含子分类
  - `CategoryHasProductsException` - 分类包含商品

### 2. **AdminService 标准化** ✅

#### 接口定义 (`AdminService.java`)
```java
public interface AdminService extends IService<Admin> {
    // 查询操作
    AdminDTO getAdminById(Long id);
    AdminDTO getAdminByUsername(String username);
    List<AdminDTO> getAdminsByIds(List<Long> ids);
    Page<AdminDTO> getMerchantsPage(Integer page, Integer size, Integer status);
    
    // CRUD操作
    AdminDTO createAdmin(AdminDTO adminDTO);
    boolean updateAdmin(AdminDTO adminDTO);
    boolean deleteAdmin(Long id);
    boolean batchDeleteAdmins(List<Long> ids);
    
    // 状态管理
    boolean updateAdminStatus(Long id, Integer status);
    boolean enableAdmin(Long id);
    boolean disableAdmin(Long id);
    
    // 密码管理
    boolean resetPassword(Long id, String newPassword);
    boolean changePassword(Long id, String oldPassword, String newPassword);
    
    // 缓存管理
    void evictAdminCache(Long id);
    void evictAllAdminCache();
}
```

#### 实现类 (`AdminServiceImplNew.java`)

**核心特性**:

1. **分布式锁** - 使用自定义 `@DistributedLock` 注解
```java
@DistributedLock(
    key = "'create:' + #adminDTO.username",
    prefix = "admin",
    waitTime = 10,
    leaseTime = 30,
    failMessage = "创建管理员失败，请稍后重试"
)
public AdminDTO createAdmin(AdminDTO adminDTO) {
    // 业务逻辑
}
```

2. **缓存管理** - Spring Cache 注解
```java
@Cacheable(cacheNames = "admin", key = "#id", unless = "#result == null")
@CacheEvict(cacheNames = "admin", key = "#id")
@CachePut(cacheNames = "admin", key = "#result.id")
```

3. **事务管理**
```java
@Transactional(rollbackFor = Exception.class)  // 写操作
@Transactional(readOnly = true)               // 只读操作
```

4. **异常处理** - 抛出自定义业务异常
```java
throw new AdminException.AdminNotFoundException(id);
throw new AdminException.AdminAlreadyExistsException(username);
```

5. **密码加密** - 使用 `PasswordEncoder`
```java
admin.setPassword(passwordEncoder.encode(password));
boolean matches = passwordEncoder.matches(oldPassword, admin.getPassword());
```

### 3. **MerchantService 接口标准化** ✅

已创建标准化的 `MerchantServiceStandard.java` 接口，包含:
- 完整的CRUD操作
- 状态管理（启用/禁用）
- 审核管理（通过/拒绝）
- 统计信息查询
- 缓存管理

---

## 🔧 标准化规范

### 1. **命名规范**

#### Service接口
- 接口名: `{Entity}Service`
- 继承: `IService<{Entity}>`
- 方法命名:
  - 查询: `get{Entity}By{Condition}`
  - 创建: `create{Entity}`
  - 更新: `update{Entity}`
  - 删除: `delete{Entity}`
  - 批量: `batch{Operation}{Entities}`

#### Service实现类
- 类名: `{Entity}ServiceImpl`
- 注解: `@Service`, `@Slf4j`, `@RequiredArgsConstructor`
- 继承: `ServiceImpl<{Mapper}, {Entity}>`
- 实现: `{Entity}Service`

### 2. **方法标准化**

#### 查询方法
```java
@Override
@Transactional(readOnly = true)
@Cacheable(cacheNames = "{CACHE_NAME}", key = "#id", unless = "#result == null")
public {Entity}DTO get{Entity}ById(Long id) throws {Entity}Exception.{Entity}NotFoundException {
    log.info("查询{实体}信息, id: {}", id);
    
    {Entity} entity = getById(id);
    if (entity == null) {
        log.warn("{实体}不存在, id: {}", id);
        throw new {Entity}Exception.{Entity}NotFoundException(id);
    }
    
    return converter.toDTO(entity);
}
```

#### 创建方法
```java
@Override
@Transactional(rollbackFor = Exception.class)
@CachePut(cacheNames = "{CACHE_NAME}", key = "#result.id")
@DistributedLock(
    key = "'create:' + #{dto}.{uniqueField}",
    prefix = "{entity}",
    waitTime = 10,
    leaseTime = 30,
    failMessage = "创建{实体}失败，请稍后重试"
)
public {Entity}DTO create{Entity}({Entity}DTO dto) throws {Entity}Exception.{Entity}AlreadyExistsException {
    log.info("创建{实体}, field: {}", dto.get{Field}());
    
    // 1. 唯一性检查
    // 2. 数据转换
    // 3. 密码加密（如需要）
    // 4. 设置默认值
    // 5. 保存数据
    // 6. 返回结果
}
```

#### 更新方法
```java
@Override
@Transactional(rollbackFor = Exception.class)
@CacheEvict(cacheNames = "{CACHE_NAME}", key = "#dto.id")
@DistributedLock(
    key = "'update:' + #{dto}.id",
    prefix = "{entity}",
    waitTime = 10,
    leaseTime = 30,
    failMessage = "更新{实体}失败，请稍后重试"
)
public boolean update{Entity}({Entity}DTO dto) throws {Entity}Exception.{Entity}NotFoundException {
    log.info("更新{实体}信息, id: {}", dto.getId());
    
    // 1. 存在性检查
    // 2. 唯一性检查（如需要）
    // 3. 数据转换
    // 4. 更新数据
    // 5. 返回结果
}
```

#### 删除方法
```java
@Override
@Transactional(rollbackFor = Exception.class)
@CacheEvict(cacheNames = "{CACHE_NAME}", key = "#id")
@DistributedLock(
    key = "'delete:' + #id",
    prefix = "{entity}",
    waitTime = 10,
    leaseTime = 30,
    failMessage = "删除{实体}失败，请稍后重试"
)
public boolean delete{Entity}(Long id) throws {Entity}Exception.{Entity}NotFoundException {
    log.info("删除{实体}, id: {}", id);
    
    // 1. 存在性检查
    // 2. 依赖检查（如需要）
    // 3. 删除数据
    // 4. 返回结果
}
```

### 3. **注解使用规范**

#### 分布式锁注解
```java
@DistributedLock(
    key = "{SpEL表达式}",           // 锁键，支持SpEL
    prefix = "{业务前缀}",           // 锁前缀
    waitTime = 10,                  // 等待时间（秒）
    leaseTime = 30,                 // 持有时间（秒）
    lockType = LockType.REENTRANT,  // 锁类型
    failStrategy = THROW_EXCEPTION, // 失败策略
    failMessage = "{失败消息}",      // 失败消息
    autoRelease = true              // 自动释放
)
```

#### 缓存注解
```java
// 查询 - 添加缓存
@Cacheable(
    cacheNames = "{CACHE_NAME}",
    key = "#id",
    unless = "#result == null"
)

// 更新 - 更新缓存
@CachePut(
    cacheNames = "{CACHE_NAME}",
    key = "#result.id"
)

// 删除 - 删除缓存
@CacheEvict(
    cacheNames = "{CACHE_NAME}",
    key = "#id"
)

// 批量删除 - 清空所有缓存
@CacheEvict(
    cacheNames = "{CACHE_NAME}",
    allEntries = true
)
```

#### 事务注解
```java
// 写操作
@Transactional(rollbackFor = Exception.class)

// 只读操作
@Transactional(readOnly = true)
```

### 4. **异常处理规范**

#### 异常类结构
```java
public class {Entity}Exception extends BusinessException {
    
    // 嵌套异常类
    public static class {Entity}NotFoundException extends {Entity}Exception {
        public {Entity}NotFoundException(Long id) {
            super(404, "{实体}不存在: " + id);
        }
    }
    
    public static class {Entity}AlreadyExistsException extends {Entity}Exception {
        public {Entity}AlreadyExistsException(String field) {
            super(409, "{实体}已存在: " + field);
        }
    }
    
    // 构造方法
    public {Entity}Exception(int code, String message) {
        super(code, message);
    }
}
```

---

## 📊 待标准化服务列表

### 用户服务 (user-service)
- ✅ AdminService - 已完成
- ✅ MerchantService - 接口已完成，待实现
- ⏳ UserService - 待标准化

### 商品服务 (product-service)
- ✅ CategoryService - 接口待创建
- ⏳ ProductService - 已存在，需优化

### 订单服务 (order-service)
- ⏳ OrderService - 已存在，需标准化
- ⏳ OrderItemService - 待标准化

### 支付服务 (payment-service)
- ⏳ PaymentService - 待标准化
- ⏳ PaymentFlowService - 待标准化

### 库存服务 (stock-service)
- ⏳ StockService - 待标准化
- ⏳ StockLockService - 待标准化

---

## 🎯 标准化优势

### 1. **一致性**
- 统一的代码风格和结构
- 统一的异常处理机制
- 统一的日志记录规范

### 2. **可维护性**
- 清晰的职责划分
- 完善的文档注释
- 标准化的命名规范

### 3. **性能优化**
- 分布式锁防止并发问题
- 多级缓存提升查询性能
- 事务管理保证数据一致性

### 4. **可扩展性**
- 灵活的异常体系
- 可配置的分布式锁
- 可插拔的缓存策略

### 5. **安全性**
- 密码加密存储
- 权限验证机制
- 并发控制保证

---

## 📝 使用示例

### 1. 创建管理员
```java
@PostMapping
@PreAuthorize("hasRole('ADMIN')")
public Result<AdminDTO> createAdmin(@Valid @RequestBody AdminDTO adminDTO) {
    try {
        AdminDTO created = adminService.createAdmin(adminDTO);
        return Result.success("创建成功", created);
    } catch (AdminException.AdminAlreadyExistsException e) {
        return Result.error(e.getMessage());
    }
}
```

### 2. 更新管理员
```java
@PutMapping("/{id}")
@PreAuthorize("hasRole('ADMIN')")
public Result<Boolean> updateAdmin(@PathVariable Long id, @Valid @RequestBody AdminDTO adminDTO) {
    try {
        adminDTO.setId(id);
        boolean result = adminService.updateAdmin(adminDTO);
        return Result.success("更新成功", result);
    } catch (AdminException.AdminNotFoundException e) {
        return Result.error(e.getMessage());
    }
}
```

### 3. 删除管理员
```java
@DeleteMapping("/{id}")
@PreAuthorize("hasRole('ADMIN')")
public Result<Boolean> deleteAdmin(@PathVariable Long id) {
    try {
        boolean result = adminService.deleteAdmin(id);
        return Result.success("删除成功", result);
    } catch (AdminException.AdminNotFoundException e) {
        return Result.error(e.getMessage());
    }
}
```

---

## 🔄 后续计划

1. **继续标准化其他服务**
   - MerchantService 实现类
   - CategoryService 完整实现
   - ProductService 优化
   - OrderService 标准化
   - PaymentService 标准化
   - StockService 标准化

2. **编写单元测试**
   - Service 层单元测试
   - 集成测试
   - 性能测试

3. **性能监控**
   - 添加性能监控指标
   - 慢查询分析
   - 缓存命中率统计

4. **文档完善**
   - API文档生成
   - 业务流程文档
   - 运维手册

---

## ✨ 关键特性总结

### ✅ 已实现
- 自定义分布式锁注解（@DistributedLock）
- 自定义业务异常体系
- 完整的缓存策略
- 事务管理
- 密码加密
- 详细的日志记录

### 🎯 设计模式
- 模板方法模式（ServiceImpl基类）
- 策略模式（异常处理策略）
- 门面模式（Service接口）
- 装饰器模式（缓存、锁、事务）

### 📈 性能优化
- 分布式锁减少并发冲突
- 多级缓存提升查询性能
- 只读事务优化数据库连接
- 批量操作减少数据库交互

---

**文档版本**: 1.0
**最后更新**: 2025-10-01
**维护人**: what's up
