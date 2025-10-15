# User Service API Documentation

## 服务概述

用户服务负责用户管理、商家管理、用户地址、用户统计等功能。

**服务端口**: 8081
**Gateway路由前缀**: `/users`, `/merchant`, `/admin`

---

## 用户管理模块

### 1. 用户查询

#### 1.1 分页查询用户
- **路径**: `GET /users`
- **权限**: `ROLE_ADMIN` + `SCOPE_admin:read`
- **参数**: page, size, username, email, phone, userType, status

#### 1.2 根据ID查询用户
- **路径**: `GET /users/{id}`
- **权限**: `hasAuthority('SCOPE_read')` (用户本人或ADMIN)
- **返回**: UserVO

#### 1.3 根据用户名查询
- **路径**: `GET /users/username/{username}`
- **权限**: `hasAuthority('SCOPE_read')`

#### 1.4 批量查询用户
- **路径**: `POST /users/batch`
- **权限**: `hasAuthority('SCOPE_read')`
- **请求体**: `List<Long> userIds`

### 2. 用户管理

#### 2.1 创建用户
- **路径**: `POST /users`
- **权限**: `ROLE_ADMIN`
- **请求体**: UserRequestDTO

#### 2.2 更新用户信息
- **路径**: `PUT /users/{id}`
- **权限**: `ROLE_ADMIN` 或用户本人
- **请求体**: UserRequestDTO

#### 2.3 修改密码
- **路径**: `PATCH /users/{id}/password`
- **权限**: 用户本人或ADMIN
- **参数**: oldPassword, newPassword

#### 2.4 删除用户
- **路径**: `DELETE /users/{id}`
- **权限**: `ROLE_ADMIN`

#### 2.5 批量删除用户
- **路径**: `DELETE /users/batch`
- **权限**: `ROLE_ADMIN`
- **参数**: `List<Long> ids`

### 3. 用户状态管理

#### 3.1 启用用户
- **路径**: `PATCH /users/{id}/enable`
- **权限**: `ROLE_ADMIN`

#### 3.2 禁用用户
- **路径**: `PATCH /users/{id}/disable`
- **权限**: `ROLE_ADMIN`

#### 3.3 批量启用用户
- **路径**: `PATCH /users/batch/enable`
- **权限**: `ROLE_ADMIN`
- **参数**: `List<Long> ids`

#### 3.4 批量禁用用户
- **路径**: `PATCH /users/batch/disable`
- **权限**: `ROLE_ADMIN`
- **参数**: `List<Long> ids`

---

## 用户地址模块

### 1. 地址管理

#### 1.1 查询用户地址列表
- **路径**: `GET /users/{userId}/addresses`
- **权限**: 用户本人或ADMIN

#### 1.2 获取地址详情
- **路径**: `GET /users/{userId}/addresses/{addressId}`
- **权限**: 用户本人或ADMIN

#### 1.3 添加地址
- **路径**: `POST /users/{userId}/addresses`
- **权限**: 用户本人或ADMIN
- **请求体**: UserAddressDTO

#### 1.4 更新地址
- **路径**: `PUT /users/{userId}/addresses/{addressId}`
- **权限**: 用户本人或ADMIN

#### 1.5 删除地址
- **路径**: `DELETE /users/{userId}/addresses/{addressId}`
- **权限**: 用户本人或ADMIN

#### 1.6 设置默认地址
- **路径**: `PATCH /users/{userId}/addresses/{addressId}/default`
- **权限**: 用户本人或ADMIN

---

## 用户统计模块

### 1. 统计查询

#### 1.1 获取用户统计信息
- **路径**: `GET /users/{userId}/statistics`
- **权限**: 用户本人或ADMIN
- **返回**: 订单数、收藏数、评论数等统计信息

#### 1.2 获取用户行为日志
- **路径**: `GET /users/{userId}/activity-logs`
- **权限**: 用户本人或ADMIN
- **参数**: page, size

