# RocketMQ Stream 消息测试文档

## 📋 目录

1. [测试概述](#测试概述)
2. [测试环境配置](#测试环境配置)
3. [运行测试](#运行测试)
4. [测试用例说明](#测试用例说明)
5. [测试结果验证](#测试结果验证)
6. [常见问题](#常见问题)
7. [最佳实践](#最佳实践)

---

## 测试概述

本测试套件使用 **Spring Cloud Stream Test Binder** 对RocketMQ消息流进行集成测试，无需启动真实的RocketMQ服务。测试覆盖三个核心服务的消息生产和消费功能。

### 测试范围

| 服务 | 生产者测试 | 消费者测试 | 测试文件 |
|------|-----------|-----------|---------|
| **Order Service** | OrderMessageProducerTest | OrderMessageConsumerTest | 2个测试类，11个测试用例 |
| **Stock Service** | StockMessageProducerTest | StockMessageConsumerTest | 2个测试类，12个测试用例 |
| **Payment Service** | PaymentMessageProducerTest | PaymentMessageConsumerTest | 2个测试类，14个测试用例 |

**总计**: 6个测试类，37个测试用例

### 技术栈

- **Spring Boot 3.5.3** - 测试框架基础
- **Spring Cloud Stream** - 消息抽象层
- **Spring Cloud Stream Test Binder** - 测试工具
- **JUnit 5** - 测试引擎
- **Mockito** - Mock框架
- **AssertJ** - 断言库

---

## 测试环境配置

### 1. 依赖配置

所有服务的 `pom.xml` 中已包含必要的测试依赖：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-stream</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-stream-test-binder</artifactId>
    <scope>test</scope>
</dependency>
```

### 2. 测试配置文件

每个服务都有独立的测试配置文件 `application-test.yml`：

**位置**:
- `order-service/src/test/resources/application-test.yml`
- `stock-service/src/test/resources/application-test.yml`
- `payment-service/src/test/resources/application-test.yml`

**关键配置**:
```yaml
spring:
  cloud:
    stream:
      # 使用测试绑定器
      default-binder: test-binder

      # 定义测试topic
      bindings:
        orderCreatedProducer-out-0:
          destination: test-order-created
```

### 3. 测试注解说明

每个测试类使用以下注解：

```java
@SpringBootTest(properties = {
    "spring.cloud.stream.default-binder=test-binder"
})
@Import(TestChannelBinderConfiguration.class)  // 导入测试Binder配置
@ActiveProfiles("test")                         // 激活test配置文件
@DisplayName("测试类描述")
```

---

## 运行测试

### 方式一：Maven命令行

#### 运行所有测试
```bash
# 在项目根目录
mvn clean test

# 运行特定服务的测试
mvn test -pl order-service
mvn test -pl stock-service
mvn test -pl payment-service
```

#### 运行特定测试类
```bash
# Order Service - 生产者测试
mvn test -pl order-service -Dtest=OrderMessageProducerTest

# Stock Service - 消费者测试
mvn test -pl stock-service -Dtest=StockMessageConsumerTest

# Payment Service - 所有消息测试
mvn test -pl payment-service -Dtest=*MessageTest
```

#### 运行特定测试方法
```bash
# 运行单个测试方法
mvn test -pl order-service -Dtest=OrderMessageProducerTest#testSendOrderCreatedEvent

# 使用通配符
mvn test -pl stock-service -Dtest=StockMessageProducerTest#testSend*
```

### 方式二：IDE运行

#### IntelliJ IDEA
1. 打开测试类文件
2. 点击测试类或方法旁边的绿色运行按钮
3. 选择 "Run 'TestClassName'" 或 "Debug 'TestClassName'"

#### Eclipse
1. 右键点击测试类
2. 选择 "Run As" → "JUnit Test"

### 方式三：并行执行（提高速度）

```bash
# Maven并行执行测试
mvn clean test -T 4  # 使用4个线程

# 特定服务并行测试
mvn test -pl order-service,stock-service,payment-service -T 3
```

---

## 测试用例说明

### Order Service 测试

#### OrderMessageProducerTest（订单消息生产者测试）

| 测试用例 | 描述 | 验证点 |
|---------|------|--------|
| `testSendOrderCreatedEvent` | 发送订单创建事件 | 消息发送成功、消息头正确 |
| `testSendOrderCancelledEvent` | 发送订单取消事件 | 消息发送成功、eventType正确 |
| `testSendOrderCreatedEventWithEmptyProducts` | 空商品列表场景 | 空列表也能正常发送 |
| `testSendMultipleOrderCreatedEvents` | 批量发送测试 | 所有消息都成功发送 |

**关键验证代码**:
```java
// 验证消息发送成功
assertThat(result).isTrue();

// 验证消息已接收
Message<byte[]> receivedMessage = outputDestination.receive(5000, "order-created");
assertThat(receivedMessage).isNotNull();

// 验证消息头
assertThat(receivedMessage.getHeaders())
    .containsKey("eventId")
    .containsKey("eventType");
```

#### OrderMessageConsumerTest（订单消息消费者测试）

| 测试用例 | 描述 | 验证点 |
|---------|------|--------|
| `testConsumePaymentSuccessEvent` | 消费支付成功事件 | 订单服务方法被正确调用 |
| `testConsumeStockFreezeFailedEvent` | 消费库存冻结失败事件 | 订单取消方法被调用 |
| `testConsumePaymentSuccessEvent_Failure` | 支付成功处理失败场景 | 失败场景正确处理 |
| `testConsumMultiplePaymentSuccessEvents` | 批量消费测试 | 所有消息都被消费 |
| `testConsumePaymentSuccessEventWithNullValues` | 空值处理测试 | 空值场景不崩溃 |

**关键验证代码**:
```java
// Mock业务服务
when(orderService.updateOrderStatusAfterPayment(anyLong(), anyLong(), anyString()))
    .thenReturn(true);

// 发送消息
inputDestination.send(message, "payment-success");

// 等待异步处理
TimeUnit.MILLISECONDS.sleep(1000);

// 验证方法被调用
verify(orderService, times(1))
    .updateOrderStatusAfterPayment(
        eq(event.getOrderId()),
        eq(event.getPaymentId()),
        eq(event.getTransactionNo())
    );
```

---

### Stock Service 测试

#### StockMessageProducerTest（库存消息生产者测试）

| 测试用例 | 描述 | 验证点 |
|---------|------|--------|
| `testSendStockFreezeFailedEvent` | 发送库存冻结失败事件 | 消息发送成功、事件类型正确 |
| `testSendStockFreezeFailedEvent_InsufficientStock` | 库存不足场景 | 失败原因包含详细信息 |
| `testSendStockFreezeFailedEvent_SystemError` | 系统异常场景 | 异常信息正确传递 |
| `testSendMultipleStockFreezeFailedEvents` | 批量发送测试 | 5个消息全部发送成功 |
| `testSendStockFreezeFailedEvent_LongReason` | 长文本原因测试 | 长文本不影响发送 |

#### StockMessageConsumerTest（库存消息消费者测试）

| 测试用例 | 描述 | 验证点 |
|---------|------|--------|
| `testConsumeOrderCreatedEvent_SufficientStock` | 库存充足场景 | 库存检查和冻结成功 |
| `testConsumeOrderCreatedEvent_InsufficientStock` | 库存不足场景 | 发送失败事件 |
| `testConsumeOrderCreatedEvent_AlreadyFrozen` | 已冻结场景（幂等性） | 跳过重复处理 |
| `testConsumePaymentSuccessEvent` | 消费支付成功事件 | 库存扣减检查 |
| `testConsumePaymentSuccessEvent_AlreadyDeducted` | 已扣减场景（幂等性） | 跳过重复处理 |
| `testConsumeMultipleOrderCreatedEvents` | 批量消费测试 | 3个订单都正确处理 |

**特殊验证 - 库存不足场景**:
```java
// Mock库存不足
when(stockService.checkStockSufficient(eq(1001L), eq(10))).thenReturn(false);

// 验证失败事件被发送
verify(stockMessageProducer, times(1))
    .sendStockFreezeFailedEvent(
        eq(event.getOrderId()),
        eq(event.getOrderNo()),
        contains("库存不足")  // 失败原因包含"库存不足"
    );
```

---

### Payment Service 测试

#### PaymentMessageProducerTest（支付消息生产者测试）

| 测试用例 | 描述 | 验证点 |
|---------|------|--------|
| `testSendPaymentSuccessEvent_Alipay` | 支付宝支付 | 支付方式为ALIPAY |
| `testSendPaymentSuccessEvent_Wechat` | 微信支付 | 支付方式为WECHAT |
| `testSendPaymentSuccessEvent_UnionPay` | 银联支付 | 支付方式为UNIONPAY |
| `testSendPaymentSuccessEvent_SmallAmount` | 小额支付（0.01元） | 小额金额正确处理 |
| `testSendPaymentSuccessEvent_LargeAmount` | 大额支付（99999.99元） | 大额金额正确处理 |
| `testSendMultiplePaymentSuccessEvents` | 批量发送测试 | 10个消息全部成功 |
| `testSendPaymentSuccessEvent_VerifyHeaders` | 消息头验证 | 所有消息头字段完整 |
| `testSendPaymentSuccessEvent_Concurrent` | 并发发送测试 | 5线程并发无问题 |

**并发测试示例**:
```java
int threadCount = 5;
int eventsPerThread = 2;

Thread[] threads = new Thread[threadCount];
for (int t = 0; t < threadCount; t++) {
    threads[t] = new Thread(() -> {
        for (int i = 0; i < eventsPerThread; i++) {
            boolean result = paymentMessageProducer.sendPaymentSuccessEvent(...);
            assertThat(result).isTrue();
        }
    });
    threads[t].start();
}

// 等待所有线程完成
for (Thread thread : threads) {
    thread.join();
}

// 验证所有消息
int totalEvents = threadCount * eventsPerThread;
for (int i = 0; i < totalEvents; i++) {
    Message<byte[]> receivedMessage = outputDestination.receive(5000, "payment-success");
    assertThat(receivedMessage).isNotNull();
}
```

#### PaymentMessageConsumerTest（支付消息消费者测试）

| 测试用例 | 描述 | 验证点 |
|---------|------|--------|
| `testConsumeOrderCreatedEvent_CompleteFlow` | 完整支付流程 | 创建支付→完成支付→发送事件 |
| `testConsumeOrderCreatedEvent_PaymentExists` | 支付记录已存在（幂等性） | 跳过重复处理 |
| `testConsumeOrderCreatedEvent_CreatePaymentFailed` | 创建支付失败 | 流程正确中断 |
| `testConsumeOrderCreatedEvent_ProcessPaymentFailed` | 处理支付失败 | 不发送成功事件 |
| `testConsumeOrderCreatedEvent_SmallAmount` | 小额订单（0.01元） | 小额支付流程完整 |
| `testConsumeMultipleOrderCreatedEvents` | 批量消费测试 | 3个订单都完成流程 |

**完整流程验证**:
```java
// 验证创建支付被调用
verify(paymentService, times(1)).createPayment(argThat(dto ->
    dto.getOrderId().equals(event.getOrderId()) &&
    dto.getOrderNo().equals(event.getOrderNo()) &&
    dto.getUserId().equals(event.getUserId()) &&
    dto.getAmount().equals(event.getTotalAmount())
));

// 验证处理支付成功被调用
verify(paymentService, times(1)).processPaymentSuccess(eq(5001L));

// 验证发送支付成功事件被调用
verify(paymentMessageProducer, times(1)).sendPaymentSuccessEvent(
    eq(5001L),
    eq(event.getOrderId()),
    eq(event.getOrderNo()),
    eq(event.getUserId()),
    eq(event.getTotalAmount()),
    eq("ALIPAY"),
    anyString()
);
```

---

## 测试结果验证

### 成功标准

✅ **所有测试用例通过**
```
[INFO] Tests run: 37, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

✅ **消息发送验证**
- 消息发送方法返回 `true`
- `OutputDestination.receive()` 能接收到消息
- 消息头包含 `eventId`、`eventType`、`contentType`

✅ **消息消费验证**
- Mock的业务服务方法被正确调用
- 调用次数符合预期（使用 `times(n)`）
- 方法参数匹配（使用 `eq()`、`argThat()`）

✅ **幂等性验证**
- 重复消息不会重复处理
- 使用 `never()` 验证某些方法未被调用

### 查看测试报告

#### Maven Surefire报告

测试完成后，查看详细报告：

```bash
# 生成HTML报告
mvn surefire-report:report

# 报告位置
# order-service/target/surefire-reports/index.html
# stock-service/target/surefire-reports/index.html
# payment-service/target/surefire-reports/index.html
```

#### 命令行输出

每个测试用例都会输出日志：

```
✅ 订单创建事件发送成功
   订单号: ORD202501150001
   订单ID: 10001
   金额: 299.99

✅ 支付成功事件消费成功
   订单号: ORD202501150001
   支付金额: 299.99
   支付方式: ALIPAY
```

---

## 常见问题

### 1. 测试超时失败

**问题**:
```
java.lang.AssertionError:
Expecting value to be true but was false
```

**原因**:
- 异步消息处理未完成就进行断言
- 等待时间不足

**解决方案**:
```java
// 增加等待时间
TimeUnit.MILLISECONDS.sleep(1500);  // 从1000增加到1500

// 或使用更长的超时时间
Message<byte[]> receivedMessage = outputDestination.receive(10000, "topic-name");
```

### 2. Mock未生效

**问题**:
```
Wanted but not invoked:
orderService.updateOrderStatusAfterPayment(...)
```

**原因**:
- Mock配置错误
- 参数匹配失败
- 异步处理未完成

**解决方案**:
```java
// 使用更宽松的参数匹配
when(orderService.updateOrderStatusAfterPayment(anyLong(), anyLong(), anyString()))
    .thenReturn(true);

// 检查是否等待足够时间
TimeUnit.MILLISECONDS.sleep(1000);

// 使用atLeastOnce()而不是times(1)
verify(orderService, atLeastOnce()).updateOrderStatusAfterPayment(...);
```

### 3. 消息未接收到

**问题**:
```
java.lang.AssertionError:
Expecting value to be not null but was null
```

**原因**:
- Topic名称不匹配
- 消息发送失败
- Binder配置错误

**解决方案**:
```java
// 检查topic名称是否一致
outputDestination.receive(5000, "order-created");  // 与配置中的destination一致

// 检查消息发送是否成功
boolean result = orderMessageProducer.sendOrderCreatedEvent(event);
assertThat(result).isTrue();

// 启用DEBUG日志查看详情
logging.level.org.springframework.cloud.stream: DEBUG
```

### 4. 并发测试不稳定

**问题**: 并发测试时而通过时而失败

**解决方案**:
```java
// 增加等待时间
Thread.sleep(2000);

// 使用CountDownLatch精确控制
CountDownLatch latch = new CountDownLatch(threadCount);
threads[t] = new Thread(() -> {
    try {
        // ... 测试逻辑
    } finally {
        latch.countDown();
    }
});
latch.await(10, TimeUnit.SECONDS);

// 使用atLeast验证而不是精确次数
verify(paymentService, atLeast(eventCount)).createPayment(any());
```

### 5. H2数据库冲突

**问题**: 多个测试类同时运行时数据库冲突

**解决方案**:
```yaml
# application-test.yml
spring:
  datasource:
    # 每个测试使用独立的数据库名
    url: jdbc:h2:mem:test_order_db_${random.uuid};MODE=MySQL
```

---

## 最佳实践

### 1. 测试命名规范

```java
// ✅ 好的命名 - 描述性强
testSendOrderCreatedEvent_InsufficientStock()
testConsumePaymentSuccessEvent_AlreadyProcessed()

// ❌ 不好的命名
test1()
testOrder()
```

### 2. 使用DisplayName注解

```java
@Test
@DisplayName("测试消费订单创建事件 - 库存不足场景")
void testConsumeOrderCreatedEvent_InsufficientStock() {
    // ...
}
```

### 3. Given-When-Then结构

```java
@Test
void testSendOrderCreatedEvent() {
    // Given - 准备测试数据
    OrderCreatedEvent event = OrderCreatedEvent.builder()
        .orderId(10001L)
        .build();

    // When - 执行被测试方法
    boolean result = orderMessageProducer.sendOrderCreatedEvent(event);

    // Then - 验证结果
    assertThat(result).isTrue();
}
```

### 4. 使用AssertJ流式断言

```java
// ✅ 推荐 - AssertJ流式风格
assertThat(receivedMessage)
    .isNotNull()
    .extracting(Message::getHeaders)
    .containsKey("eventId")
    .containsKey("eventType");

// ❌ 不推荐 - JUnit传统风格
assertTrue(result);
assertNotNull(receivedMessage);
```

### 5. Mock返回值一致性

```java
// ✅ 好的Mock - 返回值类型一致
when(paymentService.createPayment(any())).thenReturn(5001L);

// ❌ 不好的Mock - 可能返回null
when(paymentService.createPayment(any())).thenReturn(null);  // 应该在特定测试中
```

### 6. 测试隔离性

```java
// 每个测试方法都应该独立运行，不依赖其他测试
@BeforeEach
void setUp() {
    // 重置Mock状态
    Mockito.reset(orderService, stockService);
}
```

### 7. 异步等待策略

```java
// ✅ 好的等待策略 - 足够的时间
TimeUnit.MILLISECONDS.sleep(1000);
verify(orderService, times(1)).updateOrder(...);

// 或使用Awaitility库
await().atMost(2, SECONDS)
    .untilAsserted(() -> verify(orderService).updateOrder(...));
```

### 8. 日志输出

```java
// 在测试中输出关键信息，便于调试
System.out.println("✅ 订单创建事件发送成功");
System.out.println("   订单号: " + event.getOrderNo());
System.out.println("   订单ID: " + event.getOrderId());
```

---

## 测试覆盖率

使用JaCoCo查看代码覆盖率：

```bash
# 生成覆盖率报告
mvn clean test jacoco:report

# 查看报告
# order-service/target/site/jacoco/index.html
# stock-service/target/site/jacoco/index.html
# payment-service/target/site/jacoco/index.html
```

**目标覆盖率**:
- 消息生产者类：**90%+**
- 消息消费者类：**85%+**
- 整体项目：**70%+**

---

## 持续集成

### GitHub Actions示例

```yaml
name: Message Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Run Message Tests
        run: |
          mvn clean test -pl order-service,stock-service,payment-service

      - name: Upload Test Reports
        if: always()
        uses: actions/upload-artifact@v3
        with:
          name: test-reports
          path: '**/target/surefire-reports'
```

---

## 总结

本测试套件提供了全面的RocketMQ消息流测试，涵盖：

✅ **消息生产测试** - 验证消息发送功能
✅ **消息消费测试** - 验证消息接收和处理逻辑
✅ **幂等性测试** - 验证重复消息不会重复处理
✅ **异常场景测试** - 验证错误处理机制
✅ **并发测试** - 验证高并发场景稳定性
✅ **边界测试** - 验证极值和边界条件

**运行测试无需依赖外部环境**，所有测试都使用内存模拟，执行速度快，适合CI/CD集成。

---

**文档版本**: v1.0.0
**最后更新**: 2025-01-15
**作者**: what's up

