# Product Service (商品服务)

## 服务概述

Product Service 是电商平台的**核心商品管理服务**
,负责商品、分类、店铺的全生命周期管理。提供商品CRUD、分类管理、库存查询、商品搜索同步等功能,支持多级缓存(Caffeine + Redis)
提升查询性能。

- **服务端口**: 8084
- **服务名称**: product-service
- **数据库**: MySQL (products数据库)
- **缓存策略**: 两级缓存 (Caffeine L1 + Redis L2)

## 技术栈

| 技术                     | 版本                 | 用途        |
|------------------------|--------------------|-----------|
| Spring Boot            | 3.5.3              | 应用框架      |
| MySQL                  | 9.3.0              | 数据持久化     |
| MyBatis Plus           | 最新                 | ORM框架     |
| Redis                  | -                  | 二级缓存      |
| Caffeine               | -                  | 本地一级缓存    |
| Spring Security OAuth2 | -                  | 资源服务器     |
| RocketMQ               | -                  | 商品事件、搜索同步 |
| MapStruct              | 1.5.5.Final        | DTO转换     |
| Nacos                  | 2025.0.0.0-preview | 服务注册与配置   |

## 核心功能

### 1. 商品管理 (/api/product)

**ProductController** - 商品CRUD与查询

- ✅ POST `/api/product` - 创建商品
- ✅ PUT `/api/product/{id}` - 更新商品信息
- ✅ DELETE `/api/product/{id}` - 删除商品(软删除)
- ✅ GET `/api/product/{id}` - 根据ID查询商品详情
- ✅ GET `/api/product` - 分页查询商品列表
- ✅ GET `/api/product/category/{categoryId}` - 按分类查询
- ✅ GET `/api/product/shop/{shopId}` - 按店铺查询
- ✅ POST `/api/product/batch` - 批量创建商品
- ✅ PUT `/api/product/batch/status` - 批量更新状态
- ✅ GET `/api/product/search` - 商品搜索(关键词)
- ✅ GET `/api/product/hot` - 热门商品列表
- ✅ GET `/api/product/recommended` - 推荐商品列表

### 2. 分类管理 (/api/category)

**CategoryController** - 完整的商品分类管理(支持多级分类树)

- ✅ GET `/api/category` - 分页查询商品分类(支持父分类、状态筛选)
- ✅ GET `/api/category/{id}` - 根据ID获取分类详情
- ✅ GET `/api/category/tree` - 获取树形分类结构(支持只返回启用的分类)
- ✅ GET `/api/category/{id}/children` - 获取子分类列表(支持递归获取)
- ✅ POST `/api/category` - 创建商品分类(需管理员权限)
- ✅ PUT `/api/category/{id}` - 更新分类信息(需管理员权限)
- ✅ DELETE `/api/category/{id}` - 删除分类(逻辑删除,支持级联删除子分类)
- ✅ PATCH `/api/category/{id}/status` - 更新分类状态(启用/禁用)
- ✅ PATCH `/api/category/{id}/sort` - 更新分类排序值
- ✅ PATCH `/api/category/{id}/move` - 移动分类到新的父分类下
- ✅ DELETE `/api/category/batch` - 批量删除分类
- ✅ PATCH `/api/category/batch/status` - 批量更新分类状态
- ✅ POST `/api/category/batch` - 批量创建分类

**分类特性**:

- 支持3级分类树结构
- 分类排序功能
- 启用/禁用状态控制
- 级联删除子分类
- 分类移动功能

### 3. 内部服务接口 (/internal/products)

**ProductFeignController** - 供其他服务调用

- ✅ GET `/internal/products/{id}` - 根据ID查询商品
- ✅ POST `/internal/products/batch` - 批量查询商品
- ✅ PUT `/internal/products/{id}/stock` - 更新库存(供stock-service)
- ✅ GET `/internal/products/{id}/stock` - 查询库存

## 数据模型

### 核心实体

#### Product (products表)

