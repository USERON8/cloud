# 搜索服务 (Search Service)

## 简介

搜索服务是电商平台的全文搜索服务，基于Elasticsearch实现商品和订单的搜索功能。提供高效的搜索能力，支持全文检索、高级筛选和排序功能。

## 核心功能

1. **商品搜索**
   - 商品信息的全文检索
   - 支持关键词搜索
   - 支持分类、价格区间等筛选条件

2. **订单搜索**
   - 订单信息的全文检索
   - 支持订单号、用户信息等关键词搜索
   - 支持时间区间、状态等筛选条件

3. **高级搜索**
   - 支持复杂的组合查询条件
   - 支持搜索结果排序
   - 支持分页查询

## 技术栈

- Spring Boot
- Elasticsearch
- Nacos (服务注册与发现、配置管理)
- Feign (服务间调用)
- Redis (缓存)

## 核心接口

### 搜索接口

- `GET /search/product` - 搜索商品
- `GET /search/order` - 搜索订单
- `POST /search/advanced` - 高级搜索

## 部署说明

```bash
# 编译打包
mvn clean package

# 运行服务
java -jar target/search-service.jar
```