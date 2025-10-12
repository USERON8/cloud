# Feign接口设计规范

## 概述

本文档定义了Spring Cloud微服务架构中Feign客户端接口的设计规范，确保服务间调用的一致性和可维护性。

## 核心原则

### 1. 直接返回业务对象
- **强制要求**: 所有Feign接口直接返回业务对象(DTO/VO)或基础类型
- **仅用于内部调用**: Feign接口仅用于微服务内部调用，不需要Result包装
- **统一返回值**: 查询接口返回DTO/VO，操作接口返回Boolean或基础类型

### 2. 命名规范

#### 接口命名
```java
// ✅ 正确的命名
public interface UserFeignClient { }
public interface OrderFeignClient { }
public interface ProductFeignClient { }

// ❌ 错误的命名
public interface UserServiceClient { }
public interface OrderInternal { }
public interface ProductRPC { }
```

#### 方法命名
```java
// ✅ 正确的命名
Result<UserDTO> getUserById(@PathVariable("id") Long id);
Result<List<OrderVO>> getOrdersByUserId(@PathVariable("userId") Long userId);
Result<Boolean> updateProductStatus(@PathVariable("id") Long id, @RequestParam Integer status);

// ❌ 错误的命名
Result<UserDTO> findUser(@PathVariable("id") Long id);
Result<List<OrderVO>> queryOrders(@PathVariable("userId") Long userId);
Result<Boolean> changeStatus(@PathVariable("id") Long id, @RequestParam Integer status);
```

### 3. 路径规范

#### RESTful路径设计
```java
// ✅ 正确的路径设计
@GetMapping("/{id}")
@PutMapping("/{id}")
@DeleteMapping("/{id}")
@PostMapping("/{id}/status/{status}")
@GetMapping("/user/{userId}")
@GetMapping("/order/{orderId}")

// ❌ 错误的路径设计
@GetMapping("/id/{id}")
@GetMapping("/get-by-id/{id}")
@PostMapping("/update-status/{id}/{status}")
@GetMapping("/get-user/{userId}")
```

#### 批量操作路径
```java
// ✅ 正确的批量操作路径
@DeleteMapping("/batch")
@PutMapping("/batch/enable")
@PostMapping("/batch/cancel")
@GetMapping("/batch")

// ❌ 错误的批量操作路径
@PostMapping("/batch-delete")
@PutMapping("/batch-enable")
@PostMapping("/batch-cancel")
```

## 接口设计模板

### 1. 基础CRUD操作模板

```java
@FeignClient(name = "service-name", path = "/internal/service", contextId = "serviceFeignClient")
public interface ServiceFeignClient {

    /**
     * 根据ID查询
     *
     * @param id 资源ID
     * @return 资源信息，不存在时返回null
     */
    @GetMapping("/{id}")
    ServiceVO getById(@PathVariable("id") Long id);

    /**
     * 创建资源
     *
     * @param requestDTO 创建请求
     * @return 创建的资源ID
     */
    @PostMapping("/create")
    Long create(@RequestBody ServiceRequestDTO requestDTO);

    /**
     * 更新资源
     *
     * @param id 资源ID
     * @param requestDTO 更新请求
     * @return 是否更新成功
     */
    @PutMapping("/{id}")
    Boolean update(@PathVariable("id") Long id, @RequestBody ServiceRequestDTO requestDTO);

    /**
     * 删除资源
     *
     * @param id 资源ID
     * @return 是否删除成功
     */
    @DeleteMapping("/{id}")
    Boolean delete(@PathVariable("id") Long id);
}
```

### 2. 状态管理操作模板

```java
/**
 * 更新状态
 *
 * @param id 资源ID
 * @param status 状态值
 * @return 是否更新成功
 */
@PatchMapping("/{id}/status")
Boolean updateStatus(@PathVariable("id") Long id, @RequestParam Integer status);

/**
 * 启用资源
 *
 * @param id 资源ID
 * @return 是否启用成功
 */
@PutMapping("/{id}/enable")
Boolean enable(@PathVariable("id") Long id);

/**
 * 禁用资源
 *
 * @param id 资源ID
 * @return 是否禁用成功
 */
@PutMapping("/{id}/disable")
Boolean disable(@PathVariable("id") Long id);
```

### 3. 业务操作模板

```java
/**
 * 业务操作方法
 *
 * @param id 资源ID
 * @param param 业务参数
 * @return 操作结果
 */
@PostMapping("/{id}/action")
Boolean action(@PathVariable("id") Long id, @RequestParam(required = false) String param);
```

### 4. 批量操作模板

