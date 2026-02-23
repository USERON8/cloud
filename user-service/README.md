# User Service (用户服务)

## 服务概述

User Service 是微服务架构中的**核心业务服务**,负责用户、商户、管理员三类用户的完整生命周期管理。提供用户注册、信息管理、地址管理、数据导出、统计分析等全方位功能,并为
auth-service 提供用户认证数据支持。

- **服务端口**: 8082
- **服务名称**: user-service
- **数据库**: MySQL (users数据库)
- **用户类型**: USER(普通用户) | MERCHANT(商户) | ADMIN(管理员)

## 技术栈

| 技术                         | 版本                 | 用途            |
|----------------------------|--------------------|---------------|
| Spring Boot                | 3.5.3              | 应用框架          |
| MySQL                      | 9.3.0              | 持久化存储         |
| MyBatis Plus               | 最新                 | ORM框架         |
| Redis                      | -                  | 缓存、分布式锁       |
| Redisson                   | -                  | 分布式锁实现        |
| Spring Security OAuth2     | -                  | 资源服务器(JWT验证)  |
| Spring Cloud Alibaba Nacos | 2025.0.0.0-preview | 服务注册与配置       |
| RocketMQ                   | -                  | 异步消息(用户事件、日志) |
| MapStruct                  | 1.5.5.Final        | DTO转换         |
| MinIO                      | -                  | 头像文件存储        |

## 核心功能

### 1. 用户管理 (/api/manage/users)

**UserManageController** - 普通用户管理

- ✅ POST `/api/manage/users` - 创建用户
- ✅ PUT `/api/manage/users/{id}` - 更新用户信息
- ✅ DELETE `/api/manage/users/{id}` - 删除用户(软删除)
- ✅ PUT `/api/manage/users/batch/activate` - 批量激活用户
- ✅ PUT `/api/manage/users/batch/deactivate` - 批量禁用用户
- ✅ POST `/api/manage/users/avatar/upload` - 上传头像(支持本地/MinIO)
- ✅ PUT `/api/manage/users/{id}/password` - 修改密码
- ✅ PUT `/api/manage/users/current/password` - 修改当前用户密码

### 2. 用户查询 (/api/query/users)

**UserQueryController** - 用户查询与搜索

- ✅ GET `/api/query/users/{id}` - 根据ID查询用户
- ✅ GET `/api/query/users/username/{username}` - 根据用户名查询
- ✅ GET `/api/query/users/email/{email}` - 根据邮箱查询
- ✅ GET `/api/query/users` - 分页查询用户列表
- ✅ GET `/api/query/users/search` - 关键词搜索用户
- ✅ GET `/api/query/users/current` - 获取当前登录用户信息
- ✅ GET `/api/query/users/{userId}/exists` - 检查用户是否存在

### 3. 用户统计 (/api/statistics)

**UserStatisticsController** - 用户数据统计分析

- ✅ GET `/api/statistics/users/dashboard` - 用户统计仪表板
- ✅ GET `/api/statistics/users/growth-trend` - 用户增长趋势(按天/月/年)
- ✅ GET `/api/statistics/users/type-distribution` - 用户类型分布
- ✅ GET `/api/statistics/users/registration-source` - 注册来源统计
- ✅ GET `/api/statistics/users/active-users` - 活跃用户统计
- ✅ GET `/api/statistics/users/retention-rate` - 用户留存率分析
- ✅ POST `/api/statistics/users/export` - 导出用户统计报表

### 4. 用户地址 (/api/user/address)

**UserAddressController** - 用户收货地址管理

- ✅ POST `/api/user/address` - 创建收货地址
- ✅ PUT `/api/user/address/{id}` - 更新收货地址
- ✅ DELETE `/api/user/address/{id}` - 删除收货地址
- ✅ GET `/api/user/address/{id}` - 查询地址详情
- ✅ GET `/api/user/address/user/{userId}` - 获取用户所有地址
- ✅ GET `/api/user/address/user/{userId}/default` - 获取默认地址
- ✅ PUT `/api/user/address/{id}/default` - 设置默认地址

