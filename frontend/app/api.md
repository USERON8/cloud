# 微服务系统API文档

## 1. 认证服务 (Auth Service)

### 1.1 用户注册

- **URL**: `/auth/register`
- **Method**: POST
- **Description**: 用户注册
- **Request Body**:
  ```json
  {
    "username": "string",
    "password": "string",
    "email": "string",
    "phone": "string",
    "nickname": "string",
    "userType": "string"
  }
  ```
- **Response**:
  ```json
  {
    "code": "integer",
    "message": "string",
    "data": {
      "token": "string",
      "expiresIn": "long",
      "userType": "string",
      "nickname": "string"
    }
  }
  ```

### 12. OAuth2授权码模式登录

- **URL**: `/oauth2/authorize`
- **Method**: GET
- **Description**: OAuth2授权码模式登录授权端点
- **Query Parameters**:
  - `response_type`: 响应类型，固定值为"code"
  - `client_id`: 客户端ID
  - `redirect_uri`: 重定向URI
  - `scope`: 请求权限范围
  - `state`: 状态参数（可选）
- **Response**: 重定向到客户端回调地址

### 1.8 获取访问令牌

- **URL**: `/oauth2/token`
- **Method**: POST
- **Description**: 使用授权码获取访问令牌
- **Request Body (application/x-www-form-urlencoded)**:
  ```
  grant_type=authorization_code
  code=授权码
  redirect_uri=重定向URI
  client_id=客户端ID
  client_secret=客户端密钥
  ```
- **Response**:
  ```json
  {
    "access_token": "string",
    "token_type": "string",
    "expires_in": "integer",
    "refresh_token": "string",
    "scope": "string"
  }
  ```

### 1.2 商家注册

- **URL**: `/auth/register-merchant`
- **Method**: POST
- **Description**: 商家注册，需要上传营业执照和身份证正反面图片
- **Request Body (multipart/form-data)**:
  ```
  username: "string"
  password: "string"
  email: "string"
  phone: "string"
  nickname: "string"
  businessLicenseNumber: "string"
  businessLicenseFile: "file"
  idCardFrontFile: "file"
  idCardBackFile: "file"
  contactPhone: "string"
  contactAddress: "string"
  ```
- **Response**:
  ```json
  {
    "code": "integer",
    "message": "string",
    "data": {
      "token": "string",
      "expiresIn": "long",
      "userType": "string",
      "nickname": "string"
    }
  }
  ```

### 1.3 用户注册并登录

- **URL**: `/auth/register-and-login`
- **Method**: POST
- **Description**: 注册新用户并自动登录
- **Request Body**:
  ```json
  {
    "username": "string",
    "password": "string",
    "email": "string",
    "nickname": "string"
  }
  ```
- **Response**:
  ```json
  {
    "code": "integer",
    "message": "string",
    "data": {
      "token": "string",
      "expiresIn": "long",
      "userType": "string",
      "nickname": "string"
    }
  }
  ```

### 1.4 用户登录

- **URL**: `/auth/login`
- **Method**: POST
- **Description**: 用户登录系统
- **Request Body**:
  ```json
  {
    "username": "string",
    "password": "string"
  }
  ```
- **Response**:
  ```json
  {
    "code": "integer",
    "message": "string",
    "data": {
      "token": "string",
      "expiresIn": "long",
      "userType": "string",
      "nickname": "string"
    }
  }
  ```

### 1.5 用户登出

- **URL**: `/auth/logout`
- **Method**: POST
- **Description**: 用户登出系统
- **Response**:
  ```json
  {
    "code": "integer",
    "message": "string",
    "data": null
  }
  ```

### 1.6 刷新令牌

- **URL**: `/auth/refresh`
- **Method**: POST
- **Description**: 刷新用户访问令牌
- **Response**:
  ```json
  {
    "code": "integer",
    "message": "string",
    "data": {
      "token": "string",
      "expiresIn": "long",
      "userType": "string",
      "nickname": "string"
    }
  }
  ```

## 2. 用户服务 (User Service)

### 2.1 用户注册

- **URL**: `/users/register`
- **Method**: POST
- **Description**: 用户注册
- **Request Body**:
  ```json
  {
    "username": "string",
    "password": "string",
    "email": "string",
    "nickname": "string"
  }
  ```
- **Response**:
  ```json
  {
    "code": "integer",
    "message": "string",
    "data": "string"
  }
  ```

