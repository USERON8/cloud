package com.cloud.log;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.ApplicationContext;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 日志服务启动测试
 * 
 * @author CloudDevAgent
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
public class LogServiceTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    public void testApplicationContextLoads() {
        assertNotNull(applicationContext, "应用上下文应该成功加载");
        
        // 验证RedisTemplate相关Bean不存在
        assertFalse(applicationContext.containsBean("redisTemplate"), 
                   "RedisTemplate应该被排除");
        
        System.out.println("✅ log-service启动成功，Redis配置已正确排除");
    }
}
