   ### 模块说明### 支付服务接口### 商品服务接口### 搜索服务接口

#### 搜索接口
- `GET /search/product` - 搜索商品
- `GET /search/order` - 搜索订单

#### 商品查询接口
- `GET /product/query/{id}` - 根据ID获取商品详情
- `GET /product/query/page` - 分页查询商品

#### 商品管理接口
- `POST /product/manage` - 创建商品
- `PUT /product/manage/{id}` - 更新商品信息
- `PUT /product/manage/{id}/status` - 更新商品状态（上架/下架）
- `DELETE /product/manage/{id}` - 删除商品

#### 支付接口
- `POST /payments` - 创建支付记录
- `GET /payments/{id}` - 根据ID获取支付详情
- `GET /payments/page` - 分页查询支付记录
- `PUT /payments/{id}/success` - 更新支付状态为成功
- `PUT /payments/{id}/fail` - 更新支付状态为失败
- `PUT /payments/{id}/refund` - 申请退款

1. **auth** - 认证授权服务
    - 提供用户认证和权限验证功能
    - 基于Spring Security和OAuth2授权服务器实现
    - 使用Feign调用user服务获取用户信息
    - 支持OAuth2授权码模式和客户端凭证模式

2. **common** - 公共模块
    - 包含通用配置、工具类、异常处理等
    - 定义统一返回结果格式
    - 提供基础实体类和枚举
    - 包含DTO和VO对象用于服务间通信

3. **gateway** - 网关服务
    - 基于Spring Cloud Gateway实现
    - 路由转发、负载均衡
    - 统一入口和安全控制
    - 集成Nacos实现动态路由配置
    - 使用OAuth2资源服务器验证JWT令牌

4. **stock** - 库存服务
    - 核心业务模块，提供库存管理功能
    - 支持同步和异步接口调用
    - 包含库存查询、分页、批量操作等
    - 使用MyBatis-Plus操作数据库
    - 集成Redis缓存提升性能

5. **user** - 用户服务
    - 用户信息管理服务
    - 提供用户注册、查询等接口
    - 使用BCrypt进行密码加密
    - 集成MyBatis-Plus操作数据库

6. **order** - 订单服务
    - 订单管理服务
    - 提供订单创建、查询、更新等接口
    - 使用MyBatis-Plus操作数据库
    - 集成消息队列处理订单状态变更

7. **payment** - 支付服务
    - 支付管理服务
    - 提供支付创建、查询、回调处理等接口
    - 集成第三方支付平台
    - 使用MyBatis-Plus操作数据库

8. **product** - 商品服务
    - 商品管理服务
    - 提供商品信息查询、管理等接口
    - 使用MyBatis-Plus操作数据库
    - 集成Redis缓存商品信息

9. **search** - 搜索服务
    - 搜索服务
    - 基于Elasticsearch实现商品和订单搜索功能
    - 提供全文检索和过滤功能

10. **api** - API模块
    - 提供统一的API接口定义
    - 包含Feign客户端接口定义
    - 用于服务间通信

11. **admin** - 管理员服务
    - 管理员管理功能
    - 提供商家、用户、订单等管理接口
    - 使用Feign调用其他服务获取数据

12. **merchant** - 商家服务
    - 商家信息管理服务
    - 提供商家注册、店铺管理等功能
    - 使用MyBatis-Plus操作数据库

13. **log** - 日志服务
    - 系统操作日志管理服务
    - 提供各服务操作日志的收集和查询
    - 使用MyBatis-Plus操作数据库
    - 通过消息队列接收其他服务的操作日志

14. **frontend** - 前端应用
    - 基于Vue3的单页面应用
    - 使用Element Plus组件库
    - 通过Axios调用后端API

## 核心功能

### 库存管理功能

1. 商品库存查询（同步/异步）
2. 库存分页查询
3. 批量库存查询
4. 库存统计信息查询
5. 高并发场景下的并发查询

### 认证授权功能

1. 用户登录认证（支持OAuth2授权码模式）
2. JWT Token生成与验证
3. 接口权限控制
4. 用户信息管理
5. 客户端凭证模式用于服务间通信

### 订单管理功能