### 5. 商户管理 (/api/merchant)

**MerchantController** - 商户信息管理

- ✅ POST `/api/merchant` - 创建商户
- ✅ PUT `/api/merchant/{id}` - 更新商户信息
- ✅ GET `/api/merchant/{id}` - 查询商户详情
- ✅ GET `/api/merchant` - 分页查询商户列表
- ✅ DELETE `/api/merchant/{id}` - 删除商户

**MerchantAuthController** - 商户认证管理

- ✅ POST `/api/merchant/auth/submit` - 提交商户认证申请
- ✅ POST `/api/merchant/auth/{id}/approve` - 审批通过
- ✅ POST `/api/merchant/auth/{id}/reject` - 审批拒绝
- ✅ GET `/api/merchant/auth/{id}` - 查询认证详情
- ✅ GET `/api/merchant/auth/merchant/{merchantId}` - 查询商户认证状态
- ✅ GET `/api/merchant/auth` - 分页查询认证列表(支持状态筛选)

### 6. 管理员管理 (/api/admin)

**AdminController** - 管理员账户管理

- ✅ POST `/api/admin` - 创建管理员
- ✅ PUT `/api/admin/{id}` - 更新管理员信息
- ✅ GET `/api/admin/{id}` - 查询管理员详情
- ✅ GET `/api/admin/username/{username}` - 根据用户名查询
- ✅ GET `/api/admin` - 分页查询管理员列表
- ✅ DELETE `/api/admin/{id}` - 删除管理员
- ✅ PUT `/api/admin/{id}/password` - 重置管理员密码

### 7. 内部服务接口 (/internal/users)

**UserFeignController** - 供其他服务调用(不对外暴露)

- ✅ POST `/internal/users/register` - 用户注册(供auth-service调用)
- ✅ GET `/internal/users/username/{username}` - 根据用户名查询
- ✅ GET `/internal/users/{userId}/password` - 获取用户密码(供认证使用)
- ✅ GET `/internal/users/email/{email}` - 根据邮箱查询
- ✅ POST `/internal/users/oauth2/find-or-create` - OAuth2用户查找或创建

### 8. 系统监控 (/api/thread-pool)

**ThreadPoolMonitorController** - 线程池监控

- ✅ GET `/api/thread-pool/status` - 查看线程池状态(核心线程数、活跃线程数、队列大小等)
- ✅ GET `/api/thread-pool/metrics` - 线程池性能指标(已完成任务数、拒绝任务数等)
- ✅ POST `/api/thread-pool/adjust` - 动态调整线程池参数(核心线程数、最大线程数)
- ✅ GET `/api/thread-pool/health` - 线程池健康检查(检测线程池阻塞、队列积压)

## 数据模型

### 核心实体

#### User (users表)

```sql
CREATE TABLE users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL UNIQUE,    -- 用户名
  password VARCHAR(255) NOT NULL,          -- 加密密码
  phone VARCHAR(20),                       -- 手机号
  nickname VARCHAR(50),                    -- 昵称
  avatar_url VARCHAR(500),                 -- 头像URL
  email VARCHAR(100),                      -- 邮箱
  status INT DEFAULT 1,                    -- 0:禁用 1:启用
  user_type VARCHAR(20) NOT NULL,          -- USER/MERCHANT/ADMIN
  github_id BIGINT,                        -- GitHub用户ID
  github_username VARCHAR(100),            -- GitHub用户名
  oauth_provider VARCHAR(50),              -- OAuth提供商
  oauth_provider_id VARCHAR(100),          -- OAuth用户ID
  created_at DATETIME,
  updated_at DATETIME,
  deleted TINYINT DEFAULT 0,
  version INT DEFAULT 0
);
```

#### UserAddress (user_addresses表)

