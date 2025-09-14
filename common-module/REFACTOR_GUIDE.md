# Common Module 重构使用指南

## 概述

本次重构优化了 `common-module` 模块，提取了各服务的公共配置、DTO/VO、异常处理等，减少了代码冗余，提高了代码复用性。

## 主要改进

### 1. 统一基础配置 (BaseConfig)

**文件位置**: `com.cloud.common.config.BaseConfig`

所有服务现在只需要继承 `BaseConfig` 即可获得所有基础配置：

```java
@Configuration
public class ServiceConfig extends BaseConfig {
    // 可以在这里添加服务特定的配置
}
```

**包含的配置**:

- BaseRedisConfig - Redis 配置
- BaseMyBatisPlusConfig - MyBatis-Plus 配置
- BaseJwtConfig - JWT 配置
- BaseWebConfig - Web 配置
- BaseAsyncConfig - 异步配置
- BaseLogConfig - 日志配置
- BaseMetricsConfig - 监控配置
- BaseKnife4jConfig - API 文档配置

### 2. 统一 Knife4j 配置 (BaseKnife4jConfig)

**文件位置**: `com.cloud.common.config.BaseKnife4jConfig`

各服务可以继承此类并自定义 API 文档信息：

```java
@Configuration
public class ServiceKnife4jConfig extends BaseKnife4jConfig {
    @Override
    protected String getServiceTitle() {
        return "用户服务 API 文档";
    }
    
    @Override
    protected String getServiceDescription() {
        return "用户管理相关的 RESTful API 文档";
    }
}
```

### 3. 增强的全局异常处理器

**文件位置**: `com.cloud.common.exception.GlobalExceptionHandler`

现在包含所有常见异常的处理：

- 参数校验异常
- 约束校验异常
- 非法参数异常
- 空指针异常
- 业务异常
- 系统异常等

各服务可以继承此类添加特定异常处理：

```java
@RestControllerAdvice
public class UserServiceExceptionHandler extends GlobalExceptionHandler {
    // 添加用户服务特定的异常处理
}
```

### 4. 通用异常类

#### EntityNotFoundException

**文件位置**: `com.cloud.common.exception.EntityNotFoundException`

用于替代各服务中的 `*NotFoundException`：

```java
// 替代前
throw new UserNotFoundException(userId);

// 替代后
throw EntityNotFoundException.user(userId);
// 或
throw new EntityNotFoundException("用户", userId);
```

#### InvalidStatusException

**文件位置**: `com.cloud.common.exception.InvalidStatusException`

用于状态相关异常：

```java
// 使用示例
throw InvalidStatusException.order("已取消", "支付");
```

#### InsufficientException

**文件位置**: `com.cloud.common.exception.InsufficientException`

用于不足类异常：

```java
// 使用示例
throw InsufficientException.stock(productId, required, available);
throw InsufficientException.balance(userId, required, available);
```

### 5. 统一 DTO/VO 管理

所有的 DTO 和 VO 已经按功能分类整理到 `common-module` 中：

```
common-module/src/main/java/com/cloud/common/domain/
├── dto/
│   ├── auth/          # 认证相关 DTO
│   ├── order/         # 订单相关 DTO
│   ├── payment/       # 支付相关 DTO
│   ├── product/       # 商品相关 DTO
│   ├── stock/         # 库存相关 DTO
│   ├── user/          # 用户相关 DTO
│   └── merchant/      # 商家相关 DTO
└── vo/                # 所有 VO 类
```

### 6. 动态 MyBatis 配置

**文件位置**: `com.cloud.common.config.BaseMyBatisPlusConfig`

现在支持基于服务名自动扫描 Mapper：

- `user-service` → `com.cloud.user.mapper`
- `order-service` → `com.cloud.order.mapper`
- 等等

如需自定义扫描路径，可重写 `getMapperScanPackage()` 方法。

## 迁移指南

### 各服务需要做的修改

#### 1. 简化配置类

**删除或简化以下配置类**：

- RedisConfig (如果只是继承 BaseRedisConfig)
- MyBatisPlusConfig (如果只是添加 @MapperScan)
- 各种重复的基础配置

**修改示例**：

```java
// 原来的配置
@Configuration
@MapperScan("com.cloud.user.mapper")
public class MyBatisPlusConfig extends BaseMyBatisPlusConfig {
}

// 简化后 - 可以直接删除这个类，由 BaseConfig 自动处理
```

#### 2. 更新异常处理

**使用新的通用异常类**：

```java
// 更新前
throw new UserNotFoundException(userId);
throw new OrderNotFoundException(orderId);

// 更新后
throw EntityNotFoundException.user(userId);
throw EntityNotFoundException.order(orderId);
```

#### 3. 更新导入路径

**更新 DTO/VO 导入路径**：

```java
// 更新前
import com.cloud.common.domain.dto.user.UserPageDTO;

// 更新后
import com.cloud.common.domain.dto.user.UserPageQueryDTO;
```

#### 4. 继承基础配置

**在主配置类中继承 BaseConfig**：

```java
@Configuration
@EnableConfigurationProperties
public class UserServiceConfig extends BaseConfig {
    // 服务特定的配置
}
```

### 删除重复的类

以下类可以安全删除：

1. 各服务中简单继承 `BaseRedisConfig` 的 `RedisConfig` 类
2. 各服务中只添加 `@MapperScan` 的 `MyBatisPlusConfig` 类
3. 各服务中的 `*NotFoundException` 类（使用 `EntityNotFoundException` 替代）
4. 各服务中的 `Knife4jConfig` 类（如果没有特殊自定义）

## 注意事项

1. **渐进式迁移**: 建议逐个服务进行迁移，确保测试通过后再进行下一个
2. **保持兼容**: 旧的异常类和 DTO 可以暂时保留，添加 `@Deprecated` 注解
3. **测试验证**: 迁移后务必进行充分测试，确保功能正常
4. **配置验证**: 检查 `application.yml` 中的相关配置是否正确

## 收益

1. **代码减少**: 预计减少 30-50% 的重复配置代码
2. **维护性提升**: 公共配置统一管理，修改一处即可影响所有服务
3. **开发效率**: 新服务只需要继承基础配置，无需重复编写
4. **代码质量**: 统一的异常处理和 DTO 结构，提高代码一致性

## 问题反馈

如在迁移过程中遇到问题，请及时反馈和讨论。