1. 订单创建与支付
2. 订单状态跟踪
3. 订单查询与统计
4. 订单取消与退款处理

### 支付管理功能

1. 支付订单创建
2. 第三方支付平台集成（支付宝、微信等）
3. 支付回调处理
4. 支付记录查询

### 商品管理功能

1. 商品信息维护
2. 商品分类管理
3. 商品上下架操作
4. 商品信息查询

### 搜索功能

1. 商品全文搜索
2. 订单搜索
3. 高级筛选和过滤
4. 搜索结果排序

### 异步处理特性

- 使用CompletableFuture实现异步调用
- 支持超时控制
- 提供异常处理机制
- 支持批量任务并发执行

## 技术架构

### 后端技术栈

- **核心框架**: Spring Boot 3.5.3, Spring Cloud 2025.0.0, Spring Cloud Alibaba 2023.0.3.3
- **数据库**: MySQL 9.3.0, MyBatis-Plus 3.5.12
- **缓存**: Redis 8.2-rc1
- **搜索引擎**: Elasticsearch
- **服务治理**: Nacos 3.0.2 (服务注册发现、配置管理)
- **消息队列**: RocketMQ 5.3.3
- **安全框架**: Spring Security, OAuth2 Authorization Server 1.5.3, OAuth2 Resource Server 1.5.3
- **对象映射**: MapStruct 1.6.3
- **日志系统**: SLF4J 2.0.16, Logback 1.5.13
- **其他**: Lombok, HttpClient 4.5.14

### 前端技术栈

- **核心框架**: Vue 3.5.17
- **构建工具**: Vite 7.0.4
- **包管理**: pnpm
- **UI组件库**: Element Plus 2.10.4
- **HTTP库**: Axios 1.10.0
- **状态管理**: Pinia 3.0.3
- **路由**: Vue Router 4.5.1

## 部署架构

项目采用Docker容器化部署，包含以下服务：

- MySQL: 数据库服务
- Redis: 缓存服务
- Nacos: 服务注册与配置中心
- RocketMQ: 消息队列服务
- Elasticsearch: 搜索引擎服务
- Nginx: 反向代理服务器

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.x
- Docker & Docker Compose
- Node.js 16+

### 启动步骤

1. 构建项目

```bash
# 在项目根目录执行
mvn clean install
```

2. 启动基础服务

```bash
# 进入docker目录
cd docker
# 启动基础服务
docker-compose up -d
```

3. 启动应用服务

```bash
# 分别启动各个微服务
java -jar auth/target/auth.jar
java -jar user/target/user.jar
java -jar stock/target/stock.jar
java -jar order/target/order.jar
java -jar payment/target/payment.jar
java -jar product/target/product.jar
java -jar search/target/search.jar
java -jar admin/target/admin.jar
java -jar merchant/target/merchant.jar
java -jar log/target/log.jar
java -jar gateway/target/gateway.jar
```

4. 启动前端应用

```bash
# 进入前端目录
cd frontend/app
# 安装依赖
pnpm install
# 启动开发服务器
pnpm dev
```

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

### 订单服务接口

#### 核心接口

- `POST /orders` - 创建订单
- `GET /orders/{orderId}` - 根据订单ID查询订单
- `PUT /orders/{orderId}` - 更新订单信息
- `DELETE /orders/{orderId}` - 删除订单
- `GET /orders` - 分页查询订单

### 支付服务接口

#### 核心接口

- `POST /payments` - 创建支付记录
- `GET /payments/{id}` - 根据ID获取支付详情
- `PUT /payments/{id}` - 更新支付记录
- `DELETE /payments/{id}` - 删除支付记录
- `GET /payments` - 分页查询支付记录

### 商品服务接口

#### 核心接口

- `POST /products` - 创建商品
- `GET /products/{id}` - 根据商品ID查询商品信息
- `PUT /products/{id}` - 更新商品信息
- `DELETE /products/{id}` - 删除商品
- `GET /products` - 分页查询商品

### 搜索服务接口

#### 核心接口

- `GET /search/product` - 搜索商品
- `GET /search/order` - 搜索订单
- `POST /search/advanced` - 高级搜索

### 用户服务接口

#### 核心接口

