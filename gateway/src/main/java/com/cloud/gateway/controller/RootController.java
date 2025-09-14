package com.cloud.gateway.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * 根路径控制器
 * 处理[/]请求，避免默认的Whitelabel Error Page
 */
@RestController
public class RootController {

    /**
     * 处理根路径请求
     *
     * @return 欢迎信息
     */
    @GetMapping("/")
    public Mono<Map<String, String>> root() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "欢迎使用云原生微服务系统网关");
        response.put("timestamp", String.valueOf(System.currentTimeMillis()));
        return Mono.just(response);
    }
}