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







@Slf4j
@Configuration
@ConfigurationProperties(prefix = "alipay")
@Data
@Validated
public class AlipayConfig {

    


    @NotBlank(message = "鏀粯瀹濆簲鐢↖D涓嶈兘涓虹┖")
    private String appId;

    


    @NotBlank(message = "搴旂敤绉侀挜涓嶈兘涓虹┖")
    private String merchantPrivateKey;

    


    @NotBlank(message = "鏀粯瀹濆叕閽ヤ笉鑳戒负绌?)
    private String alipayPublicKey;

    


    @NotBlank(message = "鏀粯瀹濈綉鍏冲湴鍧€涓嶈兘涓虹┖")
    private String gatewayUrl;

    


    @NotBlank(message = "寮傛閫氱煡鍦板潃涓嶈兘涓虹┖")
    private String notifyUrl;

    


    private String returnUrl;

    


    private String signType = "RSA2";

    


    private String charset = "UTF-8";

    


    private String format = "json";

    


    private String timeout = "30m";

    




    @Bean
    public AlipayClient alipayClient() {
        

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