```sql
CREATE TABLE products (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  product_name VARCHAR(200) NOT NULL,      -- 商品名称
  product_code VARCHAR(50) UNIQUE,         -- 商品编码
  category_id BIGINT,                      -- 分类ID
  shop_id BIGINT,                          -- 店铺ID
  brand_id BIGINT,                         -- 品牌ID
  price DECIMAL(10,2) NOT NULL,            -- 价格
  stock INT DEFAULT 0,                     -- 库存数量
  sales_count INT DEFAULT 0,               -- 销量
  hot_score INT DEFAULT 0,                 -- 热度分数
  description TEXT,                        -- 商品描述
  main_image VARCHAR(500),                 -- 主图
  detail_images TEXT,                      -- 详情图(JSON数组)
  status INT DEFAULT 1,                    -- 0:下架 1:上架
  is_recommended TINYINT DEFAULT 0,        -- 是否推荐
  is_new TINYINT DEFAULT 0,                -- 是否新品
  is_hot TINYINT DEFAULT 0,                -- 是否热销
  created_at DATETIME,
  updated_at DATETIME,
  deleted TINYINT DEFAULT 0,
  version INT DEFAULT 0
);
```

#### Category (categories表)

```sql
CREATE TABLE categories (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  category_name VARCHAR(100) NOT NULL,     -- 分类名称
  parent_id BIGINT DEFAULT 0,              -- 父分类ID (0为顶级)
  level INT DEFAULT 1,                     -- 层级 (1/2/3)
  sort_order INT DEFAULT 0,                -- 排序
  icon_url VARCHAR(500),                   -- 图标URL
  description VARCHAR(500),                -- 描述
  status INT DEFAULT 1,                    -- 0:禁用 1:启用
  created_at DATETIME,
  updated_at DATETIME,
  deleted TINYINT DEFAULT 0
);
```

#### Shop (shops表)

```sql
CREATE TABLE shops (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  shop_name VARCHAR(100) NOT NULL,         -- 店铺名称
  merchant_id BIGINT NOT NULL,             -- 商户ID
  logo_url VARCHAR(500),                   -- 店铺Logo
  description TEXT,                        -- 店铺描述
  status INT DEFAULT 1,                    -- 0:关闭 1:营业
  created_at DATETIME,
  updated_at DATETIME,
  deleted TINYINT DEFAULT 0
);
```

## 依赖服务

| 服务             | 用途        | 通信方式                 |
|----------------|-----------|----------------------|
| search-service | 商品搜索同步    | RocketMQ异步           |
| stock-service  | 库存管理      | Feign同步调用            |
| MySQL          | 商品数据持久化   | JDBC                 |
| Redis          | 二级缓存      | RedisTemplate        |
| RocketMQ       | 商品事件、搜索同步 | Spring Cloud Stream  |
| Nacos          | 服务注册、配置管理 | Spring Cloud Alibaba |

## 配置说明

### 端口配置

```yaml
server:
  port: 8084
```

### 多级缓存配置

```yaml
cache:
  multi-level:
    local:                          # Caffeine本地缓存
      enabled: true
      initial-capacity: 150
      maximum-size: 1500
      expire-after-write: 45m
      expire-after-access: 20m
    redis:                          # Redis分布式缓存
      enabled: true
      default-expire: 60m
      key-prefix: "product:"
```

### RocketMQ 配置

```yaml
spring:
  cloud:
    stream:
      bindings:
        productLog-out-0:           # 商品日志
          destination: LOG_PRODUCT_TOPIC
        search-producer-out-0:      # 搜索同步事件
          destination: SEARCH_EVENTS_TOPIC
```

### 商品服务配置

```yaml
product:
  async:
    enabled: true                   # 启用异步处理
  statistics:
    enabled: true                   # 启用统计功能
  search:
    enabled: true                   # 启用搜索同步
  config:
    page:
      default-size: 10
      max-size: 100
    batch:
      max-size: 100
    cache:
      warmup:
        enabled: true               # 启用缓存预热
        max-size: 500
```

## 开发状态

### ✅ 已完成功能

1. **商品管理**
    - [x] 商品CRUD完整实现
    - [x] 批量创建/更新商品
    - [x] 商品状态管理(上架/下架)
    - [x] 商品搜索(关键词)
    - [x] 多维度查询(分类/店铺/品牌)
    - [x] 热门商品推荐
    - [x] 软删除支持

