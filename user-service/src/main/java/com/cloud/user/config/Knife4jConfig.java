package com.cloud.user.config;

import com.cloud.common.config.BaseKnife4jConfig;
import org.springframework.context.annotation.Configuration;






@Configuration
public class Knife4jConfig extends BaseKnife4jConfig {

    @Override
    protected String getServiceTitle() {
        return "鐢ㄦ埛鏈嶅姟 API 鏂囨。";
    }

    @Override
    protected String getServiceDescription() {
        return "鐢ㄦ埛绠＄悊銆佸晢瀹剁鐞嗐€佺敤鎴峰湴鍧€绠＄悊鐩稿叧鐨?RESTful API 鏂囨。";
    }
}
