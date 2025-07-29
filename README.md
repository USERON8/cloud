### 模块说明

1. **auth** - 认证授权服务
   - 提供用户认证和权限验证功能
   - 基于Spring Security实现

2. **common** - 公共模块
   - 包含通用配置、工具类、异常处理等
   - 定义统一返回结果格式
   - 提供基础实体类和枚举

3. **gateway** - 网关服务
   - 基于Spring Cloud Gateway实现
   - 路由转发、负载均衡
   - 统一入口和安全控制

4. **stock** - 库存服务
   - 核心业务模块，提供库存管理功能
   - 支持同步和异步接口调用
   - 包含库存查询、分页、批量操作等

5. **frontend** - 前端应用
   - 基于Vue3的单页面应用
   - 使用Element Plus组件库

## 核心功能

### 库存管理功能
1. 商品库存查询（同步/异步）
2. 库存分页查询
3. 批量库存查询
4. 库存统计信息查询
5. 高并发场景下的并发查询

### 异步处理特性
- 使用CompletableFuture实现异步调用
- 支持超时控制
- 提供异常处理机制
- 支持批量任务并发执行

## 部署架构

项目采用Docker容器化部署，包含以下服务：

- MySQL: 数据库服务
- Redis: 缓存服务
- Nacos: 服务注册与配置中心
- RocketMQ: 消息队列服务
- Nginx: 反向代理服务器

## 快速开始

### 环境要求
- JDK 17+
- Maven 3.x
- Docker & Docker Compose
- Node.js 16+

### 启动步骤


### 库存服务接口

#### 同步接口
- `GET /stock/product/{productId}` - 根据商品ID查询库存
- `POST /stock/page` - 分页查询库存

#### 异步接口
- `GET /stock/async/product/{productId}` - 异步根据商品ID查询库存
- `POST /stock/async/page` - 异步分页查询库存
- `POST /stock/async/batch` - 异步批量查询库存
- `GET /stock/async/statistics` - 异步查询库存统计信息
- `POST /stock/async/concurrent` - 并发查询多个商品库存

## 数据库设计

系统包含一个核心数据表：`tb_stock`，存储商品库存信息：

- [id](file://D:\Download\Code\sofware\cloud\common\src\main\java\domain\BaseEntity.java#L30-L31): 主键
- `product_id`: 商品ID
- `product_name`: 商品名称
- `stock_count`: 总库存数量
- `frozen_count`: 冻结库存数量
- [version](file://D:\Download\Code\sofware\cloud\common\src\main\java\domain\vo\StockVO.java#L55-L55): 版本号（用于乐观锁）

## 项目特点

1. **微服务架构** - 基于Spring Cloud Alibaba实现服务拆分和治理
2. **异步处理** - 利用CompletableFuture提升高并发场景下的响应性能
3. **容器化部署** - 使用Docker Compose实现一键部署
4. **统一网关** - 通过网关统一处理路由、限流、安全等横切关注点
5. **配置中心** - 使用Nacos实现配置的集中管理和动态刷新
6. **服务注册发现** - 基于Nacos实现服务的自动注册与发现

## 开发规范

1. 使用Lombok简化Java代码
2. 统一使用Result封装返回结果
3. 采用MapStruct进行对象转换
4. 遵循RESTful API设计规范
5. 使用Slf4j进行日志记录

## 许可证

本项目基于MIT许可证开源，详细信息请查看[LICENSE](LICENSE)文件。
