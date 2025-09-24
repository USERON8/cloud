# MyBatis Plus 配置指南

## 概述

本项目已经将MyBatis Plus配置进行了分离和优化，让各个子服务可以根据自己的业务特点进行个性化配置，同时实现了自动填充创建时间和更新时间的功能。

## 架构设计

### 1. 分层架构

```
common-module
├── MybatisPlusConfig.java          # 默认通用配置
├── MybatisPlusConfigFactory.java   # 配置工厂类
└── BaseDataSourceConfig.java       # 基础配置抽象类

各服务模块
├── OrderMybatisPlusConfig.java     # 订单服务配置
├── StockMybatisPlusConfig.java     # 库存服务配置
├── ProductMybatisPlusConfig.java   # 商品服务配置
├── PaymentMybatisPlusConfig.java   # 支付服务配置
├── SearchMybatisPlusConfig.java    # 搜索服务配置
└── UserMybatisPlusConfig.java      # 用户服务配置
```

### 2. 配置模板

#### 基础配置模板

- **createBasicInterceptor**: 仅包含分页插件
- **createStandardInterceptor**: 包含分页、乐观锁、防全表更新插件
- **createHighConcurrencyInterceptor**: 高并发场景配置，适用于库存、订单、支付服务
- **createReadOnlyInterceptor**: 只读服务配置，适用于搜索服务
- **createMultiTenantInterceptor**: 多租户配置
- **createCustomInterceptor**: 自定义配置

## 自动填充功能

### 1. 基础字段自动填充

所有继承`BaseEntity`的实体类都会自动填充以下字段：

```java

@TableField(value = "created_at", fill = FieldFill.INSERT)
private LocalDateTime createdAt;

@TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
private LocalDateTime updatedAt;

@TableField(value = "version", fill = FieldFill.INSERT)
private Integer version;
```

### 2. 操作人字段自动填充

如果实体类包含以下字段，会自动填充当前用户ID：

```java

@TableField(value = "create_by", fill = FieldFill.INSERT)
private Long createBy;

@TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
private Long updateBy;
```

### 3. 业务特定字段自动填充

各服务可以根据业务需要填充特定字段：

#### 订单服务

- `orderStatus`: 默认值 1（待支付）
- `paymentStatus`: 默认值 0（未支付）
- `orderSource`: 默认值 "WEB"

#### 库存服务

- `stockStatus`: 默认值 1（正常）
- `frozenQuantity`: 默认值 0

#### 商品服务

- `status`: 默认值 1（上架）
- `sortOrder`: 默认值 0
- `salesCount`: 默认值 0
- `viewCount`: 默认值 0

#### 支付服务

- `paymentStatus`: 默认值 0（待支付）
- `paymentMethod`: 默认值 "ALIPAY"
- `paymentChannel`: 默认值 "WEB"

#### 用户服务

- `status`: 默认值 1（正常）
- `userType`: 默认值 "USER"
- `registerSource`: 默认值 "WEB"
- `lastLoginTime`: 默认值当前时间

## 使用指南

### 1. 使用默认配置

如果服务不需要特殊配置，可以直接使用common-module提供的默认配置，无需额外配置。

### 2. 使用预定义模板

```java

@Configuration
public class MyServiceMybatisPlusConfig {

    @Bean
    @Primary
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        // 使用高并发场景配置
        return MybatisPlusConfigFactory.createHighConcurrencyInterceptor(DbType.MYSQL);
    }
}
```

### 3. 自定义配置

```java

@Configuration
public class CustomMybatisPlusConfig {

    @Bean
    @Primary
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        return MybatisPlusConfigFactory.createCustomInterceptor(
                new MybatisPlusConfigFactory.InterceptorBuilder()
                        .dbType(DbType.MYSQL)
                        .pagination(true, 1000L)
                        .optimisticLocker(true)
                        .blockAttack(false)
                        .build()
        );
    }
}
```

### 4. 自定义元数据处理器

```java

@Bean
@Primary
public MetaObjectHandler metaObjectHandler() {
    return new CustomMetaObjectHandler();
}

public static class CustomMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        // 基础字段填充
        LocalDateTime now = LocalDateTime.now();
        this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "version", Integer.class, 1);

        // 自定义字段填充
        if (metaObject.hasGetter("customField")) {
            this.strictInsertFill(metaObject, "customField", String.class, "defaultValue");
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
    }
}
```

## 最佳实践

### 1. 服务配置选择

- **高并发服务**（订单、库存、支付）：使用`createHighConcurrencyInterceptor`
- **标准业务服务**（用户、商品）：使用`createStandardInterceptor`
- **只读服务**（搜索、报表）：使用`createReadOnlyInterceptor`
- **多租户服务**：使用`createMultiTenantInterceptor`

### 2. 实体类设计

```java

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("your_table")
public class YourEntity extends BaseEntity<YourEntity> {

    // 业务字段
    @TableField("business_field")
    private String businessField;

    // 如需操作人记录，添加以下字段
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;
}
```

### 3. 配置优先级

1. 服务级配置（使用@Primary注解）
2. common-module默认配置
3. Spring Boot自动配置

## 注意事项

1. **@Primary注解**：各服务的配置类必须使用@Primary注解来覆盖默认配置
2. **字段命名**：确保数据库字段名与实体类字段名匹配（使用下划线命名）
3. **时间字段**：统一使用LocalDateTime类型
4. **乐观锁**：高并发场景必须启用乐观锁插件
5. **分页限制**：根据业务场景设置合适的分页大小限制

## 扩展功能

### 1. 多租户支持

```java

@Bean
public TenantLineHandler tenantLineHandler() {
    return new TenantLineHandler() {
        @Override
        public Expression getTenantId() {
            // 返回租户ID
            return new LongValue(getCurrentTenantId());
        }

        @Override
        public boolean ignoreTable(String tableName) {
            // 忽略某些表的多租户处理
            return "system_config".equals(tableName);
        }
    };
}
```

### 2. 数据权限

可以通过自定义拦截器实现数据权限控制：

```java
public class DataPermissionInterceptor implements InnerInterceptor {
    // 实现数据权限逻辑
}
```

这样的配置架构既保持了灵活性，又确保了一致性，让各个服务可以根据自己的业务特点进行优化配置。
