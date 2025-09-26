# 👥 User Service - 用户服务

## 📋 服务概述

用户服务是Cloud微服务电商平台的核心业务服务，负责管理用户相关的业务逻辑，包括用户注册、登录、个人信息管理、地址管理等功能。

**最近更新时间**: 2025-01-25  
**开发进度**: 90% ✅  
**服务端口**: 8082

### 🔧 技术栈版本

- **Spring Boot**: 3.5.3
- **Spring Cloud**: 2025.0.0
- **MyBatis-Plus**: 3.5.13
- **MySQL**: 8.0+
- **Redis**: 7.0+
- **MapStruct**: 1.6.3
- **RocketMQ**: 5.3.2 (端口39876)
- **OAuth2**: Spring Security OAuth2 Resource Server

## 🌟 核心功能

### 1. 用户管理 ✅
- **用户注册**: 支持用户名/邮箱注册
- **用户登录**: 多种登录方式（用户名、邮箱、OAuth）
- **信息管理**: 用户信息查询、更新、状态管理
- **多级用户**: 普通用户、商家、管理员三级体系

### 2. 地址管理 ✅
- **地址CRUD**: 添加、查询、更新、删除用户地址
- **默认地址**: 智能默认地址设置和管理
- **权限控制**: 基于用户身份的地址访问控制
- **分页查询**: 支持地址信息分页查询

### 3. OAuth2集成 ✅
- **GitHub OAuth**: 支持GitHub第三方登录
- **JWT认证**: 作为OAuth2.1资源服务器
- **权限管理**: 细粒度的权限控制机制

### 4. 事件驱动 ✅
- **用户事件**: 用户变更事件发布
- **日志收集**: 异步日志事件发送
- **消息可靠性**: 重试机制和死信队列

## 🚀 服务配置

### 端口与数据库
```yaml
server:
  port: 8082  # 用户服务端口

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/user_db  # 用户数据库
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:root}
```

### RocketMQ配置
```yaml
spring:
  cloud:
    stream:
      rocketmq:
        binder:
          name-server: 127.0.0.1:39876  # 统一端口
```

### OAuth2配置
```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          jwk-set-uri: http://127.0.0.1:80/.well-known/jwks.json
```

## 🗄️ 数据库设计

### 数据库信息
- **数据库名**: `user_db` (符合xx_db命名规范)
- **字符集**: utf8mb4
- **存储引擎**: InnoDB

### 主要表结构
| 表名 | 说明 | 主要字段 |
|------|------|----------|
| `users` | 用户主表 | id, username, password, email, github_id |
| `user_address` | 用户地址表 | id, user_id, consignee, phone, detail_address |
| `admin` | 管理员表 | id, username, real_name, role |
| `merchant` | 商家表 | id, username, merchant_name, status |
| `merchant_auth` | 商家认证表 | merchant_id, business_license, auth_status |

详细表结构请参考: [用户数据库设计](../sql/init/initdb_user.sql)

## 🔌 API接口

### 用户管理接口
```
GET    /api/v1/user/{id}           # 获取用户信息
PUT    /api/v1/user/{id}           # 更新用户信息  
POST   /api/v1/user/register       # 用户注册
POST   /api/v1/user/login          # 用户登录
POST   /api/v1/user/logout         # 用户登出
```

### 地址管理接口
```
GET    /user/address/list/{userId}        # 获取地址列表
POST   /user/address/add/{userId}         # 添加用户地址
PUT    /user/address/update/{addressId}   # 更新地址信息
DELETE /user/address/delete/{addressId}  # 删除用户地址
GET    /user/address/default/{userId}     # 获取默认地址
POST   /user/address/page                 # 分页查询地址
```

### 权限说明
- 所有接口需要JWT认证
- 用户只能操作自己的数据
- 管理员可以操作所有数据
- 使用`SecurityPermissionUtils`进行权限检查

**API文档地址**: http://localhost:8082/doc.html

## 🔄 消息队列设计

### 生产者事件
- **用户变更事件**: `user-events` topic
- **日志收集事件**: `log-collection-topic` topic

### 消息格式
```json
{
  "eventId": "uuid",
  "eventType": "USER_CREATED",
  "userId": 12345,
  "timestamp": "2025-01-25T10:30:00Z",
  "data": {
    "username": "testuser",
    "email": "test@example.com"
  }
}
```

## 📊 监控指标

### 业务指标
- 用户注册数
- 活跃用户数
- 地址管理操作数
- OAuth登录成功率

### 技术指标
- 接口响应时间
- 数据库连接池使用率
- 缓存命中率
- 消息队列消费延迟

## 🛡️ 安全特性

### 1. 数据安全
- 密码BCrypt加密
- 敏感信息脱敏
- SQL参数化查询

### 2. 接口安全
- JWT Token验证
- 细粒度权限控制
- 请求参数校验
- 防重放攻击

### 3. OAuth集成
- GitHub OAuth2登录
- PKCE安全增强
- Token生命周期管理

## 🚀 部署说明

### 环境要求
- JDK 17+
- MySQL 8.0+
- Redis 6.0+
- RocketMQ 5.3.2+
- Nacos 2.4.0+

### 启动命令
```bash
# 开发环境
java -jar target/user-service-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev

# 生产环境
java -jar target/user-service-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=prod \
  --server.port=8082
```

### 健康检查
```bash
# 检查服务状态
curl http://localhost:8082/actuator/health

# 检查服务注册
curl http://localhost:8848/nacos/v1/ns/instance/list?serviceName=user-service
```

## 🔧 开发指南

### 1. 本地开发
```bash
# 克隆代码
git clone <repository>

# 启动依赖服务
docker-compose -f docker/docker-compose.yml up -d

# 运行服务
mvn spring-boot:run
```

### 2. 单元测试
```bash
# 运行所有测试
mvn test

# 运行特定测试
mvn test -Dtest=UserServiceTest
```

### 3. 代码风格
- 遵循阿里巴巴Java开发规范
- 使用Lombok简化代码
- MapStruct处理对象转换
- 完整的JavaDoc注释

## 📈 性能优化

### 1. 查询优化
- 使用合适的索引
- 分页查询避免深度分页
- 批量操作优化

### 2. 缓存策略
- 用户基本信息Redis缓存
- 地址信息本地缓存
- 缓存更新策略

### 3. 并发控制
- 乐观锁控制并发
- 分布式锁保证一致性

## 📝 待完善功能

- [ ] 用户头像上传功能
- [ ] 批量用户操作接口
- [ ] 用户行为分析
- [ ] 实名认证功能
- [ ] 社交登录扩展（微信、QQ等）

## 🤝 贡献指南

1. Fork 项目
2. 创建特性分支
3. 提交代码更改
4. 推送到分支
5. 创建 Pull Request

## 📞 联系方式

如有问题或建议，请联系开发团队或提交Issue。

---

**版本**: v0.0.1-SNAPSHOT  
**最后更新**: 2025-01-25  
**维护者**: Cloud Platform Team