#### 1.3 获取系统用户统计
- **路径**: `GET /users/statistics/overview`
- **权限**: `ROLE_ADMIN`
- **返回**: 总用户数、活跃用户数、今日新增等

---

## 商家管理模块

### 1. 商家资料管理

#### 1.1 查询商家信息
- **路径**: `GET /merchant/profile`
- **权限**: `ROLE_MERCHANT`
- **返回**: 当前登录商家的详细信息

#### 1.2 更新商家信息
- **路径**: `PUT /merchant/profile`
- **权限**: `ROLE_MERCHANT`
- **请求体**: MerchantDTO

#### 1.3 商家认证申请
- **路径**: `POST /merchant/auth/apply`
- **权限**: 注册用户
- **请求体**: MerchantAuthDTO

#### 1.4 查询认证状态
- **路径**: `GET /merchant/auth/status`
- **权限**: 注册用户

### 2. 商家审核（管理员）

#### 2.1 查询待审核商家列表
- **路径**: `GET /merchant/auth/pending`
- **权限**: `ROLE_ADMIN`
- **参数**: page, size

#### 2.2 审核商家认证
- **路径**: `POST /merchant/auth/{authId}/review`
- **权限**: `ROLE_ADMIN`
- **参数**: approved (boolean), rejectReason

---

## 管理员功能模块

### 1. 系统管理

#### 1.1 缓存刷新
- **路径**: `POST /admin/cache/refresh`
- **权限**: `ROLE_ADMIN`
- **参数**: cacheNames (可选)

#### 1.2 缓存清理
- **路径**: `POST /admin/cache/clear`
- **权限**: `ROLE_ADMIN`

#### 1.3 异步任务监控
- **路径**: `GET /admin/async/monitor`
- **权限**: `ROLE_ADMIN`
- **返回**: 线程池运行状态

---

## Feign内部接口

### 1. 用户查询（内部调用）
- **路径**: `GET /internal/users/{userId}`
- **说明**: 供其他微服务调用，无需身份验证

### 2. 批量查询用户
- **路径**: `POST /internal/users/batch`
- **请求体**: `List<Long> userIds`

### 3. 验证用户存在
- **路径**: `GET /internal/users/{userId}/exists`
- **返回**: Boolean

---

## 数据模型

### UserVO
```json
{
  "id": 1,
  "username": "testuser",
  "email": "user@example.com",
  "phone": "13800138000",
  "nickname": "测试用户",
  "userType": "USER",
  "status": 1,
  "avatar": "http://...",
  "createdAt": "2025-01-15T10:00:00Z",
  "updatedAt": "2025-01-15T10:00:00Z"
}
```

### UserAddressDTO
```json
{
  "id": 1,
  "userId": 1,
  "receiverName": "张三",
  "receiverPhone": "13800138000",
  "province": "广东省",
  "city": "深圳市",
  "district": "南山区",
  "detailAddress": "科技园XX路XX号",
  "postcode": "518000",
  "isDefault": true
}
```

---

## 使用示例

### 1. 查询用户列表
```bash
curl -X GET "http://localhost:80/users?page=1&size=20" \
  -H "Authorization: Bearer {token}"
```

### 2. 更新用户信息
```bash
curl -X PUT "http://localhost:80/users/1" \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{"nickname":"新昵称","phone":"13900139000"}'
```

### 3. 添加用户地址
```bash
curl -X POST "http://localhost:80/users/1/addresses" \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "receiverName":"张三",
    "receiverPhone":"13800138000",
    "province":"广东省",
    "city":"深圳市",
    "district":"南山区",
    "detailAddress":"科技园XX路XX号",
    "isDefault":true
  }'
```

---

## 错误码

| 错误码 | 说明 |
|-------|------|
| 2001 | 用户不存在 |
| 2002 | 用户已被禁用 |
| 2003 | 原密码错误 |
| 2004 | 地址不存在 |
| 2005 | 商家认证申请重复 |
| 2006 | 商家认证未通过 |

---

**文档更新**: 2025-01-15