```sql
CREATE TABLE user_addresses (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,                 -- 用户ID
  receiver_name VARCHAR(50) NOT NULL,      -- 收货人
  receiver_phone VARCHAR(20) NOT NULL,     -- 收货电话
  province VARCHAR(50),                    -- 省份
  city VARCHAR(50),                        -- 城市
  district VARCHAR(50),                    -- 区县
  detail_address VARCHAR(200) NOT NULL,    -- 详细地址
  is_default TINYINT DEFAULT 0,            -- 是否默认地址
  created_at DATETIME,
  updated_at DATETIME,
  deleted TINYINT DEFAULT 0
);
```

#### Merchant (merchants表)

```sql
CREATE TABLE merchants (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,                 -- 关联用户ID
  shop_name VARCHAR(100) NOT NULL,         -- 店铺名称
  business_license VARCHAR(100),           -- 营业执照号
  contact_person VARCHAR(50),              -- 联系人
  contact_phone VARCHAR(20),               -- 联系电话
  status INT DEFAULT 1,                    -- 0:禁用 1:正常 2:待审核
  created_at DATETIME,
  updated_at DATETIME,
  deleted TINYINT DEFAULT 0
);
```

#### MerchantAuth (merchant_auth表)

```sql
CREATE TABLE merchant_auth (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  merchant_id BIGINT NOT NULL,             -- 商户ID
  auth_type VARCHAR(20) NOT NULL,          -- 认证类型
  auth_status VARCHAR(20) NOT NULL,        -- PENDING/APPROVED/REJECTED
  business_license_url VARCHAR(500),       -- 营业执照图片
  id_card_front_url VARCHAR(500),          -- 身份证正面
  id_card_back_url VARCHAR(500),           -- 身份证反面
  reject_reason VARCHAR(500),              -- 拒绝原因
  approved_by BIGINT,                      -- 审批人
  approved_at DATETIME,                    -- 审批时间
  created_at DATETIME,
  updated_at DATETIME
);
```

#### Admin (admins表)

```sql
CREATE TABLE admins (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,                 -- 关联用户ID
  admin_level INT DEFAULT 1,               -- 管理员等级
  permissions VARCHAR(1000),               -- 权限列表
  department VARCHAR(100),                 -- 所属部门
  created_at DATETIME,
  updated_at DATETIME,
  deleted TINYINT DEFAULT 0
);
```

## 依赖服务

| 服务           | 用途        | 通信方式                 |
|--------------|-----------|----------------------|
| auth-service | 提供认证数据支持  | 被Feign调用(被动)         |
| MySQL        | 用户数据持久化   | JDBC                 |
| Redis        | 缓存、分布式锁   | RedisTemplate        |
| MinIO        | 头像、认证材料存储 | MinIO SDK            |
| RocketMQ     | 用户事件发送    | Spring Cloud Stream  |
| Nacos        | 服务注册、配置管理 | Spring Cloud Alibaba |

## 配置说明

### 端口配置

```yaml
server:
  port: 8082
```

### 数据库配置

```yaml
# 通过Nacos配置中心管理
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/users?useUnicode=true&characterEncoding=utf8
    username: root
    password: ***
```

### Redis配置

```yaml
# 通过Nacos配置中心管理
spring:
  redis:
    host: localhost
    port: 6379
    database: 0
```

### OAuth2 资源服务器

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          jwk-set-uri: http://127.0.0.1:8081/.well-known/jwks.json
          issuer-uri: http://127.0.0.1:8081
```

### RocketMQ 配置

```yaml
spring:
  cloud:
    stream:
      bindings:
        user-producer-out-0:      # 用户事件
          destination: user-events
        userLog-out-0:            # 用户日志
          destination: LOG_USER_TOPIC
        logProducer-out-0:        # 通用日志
          destination: LOG_COLLECTION_TOPIC
```

### 文件上传配置

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB
```

### 自定义配置

```yaml
user:
  async:
    enabled: true           # 启用异步处理
  notification:
    enabled: true           # 启用通知功能
  statistics:
    enabled: true           # 启用统计功能
```

