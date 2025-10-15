# Search Service API Documentation

## 服务概述

搜索服务基于Elasticsearch提供商品搜索、店铺搜索、搜索建议、热门搜索等功能,支持中文分词和拼音搜索。

**服务端口**: 8087
**Gateway路由前缀**: `/search`
**Elasticsearch**: localhost:9200

---

## 商品搜索API

**基础路径**: `/api/search`

### 1. 复杂商品搜索
**路径**: `POST /api/search/complex-search`
**权限**: `SCOPE_search:read`

**请求体 (ProductSearchRequest)**:
```json
{
  "keyword": "手机",
  "categoryName": "电子产品",
  "brandName": "华为",
  "minPrice": 1000,
  "maxPrice": 5000,
  "sortBy": "price",
  "sortOrder": "asc",
  "page": 0,
  "size": 20,
  "highlightEnabled": true
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "搜索成功",
  "data": {
    "items": [...],
    "total": 128,
    "page": 0,
    "size": 20,
    "took": 45,
    "aggregations": {
      "brands": {"华为": 50, "小米": 40},
      "categories": {"手机": 80, "平板": 30}
    }
  }
}
```

---

### 2. 关键词搜索
**路径**: `GET /api/search/search`

**请求参数**:
- keyword: 搜索关键词 (required)
- page: 页码 (default: 0)
- size: 每页大小 (default: 20)
- sortBy: 排序字段 (default: hotScore)
- sortDir: 排序方向 (default: desc)

**特点**:
- 支持中文分词
- 支持拼音搜索
- 支持高亮显示
- 支持多字段搜索(name, description)

---

### 3. 分类商品搜索
**路径**: `GET /api/search/search/category/{categoryId}`

**请求参数**:
- categoryId: 分类ID (required)
- keyword: 搜索关键词 (optional)
- page: 页码 (default: 0)
- size: 每页大小 (default: 20)

---

### 4. 店铺商品搜索
**路径**: `GET /api/search/search/shop/{shopId}`

**请求参数**:
- shopId: 店铺ID (required)
- keyword: 搜索关键词 (optional)
- page: 页码 (default: 0)
- size: 每页大小 (default: 20)

---

### 5. 高级搜索
**路径**: `GET /api/search/search/advanced`

**请求参数**:
- keyword: 搜索关键词 (required)
- minPrice: 最低价格 (optional)
- maxPrice: 最高价格 (optional)
- page: 页码 (default: 0)
- size: 每页大小 (default: 20)

**说明**: 支持关键词+价格区间组合搜索

---

### 6. 智能搜索
**路径**: `GET /api/search/smart-search`

**请求参数**:
- keyword: 搜索关键词 (optional)
- categoryId: 分类ID (optional)
- minPrice: 最低价格 (optional)
- maxPrice: 最高价格 (optional)
- sortField: 排序字段 (default: score)
- sortOrder: 排序方向 (default: desc)
- page: 页码 (default: 1)
- size: 每页大小 (default: 20)

**特点**:
- 使用优化的ES引擎
- 支持多维度筛选
- 支持自定义排序
- 返回详细的聚合信息

---

### 7. 获取搜索建议
**路径**: `GET /api/search/suggestions`

**请求参数**:
- keyword: 搜索关键字 (required)
- size: 建议数量 (default: 10)

**响应示例**:
```json
{
  "code": 200,
  "data": [
    "华为手机",
    "华为笔记本",
    "华为平板",
    "华为耳机",
    "华为手表"
  ]
}
```

---

### 8. 获取热门搜索关键字
**路径**: `GET /api/search/hot-keywords`

**请求参数**:
- size: 关键字数量 (default: 10)

**响应示例**:
```json
{
  "code": 200,
  "data": [
    "iPhone 15",
    "华为Mate60",
    "小米14",
    "MacBook Pro",
    "AirPods"
  ]
}
```

---

### 9. 获取筛选聚合信息
**路径**: `POST /api/search/filters`

**说明**: 获取商品搜索的筛选聚合信息,用于构建筛选条件

**返回**: 品牌聚合、分类聚合、价格区间聚合等

---

### 10. 推荐商品
**路径**: `GET /api/search/recommended`

**请求参数**:
- page: 页码 (default: 0)
- size: 每页大小 (default: 20)

**说明**: 获取标记为推荐的商品列表

---

### 11. 新品推荐
**路径**: `GET /api/search/new`

**说明**: 按创建时间倒序返回新品列表

---

### 12. 热销商品
**路径**: `GET /api/search/hot`

**说明**: 按销量倒序返回热销商品列表

---

### 13. 重建商品索引
**路径**: `POST /api/search/rebuild-index`
**权限**: `@permissionManager.hasAdminAccess()`

**说明**: 管理员手动触发重建商品搜索索引

---

## 店铺搜索API

**基础路径**: `/api/search/shops`

