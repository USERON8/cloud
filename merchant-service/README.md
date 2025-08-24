# 商家服务 (Merchant Service)

## 简介

商家服务是电商平台的核心服务之一，为商家提供注册、认证、店铺管理等功能。该服务实现了商家的生命周期管理，包括认证审核、店铺管理等。

## 核心功能

1. **商家认证**
   - 商家提交认证信息
   - 认证信息审核管理

2. **店铺管理**
   - 创建和管理店铺
   - 店铺信息维护

3. **商家信息管理**
   - 商家基本信息维护
   - 商家状态管理

## 技术栈

- Spring Boot
- Spring Security (OAuth2 Resource Server)
- MyBatis-Plus
- MySQL
- Nacos (服务注册与发现、配置管理)
- Feign (服务间调用)
- Redis (缓存)

## 核心接口

### 认证接口

- `POST /merchant-auth/submit` - 提交认证信息
- `GET /merchant-auth/me` - 获取当前商家认证信息

### 管理接口（管理员专用）

- `PUT /admin/merchants/{id}/approve` - 审核通过商家
- `PUT /admin/merchants/{id}/reject` - 拒绝商家申请
- `PUT /admin/shops/{id}/approve` - 审核通过店铺
- `PUT /admin/shops/{id}/reject` - 拒绝店铺申请

### 查询接口

- `GET /merchant/query/info` - 获取当前商家信息
- `GET /merchant/query/shops` - 获取当前商家所有店铺

## 权限控制

该服务使用OAuth2 Resource Server进行权限验证：
- 商家接口需要`ROLE_MERCHANT`角色
- 管理员接口需要`ROLE_ADMIN`角色

## 部署说明

```bash
# 编译打包
mvn clean package

# 运行服务
java -jar target/merchant-service.jar
```