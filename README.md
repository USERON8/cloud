### 模块说明

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

6. **frontend** - 前端应用
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

## 开发规范

1. 使用Lombok简化Java代码
2. 统一使用Result封装返回结果
3. 采用MapStruct进行对象转换
4. 遵循RESTful API设计规范
5. 使用Slf4j进行日志记录
6. 使用统一异常处理机制
7. 遵循Maven多模块项目结构规范
8. 遵循微服务设计规范，确保服务间松耦合
