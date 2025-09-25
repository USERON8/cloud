# 商品服务 (product-service)

## 1. 模块概述

商品服务是电商平台系统中的核心服务之一，负责管理商品信息、商品分类、商品库存等与商品相关的功能。该服务基于Spring Boot和Spring
Cloud构建，采用微服务架构设计，支持高并发访问和水平扩展。

### 1.1 核心功能

- 商品CRUD管理
- 商品分页查询和条件筛选
- 商品状态管理（上架/下架）
- 商品库存管理
- 商品分类管理
- 商品缓存优化（多级缓存）
- Feign客户端接口支持

### 1.2 技术栈

- **核心框架**: Spring Boot 3.5.3, Spring Cloud 2025.0.0
- **安全框架**: Spring Security, OAuth2 Resource Server
- **数据库**: MySQL 9.3.0, MyBatis-Plus 3.5.13
- **缓存**: Redis + Caffeine 多级缓存
- **API文档**: Swagger/OpenAPI 3.0, Knife4j 4.5.0
- **服务治理**: Nacos
- **对象映射**: MapStruct 1.6.3
- **其他**: Lombok

## 2. 服务架构

### 2.1 整体架构

商品服务采用经典的分层架构模式，从上到下分为：

```
┌─────────────────────────────────────────────────────────────┐
│                    Controller Layer                         │
├─────────────────────────────────────────────────────────────┤
│                      Service Layer                          │
├─────────────────────────────────────────────────────────────┤
│                   Data Access Layer                         │
├─────────────────────────────────────────────────────────────┤
│                    Database Layer                           │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 模块结构

```
product-service/
├── src/main/java/com/cloud/product/
│   ├── ProductApplication.java              # 启动类
│   ├── annotation/                          # 多级缓存注解
│   │   ├── MultiLevelCacheable.java
│   │   ├── MultiLevelCachePut.java
│   │   ├── MultiLevelCacheEvict.java
│   │   └── MultiLevelCaching.java
│   ├── aop/                                # AOP切面
│   │   └── MultiLevelCacheAspect.java
│   ├── config/                             # 配置类
│   │   ├── Knife4jConfig.java
│   │   ├── LocalCacheConfig.java
│   │   ├── RedisConfig.java
│   │   ├── ResourceServerConfig.java
│   │   └── SecurityConfig.java
│   ├── controller/                         # 控制器层
│   │   └── product/
│   │       ├── ProductController.java      # Feign接口实现
│   │       ├── ProductManageController.java # 管理接口
│   │       └── ProductQueryController.java  # 查询接口
│   ├── converter/                          # 对象转换器
│   │   ├── CategoryConverter.java
│   │   ├── ProductConverter.java
│   │   └── ShopConverter.java
│   ├── dto/                               # 数据传输对象
│   │   ├── ProductPageDTO.java
│   │   ├── ShopPageDTO.java
│   │   └── ShopRequestDTO.java
│   ├── exception/                         # 异常处理
│   │   ├── GlobalExceptionHandler.java
│   │   ├── CategoryNotFoundException.java
│   │   ├── ProductNotFoundException.java
│   │   ├── ProductServiceException.java
│   │   └── ProductStatusException.java
│   ├── mapper/                            # 数据访问层
│   │   ├── CategoryMapper.java
│   │   ├── ProductMapper.java
│   │   └── ShopMapper.java
│   ├── module/entity/                     # 实体类
│   │   ├── Category.java
│   │   ├── Product.java
│   │   └── Shop.java
│   ├── security/                          # 权限辅助
│   │   └── ShopPermissionHelper.java
│   ├── service/                           # 业务逻辑层
│   │   ├── CategoryService.java
│   │   ├── ProductService.java
│   │   ├── ShopService.java
│   │   └── impl/                          # 业务逻辑实现
│   │       ├── CategoryServiceImpl.java
│   │       ├── ProductServiceImpl.java
│   │       └── ShopServiceImpl.java
│   ├── utils/                             # 工具类
│   │   └── ResponseHelper.java
│   └── vo/                                # 视图对象 (已迁移到 common-module)
│       └── ShopVO.java
├── src/main/resources/
│   ├── mapper/                            # MyBatis XML映射文件
│   │   ├── CategoryMapper.xml
│   │   ├── ProductMapper.xml
│   │   └── ShopMapper.xml
│   ├── application.yml                    # 主配置文件
│   └── application-dev.yml                # 开发环境配置
└── src/test/java/com/cloud/product/        # 测试代码
```

## 3. 数据库设计

### 3.1 商品表 (products)

| 字段名            | 类型              | 描述           |
|----------------|-----------------|--------------|
| id             | BIGINT UNSIGNED | 主键，商品ID      |
| shop_id        | BIGINT UNSIGNED | 店铺ID         |
| product_name   | VARCHAR(100)    | 商品名称         |
| price          | DECIMAL(10,2)   | 售价           |
| stock_quantity | INT             | 库存数量         |
| category_id    | BIGINT UNSIGNED | 分类ID         |
| status         | TINYINT         | 状态：0-下架，1-上架 |
| created_at     | DATETIME        | 创建时间         |
| updated_at     | DATETIME        | 更新时间         |
| deleted        | TINYINT         | 软删除标记        |

### 3.2 商品分类表 (category)

| 字段名        | 类型              | 描述           |
|------------|-----------------|--------------|
| id         | BIGINT UNSIGNED | 主键，分类ID      |
| parent_id  | BIGINT UNSIGNED | 父分类ID        |
| name       | VARCHAR(50)     | 分类名称         |
| level      | TINYINT         | 层级           |
| status     | TINYINT         | 状态：0-禁用，1-启用 |
| created_at | DATETIME        | 创建时间         |
| updated_at | DATETIME        | 更新时间         |
| deleted    | TINYINT         | 软删除标记        |

### 3.3 商家店铺表 (merchant_shop)

| 字段名           | 类型              | 描述           |
|---------------|-----------------|--------------|
| id            | BIGINT UNSIGNED | 主键，店铺ID      |
| merchant_id   | BIGINT UNSIGNED | 商家ID         |
| shop_name     | VARCHAR(100)    | 店铺名称         |
| avatar_url    | VARCHAR(255)    | 店铺头像URL      |
| description   | TEXT            | 店铺描述         |
| contact_phone | VARCHAR(20)     | 客服电话         |
| address       | VARCHAR(255)    | 详细地址         |
| status        | TINYINT         | 状态：0-关闭，1-营业 |
| created_at    | DATETIME        | 创建时间         |
| updated_at    | DATETIME        | 更新时间         |
| deleted       | TINYINT         | 软删除标记        |

## 4. 分页查询实现

### 4.1 MyBatis-Plus分页插件配置

项目使用MyBatis-Plus 3.5.13版本的分页插件，在`common-module`中统一配置：

```java
@Bean
public MybatisPlusInterceptor mybatisPlusInterceptor() {
    MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
    // 分页插件（必须指定数据库类型）
    interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
    // 乐观锁插件
    interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
    // 防全表更新插件
    interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());
    return interceptor;
}
```

### 4.2 商品分页查询实现

#### 4.2.1 服务层实现

```java
@Override
@Transactional(readOnly = true)
@MultiLevelCacheable(
    value = "productListCache",
    key = "'page:' + #pageDTO.pageNum + ':' + #pageDTO.pageSize + ':' + (#pageDTO.name ?: 'null')",
    expire = 30, timeUnit = TimeUnit.MINUTES
)
public PageResult<ProductVO> getProductsPage(ProductPageDTO pageDTO) {
    log.debug("分页查询商品: {}", pageDTO);
    
    // 1. 构造分页对象
    Page<Product> page = new Page<>(pageDTO.getPageNum(), pageDTO.getPageSize());
    
    // 2. 构造查询条件
    LambdaQueryWrapper<Product> queryWrapper = buildQueryWrapper(pageDTO);
    
    // 3. 执行分页查询
    Page<Product> productPage = page(page, queryWrapper);
    
    // 4. 转换为VO并封装结果
    List<ProductVO> productVOs = productConverter.toVOList(productPage.getRecords());
    return PageResult.of(productVOs, productPage.getTotal(), pageDTO.getPageNum(), pageDTO.getPageSize());
}

