# Stock Service API Documentation

## 服务概述

库存服务负责商品库存管理、库存操作、库存预留等功能,支持分布式锁保证数据一致性。

**服务端口**: 8084
**Gateway路由前缀**: `/stock`, `/stocks`

---

## 库存查询API

| 接口 | 路径 | 方法 | 权限 | 说明 |
|------|------|------|------|------|
| 分页查询库存 | `/stocks/page` | POST | ADMIN/MERCHANT | 分页查询库存列表 |
| 根据ID查询 | `/stocks/{id}` | GET | ADMIN/MERCHANT | 获取库存详情 |
| 根据商品ID查询 | `/stocks/product/{productId}` | GET | 公开 | 查询商品库存 |
| 批量查询 | `/stocks/batch/query` | POST | 公开 | 批量查询商品库存 |
| 检查库存 | `/stocks/check/{productId}/{quantity}` | GET | 公开 | 检查库存是否充足 |

---

## 库存管理API

| 接口 | 路径 | 方法 | 权限 | 说明 |
|------|------|------|------|------|
| 创建库存 | `/stocks` | POST | ADMIN + `admin:write` | 创建库存记录 |
| 更新库存 | `/stocks/{id}` | PUT | ADMIN/MERCHANT | 更新库存信息 |
| 删除库存 | `/stocks/{id}` | DELETE | ADMIN | 删除库存记录 |
| 批量删除 | `/stocks` | DELETE | ADMIN | 批量删除库存 |

---

## 库存操作API

### 基本操作

| 接口 | 路径 | 方法 | 分布式锁 | 说明 |
|------|------|------|----------|------|
| 库存入库 | `/stocks/stock-in` | POST | `stock:in:{productId}` | 增加库存 |
| 库存出库 | `/stocks/stock-out` | POST | `stock:out:{productId}` | 扣减库存 |
| 预留库存 | `/stocks/reserve` | POST | `stock:reserve:{productId}` | 预留库存 |
| 释放预留 | `/stocks/release` | POST | `stock:release:{productId}` | 释放预留库存 |

#### 入库接口详情
**路径**: `POST /stocks/stock-in`
**权限**: `ROLE_ADMIN` or `ROLE_MERCHANT`

**请求参数**:
- productId: 商品ID (required)
- quantity: 入库数量 (required, min=1)
- remark: 备注 (optional)

**响应示例**:
```json
{
  "code": 200,
  "message": "入库成功",
  "data": true
}
```

#### 出库接口详情
**路径**: `POST /stocks/stock-out`
**权限**: `ROLE_ADMIN` or `ROLE_MERCHANT`

**请求参数**:
- productId: 商品ID (required)
- quantity: 出库数量 (required, min=1)
- orderId: 订单ID (optional)
- orderNo: 订单号 (optional)
- remark: 备注 (optional)

#### 预留接口详情
**路径**: `POST /stocks/reserve`
**说明**: 预留库存会从可用库存扣减,但不会真正出库

**请求参数**:
- productId: 商品ID (required)
- quantity: 预留数量 (required, min=1)

---

### 高级功能 - 秒杀

#### 秒杀库存扣减
**路径**: `POST /stocks/seckill/{productId}`
**分布式锁**: `seckill:stock:{productId}` (公平锁)
**锁策略**: FAIL_FAST (快速失败)

**特点**:
- 使用公平锁确保秒杀公平性
- 快速失败策略,避免线程长时间等待
- 自动检查库存,库存不足立即返回失败

**请求参数**:
- quantity: 扣减数量 (default: 1)
- orderId: 订单ID (required)
- orderNo: 订单号 (required)

**响应示例**:
```json
{
  "code": 200,
  "message": "秒杀成功",
  "data": true
}
```

---

## 批量操作API

| 接口 | 路径 | 方法 | 最大数量 | 说明 |
|------|------|------|----------|------|
| 批量创建 | `/stocks/batch` | POST | 100 | 批量创建库存记录 |
| 批量更新 | `/stocks/batch` | PUT | 100 | 批量更新库存信息 |
| 批量入库 | `/stocks/stock-in/batch` | POST | 100 | 批量入库操作 |
| 批量出库 | `/stocks/stock-out/batch` | POST | 100 | 批量出库操作 |
| 批量预留 | `/stocks/reserve/batch` | POST | 100 | 批量预留库存 |

**批量操作请求体示例**:
```json
[
  {
    "productId": 1,
    "quantity": 100,
    "remark": "批量入库"
  },
  {
    "productId": 2,
    "quantity": 50,
    "remark": "批量入库"
  }
]
```

---

## Feign内部接口

**基础路径**: `/internal/stock`

| 接口 | 路径 | 方法 | 说明 |
|------|------|------|------|
| 查询库存 | `/{stockId}` | GET | 根据库存ID查询 |
| 查询商品库存 | `/product/{productId}` | GET | 根据商品ID查询 |
| 批量查询 | `/batch` | POST | 批量查询库存 |
| 检查库存 | `/check/{productId}/{quantity}` | GET | 检查库存是否充足 |
| 扣减库存 | `/deduct` | POST | 扣减库存 |
| 预留库存 | `/reserve` | POST | 预留库存 |
| 释放预留 | `/release` | POST | 释放预留库存 |
| 库存入库 | `/stock-in` | POST | 库存入库 |

---

## 数据模型

### StockDTO
```json
{
  "id": 1,
  "productId": 1,
  "productName": "商品名称",
  "quantity": 100,
  "availableQuantity": 90,
  "reservedQuantity": 10,
  "minStock": 10,
  "maxStock": 1000,
  "version": 5,
  "createdAt": "2025-01-15T10:00:00Z"
}
```

### StockVO
```json
{
  "id": 1,
  "productId": 1,
  "quantity": 100,
  "availableQuantity": 90,
  "reservedQuantity": 10,
  "status": "NORMAL",
  "lowStockAlert": false
}
```

---

## 分布式锁配置

### 锁类型
- **REENTRANT**: 可重入锁 (默认)
- **FAIR**: 公平锁 (秒杀场景)

### 锁策略
- **THROW_EXCEPTION**: 抛出异常 (默认)
- **FAIL_FAST**: 快速失败,返回false

### 超时配置
- waitTime: 等待锁时间 (3-5秒)
- leaseTime: 锁持有时间 (10-30秒)

---

## 使用示例

### 1. 库存入库
```bash
curl -X POST "http://localhost:80/stocks/stock-in?productId=1&quantity=100&remark=采购入库" \
  -H "Authorization: Bearer {token}"
```

### 2. 库存出库
```bash
curl -X POST "http://localhost:80/stocks/stock-out?productId=1&quantity=10&orderId=1" \
  -H "Authorization: Bearer {token}"
```

### 3. 秒杀扣减
```bash
curl -X POST "http://localhost:80/stocks/seckill/1?quantity=1&orderId=100&orderNo=SK001" \
  -H "Authorization: Bearer {token}"
```

---

## 错误码

| 错误码 | 说明 |
|-------|------|
| 5001 | 库存记录不存在 |
| 5002 | 库存不足 |
| 5003 | 预留库存不足 |
| 5004 | 库存操作获取锁失败 |
| 5005 | 商品不存在 |

**文档更新**: 2025-01-15