## 开发状态

### ✅ 已完成功能

1. **用户基础管理**
    - [x] 用户CRUD完整实现
    - [x] 用户状态管理(启用/禁用)
    - [x] 批量操作(批量激活/禁用)
    - [x] 密码加密存储(BCrypt)
    - [x] 头像上传(本地/MinIO)
    - [x] 软删除支持

2. **用户查询与搜索**
    - [x] 多维度查询(ID/用户名/邮箱)
    - [x] 分页列表查询
    - [x] 关键词搜索
    - [x] 用户存在性检查
    - [x] 当前用户信息获取

3. **收货地址管理**
    - [x] 地址CRUD操作
    - [x] 默认地址设置
    - [x] 用户多地址支持
    - [x] 地址信息完整性验证

4. **商户管理**
    - [x] 商户基础信息管理
    - [x] 商户认证流程(提交/审批/拒绝)
    - [x] 认证材料上传(营业执照/身份证)
    - [x] 商户状态管理
    - [x] 认证记录查询

5. **管理员管理**
    - [x] 管理员账户管理
    - [x] 管理员等级设置
    - [x] 权限配置
    - [x] 密码重置功能

6. **统计分析**
    - [x] 用户统计仪表板
    - [x] 用户增长趋势分析
    - [x] 用户类型分布
    - [x] 注册来源统计
    - [x] 活跃用户统计
    - [x] 用户留存率分析
    - [x] Excel数据导出

7. **OAuth2集成**
    - [x] GitHub OAuth2用户创建
    - [x] OAuth用户信息同步
    - [x] 多OAuth提供商支持

8. **内部服务接口**
    - [x] 用户注册接口(供auth-service)
    - [x] 用户认证信息查询
    - [x] OAuth2用户查找或创建

9. **异步处理**
    - [x] 用户事件异步发送(RocketMQ)
    - [x] 日志异步记录
    - [x] 自定义线程池配置
    - [x] 线程池监控与动态调整
    - [x] 线程池健康检查
    - [x] 异步任务执行追踪

10. **数据转换**
    - [x] MapStruct自动转换
    - [x] UserConverter(Entity/DTO/VO)
    - [x] AdminConverter
    - [x] MerchantConverter
    - [x] UserAddressConverter
    - [x] MerchantAuthConverter

11. **性能优化**
    - [x] 用户信息缓存(Redis)
    - [x] 多线程异步处理
    - [x] 批量操作优化
    - [x] 分页查询优化

### 🚧 进行中功能

1. **数据导出优化**
    - [ ] 大数据量导出优化(流式导出)
    - [ ] 自定义导出模板
    - [ ] 定时导出任务

2. **用户画像**
    - [ ] 用户行为分析
    - [ ] 用户标签体系
    - [ ] 用户偏好推荐

### 📋 计划中功能

1. **实名认证**
    - [ ] 身份证OCR识别
    - [ ] 人脸识别验证
    - [ ] 第三方实名认证对接

2. **积分系统**
    - [ ] 用户积分管理
    - [ ] 积分规则配置
    - [ ] 积分兑换功能

3. **会员等级**
    - [ ] 会员等级体系
    - [ ] 等级权益配置
    - [ ] 自动升降级

4. **用户标签**
    - [ ] 标签管理
    - [ ] 用户打标签
    - [ ] 基于标签的用户分组

5. **消息通知**
    - [ ] 站内信
    - [ ] 短信通知
    - [ ] 邮件通知
    - [ ] Push推送

### ⚠️ 技术债

1. **性能优化**
    - 用户列表查询考虑增加Redis缓存
    - 统计分析考虑使用ClickHouse或ES
    - 头像上传优化(CDN加速)

2. **数据安全**
    - 敏感信息脱敏(手机号/邮箱/身份证)
    - 数据导出权限细化
    - 操作日志完善

3. **测试覆盖**
    - 单元测试覆盖率提升
    - 集成测试补充
    - 性能测试(并发注册/查询)

