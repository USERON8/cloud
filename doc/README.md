# 项目文档中心

Spring Cloud微服务电商平台的技术文档，按照体系级和服务级分类组织。

## 📁 文档结构

```
doc/
├── README.md                # 本文件 - 文档导航
├── system/                  # 体系级文档
│   ├── API_DOCUMENTATION_INDEX.md      # API文档索引
│   ├── API_DOC_GATEWAY.md              # 网关配置文档
│   ├── DEPLOYMENT_CHECKLIST.md         # 部署清单
│   ├── PROJECT_CHECKLIST.md            # 项目清单
│   └── P1_SUMMARY.md                   # P1阶段总结
│
└── services/                # 服务级文档（按服务分类）
    ├── auth/                # 认证服务
    ├── user/                # 用户服务
    ├── order/               # 订单服务
    ├── product/             # 商品服务
    ├── payment/             # 支付服务
    ├── stock/               # 库存服务
    └── search/              # 搜索服务
```

---

## 📚 快速导航

### 体系级文档

| 文档 | 说明 |
|------|------|
| [API文档索引](system/API_DOCUMENTATION_INDEX.md) | 所有服务API的索引和快速访问 |
| [网关API文档](system/API_DOC_GATEWAY.md) | Gateway路由配置、跨域、限流 |
| [部署清单](system/DEPLOYMENT_CHECKLIST.md) | 生产环境部署检查事项 |
| [项目清单](system/PROJECT_CHECKLIST.md) | 开发测试任务清单 |
| [P1总结](system/P1_SUMMARY.md) | P1阶段功能总结 |

### 服务级文档

| 服务 | 端口 | 文档 | 核心功能 |
|------|------|------|---------|
| **auth-service** | 8081 | [API文档](services/auth/API_DOC_AUTH_SERVICE.md) | OAuth2.1、JWT、Token管理 |
| **user-service** | 8081 | [API文档](services/user/API_DOC_USER_SERVICE.md) | 用户管理、商户管理、地址管理 |
| **product-service** | 8083 | [API文档](services/product/API_DOC_PRODUCT_SERVICE.md) | 商品CRUD、分类管理 |
| **order-service** | 8082 | [API文档](services/order/API_DOC_ORDER_SERVICE.md)<br/>[退款流程](services/order/REFUND_FLOW_GUIDE.md) | 订单管理、退款处理 |
| **payment-service** | 8085 | [API文档](services/payment/API_DOC_PAYMENT_SERVICE.md) | 支付宝/微信支付、退款 |
| **stock-service** | 8084 | [API文档](services/stock/API_DOC_STOCK_SERVICE.md) | 库存管理、预占释放 |
| **search-service** | 8087 | [API文档](services/search/API_DOC_SEARCH_SERVICE.md) | ES搜索、筛选、建议 |

---

## 🌐 API路径规范

### 统一前缀
- **用户端API**: `/api/**` - 对外RESTful接口
- **内部调用**: `/internal/**` - Feign服务间调用
- **管理端**: `/api/admin/**` 或 `/api/manage/**`

### 网关访问
通过Gateway访问服务的URL格式：
```
http://localhost:80/{service-name}/api/{resource-path}
```

示例：
```bash
# 用户服务
http://localhost:80/user/api/query/users/1

# 商品服务
http://localhost:80/product/api/product/1

# 订单服务
http://localhost:80/order/api/orders/1

# 搜索服务
http://localhost:80/search/api/search/basic?keyword=手机
```

---

## 🚀 快速开始

### 1. 启动基础设施
```bash
cd docker && docker-compose up -d
```

### 2. 启动服务
```bash
# 认证服务（必须最先启动）
cd auth-service && mvn spring-boot:run

# 业务服务（可并行启动）
cd user-service && mvn spring-boot:run
cd product-service && mvn spring-boot:run
cd order-service && mvn spring-boot:run
# ... 其他服务
```

### 3. 访问文档
- **Gateway聚合文档**: http://localhost:80/doc.html
- **各服务Swagger**: http://localhost:{port}/doc.html

---

## 📖 文档规范

### 编写规范
1. API文档必须包含：端点路径、请求方法、参数、响应格式、示例
2. 路径必须包含完整前缀（`/api/**`）
3. 新增功能需同步更新文档

### 文档位置
- **体系级文档** → `doc/system/`
- **服务级文档** → `doc/services/{服务名}/`
- **专题文档** → 放在对应服务目录下

---

## 📊 文档统计

- **体系级文档**: 5个
- **服务级文档**: 8个
- **覆盖服务**: 7个
- **总计**: 13个文档

---

**最后更新**: 2025-01-15
