# Product Service API Documentation

## 服务概述

商品服务负责商品管理、分类管理、品牌管理等功能。

**服务端口**: 8083
**Gateway路由前缀**: `/product`, `/category`

---

## 商品管理模块API

### 查询接口

| 接口 | 路径 | 方法 | 权限 | 说明 |
|------|------|------|------|------|
| 分页查询商品 | `/product` | GET | `SCOPE_product:read` | 支持name,categoryId,brandId,status筛选 |
| 根据ID查询 | `/product/{id}` | GET | `SCOPE_product:read` | 获取商品详情 |
| 根据名称搜索 | `/product/search` | GET | `SCOPE_product:read` | 模糊搜索商品名称 |
| 根据分类查询 | `/product/category/{categoryId}` | GET | `SCOPE_product:read` | 查询分类下商品 |
| 根据品牌查询 | `/product/brand/{brandId}` | GET | `SCOPE_product:read` | 查询品牌下商品 |
| 批量查询 | `/product/batch` | GET | `SCOPE_product:read` | 根据ID列表批量查询 |

### 管理接口

| 接口 | 路径 | 方法 | 权限 | 说明 |
|------|------|------|------|------|
| 创建商品 | `/product` | POST | MERCHANT/ADMIN + `product:create` | 创建新商品 |
| 更新商品 | `/product/{id}` | PUT | MERCHANT/ADMIN + `product:write` | 全量更新 |
| 部分更新 | `/product/{id}` | PATCH | MERCHANT/ADMIN + `product:write` | 部分字段更新 |
| 删除商品 | `/product/{id}` | DELETE | MERCHANT/ADMIN + `product:write` | 软删除 |
| 更新状态 | `/product/{id}/status` | PATCH | MERCHANT/ADMIN + `product:write` | 上架/下架 |

### 批量操作接口

| 接口 | 路径 | 方法 | 最大数量 | 说明 |
|------|------|------|----------|------|
| 批量创建 | `/product/batch` | POST | 100 | 批量创建商品 |
| 批量更新 | `/product/batch` | PUT | 100 | 批量更新商品 |
| 批量删除 | `/product/batch` | DELETE | 100 | 批量删除商品 |
| 批量上架 | `/product/batch/enable` | PUT | 100 | 批量上架 |
| 批量下架 | `/product/batch/disable` | PUT | 100 | 批量下架 |

---

## 分类管理模块API

### 查询接口

| 接口 | 路径 | 方法 | 说明 |
|------|------|------|------|
| 分页查询分类 | `/category` | GET | 支持parentId,status筛选 |
| 根据ID查询 | `/category/{id}` | GET | 获取分类详情 |
| 获取分类树 | `/category/tree` | GET | 获取树形分类结构 |
| 获取子分类 | `/category/{id}/children` | GET | 获取子分类,支持递归 |

### 管理接口

| 接口 | 路径 | 方法 | 权限 | 说明 |
|------|------|------|------|------|
| 创建分类 | `/category` | POST | ADMIN + `admin:write` | 创建新分类 |
| 更新分类 | `/category/{id}` | PUT | ADMIN + `admin:write` | 更新分类信息 |
| 删除分类 | `/category/{id}` | DELETE | ADMIN + `admin:write` | 删除分类(支持级联) |
| 更新状态 | `/category/{id}/status` | PATCH | ADMIN + `admin:write` | 启用/禁用分类 |
| 更新排序 | `/category/{id}/sort` | PATCH | ADMIN + `admin:write` | 调整排序值 |
| 移动分类 | `/category/{id}/move` | PATCH | ADMIN + `admin:write` | 移动到新父分类 |

---

## Feign内部接口

### 商品相关

**基础路径**: `/internal/product`

| 接口 | 路径 | 方法 | 说明 |
|------|------|------|------|
| 创建商品 | `/create` | POST | 内部创建商品 |
| 查询商品 | `/{id}` | GET | 根据ID查询 |
| 更新商品 | `/{id}` | PUT | 全量更新 |
| 部分更新 | `/{id}` | PATCH | 部分更新 |
| 删除商品 | `/{id}` | DELETE | 删除商品 |
| 批量查询 | `/batch` | GET | 批量查询商品 |
| 分类商品 | `/category/{categoryId}` | GET | 查询分类商品 |
| 品牌商品 | `/brand/{brandId}` | GET | 查询品牌商品 |

### 库存操作

| 接口 | 路径 | 方法 | 说明 |
|------|------|------|------|
| 检查库存 | `/{productId}/stock/check` | GET | 检查库存是否充足 |
| 扣减库存 | `/{productId}/stock/deduct` | POST | 扣减商品库存 |
| 恢复库存 | `/{productId}/stock/restore` | POST | 恢复商品库存 |

---

## 数据模型

### ProductVO
```json
{
  "id": 1,
  "name": "商品名称",
  "description": "商品描述",
  "price": 99.99,
  "stock": 100,
  "salesCount": 50,
  "categoryId": 1,
  "categoryName": "分类名称",
  "brandId": 1,
  "brandName": "品牌名称",
  "images": ["url1", "url2"],
  "status": 1,
  "attributes": {"颜色": "红色"},
  "createdAt": "2025-01-15T10:00:00Z"
}
```

### CategoryDTO
```json
{
  "id": 1,
  "name": "分类名称",
  "parentId": 0,
  "level": 1,
  "sort": 100,
  "icon": "icon-url",
  "status": 1,
  "children": []
}
```

---

## 错误码

| 错误码 | 说明 |
|-------|------|
| 4001 | 商品不存在 |
| 4002 | 分类不存在 |
| 4003 | 品牌不存在 |
| 4004 | 库存不足 |
| 4005 | 商品已下架 |
| 4006 | 分类下有商品,无法删除 |

**文档更新**: 2025-01-15
