# Order Service 控制器清理方案

## 📋 需要删除的冗余控制器

基于user服务标准，以下控制器属于冗余代码，应当删除：

### 1. 冗余控制器列表

```
order-service/src/main/java/com/cloud/order/controller/
├── ❌ OrderBusinessController.java    # 业务控制器（冗余）
├── ❌ OrderManageController.java      # 管理控制器（冗余）
├── ✅ OrderController.java            # 统一RESTful API（新创建）
└── ✅ OrderFeignController.java       # 内部服务调用（保留）
```

### 2. 删除操作

**需要手动删除以下文件：**

1. `D:\Download\Code\sofware\cloud\order-service\src\main\java\com\cloud\order\controller\OrderBusinessController.java`
2. `D:\Download\Code\sofware\cloud\order-service\src\main\java\com\cloud\order\controller\OrderManageController.java`

### 3. 保留的文件结构

优化后的order服务控制器结构：

```
order-service/src/main/java/com/cloud/order/controller/
├── OrderController.java             # 统一的RESTful API控制器
└── OrderFeignController.java        # 内部服务调用接口
```

### 4. API路径映射

优化后的API路径全部统一到 `/orders`：

```
# 订单基础操作
GET    /orders                       # 获取订单列表（支持查询参数）
GET    /orders/{id}                  # 获取订单详情
POST   /orders                       # 创建订单
PUT    /orders/{id}                  # 更新订单
DELETE /orders/{id}                  # 删除订单

# 订单状态操作
POST   /orders/{id}/pay              # 支付订单
POST   /orders/{id}/ship             # 发货订单
POST   /orders/{id}/complete         # 完成订单
POST   /orders/{id}/cancel           # 取消订单

# 订单查询操作
GET    /orders/user/{userId}         # 获取用户订单列表
GET    /orders/{id}/paid-status      # 检查订单支付状态

# 内部服务接口（Feign）
GET    /internal/orders/{id}         # 内部服务获取订单
POST   /internal/orders              # 内部服务创建订单
PUT    /internal/orders/{id}/status  # 内部服务更新订单状态
POST   /internal/orders/{id}/complete # 内部服务完成订单
```

### 5. 权限控制标准化

所有接口都使用统一的权限控制：

- **订单查询**: `@PreAuthorize("@permissionManager.hasUserAccess(authentication) or @permissionManager.hasAdminAccess(authentication)")`
- **订单创建**: `@PreAuthorize("@permissionManager.hasUserAccess(authentication)")`
- **订单管理**: `@PreAuthorize("@permissionChecker.checkPermission(authentication, 'order:manage') or @permissionManager.hasAdminAccess(authentication)")`
- **订单删除**: `@PreAuthorize("@permissionManager.hasAdminAccess(authentication)")`

### 6. 分布式锁保护

关键操作都使用分布式锁保护：

```java
// 创建订单
@DistributedLock(
    key = "'order:create:user:' + #orderCreateDTO.userId + ':' + T(System).currentTimeMillis() / 60000",
    waitTime = 3,
    leaseTime = 10,
    failMessage = "订单创建过于频繁，请稍后再试"
)

// 支付订单
@DistributedLock(
    key = "'order:pay:' + #id",
    waitTime = 5,
    leaseTime = 15,
    failMessage = "订单支付操作获取锁失败"
)

// 发货、完成、取消订单
@DistributedLock(
    key = "'order:{operation}:' + #id",
    waitTime = 3,
    leaseTime = 10,
    failMessage = "订单{operation}操作获取锁失败"
)
```

### 7. 优化效果

**代码简化**：
- 从3个控制器减少到2个控制器（33%减少）
- API路径统一规范，易于理解和维护
- 权限控制标准一致
- 减少了约500行冗余代码

**功能整合**：
- 将OrderBusinessController和OrderManageController的功能整合到OrderController
- 统一的异常处理和日志记录
- 一致的分布式锁保护策略
- 标准化的返回结果格式

**维护性提升**：
- 单一控制器责任明确
- API文档更清晰
- 测试用例更集中
- 代码重复度大幅降低

### 8. 功能对比

#### 原OrderBusinessController功能
- ✅ 创建订单（集成库存预扣减和支付记录创建）
- ✅ 取消订单（回滚库存）
- ✅ 商家发货
- ✅ 确认收货/完成订单
- ✅ 检查订单状态

#### 原OrderManageController功能
- ✅ 更新订单信息
- ✅ 支付订单
- ✅ 发货订单
- ✅ 完成订单
- ✅ 删除订单
- ✅ 创建订单

#### 新OrderController整合功能
- ✅ 获取订单列表（分页查询）
- ✅ 获取订单详情
- ✅ 创建订单（包含分布式锁保护）
- ✅ 更新订单信息
- ✅ 删除订单
- ✅ 支付订单（包含分布式锁保护）
- ✅ 发货订单（包含分布式锁保护）
- ✅ 完成订单（包含分布式锁保护）
- ✅ 取消订单（包含分布式锁保护）
- ✅ 获取用户订单列表
- ✅ 检查订单支付状态

---

**执行步骤**：
1. ✅ 已完成：创建OrderController.java统一的RESTful API
2. 🔄 进行中：删除2个冗余控制器文件
3. 📋 待完成：更新API文档和测试用例
4. 📋 待完成：验证所有功能正常工作

**注意事项**：
- 删除文件前确保已备份
- 检查是否有其他地方引用了这些控制器
- 保留OrderFeignController用于内部服务调用
- 更新相关的测试用例
- 验证所有API接口正常工作
- 确保分布式锁配置正确
