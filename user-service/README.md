# 用户服务 (user-service)

## 1. 模块概述

用户服务是电商平台系统中的核心服务之一，负责管理用户信息、用户地址、用户头像等与用户相关的功能。该服务基于Spring Boot和Spring Cloud构建，采用微服务架构设计，支持高并发访问和水平扩展。

### 1.1 核心功能

- 用户注册与登录
- 用户信息管理
- 用户地址管理
- 用户头像上传与管理
- 用户权限控制
- 用户数据缓存优化

### 1.2 技术栈

- **核心框架**: Spring Boot 3.5.3, Spring Cloud 2025.0.0
- **安全框架**: Spring Security, OAuth2 Resource Server
- **数据库**: MySQL 9.3.0, MyBatis-Plus 3.5.12
- **缓存**: Redis 8.2-rc1
- **对象存储**: MinIO
- **API文档**: Swagger/OpenAPI 3.0, Knife4j
- **服务治理**: Nacos 3.0.2
- **对象映射**: MapStruct 1.6.3
- **其他**: Lombok

## 2. 服务架构

### 2.1 整体架构

用户服务采用经典的分层架构模式，从上到下分为：

```
┌─────────────────────────────────────────────────────────────┐
│                      API Controller Layer                   │
├─────────────────────────────────────────────────────────────┤
│                        Service Layer                        │
├─────────────────────────────────────────────────────────────┤
│                     Data Access Layer                       │
├─────────────────────────────────────────────────────────────┤
│                      Database Layer                         │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 模块结构

```
user-service/
├── src/main/java/com/cloud/user/
│   ├── UserApplication.java              # 启动类
│   ├── config/                          # 配置类
│   ├── controller/                      # 控制器层
│   ├── converter/                       # 对象转换器
│   ├── exception/                       # 异常处理
│   ├── interceptor/                     # 拦截器
│   ├── mapper/                          # 数据访问层
│   ├── module/entity/                   # 实体类
│   └── service/                         # 业务逻辑层
│       └── impl/                        # 业务逻辑实现
├── src/main/resources/
│   ├── mapper/                          # MyBatis XML映射文件
│   ├── application.yml                  # 主配置文件
│   └── application-dev.yml              # 开发环境配置
└── src/test/java/com/cloud/user/        # 测试代码
```

## 3. 核心功能详解

### 3.1 用户管理

#### 3.1.1 用户注册
用户可以通过提供用户名、密码、手机号等信息进行注册。系统会对用户名进行唯一性校验，并对密码进行BCrypt加密存储。

#### 3.1.2 用户信息管理
支持用户信息的查询、更新操作。用户可以修改昵称、手机号、邮箱等信息。管理员可以对用户进行启用、禁用、删除等操作。

#### 3.1.3 用户权限控制
基于Spring Security和OAuth2实现细粒度的权限控制：
- 普通用户只能操作自己的信息
- 管理员可以操作所有用户信息
- 不同角色具有不同的操作权限

### 3.2 地址管理

#### 3.2.1 地址添加
用户可以添加多个收货地址，包括收货人姓名、手机号、详细地址等信息。

#### 3.2.2 地址维护
支持地址的查询、更新、删除操作，并提供权限验证，确保用户只能操作自己的地址。

### 3.3 头像管理

#### 3.3.1 头像上传
支持用户上传头像图片，系统会将图片存储到MinIO对象存储中，并生成访问URL。

#### 3.3.2 头像获取
提供接口根据用户ID获取用户头像，支持本地文件和MinIO存储两种方式。

## 4. 核心组件说明

### 4.1 配置类

- `CacheConfig`: Redis缓存配置
- `JwtConfigProperties`: JWT配置属性
- `Knife4jConfig`: API文档配置
- `MinioConfig`: MinIO对象存储配置
- `MyBatisPlusConfig`: MyBatis-Plus配置
- `ResourceServerConfig`: OAuth2资源服务器配置
- `SecurityConfig`: Spring Security安全配置
- `WebConfig`: Web相关配置
- `ActuatorConfig`: 监控配置

### 4.2 控制器层

- `UserManageController`: 用户管理接口
- `UserQueryController`: 用户查询接口
- `AddressController`: 地址管理接口
- `UserAvatarController`: 用户头像管理接口
- `UserFeignController`: Feign客户端接口

### 4.3 服务层

- `UserService`: 用户服务接口
- `UserServiceImpl`: 用户服务实现
- `UserAddressService`: 用户地址服务接口
- `UserAddressServiceImpl`: 用户地址服务实现
- `UserAvatarService`: 用户头像服务接口
- `UserAvatarServiceImpl`: 用户头像服务实现

### 4.4 异常处理

- `GlobalExceptionHandler`: 全局异常处理器
- `UserServiceException`: 用户服务自定义异常

## 5. 数据库设计

### 5.1 用户表 (users)

| 字段名 | 类型 | 描述 |
|--------|------|------|
| id | BIGINT | 主键 |
| username | VARCHAR(50) | 用户名 |
| password | VARCHAR(100) | 密码(加密存储) |
| nickname | VARCHAR(50) | 昵称 |
| phone | VARCHAR(20) | 手机号 |
| email | VARCHAR(100) | 邮箱 |
| avatar_url | VARCHAR(255) | 头像URL |
| user_type | VARCHAR(20) | 用户类型(ADMIN/USER) |
| status | TINYINT | 状态(0-禁用,1-启用) |
| deleted | TINYINT | 逻辑删除标识 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

### 5.2 用户地址表 (user_address)

| 字段名 | 类型 | 描述 |
|--------|------|------|
| id | BIGINT | 主键 |
| user_id | BIGINT | 用户ID |
| receiver_name | VARCHAR(50) | 收货人姓名 |
| receiver_phone | VARCHAR(20) | 收货人手机号 |
| province | VARCHAR(50) | 省 |
| city | VARCHAR(50) | 市 |
| district | VARCHAR(50) | 区 |
| detail_address | VARCHAR(200) | 详细地址 |
| is_default | TINYINT | 是否默认地址 |
| deleted | TINYINT | 逻辑删除标识 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

## 6. API接口文档

### 6.1 用户管理接口

#### 注册用户
```
POST /user/create/user
权限: ADMIN
请求体:
{
  "username": "用户名",
  "password": "密码",
  "phone": "手机号",
  "email": "邮箱"
}
```

#### 更新用户
```
PUT /user/update/{id}
权限: ADMIN 或 用户本人
请求体:
{
  "nickname": "昵称",
  "phone": "手机号",
  "email": "邮箱"
}
```

#### 删除用户
```
DELETE /user/delete/{id}
权限: ADMIN
```

#### 禁用用户
```
PUT /user/disable/{id}
权限: ADMIN
```

#### 启用用户
```
PUT /user/enable/{id}
权限: ADMIN
```

### 6.2 用户查询接口

#### 获取当前用户信息
```
GET /user/info
权限: 所有登录用户
```

#### 获取所有用户
```
GET /user/admin/users
权限: ADMIN
```

#### 分页获取用户列表
```
GET /user/admin/users/page?page=1&size=10&username=关键字
权限: ADMIN
```

### 6.3 地址管理接口

#### 新增地址
```
POST /address/add
权限: 所有登录用户
请求体:
{
  "receiverName": "收货人姓名",
  "receiverPhone": "收货人手机号",
  "province": "省",
  "city": "市",
  "district": "区",
  "detailAddress": "详细地址"
}
```

#### 更新地址
```
POST /address/update
权限: 地址所属用户
请求体:
{
  "id": "地址ID",
  "receiverName": "收货人姓名",
  "receiverPhone": "收货人手机号",
  "province": "省",
  "city": "市",
  "district": "区",
  "detailAddress": "详细地址"
}
```

#### 删除地址
```
POST /address/delete
权限: 地址所属用户
请求体:
{
  "id": "地址ID"
}
```

#### 获取地址详情
```
POST /address/get
权限: 地址所属用户
请求体:
{
  "id": "地址ID"
}
```

### 6.4 头像管理接口

#### 上传头像
```
POST /user/avatar/upload
权限: 所有登录用户
参数: file (文件)
```

#### 获取头像
```
GET /user/avatar/{userId}
权限: 所有用户
```

## 7. 安全设计

### 7.1 认证机制
使用OAuth2 Resource Server进行认证，通过JWT Token验证用户身份。

### 7.2 权限控制
基于角色的访问控制(RBAC)：
- ROLE_ADMIN: 管理员角色，可以操作所有用户数据
- ROLE_USER: 普通用户角色，只能操作自己的数据

### 7.3 数据安全
- 密码使用BCrypt加密存储
- 敏感操作进行权限验证
- 防止越权访问

## 8. 性能优化

### 8.1 缓存策略
使用Redis缓存用户信息和地址信息，减少数据库访问压力。

### 8.2 数据库优化
- 合理设计索引
- 使用MyBatis-Plus提高开发效率
- 逻辑删除避免数据丢失

## 9. 部署说明

### 9.1 环境要求
- JDK 17+
- MySQL 8.0+
- Redis 6.0+
- MinIO

### 9.2 配置文件
主要配置项在`application-dev.yml`中：
- 数据库连接配置
- Redis连接配置
- MinIO配置
- Nacos配置

### 9.3 启动方式
```bash
# 编译打包
mvn clean package

# 运行服务
java -jar user-service-0.0.1-SNAPSHOT.jar
```

## 10. 监控与运维

### 10.1 健康检查
通过Actuator提供健康检查端点：
- `/actuator/health`: 健康状态
- `/actuator/info`: 应用信息

### 10.2 日志记录
使用Slf4j记录详细的操作日志和错误日志，便于问题排查。

## 11. 测试策略

### 11.1 单元测试
包含服务层和控制器层的单元测试，确保核心功能正确性。

### 11.2 集成测试
使用MockMvc进行接口集成测试，验证API行为。

## 12. 扩展性设计

### 12.1 微服务集成
通过Feign客户端与其他服务进行通信。

### 12.2 可配置性
通过Nacos配置中心实现配置的动态更新。