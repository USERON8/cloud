package com.cloud.payment.config;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

@Configuration
@ConfigurationProperties(prefix = "alipay")
@Data
@Validated
public class AlipayConfig {

    @NotBlank(message = "Alipay appId cannot be blank")
    private String appId;

    @NotBlank(message = "Alipay merchant private key cannot be blank")
    private String merchantPrivateKey;

    @NotBlank(message = "Alipay public key cannot be blank")
    private String alipayPublicKey;

    @NotBlank(message = "Alipay gateway url cannot be blank")
    private String gatewayUrl;

    @NotBlank(message = "Alipay notify url cannot be blank")
    private String notifyUrl;

    private String returnUrl;

    private String appEncryptKey;

    private String signType = "RSA2";

    private String charset = "UTF-8";

    private String format = "json";

    private String timeout = "30m";

    @Bean
    public AlipayClient alipayClient() {
        if (StringUtils.hasText(appEncryptKey)) {
            return new DefaultAlipayClient(
                    gatewayUrl,
                    appId,
                    merchantPrivateKey,
                    format,
                    charset,
                    alipayPublicKey,
                    signType,
                    appEncryptKey
            );
        }

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
