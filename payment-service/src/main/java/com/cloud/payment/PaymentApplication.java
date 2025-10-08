package com.cloud.payment;


import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication(
        scanBasePackages = {"com.cloud.payment", "com.cloud.common"}
)
@EnableDiscoveryClient
@Slf4j
@EnableAspectJAutoProxy(proxyTargetClass = true)
@MapperScan("com.cloud.payment.mapper")
public class PaymentApplication {
    public static void main(String[] args) {
        System.setProperty("nacos.logging.default.config.enabled", "false");
        System.setProperty("nacos.logging.config", "");
        System.setProperty("nacos.logging.path", "");

        System.out.println("正在启动支付服务...");
        SpringApplication.run(PaymentApplication.class, args);
        System.out.println("支付服务启动完成！");
    }
}
