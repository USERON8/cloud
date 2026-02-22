package com.cloud.stock.config;

import com.cloud.common.config.BaseKnife4jConfig;
import org.springframework.context.annotation.Configuration;






@Configuration
public class Knife4jConfig extends BaseKnife4jConfig {

    @Override
    protected String getServiceTitle() {
        return "搴撳瓨鏈嶅姟 API 鏂囨。";
    }

    @Override
    protected String getServiceDescription() {
        return "搴撳瓨绠＄悊銆佸簱瀛樻煡璇€佸簱瀛樺嚭鍏ュ簱鎿嶄綔鐩稿叧鐨?RESTful API 鏂囨。";
    }
}