### 1. 复杂店铺搜索
**路径**: `POST /api/search/shops/complex-search`

**请求体 (ShopSearchRequest)**:
```json
{
  "keyword": "美食",
  "merchantId": 1,
  "status": 1,
  "sortBy": "rating",
  "sortOrder": "desc",
  "page": 0,
  "size": 20
}
```

**响应示例**:
```json
{
  "code": 200,
  "data": {
    "items": [...],
    "total": 50,
    "page": 0,
    "size": 20,
    "took": 30
  }
}
```

---

### 2. 根据店铺ID查询
**路径**: `GET /api/search/shops/{shopId}`

**响应示例**:
```json
{
  "code": 200,
  "data": {
    "shopId": 1,
    "shopName": "美食店铺",
    "merchantId": 1,
    "status": 1,
    "rating": 4.8,
    "address": "深圳市南山区",
    "hotScore": 95
  }
}
```

---

### 3. 获取店铺搜索建议
**路径**: `GET /api/search/shops/suggestions`

**请求参数**:
- keyword: 搜索关键字 (required)
- size: 建议数量 (default: 10)

---

### 4. 获取热门店铺
**路径**: `GET /api/search/shops/hot-shops`

**请求参数**:
- size: 店铺数量 (default: 10)

---

### 5. 推荐店铺
**路径**: `GET /api/search/shops/recommended`

**请求参数**:
- page: 页码 (default: 0)
- size: 每页大小 (default: 20)

**说明**: 获取标记为推荐的店铺列表,仅返回营业中的店铺

---

### 6. 按地区搜索店铺
**路径**: `GET /api/search/shops/by-location`

**请求参数**:
- location: 地区关键字 (required)
- page: 页码 (default: 0)
- size: 每页大小 (default: 20)

**说明**: 根据地区关键字搜索店铺,按评分倒序排列

---

### 7. 获取店铺筛选聚合信息
**路径**: `POST /api/search/shops/filters`

**说明**: 获取店铺搜索的筛选聚合信息

---

## 数据模型

### ProductDocument
```json
{
  "id": "1",
  "productId": 1,
  "name": "华为Mate60 Pro",
  "description": "华为旗舰手机",
  "price": 6999.00,
  "categoryId": 1,
  "categoryName": "手机",
  "brandId": 1,
  "brandName": "华为",
  "shopId": 1,
  "shopName": "华为官方旗舰店",
  "images": ["url1", "url2"],
  "salesCount": 10000,
  "rating": 4.9,
  "hotScore": 98,
  "status": 1,
  "isNew": true,
  "isHot": true,
  "recommended": true,
  "createdAt": "2025-01-15T10:00:00Z"
}
```

### ShopDocument
```json
{
  "id": "1",
  "shopId": 1,
  "shopName": "华为官方旗舰店",
  "merchantId": 1,
  "status": 1,
  "rating": 4.9,
  "address": "深圳市南山区科技园",
  "hotScore": 98,
  "recommended": true,
  "description": "华为官方授权店铺"
}
```

---

## 使用示例

### 1. 关键词搜索商品
```bash
curl -X GET "http://localhost:80/api/search/search?keyword=手机&page=0&size=20"
```

### 2. 复杂商品搜索
```bash
curl -X POST "http://localhost:80/api/search/complex-search" \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "keyword": "手机",
    "minPrice": 1000,
    "maxPrice": 5000,
    "sortBy": "price",
    "sortOrder": "asc"
  }'
```

### 3. 获取搜索建议
```bash
curl -X GET "http://localhost:80/api/search/suggestions?keyword=华为&size=10"
```

### 4. 店铺搜索
```bash
curl -X POST "http://localhost:80/api/search/shops/complex-search" \
  -H "Content-Type: application/json" \
  -d '{
    "keyword": "美食",
    "status": 1
  }'
```

---

## Elasticsearch索引配置

### 商品索引 (products)
- **分词器**: IK中文分词器
- **拼音分词**: 支持拼音搜索
- **字段**: name, description, categoryName, brandName
- **排序字段**: price, salesCount, rating, hotScore, createdAt

### 店铺索引 (shops)
- **分词器**: IK中文分词器
- **字段**: shopName, address, description
- **排序字段**: rating, hotScore

---

## 搜索优化

1. **分页**: 使用from+size分页,避免深度分页
2. **聚合**: 提供品牌、分类、价格区间聚合
3. **高亮**: 支持搜索结果高亮显示
4. **建议**: 使用suggest API提供搜索建议
5. **缓存**: 热门搜索结果可缓存

---

## 错误码

| 错误码 | 说明 |
|-------|------|
| 7001 | Elasticsearch连接失败 |
| 7002 | 索引不存在 |
| 7003 | 搜索查询语法错误 |
| 7004 | 商品/店铺不存在 |

**文档更新**: 2025-01-15
