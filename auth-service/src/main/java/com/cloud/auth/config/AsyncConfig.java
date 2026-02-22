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
@ConditionalOnProperty(name = "auth.async.enabled", havingValue = "true", matchIfMissing = true)
public class AsyncConfig extends BaseAsyncConfig {

    




    @Bean("authAsyncExecutor")
    public Executor authAsyncExecutor() {
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                3,
                8,
                300,
                "auth-async-"
        );
        executor.initialize();

        
        return executor;
    }

    




    @Bean("authTokenExecutor")
    public Executor authTokenExecutor() {
        ThreadPoolTaskExecutor executor = createQueryExecutor("auth-token-");
        executor.initialize();

        
        return executor;
    }

    




    @Bean("authSecurityLogExecutor")
    public Executor authSecurityLogExecutor() {
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                2,
                6,
                500,
                "auth-security-log-"
        );
        executor.initialize();

        
        return executor;
    }

    




    @Bean("authOAuth2Executor")
    @ConditionalOnProperty(name = "auth.oauth2.enabled", havingValue = "true", matchIfMissing = true)
    public Executor authOAuth2Executor() {
        ThreadPoolTaskExecutor executor = createIOExecutor("auth-oauth2-");
        executor.initialize();

        
        return executor;
    }

    




    @Bean("authSessionExecutor")
    public Executor authSessionExecutor() {
        ThreadPoolTaskExecutor executor = createThreadPoolTaskExecutor(
                2,
                5,
                200,
                "auth-session-"
        );
        executor.initialize();

        
        return executor;
    }

    



    @Bean("authCommonAsyncExecutor")
    public Executor authCommonAsyncExecutor() {
        ThreadPoolTaskExecutor executor = createCommonAsyncExecutor();
        executor.initialize();

        
        return executor;
    }

}