## 本地运行

### 前置条件

1. **基础设施启动**

```bash
cd docker
docker-compose up -d mysql redis nacos rocketmq minio
```

2. **数据库初始化**

```bash
# 导入SQL脚本
mysql -h localhost -u root -p < sql/init/user-service/init.sql
```

### 启动服务

```bash
# 使用Maven启动
cd user-service
mvn spring-boot:run

# 或指定环境
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 或使用JAR包启动
mvn clean package -DskipTests
java -jar target/user-service-0.0.1-SNAPSHOT.jar
```

### 验证服务

```bash
# 健康检查
curl http://localhost:8082/actuator/health

# 查询用户列表(需要token)
curl -H "Authorization: Bearer YOUR_TOKEN" \
  http://localhost:8082/api/query/users

# API文档
浏览器打开: http://localhost:8082/doc.html
```

## 测试

### 运行单元测试

```bash
mvn test
```

### 运行特定测试类

```bash
mvn test -Dtest=UserServiceImplTest
```

### 运行集成测试

```bash
mvn test -Dtest=UserManageControllerTest
```

### 手动测试流程

#### 1. 创建用户

```bash
curl -X POST "http://localhost:8082/api/manage/users" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "newuser",
    "password": "password123",
    "email": "newuser@example.com",
    "phone": "13900139000",
    "nickname": "新用户",
    "userType": "USER"
  }'
```

#### 2. 查询用户

```bash
curl -H "Authorization: Bearer YOUR_TOKEN" \
  "http://localhost:8082/api/query/users/1"
```

#### 3. 更新用户

```bash
curl -X PUT "http://localhost:8082/api/manage/users/1" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "nickname": "更新昵称",
    "avatarUrl": "https://example.com/avatar.jpg"
  }'
```

#### 4. 创建收货地址

```bash
curl -X POST "http://localhost:8082/api/user/address" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "receiverName": "张三",
    "receiverPhone": "13800138000",
    "province": "广东省",
    "city": "深圳市",
    "district": "南山区",
    "detailAddress": "科技园XX路XX号",
    "isDefault": 1
  }'
```

## 注意事项

### 权限控制

所有API端点都受OAuth2保护,需要在请求头中携带有效的JWT令牌:

```
Authorization: Bearer eyJhbGciOiJSUzI1NiIs...
```

权限要求:

- **用户接口**: 需要 `SCOPE_read` 或 `SCOPE_write`
- **管理员接口**: 需要 `ROLE_ADMIN` + `SCOPE_admin:write`
- **内部接口**: 仅限服务间调用(通过Feign)

### 数据安全

1. **密码安全**: 使用BCrypt加密,不存储明文密码
2. **敏感数据**: 生产环境必须对手机号/邮箱/身份证脱敏
3. **文件上传**: 限制文件大小和格式,防止恶意上传
4. **SQL注入**: 使用MyBatis Plus预编译防止SQL注入

### 性能建议

1. **缓存策略**: 用户信息使用Redis缓存,TTL 30分钟
2. **分页查询**: 大数据量查询必须分页,默认10条/页
3. **批量操作**: 批量激活/禁用使用事务保证一致性
4. **异步处理**: 非关键路径操作使用异步(如发送事件/日志)

### 监控指标

重点关注:

- 用户注册速率 (registrations/hour)
- 用户查询QPS
- 数据库连接池使用率
- Redis缓存命中率
- 异步线程池队列长度

## 相关文档

- [API文档 - User Service](../doc/services/user/API_DOC_USER_SERVICE.md)
- [项目整体文档](../doc/README.md)
- [MapStruct使用指南](../common-module/src/main/java/com/cloud/common/converter/MapStructGuide.java)

## 快速链接

- Knife4j API文档: http://localhost:8082/doc.html
- Actuator Health: http://localhost:8082/actuator/health
- Nacos控制台: http://localhost:8848/nacos
- MinIO控制台: http://localhost:9001
