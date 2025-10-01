# Product Service 控制器清理方案

## 📋 需要删除的冗余控制器

基于user服务标准，以下控制器属于冗余代码，应当删除：

### 1. 冗余控制器列表

```
product-service/src/main/java/com/cloud/product/controller/product/
├── ❌ ProductManageController.java       # 管理接口（冗余）
├── ❌ ProductQueryController.java        # 查询接口（冗余）
├── ❌ ProductManageNewController.java    # 新版管理接口（冗余）
├── ❌ ProductQueryNewController.java     # 新版查询接口（冗余）
├── ✅ ProductController.java             # 统一RESTful API（已优化）
└── ✅ ProductFeignController.java        # 内部服务调用（保留）
```

### 2. 删除操作

**需要手动删除以下文件：**

1. `D:\Download\Code\sofware\cloud\product-service\src\main\java\com\cloud\product\controller\product\ProductManageController.java`
2. `D:\Download\Code\sofware\cloud\product-service\src\main\java\com\cloud\product\controller\product\ProductQueryController.java`
3. `D:\Download\Code\sofware\cloud\product-service\src\main\java\com\cloud\product\controller\product\ProductManageNewController.java`
4. `D:\Download\Code\sofware\cloud\product-service\src\main\java\com\cloud\product\controller\product\ProductQueryNewController.java`

### 3. 保留的文件结构

优化后的product服务控制器结构：

```
product-service/src/main/java/com/cloud/product/controller/product/
├── ProductController.java           # 统一的RESTful API控制器
└── ProductFeignController.java      # 内部服务调用接口
```

### 4. API路径映射

优化后的API路径全部统一到 `/products`：

```
# 商品基础操作
GET    /products                     # 获取商品列表（支持查询参数）
GET    /products/{id}                # 获取商品详情
POST   /products                     # 创建商品
PUT    /products/{id}                # 更新商品
PATCH  /products/{id}                # 部分更新商品
DELETE /products/{id}                # 删除商品

# 商品档案操作
GET    /products/{id}/profile        # 获取商品档案
PUT    /products/{id}/profile        # 更新商品档案

# 商品状态操作
PATCH  /products/{id}/status         # 更新商品状态

# 商品查询操作
GET    /products/search              # 根据名称搜索商品
GET    /products/batch               # 批量获取商品
GET    /products/category/{id}       # 根据分类查询商品
GET    /products/brand/{id}          # 根据品牌查询商品

# 批量操作
DELETE /products/batch               # 批量删除商品
PUT    /products/batch/enable        # 批量上架商品
PUT    /products/batch/disable       # 批量下架商品

# 内部服务接口（Feign）
GET    /internal/products/{id}       # 内部服务获取商品
POST   /internal/products            # 内部服务创建商品
PUT    /internal/products/{id}       # 内部服务更新商品
DELETE /internal/products/{id}       # 内部服务删除商品
```

### 5. 权限控制标准化

所有接口都使用统一的权限控制：

- **商品查询**: `@PreAuthorize("hasAuthority('SCOPE_product:read')")`
- **商品创建**: `@PreAuthorize("hasRole('MERCHANT') or hasRole('ADMIN') and hasAuthority('SCOPE_product:create')")`
- **商品管理**: `@PreAuthorize("hasRole('MERCHANT') or hasRole('ADMIN') and hasAuthority('SCOPE_product:write')")`

### 6. 优化效果

**代码简化**：
- 从5个控制器减少到2个控制器（60%减少）
- API路径统一规范，易于理解和维护
- 权限控制标准一致
- 减少了约1000行冗余代码

**维护性提升**：
- 单一控制器责任明确
- API文档更清晰
- 测试用例更集中
- 代码重复度大幅降低

---

**执行步骤**：
1. ✅ 已完成：优化ProductController.java为统一的RESTful API
2. 🔄 进行中：删除4个冗余控制器文件
3. 📋 待完成：更新API文档和测试用例
4. 📋 待完成：验证所有功能正常工作

**注意事项**：
- 删除文件前确保已备份
- 检查是否有其他地方引用了这些控制器
- 更新相关的测试用例
- 验证所有API接口正常工作