```java
// ==================== 批量操作接口 ====================

/**
 * 批量查询
 *
 * @param ids ID列表
 * @return 资源列表，无数据时返回空列表
 */
@GetMapping("/batch")
List<ServiceVO> getByIds(@RequestParam List<Long> ids);

/**
 * 批量创建
 *
 * @param requestList 创建请求列表
 * @return 创建成功的数量
 */
@PostMapping("/batch")
Integer createBatch(@RequestBody List<ServiceRequestDTO> requestList);

/**
 * 批量删除
 *
 * @param ids ID列表
 * @return 是否删除成功
 */
@DeleteMapping("/batch")
Boolean deleteBatch(@RequestBody List<Long> ids);

/**
 * 批量状态更新
 *
 * @param ids ID列表
 * @param status 状态值
 * @return 是否更新成功
 */
@PutMapping("/batch/status")
Boolean updateStatusBatch(@RequestBody List<Long> ids, @RequestParam Integer status);
```

## 使用示例

### 客户端调用示例

```java
@Service
@RequiredArgsConstructor
public class OrderService {

    private final ProductFeignClient productFeignClient;
    private final StockFeignClient stockFeignClient;
    private final PaymentFeignClient paymentFeignClient;

    public void createOrder(OrderCreateDTO orderCreateDTO) {
        // 1. 检查商品信息
        ProductVO product = productFeignClient.getProductById(orderCreateDTO.getProductId());
        if (product == null) {
            throw new BusinessException("商品不存在");
        }

        // 2. 检查库存
        boolean hasStock = stockFeignClient.checkStock(orderCreateDTO.getProductId(), orderCreateDTO.getQuantity());
        if (!hasStock) {
            throw new BusinessException("库存不足");
        }

        // 3. 创建订单
        // ... 订单创建逻辑

        // 4. 扣减库存
        boolean deductSuccess = stockFeignClient.deductStock(orderCreateDTO.getProductId(), orderCreateDTO.getQuantity());
        if (!deductSuccess) {
            throw new BusinessException("库存扣减失败");
        }

        // 5. 创建支付记录
        PaymentDTO paymentDTO = buildPaymentDTO(order);
        Long paymentId = paymentFeignClient.createPayment(paymentDTO);
        if (paymentId == null) {
            throw new BusinessException("支付记录创建失败");
        }
    }
}
```

### 错误处理示例

```java
@Service
@RequiredArgsConstructor
public class ProductService {

    private final StockFeignClient stockFeignClient;

    public void checkProductStock(Long productId, Integer quantity) {
        try {
            Boolean hasStock = stockFeignClient.checkStock(productId, quantity);
            if (hasStock == null || !hasStock) {
                throw new BusinessException("商品库存不足");
            }

        } catch (Exception e) {
            log.error("库存检查异常，商品ID: {}, 数量: {}", productId, quantity, e);
            throw new BusinessException("库存服务不可用");
        }
    }
}
```

## 版本控制

### Feign接口版本规范

1. **路径版本控制** (推荐)
```java
@FeignClient(name = "user-service", path = "/internal/v1/user", contextId = "userFeignClient")
public interface UserFeignClient { }
```

2. **Header版本控制** (可选)
```java
@FeignClient(name = "user-service", path = "/internal/user", contextId = "userFeignClient", headers = "API-Version: 1.0")
public interface UserFeignClient { }
```

## 注意事项

### 1. 参数验证
- Feign客户端不进行参数验证，由服务端Controller负责
- 使用Bean Validation注解时，确保服务端验证逻辑完整

### 2. 异常处理
- 直接捕获Feign调用异常
- 业务异常应该在调用方进行适当处理和转换
- 返回值为null时需要做非空校验

### 3. 超时配置
```yaml
feign:
  client:
    config:
      default:
        connectTimeout: 5000
        readTimeout: 10000
  httpclient:
    enabled: true
    max-connections: 200
    max-connections-per-route: 50
```

### 4. 熔断配置
```yaml
feign:
  hystrix:
    enabled: true
hystrix:
  command:
    default:
      execution:
        isolation:
          thread:
            timeoutInMilliseconds: 10000
      circuitBreaker:
        requestVolumeThreshold: 20
        errorThresholdPercentage: 50
```

## 最佳实践

1. **保持接口简洁**: 避免过于复杂的Feign接口设计
2. **统一错误处理**: 使用 `FeignResponseWrapper` 统一处理返回结果
3. **合理使用批量操作**: 对于高频操作，优先使用批量接口
4. **版本管理**: 接口变更时使用版本控制，避免破坏性更新
5. **文档同步**: 确保Feign接口文档与服务端API文档保持同步

## 检查清单

- [ ] 所有Feign接口直接返回业务对象(DTO/VO)或基础类型
- [ ] 接口命名遵循 `{Service}FeignClient` 规范
- [ ] 方法命名使用 `get/update/delete/create` 等标准动词
- [ ] 路径遵循RESTful设计规范
- [ ] 批量操作使用 `/batch` 路径
- [ ] 参数名称与路径变量名称一致
- [ ] 对返回值进行null检查
- [ ] 配置合理的超时和熔断参数
- [ ] 注释中明确说明null返回值的处理规则