private LambdaQueryWrapper<Product> buildQueryWrapper(ProductPageDTO pageDTO) {
    LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();
    
    if (StringUtils.hasText(pageDTO.getName())) {
        queryWrapper.like(Product::getName, pageDTO.getName());
    }
    if (pageDTO.getCategoryId() != null) {
        queryWrapper.eq(Product::getCategoryId, pageDTO.getCategoryId());
    }
    if (pageDTO.getStatus() != null) {
        queryWrapper.eq(Product::getStatus, pageDTO.getStatus());
    }
    if (pageDTO.getMinPrice() != null) {
        queryWrapper.ge(Product::getPrice, pageDTO.getMinPrice());
    }
    if (pageDTO.getMaxPrice() != null) {
        queryWrapper.le(Product::getPrice, pageDTO.getMaxPrice());
    }
    
    // 默认按创建时间倒序
    queryWrapper.orderByDesc(Product::getCreateTime);
    return queryWrapper;
}
```

### 4.3 分页查询性能优化

1. **多级缓存策略**：使用Redis + Caffeine多级缓存，提升热点数据访问性能
2. **索引优化**：为常用查询字段建立复合索引，如(category_id, status, create_time)
3. **查询条件优化**：合理使用查询条件，避免全表扫描
4. **分页参数限制**：限制每页最大数量和最大页码，防止深度分页

## 5. API接口文档

### 4.1 商品查询接口

#### 获取商品详情

```
GET /api/v1/products/{id}
权限: 无需权限
响应: ProductVO
```

#### 分页查询商品

```
GET /api/v1/products/page?pageNum=1&pageSize=10&keyword=关键字
权限: 无需权限
参数: ProductPageDTO
响应: PageResult<ProductVO>
```

#### 批量获取商品

```
GET /api/v1/products/batch?ids=1,2,3
权限: 无需权限
响应: List<ProductVO>
```

#### 搜索商品

```
GET /api/v1/products/search?keyword=关键字&status=1
权限: 无需权限
响应: List<ProductVO>
```

### 4.2 商品管理接口

#### 创建商品

```
POST /api/v1/products/manage
权限: PRODUCT_CREATE
请求体: ProductRequestDTO
响应: Long (商品ID)
```

#### 更新商品

```
PUT /api/v1/products/manage/{id}
权限: PRODUCT_UPDATE
请求体: ProductRequestDTO
响应: Boolean
```

#### 删除商品

```
DELETE /api/v1/products/manage/{id}
权限: PRODUCT_DELETE
响应: Boolean
```

#### 批量删除商品

```
DELETE /api/v1/products/manage/batch
权限: PRODUCT_DELETE
请求体: List<Long> (商品ID列表)
响应: Boolean
```

### 4.3 商品状态管理

#### 上架商品

```
PUT /api/v1/products/manage/{id}/enable
权限: PRODUCT_STATUS
响应: Boolean
```

#### 下架商品

```
PUT /api/v1/products/manage/{id}/disable
权限: PRODUCT_STATUS
响应: Boolean
```

#### 批量上架商品

```
PUT /api/v1/products/manage/batch/enable
权限: PRODUCT_STATUS
请求体: List<Long> (商品ID列表)
响应: Boolean
```

#### 批量下架商品

```
PUT /api/v1/products/manage/batch/disable
权限: PRODUCT_STATUS
请求体: List<Long> (商品ID列表)
响应: Boolean
```

### 4.4 库存管理

#### 更新库存

```
PUT /api/v1/products/manage/{id}/stock?stock=100
权限: PRODUCT_STOCK
响应: Boolean
```

#### 增加库存

```
PUT /api/v1/products/manage/{id}/stock/increase?quantity=10
权限: PRODUCT_STOCK
响应: Boolean
```

#### 减少库存

```
PUT /api/v1/products/manage/{id}/stock/decrease?quantity=5
权限: PRODUCT_STOCK
响应: Boolean
```

#### 检查库存

```
GET /api/v1/products/manage/{id}/stock/check?quantity=5
权限: PRODUCT_VIEW
响应: Boolean
```

### 4.5 缓存管理

#### 清除商品缓存

```
DELETE /api/v1/products/manage/cache/{id}
权限: PRODUCT_CACHE
响应: String
```

#### 清除所有商品缓存

```
DELETE /api/v1/products/manage/cache/all
权限: PRODUCT_CACHE
响应: String
```

#### 预热商品缓存

```
POST /api/v1/products/cache/warmup
权限: 无需权限
请求体: List<Long> (商品ID列表)
响应: String
```

## 5. 缓存策略

### 5.1 多级缓存架构

- **L1缓存**: Caffeine本地缓存
    - 初始容量：150
    - 最大容量：1500
    - 写入后过期：45分钟
    - 访问后过期：20分钟

- **L2缓存**: Redis分布式缓存
    - 数据库：1
    - 默认过期时间：60分钟
    - Key前缀：product:

### 5.2 缓存Key规范

- 商品详情：`product:productCache:{id}`
- 商品列表：`product:productListCache:{查询条件hash}`
- 统计数据：`product:productStatsCache:{统计类型}`

### 5.3 缓存更新策略

- **查询操作**: 优先L1缓存，未命中查L2缓存，最后查数据库
- **写操作**: 先更新数据库，后更新/清除缓存
- **批量操作**: 清除相关缓存分区

## 6. 配置说明

### 6.1 服务配置

- **端口**: 8083 (按照开发文档规范)
- **数据库**: product_db
- **Redis数据库**: 1

### 6.2 依赖版本

遵循项目根目录READ.md文档中的版本规范：

- Spring Boot: 3.5.3
- Spring Cloud: 2025.0.0
- MyBatis Plus: 3.5.13
- MySQL Driver: 9.3.0
- Redis: 3.5.3
- Caffeine: 3.2.2
- Knife4j: 4.5.0

## 7. 权限设计

### 7.1 权限标识

- `PRODUCT_VIEW`: 查看商品权限
- `PRODUCT_CREATE`: 创建商品权限
- `PRODUCT_UPDATE`: 更新商品权限
- `PRODUCT_DELETE`: 删除商品权限
- `PRODUCT_STATUS`: 商品状态管理权限
- `PRODUCT_STOCK`: 库存管理权限
- `PRODUCT_CACHE`: 缓存管理权限

### 7.2 接口权限控制

所有管理接口都需要相应的权限，查询接口无需权限便于商城展示使用。

## 8. 异常处理

### 8.1 自定义异常

- `ProductNotFoundException`: 商品不存在异常
- `ProductServiceException`: 商品服务异常
- `ProductStatusException`: 商品状态异常
- `CategoryNotFoundException`: 分类不存在异常

### 8.2 全局异常处理

`GlobalExceptionHandler`统一处理所有异常，返回标准错误响应格式。

## 9. 性能优化

### 9.1 缓存优化

- 使用多级缓存减少数据库访问
- 支持缓存预热提升响应速度
- 智能缓存失效策略

### 9.2 查询优化

- 分页查询支持多维度筛选
- 复杂查询条件自动优化
- 批量操作减少网络开销

## 10. 测试策略

### 10.1 API测试

主要通过以下方式测试：

- Postman接口测试
- Knife4j在线文档测试
- 集成测试验证业务逻辑

### 10.2 测试覆盖

- 控制器层接口测试
- 服务层业务逻辑测试
- 缓存功能测试
- 异常处理测试

## 11. 部署说明

### 11.1 环境要求

- JDK 17+
- MySQL 8.0+ (使用product_db数据库)
- Redis 6.0+ (使用数据库1)
- Nacos服务

### 11.2 启动方式

```bash
# 编译打包
mvn clean package -DskipTests

