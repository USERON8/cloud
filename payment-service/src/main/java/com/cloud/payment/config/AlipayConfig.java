package com.cloud.payment.config;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * 支付宝配置类
 *
 * @author what's up
 * @since 1.0.0
 */
@Slf4j
@Configuration
@ConfigurationProperties(prefix = "alipay")
@Data
@Validated
public class AlipayConfig {

    /**
     * 支付宝应用ID
     */
    @NotBlank(message = "支付宝应用ID不能为空")
    private String appId;

    /**
     * 应用私钥
     */
    @NotBlank(message = "应用私钥不能为空")
    private String merchantPrivateKey;

    /**
     * 支付宝公钥
     */
    @NotBlank(message = "支付宝公钥不能为空")
    private String alipayPublicKey;

    /**
     * 支付宝网关地址
     */
    @NotBlank(message = "支付宝网关地址不能为空")
    private String gatewayUrl;

    /**
     * 异步通知地址
     */
    @NotBlank(message = "异步通知地址不能为空")
    private String notifyUrl;

    /**
     * 同步跳转地址
     */
    private String returnUrl;

    /**
     * 签名类型
     */
    private String signType = "RSA2";

    /**
     * 字符编码
     */
    private String charset = "UTF-8";

    /**
     * 数据格式
     */
    private String format = "json";

    /**
     * 支付超时时间
     */
    private String timeout = "30m";

    /**
     * 创建支付宝客户端
     *
     * @return AlipayClient
     */
    @Bean
    public AlipayClient alipayClient() {
        log.info("初始化支付宝客户端 - AppId: {}, Gateway: {}", appId, gatewayUrl);

        return new DefaultAlipayClient(
                gatewayUrl,
                appId,
                merchantPrivateKey,
                format,
                charset,
                alipayPublicKey,
                signType
        );
    }
}
