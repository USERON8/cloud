package com.cloud.product.config;

import com.cloud.product.service.support.ProductCacheInvalidationMessageListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
@ConditionalOnProperty(prefix = "product.cache.guard.pubsub", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ProductCachePubSubConfig {

    @Bean
    public RedisMessageListenerContainer productCacheMessageListenerContainer(
            RedisConnectionFactory redisConnectionFactory,
            ProductCacheInvalidationMessageListener messageListener,
            @Value("${product.cache.guard.pubsub.channel:product:cache:invalidate}") String channel) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);
        container.addMessageListener(messageListener, new ChannelTopic(channel));
        return container;
    }
}