# 运行服务
java -jar product-service-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

## 12. 监控与运维

### 12.1 健康检查

通过Actuator提供健康检查端点：

- `/actuator/health`: 健康状态
- `/actuator/metrics`: 性能指标
- `/actuator/caches`: 缓存状态

### 12.2 日志配置

- 日志级别：DEBUG (开发环境)
- 日志文件：logs/product-service.log
- 包含缓存、数据库操作详细日志

## 13. 店铺服务优化

### 13.1 ShopServiceImpl 优化完成

**优化时间**: 2025-01-25

**优化内容**:

1. **引入ShopConverter转换器**
    - 使用MapStruct接口定义的ShopConverter进行实体、DTO和VO之间的转换
    - 替换原有的手动属性赋值代码，提升代码简洁性和维护性

2. **完善缓存注解**
    - 为更新、删除、状态操作方法添加@MultiLevelCacheEvict注解
    - 确保数据变更时自动清除相关缓存，保持数据一致性

3. **优化代码结构**
    - 使用@RequiredArgsConstructor进行依赖注入
    - 简化了实体转换逻辑，所有转换操作统一使用ShopConverter
    - 改进了异常处理和日志记录

4. **提升性能**
    - 减少重复代码，提高执行效率
    - 优化缓存策略，提升数据访问速度
    - 统一数据转换逻辑，减少维护成本

**关键改进**:

- **createShop**: 使用shopConverter.requestDTOToEntity()转换
- **updateShop**: 添加缓存清除，使用转换器更新实体
- **deleteShop**: 添加缓存清除注解
- **查询方法**: 统一使用shopConverter.toVO()和toVOList()
- **状态操作**: 为单个和批量状态操作添加缓存清除

### 13.2 技术规范遵循

- ✅ 使用MapStruct进行对象转换
- ✅ 遵循多级缓存策略
- ✅ 符合DRY原则，消除重复代码
- ✅ 事务注解正确配置
- ✅ 日志记录完善

---

**最近更新**: 2025-01-25
**修复内容**:

1. **代码清理完成**
    - 删除冗余配置类和重复代码
    - 统一异常处理机制
    - 优化依赖管理

2. **ShopServiceImpl全面优化**
    - 集成ShopConverter，消除手动转换代码
    - 完善多级缓存注解配置
    - 优化业务逻辑实现

3. **遵循微服务设计原则**
    - 单一职责原则
    - DRY原则
    - 配置统一化

**注意**: 该服务现在完全遵循微服务最佳实践，代码更加简洁高效，缓存策略完善。
