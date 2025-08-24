# 管理员服务 (Admin Service)

## 简介

管理员服务是电商平台的后台管理服务，提供对用户、商家、订单等资源的管理功能。该服务实现了管理员的权限控制和操作审计功能。

## 核心功能

1. **用户管理**
   - 查看所有用户列表
   - 用户信息查询

2. **商家管理**
   - 查看所有商家列表
   - 审核商家申请（通过/拒绝）

3. **店铺管理**
   - 查看所有店铺列表
   - 审核店铺申请（通过/拒绝）

## 技术栈

- Spring Boot
- Spring Security (OAuth2 Resource Server)
- MyBatis-Plus
- MySQL
- Nacos (服务注册与发现、配置管理)
- Feign (服务间调用)
- Redis (缓存)

## 核心接口

### 管理接口

- `PUT /admin/manage/merchants/{id}/approve` - 审核通过商家
- `PUT /admin/manage/merchants/{id}/reject` - 拒绝商家申请
- `PUT /admin/manage/shops/{id}/approve` - 审核通过店铺
- `PUT /admin/manage/shops/{id}/reject` - 拒绝店铺申请

### 查询接口

- `GET /admin/query/users` - 获取所有用户列表
- `GET /admin/query/merchants` - 获取所有商家列表
- `GET /admin/query/shops` - 获取所有店铺列表

## 权限控制

该服务使用OAuth2 Resource Server进行权限验证，只有具有`ROLE_ADMIN`角色的用户才能访问管理员接口。

## 部署说明

```bash
# 编译打包
mvn clean package

# 运行服务
java -jar target/admin-service.jar
```