2. **分类管理** ✨ 完整实现
    - [x] 多级分类支持(3级)
    - [x] 分类树查询(递归构建)
    - [x] 父子分类关系
    - [x] 分类排序功能
    - [x] 分类状态管理(启用/禁用)
    - [x] 分类移动功能(更改父分类)
    - [x] 批量删除分类
    - [x] 批量更新分类状态
    - [x] 批量创建分类
    - [x] 级联删除子分类

3. **缓存优化**
    - [x] 两级缓存架构(Caffeine + Redis)
    - [x] 缓存预热机制
    - [x] 自动缓存失效
    - [x] 缓存统计监控

4. **搜索同步**
    - [x] 商品创建同步到ES
    - [x] 商品更新同步到ES
    - [x] 商品删除同步到ES
    - [x] RocketMQ异步解耦

5. **数据转换**
    - [x] MapStruct自动转换
    - [x] ProductConverter
    - [x] CategoryConverter
    - [x] ShopConverter

### 🚧 进行中功能

1. **商品规格管理**
    - [ ] SKU规格定义
    - [ ] 规格组合价格
    - [ ] 规格图片管理

2. **商品属性**
    - [ ] 自定义商品属性
    - [ ] 属性模板
    - [ ] 属性筛选

### 📋 计划中功能

1. **商品审核**
    - [ ] 商品发布审核
    - [ ] 审核流程配置
    - [ ] 审核记录查询

2. **品牌管理**
    - [ ] 品牌CRUD
    - [ ] 品牌授权管理
    - [ ] 品牌商品关联

3. **商品评价**
    - [ ] 评价管理
    - [ ] 评价统计
    - [ ] 评价审核

4. **商品导入导出**
    - [ ] Excel批量导入
    - [ ] 商品数据导出
    - [ ] 模板下载

### ⚠️ 技术债

1. **性能优化**
    - 商品列表查询考虑ES实现
    - 缓存策略进一步优化
    - 数据库索引优化

2. **搜索优化**
    - 搜索同步失败重试机制
    - 增量同步优化

3. **测试覆盖**
    - 缓存逻辑单元测试
    - 并发更新测试

## 本地运行

### 前置条件

```bash
cd docker
docker-compose up -d mysql redis nacos rocketmq
```

### 数据库初始化

```bash
mysql -h localhost -u root -p < sql/init/initdb_product.sql
```

### 启动服务

```bash
cd product-service
mvn spring-boot:run
```

### 验证服务

```bash
# 健康检查
curl http://localhost:8084/actuator/health

# 查询商品列表
curl -H "Authorization: Bearer YOUR_TOKEN" \
  http://localhost:8084/api/product

# API文档
浏览器打开: http://localhost:8084/doc.html
```

## 测试

### 运行测试

```bash
mvn test -Dtest=ProductServiceImplTest
```

### 手动测试

#### 创建商品

```bash
curl -X POST "http://localhost:8084/api/product" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "productName": "测试商品",
    "categoryId": 1,
    "shopId": 1,
    "price": 99.99,
    "stock": 100,
    "description": "商品描述",
    "status": 1
  }'
```

## 注意事项

### 缓存一致性

- 商品更新时自动清除缓存
- 使用版本号乐观锁防止并发更新
- 缓存预热避免缓存穿透

### 搜索同步

- 商品变更通过RocketMQ异步同步到ES
- 同步失败会记录日志(TODO:增加重试机制)
- 可通过search-service手动全量同步

### 性能建议

- 商品列表查询使用缓存
- 热门商品单独缓存
- 分类树使用Redis缓存

## 相关文档

- [API文档 - Product Service](../doc/services/product/API_DOC_PRODUCT_SERVICE.md)
- [项目整体文档](../doc/README.md)

## 快速链接

- Knife4j API文档: http://localhost:8084/doc.html
- Actuator Health: http://localhost:8084/actuator/health
- Nacos控制台: http://localhost:8848/nacos