- `GET /user/info` - 获取当前用户信息
- `GET /user/admin/users` - 获取所有用户（仅管理员）
- `GET /user/admin/users/page` - 分页获取用户列表

### 认证服务接口

#### 核心接口

- `POST /auth/register` - 用户注册
- `POST /auth/register-and-login` - 用户注册并登录
- `POST /auth/login` - 用户登录

### 管理员服务接口

#### 管理接口

- `PUT /admin/manage/merchants/{id}/approve` - 审核通过商家
- `PUT /admin/manage/merchants/{id}/reject` - 拒绝商家申请
- `PUT /admin/manage/shops/{id}/approve` - 审核通过店铺

#### 查询接口

- `GET /admin/query/users` - 获取所有用户列表
- `GET /admin/query/merchants` - 获取所有商家列表
- `GET /admin/query/shops` - 获取所有店铺列表

### 商家服务接口

#### 认证接口

- `POST /merchant-auth/submit` - 提交认证信息
- `GET /merchant-auth/me` - 获取当前商家认证信息

#### 管理接口

- `PUT /admin/merchants/{id}/approve` - 审核通过商家
- `PUT /admin/merchants/{id}/reject` - 拒绝商家申请

#### 查询接口

- `GET /merchant/query/info` - 获取当前商家信息
- `GET /merchant/query/shops` - 获取当前商家所有店铺

## 数据库设计

### 库存表 (tb_stock)

系统核心数据表，存储商品库存信息：

