package com.cloud.gateway.config;

import io.netty.channel.ChannelOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayServerNettyConfig {

  @Value("${GATEWAY_SERVER_NETTY_CONNECTION_BACKLOG:4096}")
  private int connectionBacklog;

  @Bean
  public WebServerFactoryCustomizer<NettyReactiveWebServerFactory> gatewayNettyServerCustomizer() {
    return factory ->
        factory.addServerCustomizers(
            httpServer ->
                httpServer
                    .option(ChannelOption.SO_BACKLOG, connectionBacklog)
                    .option(ChannelOption.SO_REUSEADDR, true));
  }
}
