# 项目规范文档

## 1. RocketMQ Topic命名规范

为了统一管理消息队列中的主题，我们制定了以下命名规范：

### 1.1 日志相关Topic
- LOG_ADMIN_TOPIC: 管理员操作日志
- LOG_MERCHANT_TOPIC: 商家操作日志
- LOG_USER_TOPIC: 用户操作日志
- LOG_PRODUCT_TOPIC: 商品操作日志
- LOG_PAYMENT_TOPIC: 支付操作日志
- LOG_STOCK_TOPIC: 库存操作日志
- LOG_ORDER_TOPIC: 订单操作日志

### 1.2 业务相关Topic
- PAYMENT_CREATE_TOPIC: 支付创建消息
- STOCK_FREEZE_TOPIC: 库存冻结消息
- ORDER_PAYMENT_SUCCESS_TOPIC: 订单支付成功消息
- ORDER_REFUND_SUCCESS_TOPIC: 订单退款成功消息
- ORDER_CREATE_TOPIC: 订单创建消息
- ORDER_COMPLETE_TOPIC: 订单完成消息

### 1.3 命名规范说明
1. 所有Topic名称使用大写字母
2. 使用下划线分割单词
3. 按照"业务领域_功能描述_TOPIC"的格式命名
4. 日志类消息统一以"LOG_"开头
5. 业务类消息以具体业务领域开头

## 2. Redis缓存命名规范

为了统一管理Redis缓存，我们制定了以下命名规范：

### 2.1 缓存前缀命名
- user:* : 用户相关信息缓存
- merchant:* : 商家相关信息缓存
- product:* : 商品相关信息缓存
- stock:* : 库存相关信息缓存
- order:* : 订单相关信息缓存

### 2.2 缓存过期时间
- 用户信息缓存: 1小时
- 商家信息缓存: 1小时
- 商品信息缓存: 30分钟
- 库存信息缓存: 10分钟
- 订单信息缓存: 15分钟

## 3. 通用异常规范

为了统一异常处理，避免重复代码，我们在common-module中定义了以下通用异常类：

### 3.1 异常类层次结构
- BusinessException: 业务异常基类
  - ValidationException: 参数校验异常
  - ResourceNotFoundException: 资源未找到异常
  - PermissionException: 权限异常
  - ConcurrencyException: 并发异常
- SystemException: 系统异常类

### 3.2 异常使用规范
1. 业务逻辑相关的异常应继承BusinessException或其子类
2. 系统级别的异常应使用SystemException
3. 参数校验失败应使用ValidationException
4. 资源未找到应使用ResourceNotFoundException
5. 权限不足应使用PermissionException
6. 并发操作冲突应使用ConcurrencyException

### 3.3 异常处理规范
1. 所有服务应使用GlobalExceptionHandler进行统一异常处理
2. 不应在Controller层手动捕获这些通用异常
3. 异常信息应包含足够的上下文信息，便于问题排查
4. 每个服务都应创建自己的全局异常处理器，继承common-module中的处理逻辑
5. Service层方法应直接抛出通用异常，不应自定义异常类
6. Controller层应捕获并处理特定的系统异常（如NumberFormatException等），转换为适当的业务异常

### 3.4 服务内具体业务异常规范
1. 当通用异常类无法满足特定业务场景需求时，服务可以创建仅属于服务内的具体业务异常
2. 服务内具体业务异常必须继承BusinessException或其子类
3. 服务内具体业务异常应定义在服务的exception包中
4. 服务内具体业务异常应具有明确的业务含义和错误码
5. 服务内具体业务异常应避免与通用异常功能重复
6. 服务内具体业务异常应遵循统一的命名规范，建议以"服务名+具体异常描述+Exception"命名
7. 服务内具体业务异常应有完整的文档注释，说明异常的用途和触发条件
8. 每个服务应创建自己的业务异常基类，继承自BusinessException，作为所有服务特定异常的基类
9. 业务异常类应定义明确的错误码，便于问题诊断和处理
10. 服务特定异常应与全局异常处理器配合使用，提供统一的异常处理机制
11. 全局异常处理器应为每个业务异常类添加对应的处理方法，确保异常能够被正确处理并返回统一的错误响应格式
12. 异常处理应记录详细的日志信息，便于后续的问题排查和系统监控

