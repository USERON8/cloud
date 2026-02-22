package com.cloud.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;









@Slf4j
public class RedisConfigFactory {

    






    public static RedisTemplate<String, Object> createBasicRedisTemplate(RedisConnectionFactory connectionFactory) {
        
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(RedisSerializer.json());
        template.setHashValueSerializer(RedisSerializer.json());
        template.setDefaultSerializer(RedisSerializer.json());

        template.afterPropertiesSet();
        return template;
    }

    






    public static RedisTemplate<String, Object> createHighPerformanceRedisTemplate(RedisConnectionFactory connectionFactory) {
        
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();

        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        template.setDefaultSerializer(jsonSerializer);

        
        template.setEnableTransactionSupport(true);

        template.afterPropertiesSet();
        return template;
    }

    






    public static RedisTemplate<String, Object> createCacheRedisTemplate(RedisConnectionFactory connectionFactory) {
        
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(RedisSerializer.json());
        template.setHashValueSerializer(RedisSerializer.json());
        template.setDefaultSerializer(RedisSerializer.json());

        
        template.setEnableTransactionSupport(false);

        template.afterPropertiesSet();
        return template;
    }

    






    public static RedisTemplate<String, Object> createSessionRedisTemplate(RedisConnectionFactory connectionFactory) {
        
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(RedisSerializer.json());
        template.setHashValueSerializer(RedisSerializer.json());
        template.setDefaultSerializer(RedisSerializer.json());

        
        template.setEnableTransactionSupport(true);

        template.afterPropertiesSet();
        return template;
    }

    






    public static RedisTemplate<String, Object> createMessageRedisTemplate(RedisConnectionFactory connectionFactory) {
        
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(RedisSerializer.json());
        template.setHashValueSerializer(RedisSerializer.json());
        template.setDefaultSerializer(RedisSerializer.json());

        
        template.setEnableTransactionSupport(true);

        template.afterPropertiesSet();
        return template;
    }

    





    public static RedisTemplate<String, Object> createCustomRedisTemplate(RedisTemplateBuilder builder) {
        
        return builder.build();
    }

    





    public static StringRedisTemplate createStringRedisTemplate(RedisConnectionFactory connectionFactory) {
        
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(connectionFactory);
        template.afterPropertiesSet();
        return template;
    }

    


    public static class RedisTemplateBuilder {
        private RedisConnectionFactory connectionFactory;
        private RedisSerializer<?> keySerializer = new StringRedisSerializer();
        private RedisSerializer<?> hashKeySerializer = new StringRedisSerializer();
        private RedisSerializer<?> valueSerializer = RedisSerializer.json();
        private RedisSerializer<?> hashValueSerializer = RedisSerializer.json();
        private RedisSerializer<?> defaultSerializer = RedisSerializer.json();
        private boolean enableTransactionSupport = false;

        public RedisTemplateBuilder connectionFactory(RedisConnectionFactory connectionFactory) {
            this.connectionFactory = connectionFactory;
            return this;
        }

        public RedisTemplateBuilder keySerializer(RedisSerializer<?> keySerializer) {
            this.keySerializer = keySerializer;
            return this;
        }

        public RedisTemplateBuilder hashKeySerializer(RedisSerializer<?> hashKeySerializer) {
            this.hashKeySerializer = hashKeySerializer;
            return this;
        }

        public RedisTemplateBuilder valueSerializer(RedisSerializer<?> valueSerializer) {
            this.valueSerializer = valueSerializer;
            return this;
        }

        public RedisTemplateBuilder hashValueSerializer(RedisSerializer<?> hashValueSerializer) {
            this.hashValueSerializer = hashValueSerializer;
            return this;
        }

        public RedisTemplateBuilder defaultSerializer(RedisSerializer<?> defaultSerializer) {
            this.defaultSerializer = defaultSerializer;
            return this;
        }

        public RedisTemplateBuilder enableTransactionSupport(boolean enableTransactionSupport) {
            this.enableTransactionSupport = enableTransactionSupport;
            return this;
        }

        public RedisTemplate<String, Object> build() {
            if (connectionFactory == null) {
                throw new IllegalArgumentException("RedisConnectionFactory cannot be null");
            }

            RedisTemplate<String, Object> template = new RedisTemplate<>();
            template.setConnectionFactory(connectionFactory);
            template.setKeySerializer(keySerializer);
            template.setHashKeySerializer(hashKeySerializer);
            template.setValueSerializer(valueSerializer);
            template.setHashValueSerializer(hashValueSerializer);
            template.setDefaultSerializer(defaultSerializer);
            template.setEnableTransactionSupport(enableTransactionSupport);
            template.afterPropertiesSet();

            return template;
        }
    }
}