### 2.2 获取当前用户信息

- **URL**: `/users/info`
- **Method**: GET
- **Description**: 根据JWT获取当前用户信息
- **Response**:
  ```json
  {
    "code": "integer",
    "message": "string",
    "data": {
      "username": "string"
    }
  }
  ```

## 3. 商家认证服务 (Merchant Auth Service)

### 3.1 提交商家认证申请

- **URL**: `/merchant/auth/apply`
- **Method**: POST
- **Description**: 提交商家认证申请，需要上传营业执照和身份证正反面图片
- **Request Body (multipart/form-data)**:
  ```
  shopName: "string"
  businessLicenseNumber: "string"
  businessLicenseFile: "file"
  idCardFrontFile: "file"
  idCardBackFile: "file"
  contactPhone: "string"
  contactAddress: "string"
  ```
- **Response**:
  ```json
  {
    "code": "integer",
    "message": "string",
    "data": {
      "id": "long",
      "userId": "long",
      "shopName": "string",
      "businessLicenseNumber": "string",
      "businessLicenseUrl": "string",
      "idCardFrontUrl": "string",
      "idCardBackUrl": "string",
      "contactPhone": "string",
      "contactAddress": "string",
      "status": "integer",
      "auditRemark": "string",
      "createdAt": "string",
      "updatedAt": "string"
    }
  }
  ```

### 3.2 查询当前用户的认证信息

- **URL**: `/merchant/auth/info`
- **Method**: GET
- **Description**: 查询当前用户的认证信息
- **Response**:
  ```json
  {
    "code": "integer",
    "message": "string",
    "data": {
      "id": "long",
      "userId": "long",
      "shopName": "string",
      "businessLicenseNumber": "string",
      "businessLicenseUrl": "string",
      "idCardFrontUrl": "string",
      "idCardBackUrl": "string",
      "contactPhone": "string",
      "contactAddress": "string",
      "status": "integer",
      "auditRemark": "string",
      "createdAt": "string",
      "updatedAt": "string"
    }
  }
  ```

### 3.3 分页查询待审核的商家认证申请

- **URL**: `/merchant/auth/pending`
- **Method**: GET
- **Description**: 分页查询待审核的商家认证申请，仅管理员可访问
- **Query Parameters**:
  - `page`: 页码 (默认: 1)
  - `size`: 每页数量 (默认: 10)
- **Response**:
  ```json
  {
    "code": "integer",
    "message": "string",
    "data": {
      "records": [
        {
          "id": "long",
          "userId": "long",
          "shopName": "string",
          "businessLicenseNumber": "string",
          "businessLicenseUrl": "string",
          "idCardFrontUrl": "string",
          "idCardBackUrl": "string",
          "contactPhone": "string",
          "contactAddress": "string",
          "status": "integer",
          "auditRemark": "string",
          "createdAt": "string",
          "updatedAt": "string"
        }
      ],
      "total": "long",
      "size": "long",
      "current": "long",
      "pages": "long"
    }
  }
  ```

### 3.4 审核商家认证申请

- **URL**: `/merchant/auth/audit/{authId}`
- **Method**: PUT
- **Description**: 审核商家认证申请，仅管理员可访问
- **Path Parameters**:
  - `authId`: 认证ID
- **Query Parameters**:
  - `status`: 审核状态 (1-通过, 2-拒绝)
  - `remark`: 审核备注 (可选)
- **Response**:
  ```json
  {
    "code": "integer",
    "message": "string",
    "data": null
  }
  ```

## 4. 库存服务 (Stock Service)

### 4.1 根据商品ID获取库存信息

- **URL**: `/stock/query/product/{productId}`
- **Method**: GET
- **Description**: 根据商品ID获取库存信息
- **Path Parameters**:
    - productId: 商品ID
- **Response**:
  ```json
  {
    "code": "integer",
    "message": "string",
    "data": {
      "id": "long",
      "productId": "long",
      "productName": "string",
      "quantity": "integer",
      "price": "double"
    }
  }
  ```

### 4.2 分页查询库存

- **URL**: `/stock/query/page`
- **Method**: POST
- **Description**: 分页查询库存信息
- **Request Body**:
  ```json
  {
    "page": "integer",
    "size": "integer",
    "productName": "string"
  }
  ```