#### 3.4.1 用户服务业务异常示例
1. UserServiceException - 用户服务业务异常基类
   - 继承自BusinessException
   - 作为所有用户服务特定异常的基类

2. UserNotFoundException - 用户不存在异常
   - 继承自UserServiceException
   - 用于用户查询时用户不存在的情况
   - 错误码: 40401

3. UserAlreadyExistsException - 用户已存在异常
   - 继承自UserServiceException
   - 用于用户注册时用户名已存在的情况
   - 错误码: 40901

4. FileUploadException - 文件上传异常
   - 继承自UserServiceException
   - 用于处理文件上传过程中发生的错误
   - 错误码: 50001

5. FileSizeExceededException - 文件大小超出限制异常
   - 继承自UserServiceException
   - 用于用户上传文件大小超出限制的情况
   - 错误码: 40001

6. AddressPermissionException - 地址权限异常
   - 继承自UserServiceException
   - 用于用户尝试操作不属于自己的地址的情况
   - 错误码: 40301

## 4. 通用工具类规范

为了减少重复代码，提高开发效率，我们在common-module中提供了以下通用工具类：

### 4.1 工具类列表
1. BeanUtils: 对象属性复制和转换工具类
2. CollectionUtils: 集合处理工具类
3. DateUtils: 日期时间处理工具类
4. JsonUtils: JSON序列化和反序列化工具类
5. LogUtil: 日志记录工具类
6. PageUtils: 分页处理工具类
7. StringUtils: 字符串处理工具类

### 4.2 工具类使用规范
1. 所有服务应优先使用common-module中提供的通用工具类
2. 禁止在服务中创建功能相同的工具类
3. 如需新增工具类功能，应先检查是否已有类似功能
4. 工具类方法应保持静态方法，便于直接调用
5. 工具类应具有良好的文档注释，说明方法用途和参数含义

## 5. 其他规范

### 5.1 代码规范

#### 5.1.1 命名规范

##### 包命名
- 全部小写，使用点分隔符
- 格式：com.cloud.[模块名].[子模块名]

##### 类命名
- 驼峰命名法，首字母大写
- 实体类以具体名词命名，如User、Product
- Service接口不带Impl后缀，实现类加Impl后缀
- Controller类以Controller结尾
- DTO类以DTO结尾
- VO类以VO结尾
- Converter类以Converter结尾

##### 方法命名
- 驼峰命名法，首字母小写
- 查询方法以get、find、query开头
- 修改方法以update、modify开头
- 删除方法以delete、remove开头
- 创建方法以create、save、add开头

##### 变量命名
- 驼峰命名法，首字母小写
- 尽量语义化，避免使用缩写
- 布尔类型变量建议以is、has、can等开头

#### 5.1.2 注释规范

##### 类注释
```
/**
 * 类描述
 *
 * @author 作者名
 * @since 版本号
 */
```

##### 方法注释
```
/**
 * 方法描述
 *
 * @param 参数名 参数描述
 * @return 返回值描述
 * @throws 异常类型 异常描述
 */
```

##### 字段注释
```
/**
 * 字段描述
 */
private String fieldName;
```

#### 5.1.3 代码格式

##### 缩进
- 使用4个空格缩进，不使用Tab

##### 行宽
- 每行不超过120个字符

##### 空行
- 类成员之间空一行
- 方法之间空一行
- 逻辑代码块之间可适当空行

##### 括号
- 左大括号不换行
- 右大括号单独一行

#### 5.1.4 包导入规范

##### 导入顺序
1. Java标准库包 (java.*)
2. 扩展库包 (javax.*)
3. 第三方库包
4. Spring框架包
5. 项目内部包

##### 分组
- 每组之间用空行分隔
- 组内按字母顺序排列

### 5.2 实体类规范

#### 5.2.1 继承
- 所有实体类继承BaseEntity类
- 使用Lombok注解简化代码

#### 5.2.2 注解使用
- 使用MyBatis-Plus注解
- 字段注解使用@TableField
- 主键注解使用@TableId

