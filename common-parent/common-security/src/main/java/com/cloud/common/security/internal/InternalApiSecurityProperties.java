package com.cloud.common.security.internal;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "cloud.security.internal-api")
public class InternalApiSecurityProperties {

    private boolean enabled = true;
    private String headerName = InternalApiHeaders.API_KEY_HEADER;
    private String callerHeaderName = InternalApiHeaders.CALLER_HEADER;
    private String key = System.getenv("INTERNAL_API_KEY");
}
