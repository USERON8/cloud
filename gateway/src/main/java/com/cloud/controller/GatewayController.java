package com.cloud.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * 网关服务API接口文档
 * 提供网关相关功能的REST接口，包括健康检查、状态查询等
 */
@Slf4j
@RestController
@RequestMapping("/gateway")
@Tag(name = "网关服务", description = "网关相关接口，包含健康检查、状态查询等功能")
public class GatewayController {

    /**
     * 网关健康检查接口
     * 该接口用于检查网关服务的基本运行状态
     * 接口返回表示服务正常运行的字符串
     *
     * @return 响应结果，格式为字符串"Gateway is running"表示网关正常运行
     */
    @GetMapping("/health")
    @Operation(
            summary = "网关健康检查",
            description = "检查网关服务是否正常运行，返回简单文本状态"
    )
    @ApiResponse(
            responseCode = "200",
            description = "网关正常运行",
            content = @Content(
                    mediaType = "text/plain",
                    schema = @Schema(type = "string"),
                    examples = @ExampleObject("Gateway is running")
            )
    )
    public Mono<String> healthCheck() {
        log.info("网关健康检查");
        return Mono.just("Gateway is running");
    }
}