- **Response**:
  ```json
  {
    "code": "integer",
    "message": "string",
    "data": {
      "records": [
        {
          "id": "long",
          "productId": "long",
          "productName": "string",
          "quantity": "integer",
          "price": "double"
        }
      ],
      "total": "long",
      "size": "long",
      "current": "long"
    }
  }
  ```

### 4.3 增加库存

- **URL**: `/stock/add`
- **Method**: POST
- **Description**: 增加单个库存
- **Request Body**:
  ```json
  {
    "productId": "long",
    "productName": "string",
    "quantity": "integer",
    "price": "double"
  }
  ```
- **Response**:
  ```json
  {
    "code": "integer",
    "message": "string",
    "data": "string"
  }
  ```

### 4.4 批量增加库存

- **URL**: `/stock/add/batch`
- **Method**: POST
- **Description**: 批量增加库存
- **Request Body**:
  ```json
  [
    {
      "productId": "long",
      "productName": "string",
      "quantity": "integer",
      "price": "double"
    }
  ]
  ```
- **Response**:
  ```json
  {
    "code": "integer",
    "message": "string",
    "data": "string"
  }
  ```

### 4.5 扣减库存

- **URL**: `/stock/reduce/{id}`
- **Method**: DELETE
- **Description**: 扣减库存
- **Path Parameters**:
    - id: 库存ID
- **Response**:
  ```json
  {
    "code": "integer",
    "message": "string",
    "data": "string"
  }
  ```

### 4.6 批量扣减库存

- **URL**: `/stock/reduce/batch`
- **Method**: DELETE
- **Description**: 批量扣减库存
- **Response**:
  ```json
  {
    "code": "integer",
    "message": "string",
    "data": "string"
  }
  ```

### 4.7 更新库存

- **URL**: `/stock/update`
- **Method**: POST
- **Description**: 更新库存
- **Response**:
  ```json
  {
    "code": "integer",
    "message": "string",
    "data": "string"
  }
  ```

### 4.8 异步根据商品ID查询库存

- **URL**: `/stock/async/product/{productId}`
- **Method**: GET
- **Description**: 异步查询商品库存
- **Path Parameters**:
    - productId: 商品ID
- **Response**:
  ```json
  {
    "code": "integer",
    "message": "string",
    "data": {
      "id": "long",
      "productId": "long",
      "productName": "string",
      "quantity": "integer",
      "price": "double"
    }
  }
  ```

### 4.9 异步分页查询库存

- **URL**: `/stock/async/page`
- **Method**: POST
- **Description**: 异步分页查询库存
- **Request Body**:
  ```json
  {
    "page": "integer",
    "size": "integer",
    "productName": "string"
  }
  ```
- **Response**:
  ```json
  {
    "code": "integer",
    "message": "string",
    "data": {
      "records": [
        {
          "id": "long",
          "productId": "long",
          "productName": "string",
          "quantity": "integer",
          "price": "double"
        }
      ],
      "total": "long",
      "size": "long",
      "current": "long"
    }
  }
  ```

### 4.10 异步批量查询库存

- **URL**: `/stock/async/batch`
- **Method**: POST
- **Description**: 异步批量查询库存
- **Request Body**:
  ```json
  [
    "long"
  ]
  ```
- **Response**:
  ```json
  {
    "code": "integer",
    "message": "string",
    "data": [
      {
        "id": "long",
        "productId": "long",
        "productName": "string",
        "quantity": "integer",
        "price": "double"
      }
    ]
  }
  ```

### 4.11 异步查询库存统计信息

- **URL**: `/stock/async/statistics`
- **Method**: GET
- **Description**: 异步查询库存统计信息
- **Response**:
  ```json
  {
    "code": "integer",
    "message": "string",
    "data": {
      "totalProducts": "long",
      "totalStock": "long",
      "totalValue": "double"
    }
  }
  ```

### 4.12 并发查询多个商品库存

- **URL**: `/stock/async/concurrent`
- **Method**: POST
- **Description**: 并发查询多个商品库存（演示高并发场景）
- **Request Body**:
  ```json
  [
    "long"
  ]
  ```
- **Response**:
  ```json
  {
    "code": "integer",
    "message": "string",
    "data": [
      {
        "id": "long",
        "productId": "long",
        "productName": "string",
        "quantity": "integer",
        "price": "double"
      }
    ]
  }
  ```