### 5.3 Controller规范

#### 5.3.1 注解使用
- 使用@RestController注解
- 使用@RequestMapping指定基础路径
- 使用Swagger注解描述接口

#### 5.3.2 方法结构
- 方法参数使用@RequestParam、@PathVariable、@RequestBody等注解
- 返回值使用统一的Result封装

#### 5.3.3 异常处理
- Controller层应捕获并处理特定的系统异常（如NumberFormatException等）
- Controller层应将系统异常转换为适当的业务异常
- Controller层不应直接抛出自定义异常，应通过Service层抛出通用异常

### 5.4 Service规范

#### 5.4.1 接口定义
- Service接口定义业务方法
- 实现类添加@Service注解

#### 5.4.2 实现类
- 继承ServiceImpl基类
- 实现对应的Service接口

### 5.5 Converter规范

#### 5.5.1 使用MapStruct
- 使用@Mapper注解
- 定义INSTANCE常量获取实例

#### 5.5.2 方法命名
- toDTO：实体转DTO
- toEntity：DTO转实体
- toVO：实体转VO

### 5.6 异常处理规范

#### 5.6.1 异常分类
- 业务异常：BusinessException
- 系统异常：SystemException
- 参数校验异常：ValidationException

#### 5.6.2 异常处理
- 使用全局异常处理器统一处理异常
- 异常信息应包含错误码和错误描述
- 对外暴露的异常信息应友好且安全

### 5.7 日志规范

#### 5.7.1 日志级别
- TRACE：最详细的日志信息，通常只在开发环境中使用
- DEBUG：调试信息，用于诊断问题
- INFO：一般信息，用于记录程序运行状态
- WARN：警告信息，表示可能存在问题
- ERROR：错误信息，表示发生了错误但程序可以继续运行
- FATAL：严重错误信息，表示程序无法继续运行

#### 5.7.2 日志内容
- 日志信息应包含足够的上下文信息
- 敏感信息不应记录到日志中
- 异常日志应包含完整的堆栈信息

### 5.8 配置规范

#### 5.8.1 配置文件
- 使用YAML格式配置文件
- 配置项应有明确的注释说明
- 敏感配置应通过环境变量或配置中心管理

#### 5.8.2 配置类
- 使用@Configuration注解
- 配置属性使用@ConfigurationProperties注解

### 5.9 测试规范

#### 5.9.1 单元测试
- 使用JUnit 5
- 测试类命名以Test结尾
- 测试方法命名应描述测试场景

#### 5.9.2 集成测试
- 使用@SpringBootTest注解
- 测试数据应独立且可重复
- 测试后应清理测试数据

### 5.10 文档规范

#### 5.10.1 API文档
- 使用Swagger注解
- 接口描述应清晰准确
- 参数和返回值应有详细说明

#### 5.10.2 代码注释
- 公共方法必须有注释
- 复杂逻辑应有详细注释
- 类和重要字段应有注释

### 5.11 安全规范

#### 5.11.1 认证授权
- 使用Spring Security
- 敏感接口应有权限控制
- 用户身份应有有效验证

#### 5.11.2 数据安全
- 敏感数据应加密存储
- 输入参数应有校验
- SQL查询应使用参数化查询防止注入

### 5.12 性能规范

#### 5.12.1 数据库操作
- 避免N+1查询问题
- 合理使用索引
- 大数据量操作应分批处理

#### 5.12.2 缓存使用
- 合理使用缓存减少数据库访问
- 缓存更新策略应明确
- 缓存失效时间应合理设置

### 5.13 版本控制规范

#### 5.13.1 提交信息
- 提交信息应简洁明了
- 应包含相关任务编号
- 重要变更应有详细说明

#### 5.13.2 分支管理
- 使用Git Flow分支模型
- 功能开发应在feature分支进行
- 发布版本应打标签

### 5.14 部署规范

#### 5.14.1 环境配置
- 不同环境应有独立配置
- 配置应通过环境变量覆盖
- 敏感配置应加密存储

#### 5.14.2 容器化
- 使用Docker容器化部署
- 镜像应尽量精简
- 应有健康检查机制