- [id](file://D:\Download\Code\sofware\cloud\common\src\main\java\domain\BaseEntity.java#L30-L31): 主键
- `product_id`: 商品ID
- `product_name`: 商品名称
- `stock_count`: 总库存数量
- `frozen_count`: 冻结库存数量
- `available_count`: 可用库存数量（虚拟字段，计算得出）
- [version](file://D:\Download\Code\sofware\cloud\common\src\main\java\domain\vo\StockVO.java#L55-L55): 版本号（用于乐观锁）
- `create_time`: 创建时间
- `update_time`: 更新时间

### 用户表 (user)

存储用户基本信息：

- `id`: 主键
- `username`: 用户名
- `password`: 密码（BCrypt加密存储）
- `email`: 邮箱
- `phone`: 手机号
- `nickname`: 昵称
- `avatar`: 头像URL
- `status`: 状态（0-禁用，1-启用）
- `created_at`: 创建时间
- `updated_at`: 更新时间
- `deleted`: 是否删除（0-未删除，1-已删除）

### 库存变更记录表 (stock_log)

记录库存变更历史：

- `id`: 主键
- `product_id`: 商品ID
- `order_id`: 订单ID
- `change_type`: 变更类型（1-入库，2-出库，3-冻结，4-解冻）
- `change_count`: 变更数量
- `before_count`: 变更前数量
- `after_count`: 变更后数量
- `remark`: 备注
- `create_time`: 创建时间

### 订单表 (tb_order)

存储订单信息：

- `id`: 主键
- `order_no`: 订单编号
- `user_id`: 用户ID
- `product_id`: 商品ID
- `quantity`: 商品数量
- `amount`: 订单金额
- `status`: 订单状态
- `create_time`: 创建时间
- `update_time`: 更新时间

### 支付表 (tb_payment)

存储支付信息：

- `id`: 主键
- `payment_no`: 支付编号
- `order_id`: 订单ID
- `amount`: 支付金额
- `payment_method`: 支付方式
- `status`: 支付状态
- `create_time`: 创建时间
- `update_time`: 更新时间

### 商品表 (tb_product)

存储商品信息：

- `id`: 主键
- `name`: 商品名称
- `description`: 商品描述
- `price`: 商品价格
- `category`: 商品分类
- `status`: 商品状态（上架/下架）
- `create_time`: 创建时间
- `update_time`: 更新时间

### 管理员表 (tb_admin)

存储管理员信息：

- `id`: 主键
- `username`: 用户名
- `password`: 密码（BCrypt加密存储）
- `email`: 邮箱
- `phone`: 手机号
- `nickname`: 昵称
- `avatar`: 头像URL
- `status`: 状态（0-禁用，1-启用）
- `create_time`: 创建时间
- `update_time`: 更新时间

### 商家表 (tb_merchant)

存储商家信息：

- `id`: 主键
- `merchant_name`: 商家名称
- `contact_person`: 联系人
- `contact_phone`: 联系电话
- `business_license`: 营业执照
- `status`: 状态（0-禁用，1-启用，2-待审核）
- `create_time`: 创建时间
- `update_time`: 更新时间

### 商家认证表 (tb_merchant_auth)

存储商家认证信息：

- `id`: 主键
- `merchant_id`: 商家ID
- `id_card_front`: 身份证正面
- `id_card_back`: 身份证反面
- `business_license`: 营业执照
- `auth_status`: 认证状态（0-待审核，1-审核通过，2-审核拒绝）
- `reject_reason`: 拒绝原因
- `create_time`: 创建时间
- `update_time`: 更新时间

### 商家店铺表 (tb_merchant_shop)

存储商家店铺信息：

- `id`: 主键
- `merchant_id`: 商家ID
- `shop_name`: 店铺名称
- `shop_logo`: 店铺Logo
- `description`: 店铺描述
- `address`: 店铺地址
- `status`: 状态（0-禁用，1-启用，2-待审核）
- `create_time`: 创建时间
- `update_time`: 更新时间

### 管理员操作日志表 (tb_admin_operation_log)

存储管理员操作日志：

- `id`: 主键
- `admin_id`: 管理员ID
- `operation`: 操作类型
- `method`: 请求方法
- `url`: 请求URL
- `param`: 请求参数
- `result`: 响应结果
- `ip`: 操作IP
- `create_time`: 创建时间

### 商家操作日志表 (tb_merchant_operation_log)

存储商家操作日志：

- `id`: 主键
- `merchant_id`: 商家ID
- `operation`: 操作类型
- `method`: 请求方法
- `url`: 请求URL
- `param`: 请求参数
- `result`: 响应结果
- `ip`: 操作IP
- `create_time`: 创建时间

### 订单操作日志表 (tb_order_operation_log)

存储订单操作日志：

- `id`: 主键
- `order_id`: 订单ID
- `operation`: 操作类型
- `operator`: 操作人
- `remark`: 备注
- `create_time`: 创建时间

### 库存操作日志表 (tb_stock_operation_log)

存储库存操作日志：

- `id`: 主键
- `product_id`: 商品ID
- `product_name`: 商品名称
- `before_count`: 变更前数量
- `change_count`: 变更数量
- `after_count`: 变更后数量
- `change_type`: 变更类型
- `operator`: 操作人
- `operate_time`: 操作时间
- `create_time`: 创建时间


## 项目特点

1. **微服务架构** - 基于Spring Cloud Alibaba实现服务拆分和治理
2. **异步处理** - 利用CompletableFuture提升高并发场景下的响应性能
3. **容器化部署** - 使用Docker Compose实现一键部署
4. **统一网关** - 通过网关统一处理路由、限流、安全等横切关注点
5. **配置中心** - 使用Nacos实现配置的集中管理和动态刷新
6. **服务注册发现** - 基于Nacos实现服务的自动注册与发现
7. **OAuth2安全框架** - 使用OAuth2授权码模式和客户端凭证模式实现安全认证
8. **分布式事务** - 集成Seata处理分布式事务（规划中）
9. **链路追踪** - 集成SkyWalking实现分布式链路追踪（规划中）
10. **全文搜索** - 基于Elasticsearch实现商品和订单的全文搜索功能
11. **消息队列** - 集成RocketMQ实现服务间异步通信和解耦
12. **操作日志** - 完整的操作日志记录和查询功能
13. **多角色权限** - 支持管理员、商家、用户等多角色权限控制
14. **商家认证** - 完整的商家认证审核流程
15. **开发规范** - 所有开发工作遵循 [RULE.md](RULE.md) 中定义的开发规范

## 开发规范

详细的开发规范请参考 [RULE.md](RULE.md) 文件，其中包括：

- 代码规范（命名、注释、格式等）
- 实体类规范
- Controller规范
- Service规范
- Converter规范
- 异常处理规范
- 日志规范
- 配置规范
- 测试规范
- 文档规范
- 安全规范
- 性能规范
- 版本控制规范
- 部署规范