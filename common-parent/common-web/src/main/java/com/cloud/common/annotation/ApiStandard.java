package com.cloud.common.annotation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public final class ApiStandard {

    private ApiStandard() {
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Operation(
            summary = "Standard operation",
            description = "Standard API operation",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Success"),
                    @ApiResponse(responseCode = "400", description = "Bad request"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "403", description = "Forbidden"),
                    @ApiResponse(responseCode = "404", description = "Not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public @interface StandardOperation {
        String summary() default "Standard operation";

        String description() default "Standard API operation";

        String businessTag() default "";
    }

    public @interface CrudOperations {
        @Target(ElementType.METHOD)
        @Retention(RetentionPolicy.RUNTIME)
        @Operation(summary = "List entities", description = "Query entity list")
        @interface List {
            String entityName() default "entity";
        }

        @Target(ElementType.METHOD)
        @Retention(RetentionPolicy.RUNTIME)
        @Operation(summary = "Get entity detail", description = "Query entity by ID")
        @interface Detail {
            String entityName() default "entity";
        }

        @Target(ElementType.METHOD)
        @Retention(RetentionPolicy.RUNTIME)
        @Operation(summary = "Create entity", description = "Create a new entity")
        @interface Create {
            String entityName() default "entity";
        }

        @Target(ElementType.METHOD)
        @Retention(RetentionPolicy.RUNTIME)
        @Operation(summary = "Update entity", description = "Update existing entity")
        @interface Update {
            String entityName() default "entity";
        }

        @Target(ElementType.METHOD)
        @Retention(RetentionPolicy.RUNTIME)
        @Operation(summary = "Delete entity", description = "Delete entity by ID")
        @interface Delete {
            String entityName() default "entity";
        }
    }

    public @interface BusinessOperations {
        @Target(ElementType.METHOD)
        @Retention(RetentionPolicy.RUNTIME)
        @Operation(summary = "Enable entity", description = "Enable entity business state")
        @interface Enable {
            String entityName() default "entity";
        }

        @Target(ElementType.METHOD)
        @Retention(RetentionPolicy.RUNTIME)
        @Operation(summary = "Disable entity", description = "Disable entity business state")
        @interface Disable {
            String entityName() default "entity";
        }

        @Target(ElementType.METHOD)
        @Retention(RetentionPolicy.RUNTIME)
        @Operation(summary = "Approve entity", description = "Approve entity business process")
        @interface Approve {
            String entityName() default "entity";
        }

        @Target(ElementType.METHOD)
        @Retention(RetentionPolicy.RUNTIME)
        @Operation(summary = "Reset entity", description = "Reset entity business state")
        @interface Reset {
            String entityName() default "entity";
        }
    }

    public @interface BatchOperations {
        @Target(ElementType.METHOD)
        @Retention(RetentionPolicy.RUNTIME)
        @Operation(summary = "Batch create", description = "Batch create entities")
        @interface BatchCreate {
            String entityName() default "entity";
        }

        @Target(ElementType.METHOD)
        @Retention(RetentionPolicy.RUNTIME)
        @Operation(summary = "Batch update", description = "Batch update entities")
        @interface BatchUpdate {
            String entityName() default "entity";
        }

        @Target(ElementType.METHOD)
        @Retention(RetentionPolicy.RUNTIME)
        @Operation(summary = "Batch delete", description = "Batch delete entities")
        @interface BatchDelete {
            String entityName() default "entity";
        }

        @Target(ElementType.METHOD)
        @Retention(RetentionPolicy.RUNTIME)
        @Operation(summary = "Batch enable", description = "Batch enable entities")
        @interface BatchEnable {
            String entityName() default "entity";
        }

        @Target(ElementType.METHOD)
        @Retention(RetentionPolicy.RUNTIME)
        @Operation(summary = "Batch disable", description = "Batch disable entities")
        @interface BatchDisable {
            String entityName() default "entity";
        }
    }

    public @interface Parameters {
        @Target(ElementType.PARAMETER)
        @Retention(RetentionPolicy.RUNTIME)
        @Parameter(description = "Page number", example = "1")
        @interface Page {
        }

        @Target(ElementType.PARAMETER)
        @Retention(RetentionPolicy.RUNTIME)
        @Parameter(description = "Page size", example = "20")
        @interface Size {
        }

        @Target(ElementType.PARAMETER)
        @Retention(RetentionPolicy.RUNTIME)
        @Parameter(description = "Entity ID", example = "1")
        @interface Id {
            String value() default "ID";
        }

        @Target(ElementType.PARAMETER)
        @Retention(RetentionPolicy.RUNTIME)
        @Parameter(description = "Status", example = "1")
        @interface Status {
            String description() default "Status";
        }

        @Target(ElementType.PARAMETER)
        @Retention(RetentionPolicy.RUNTIME)
        @Parameter(description = "Sort field", example = "createdAt")
        @interface Sort {
        }

        @Target(ElementType.PARAMETER)
        @Retention(RetentionPolicy.RUNTIME)
        @Parameter(description = "Keyword", example = "keyword")
        @interface Keyword {
        }
    }

    public @interface Responses {
        @Target(ElementType.METHOD)
        @Retention(RetentionPolicy.RUNTIME)
        @ApiResponses({
                @ApiResponse(responseCode = "200", description = "Success"),
                @ApiResponse(responseCode = "400", description = "Bad request"),
                @ApiResponse(responseCode = "401", description = "Unauthorized"),
                @ApiResponse(responseCode = "403", description = "Forbidden"),
                @ApiResponse(responseCode = "404", description = "Not found"),
                @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        @interface StandardResponse {
        }

        @Target(ElementType.METHOD)
        @Retention(RetentionPolicy.RUNTIME)
        @ApiResponses({
                @ApiResponse(responseCode = "200", description = "Success"),
                @ApiResponse(responseCode = "400", description = "Bad request"),
                @ApiResponse(responseCode = "401", description = "Unauthorized"),
                @ApiResponse(responseCode = "403", description = "Forbidden"),
                @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        @interface PageResponse {
        }
    }

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @Tag(name = "Service", description = "Service API")
    public @interface ServiceTag {
        String serviceName() default "service";
    }

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @Tag(name = "Module", description = "Module API")
    public @interface ModuleTag {
        String moduleName() default "module";
    }
}
