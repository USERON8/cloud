package com.cloud.auth.config;

import com.cloud.common.config.BaseKnife4jConfig;
import org.springframework.context.annotation.Configuration;






@Configuration
public class Knife4jConfig extends BaseKnife4jConfig {

    @Override
    protected String getServiceTitle() {
        return "璁よ瘉鏈嶅姟 API 鏂囨。";
    }

    @Override
    protected String getServiceDescription() {
        return "鐢ㄦ埛璁よ瘉銆丱Auth2 鎺堟潈銆佺櫥褰曟敞鍐岀浉鍏崇殑 RESTful API 鏂囨。";
    }
}
