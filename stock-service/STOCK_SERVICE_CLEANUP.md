# Stock服务代码清理和重构指导文档

## 概述
Stock服务已完成重构，合并了5个冗余控制器为2个标准控制器，遵循微服务架构最佳实践。

## 🗑️ 需要删除的冗余文件

以下控制器文件已被合并到统一的控制器中，请删除：

```bash
# 删除冗余的控制器文件
rm stock-service/src/main/java/com/cloud/stock/controller/StockBusinessController.java
rm stock-service/src/main/java/com/cloud/stock/controller/StockManageController.java
rm stock-service/src/main/java/com/cloud/stock/controller/StockQueryController.java
rm stock-service/src/main/java/com/cloud/stock/controller/manage/StockManageController.java
rm stock-service/src/main/java/com/cloud/stock/controller/query/StockQueryController.java

# 删除相关的目录（如果为空）
rmdir stock-service/src/main/java/com/cloud/stock/controller/manage
rmdir stock-service/src/main/java/com/cloud/stock/controller/query
```

## ✨ 新的控制器结构

### 1. StockController (主要RESTful API控制器)
- **路径**: `stock-service/src/main/java/com/cloud/stock/controller/StockController.java`
- **基础路径**: `/stocks`
- **职责**: 提供对外的RESTful API接口

#### 核心功能
- **基础CRUD操作**
  - `POST /stocks/page` - 分页查询库存
  - `GET /stocks/{id}` - 根据ID获取库存详情
  - `GET /stocks/product/{productId}` - 根据商品ID获取库存信息
  - `POST /stocks/batch` - 批量获取库存信息
  - `POST /stocks` - 创建库存记录
  - `PUT /stocks/{id}` - 更新库存信息
  - `DELETE /stocks/{id}` - 删除库存信息
  - `DELETE /stocks` - 批量删除库存信息

- **库存业务操作**
  - `POST /stocks/stock-in` - 库存入库（含分布式锁）
  - `POST /stocks/stock-out` - 库存出库（含分布式锁）
  - `POST /stocks/reserve` - 预留库存（含分布式锁）
  - `POST /stocks/release` - 释放预留库存（含分布式锁）
  - `GET /stocks/check/{productId}/{quantity}` - 检查库存是否充足

- **高级功能**
  - `POST /stocks/seckill/{productId}` - 秒杀库存扣减（公平锁）

### 2. StockFeignController (内部微服务调用控制器)
- **路径**: `stock-service/src/main/java/com/cloud/stock/controller/StockFeignController.java`
- **基础路径**: `/feign/stocks`
- **职责**: 提供内部微服务间调用接口

#### 核心功能
- **内部查询**
  - `GET /feign/stocks/{stockId}` - 根据库存ID获取库存信息
  - `GET /feign/stocks/product/{productId}` - 根据商品ID获取库存信息
  - `POST /feign/stocks/batch` - 批量获取库存信息
  - `GET /feign/stocks/check/{productId}/{quantity}` - 检查库存是否充足

- **内部操作**
  - `POST /feign/stocks/deduct` - 库存扣减
  - `POST /feign/stocks/reserve` - 预留库存
  - `POST /feign/stocks/release` - 释放预留库存
  - `POST /feign/stocks/stock-in` - 库存入库

## 🔐 权限控制标准化

### 外部API权限
- **管理员权限**: `hasRole('ADMIN')`
- **管理员和商家权限**: `hasRole('ADMIN') or hasRole('MERCHANT')`
- **管理员写权限**: `hasRole('ADMIN') and hasAuthority('SCOPE_admin:write')`

### 内部API权限
- Feign控制器无权限检查（内部信任调用）
- 通过网络层面的安全策略保护

## 🔒 分布式锁配置

### 库存操作分布式锁
- **库存入库**: `stock:in:{productId}` (15秒)
- **库存出库**: `stock:out:{productId}` (15秒)
- **预留库存**: `stock:reserve:{productId}` (10秒)
- **释放库存**: `stock:release:{productId}` (10秒)
- **秒杀扣减**: `seckill:stock:{productId}` (3秒，公平锁，快速失败)

## 📊 API路径映射对比

### 原始路径 → 新路径映射

| 原控制器 | 原路径 | 新控制器 | 新路径 |
|---------|--------|----------|--------|
| StockBusinessController | `/api/v1/stock/business/*` | StockController | `/stocks/seckill/*`, 分布式锁操作 |
| StockManageController | `/stocks/*` | StockController | `/stocks/*` (CRUD操作) |
| StockQueryController | `/stocks/query/*` | StockController | `/stocks` (查询操作) |
| manage/StockManageController | `/stocks/manage/*` | StockController | `/stocks/*` (管理操作) |
| StockFeignController | 原Feign接口实现 | StockFeignController | `/feign/stocks/*` |

## 🎯 重构优势

### 1. 架构统一
- 遵循RESTful设计原则
- 统一的路径命名规范
- 标准化的响应格式

### 2. 代码简化
- 从5个控制器合并为2个
- 减少代码重复和维护成本
- 统一的异常处理和日志记录

### 3. 权限规范
- 统一的权限控制策略
- 清晰的内外部接口分离
- 标准化的安全注解

### 4. 性能优化
- 分布式锁避免库存超卖
- 公平锁确保秒杀公平性
- 详细的性能监控日志

### 5. 业务隔离
- 明确分离管理操作和查询操作
- 内外部接口清晰划分
- 高级功能（如秒杀）独立实现

## ⚠️ 注意事项

### 1. 数据库迁移
如果原有控制器使用了不同的数据访问逻辑，需要确保：
- StockService中包含所有必要的业务方法
- 数据库访问层与新控制器兼容
- 缓存策略正确配置

### 2. 调用方更新
需要通知以下服务更新调用路径：
- order-service（订单服务）
- product-service（商品服务）
- user-service（用户服务）
- 其他调用stock服务的微服务

### 3. 配置更新
- 更新API网关路由配置
- 更新服务监控和日志配置
- 更新API文档（Swagger）
- 更新Feign客户端接口定义

## 🧪 测试建议

### 1. 单元测试
```bash
# 运行控制器单元测试
mvn test -Dtest=*StockController*Test
```

### 2. 集成测试
```bash
# 运行集成测试
mvn test -Dtest=*StockIntegration*Test
```

### 3. 分布式锁测试
- 测试并发场景下的库存操作
- 验证秒杀场景的公平性
- 测试锁超时和失败策略

### 4. API测试
使用Postman或其他API测试工具验证：
- 所有新API端点正常工作
- 权限控制正确执行
- 分布式锁按预期工作
- Feign接口调用正常

## 📈 监控指标

### 关键监控点
- API响应时间
- 分布式锁获取成功率
- 库存操作成功率
- 秒杀并发处理能力
- 异常发生频率

### 建议告警规则
- API响应时间 > 3秒
- 分布式锁获取失败率 > 5%
- 库存操作失败率 > 3%
- 秒杀系统可用性 < 99%

## 🚀 特色功能

### 1. 秒杀优化
- 使用公平锁确保秒杀公平性
- 快速失败策略避免长时间等待
- 专门的秒杀接口优化性能

### 2. 业务操作保护
- 所有库存变更操作都有分布式锁保护
- 不同操作使用不同的锁超时时间
- 详细的操作日志和状态跟踪

### 3. 内部服务支持
- 完整的Feign接口支持
- 专门的内部调用接口
- 简化的调用流程和错误处理

---

**重构完成时间**: $(date)
**重构人员**: what's up
**版本**: v2.0.0 (统一架构版)
