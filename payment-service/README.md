# Payment Service

支付服务，包含支付单管理和支付宝聚合接口。

- 服务名：`payment-service`
- 端口：`8086`
- 数据库脚本：`db/init/payment-service/init.sql`
- 测试数据：`db/test/payment-service/test.sql`

## 核心接口

- 支付单：`/api/payments/**`
- 支付宝：`/api/v1/payment/alipay/** (create/query/refund/close/notify/verify)`
- 内部调用：`/internal/payment/**`

## 本地启动

```bash
mvn -pl payment-service spring-boot:run
```

## Cpolar 适配

使用 cpolar 暴露支付回调地址时，直接注入环境变量即可，不需要改代码：

```bash
# 例如你的 cpolar 公网地址
export CPOLAR_PUBLIC_BASE_URL=https://xxxxxx.cpolar.cn
export CPOLAR_FRONTEND_BASE_URL=https://xxxxxx.cpolar.cn

# 或者你想完全手动指定支付宝回调地址
# export ALIPAY_NOTIFY_URL=https://xxxxxx.cpolar.cn/api/v1/payment/alipay/notify
# export ALIPAY_RETURN_URL=https://xxxxxx.cpolar.cn/payment/success

# 接口内容加密（可选）
# export ALIPAY_APP_ENCRYPT_KEY=你的appEncryptKey

mvn -pl payment-service spring-boot:run
```

优先级：
1. `ALIPAY_NOTIFY_URL` / `ALIPAY_RETURN_URL`
2. `CPOLAR_PUBLIC_BASE_URL` / `CPOLAR_FRONTEND_BASE_URL`
3. 默认本地地址（`127.0.0.1`）

