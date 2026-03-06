package com.cloud.auth.config;

import com.cloud.common.config.BaseAsyncConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;














@Slf4j
@Configuration
@EnableAsync
@ConditionalOnProperty(name = "app.async.enabled", havingValue = "true", matchIfMissing = true)
public class AsyncConfig extends BaseAsyncConfig {

    




    @Bean("authAsyncExecutor")
    public Executor authAsyncExecutor() {
        ThreadPoolTaskExecutor executor = createConfiguredExecutor(
                "authAsyncExecutor",
                3,
                8,
                300,
                60,
                "auth-async-"
        );
        executor.initialize();

        
        return executor;
    }

    




    @Bean("authTokenExecutor")
    public Executor authTokenExecutor() {
        ThreadPoolTaskExecutor executor = createConfiguredExecutor(
                "authTokenExecutor",
                4,
                16,
                500,
                60,
                "auth-token-"
        );
        executor.initialize();

        
        return executor;
    }

    




    @Bean("authSecurityLogExecutor")
    public Executor authSecurityLogExecutor() {
        ThreadPoolTaskExecutor executor = createConfiguredExecutor(
                "authSecurityLogExecutor",
                2,
                6,
                500,
                60,
                "auth-security-log-"
        );
        executor.initialize();

        
        return executor;
    }

    




    @Bean("authOAuth2Executor")
    @ConditionalOnProperty(name = "auth.oauth2.enabled", havingValue = "true", matchIfMissing = true)
    public Executor authOAuth2Executor() {
        int processors = Runtime.getRuntime().availableProcessors();
        ThreadPoolTaskExecutor executor = createConfiguredExecutor(
                "authOAuth2Executor",
                processors * 2,
                processors * 4,
                300,
                60,
                "auth-oauth2-"
        );
        executor.initialize();

        
        return executor;
    }

    




    @Bean("authSessionExecutor")
    public Executor authSessionExecutor() {
        ThreadPoolTaskExecutor executor = createConfiguredExecutor(
                "authSessionExecutor",
                2,
                5,
                200,
                60,
                "auth-session-"
        );
        executor.initialize();

        
        return executor;
    }

    



    @Bean("authCommonAsyncExecutor")
    public Executor authCommonAsyncExecutor() {
        int processors = Runtime.getRuntime().availableProcessors();
        ThreadPoolTaskExecutor executor = createConfiguredExecutor(
                "authCommonAsyncExecutor",
                Math.max(2, processors / 2),
                processors * 2,
                200,
                60,
                "common-async-"
        );
        executor.initialize();

        
        return executor;
    }

}
