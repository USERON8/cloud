package com.cloud.common.annotation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标准化API文档注解
 * 提供统一的API文档标注，确保接口文档的一致性和完整性
 *
 * @author cloud
 */
public class ApiStandard {

    /**
     * 标准化操作注解
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Operation(
            summary = "执行操作",
            description = "执行业务操作",
            responses = {
                    @ApiResponse(responseCode = "200", description = "操作成功",
                            content = @Content(schema = @Schema(implementation = Object.class))),
                    @ApiResponse(responseCode = "400", description = "请求参数错误"),
                    @ApiResponse(responseCode = "401", description = "未授权"),
                    @ApiResponse(responseCode = "403", description = "权限不足"),
                    @ApiResponse(responseCode = "404", description = "资源不存在"),
                    @ApiResponse(responseCode = "500", description = "系统内部错误")
            }
    )
    public @interface StandardOperation {
        /**
         * 操作摘要
         */
        String summary() default "执行操作";

        /**
         * 操作描述
         */
        String description() default "执行业务操作";

        /**
         * 业务标签
         */
        String businessTag() default "";
    }

    /**
     * 标准化CRUD操作注解
     */
    public @interface CrudOperations {

        /**
         * 查询列表操作
         */
        @Target(ElementType.METHOD)
        @Retention(RetentionPolicy.RUNTIME)
        @Operation(
                summary = "获取{entityName}列表",
                description = "分页查询{entityName}列表，支持查询参数过滤"
        )
        @interface List {
            String entityName() default "实体";
        }

        /**
         * 查询详情操作
         */
        @Target(ElementType.METHOD)
        @Retention(RetentionPolicy.RUNTIME)
        @Operation(
                summary = "获取{entityName}详情",
                description = "根据{entityName}ID获取详细信息"
        )
        @interface Detail {
            String entityName() default "实体";
        }

        /**
         * 创建操作
         */
        @Target(ElementType.METHOD)
        @Retention(RetentionPolicy.RUNTIME)
        @Operation(
                summary = "创建{entityName}",
                description = "创建新的{entityName}"
        )
        @interface Create {
            String entityName() default "实体";
        }

        /**
         * 更新操作
         */
        @Target(ElementType.METHOD)
        @Retention(RetentionPolicy.RUNTIME)
        @Operation(
                summary = "更新{entityName}",
                description = "更新{entityName}信息"
        )
        @interface Update {
            String entityName() default "实体";
        }

        /**
         * 删除操作
         */
        @Target(ElementType.METHOD)
        @Retention(RetentionPolicy.RUNTIME)
        @Operation(
                summary = "删除{entityName}",
                description = "删除{entityName}"
        )
        @interface Delete {
            String entityName() default "实体";
        }
    }

    /**
     * 标准化业务操作注解
     */
    public @interface BusinessOperations {

        /**
         * 启用操作
         */
        @Target(ElementType.METHOD)
        @Retention(RetentionPolicy.RUNTIME)
        @Operation(
                summary = "启用{entityName}",
                description = "将{entityName}状态设置为启用"
        )
        @interface Enable {
            String entityName() default "实体";
        }

        /**
         * 禁用操作
         */
        @Target(ElementType.METHOD)
        @Retention(RetentionPolicy.RUNTIME)
        @Operation(
                summary = "禁用{entityName}",
                description = "将{entityName}状态设置为禁用"
        )
        @interface Disable {
            String entityName() default "实体";
        }

        /**
         * 审核操作
         */
        @Target(ElementType.METHOD)
        @Retention(RetentionPolicy.RUNTIME)
        @Operation(
                summary = "审核{entityName}",
                description = "审核{entityName}，通过或拒绝"
        )
        @interface Approve {
            String entityName() default "实体";
        }

        /**
         * 重置操作
         */
        @Target(ElementType.METHOD)
        @Retention(RetentionPolicy.RUNTIME)
        @Operation(
                summary = "重置{entityName}",
                description = "重置{entityName}到初始状态"
        )
        @interface Reset {
            String entityName() default "实体";
        }
    }

    /**
     * 标准化批量操作注解
     */
    public @interface BatchOperations {

        /**
         * 批量创建操作
         */
        @Target(ElementType.METHOD)
        @Retention(RetentionPolicy.RUNTIME)
        @Operation(
                summary = "批量创建{entityName}",
                description = "批量创建多个{entityName}"
        )
        @interface BatchCreate {
            String entityName() default "实体";
        }

        /**
         * 批量更新操作
         */
        @Target(ElementType.METHOD)
        @Retention(RetentionPolicy.RUNTIME)
        @Operation(
                summary = "批量更新{entityName}",
                description = "批量更新多个{entityName}"
        )
        @interface BatchUpdate {
            String entityName() default "实体";
        }

        /**
         * 批量删除操作
         */
        @Target(ElementType.METHOD)
        @Retention(RetentionPolicy.RUNTIME)
        @Operation(
                summary = "批量删除{entityName}",
                description = "根据ID列表批量删除{entityName}"
        )
        @interface BatchDelete {
            String entityName() default "实体";
        }

        /**
         * 批量启用操作
         */
        @Target(ElementType.METHOD)
        @Retention(RetentionPolicy.RUNTIME)
        @Operation(
                summary = "批量启用{entityName}",
                description = "批量将多个{entityName}设置为启用状态"
        )
        @interface BatchEnable {
            String entityName() default "实体";
        }

        /**
         * 批量禁用操作
         */
        @Target(ElementType.METHOD)
        @Retention(RetentionPolicy.RUNTIME)
        @Operation(
                summary = "批量禁用{entityName}",
                description = "批量将多个{entityName}设置为禁用状态"
        )
        @interface BatchDisable {
            String entityName() default "实体";
        }
    }

    /**
     * 标准化参数注解
     */
    public @interface Parameters {

        /**
         * 分页参数
         */
        @Target(ElementType.PARAMETER)
        @Retention(RetentionPolicy.RUNTIME)
        @Parameter(description = "页码", example = "1")
        @interface Page {
        }

        /**
         * 每页数量参数
         */
        @Target(ElementType.PARAMETER)
        @Retention(RetentionPolicy.RUNTIME)
        @Parameter(description = "每页数量", example = "20")
        @interface Size {
        }

        /**
         * ID参数
         */
        @Target(ElementType.PARAMETER)
        @Retention(RetentionPolicy.RUNTIME)
        @Parameter(description = "实体ID", example = "1")
        @interface Id {
            String value() default "ID";
        }

        /**
         * 状态参数
         */
        @Target(ElementType.PARAMETER)
        @Retention(RetentionPolicy.RUNTIME)
        @Parameter(description = "状态", example = "1")
        @interface Status {
            String description() default "状态";
        }

        /**
         * 排序参数
         */
        @Target(ElementType.PARAMETER)
        @Retention(RetentionPolicy.RUNTIME)
        @Parameter(description = "排序字段", example = "createdAt")
        @interface Sort {
        }

        /**
         * 搜索关键词参数
         */
        @Target(ElementType.PARAMETER)
        @Retention(RetentionPolicy.RUNTIME)
        @Parameter(description = "搜索关键词", example = "keyword")
        @interface Keyword {
        }
    }

    /**
     * 标准化响应注解
     */
    public @interface Responses {

        /**
         * 成功响应
         */
        @Target(ElementType.METHOD)
        @Retention(RetentionPolicy.RUNTIME)
        @ApiResponses({
                @ApiResponse(responseCode = "200", description = "操作成功"),
                @ApiResponse(responseCode = "400", description = "请求参数错误"),
                @ApiResponse(responseCode = "401", description = "未授权"),
                @ApiResponse(responseCode = "403", description = "权限不足"),
                @ApiResponse(responseCode = "404", description = "资源不存在"),
                @ApiResponse(responseCode = "500", description = "系统内部错误")
        })
        @interface StandardResponse {
        }

        /**
         * 分页响应
         */
        @Target(ElementType.METHOD)
        @Retention(RetentionPolicy.RUNTIME)
        @ApiResponses({
                @ApiResponse(responseCode = "200", description = "查询成功"),
                @ApiResponse(responseCode = "400", description = "查询参数错误"),
                @ApiResponse(responseCode = "401", description = "未授权"),
                @ApiResponse(responseCode = "403", description = "权限不足"),
                @ApiResponse(responseCode = "500", description = "系统内部错误")
        })
        @interface PageResponse {
        }
    }

    /**
     * 标准化标签注解
     */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @Tag(name = "{serviceName}服务", description = "{serviceName}相关的API接口")
    public @interface ServiceTag {
        String serviceName() default "业务";
    }

    /**
     * 标准化业务模块注解
     */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @Tag(name = "{moduleName}模块", description = "{moduleName}相关的业务功能接口")
    public @interface ModuleTag {
        String moduleName() default "业务";
    }
}