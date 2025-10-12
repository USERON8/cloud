# API文档标准规范

## 概述

本文档定义了Spring Cloud微服务项目API文档的标准规范，确保所有服务API文档的一致性、完整性和可维护性。

## 文档框架

### 技术栈

- **Swagger/OpenAPI 3.0** - API规范标准
- **Knife4j 4.5.0** - API文档增强工具
- **SpringDoc OpenAPI** - Spring Boot集成

### 集成方式

**Gateway聚合文档**:
- 访问地址: http://localhost:80/doc.html
- 所有服务API自动聚合到Gateway

**独立服务文档**:
- 访问地址: http://localhost:{PORT}/doc.html
- OpenAPI规范: http://localhost:{PORT}/v3/api-docs

## API文档结构

### 1. 基础注解规范

#### 类级别注解

```java
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "用户管理", description = "用户信息的增删改查操作")
@Validated
public class UserController {

}
```

**必需注解**:
- `@Tag` - API分组和描述
- `@RequestMapping` - 基础路径映射

#### 方法级别注解

```java
@GetMapping("/{id}")
@Operation(summary = "获取用户详情",
          description = "根据用户ID获取用户的详细信息，包括基本信息、状态等")
@Parameter(name = "id", description = "用户ID", required = true, example = "1")
public Result<UserVO> getUserById(
        @PathVariable @Min(1) Long id) {

}
```

**必需注解**:
- `@Operation` - 操作说明
- `@Parameter` - 参数说明（路径参数）

**可选注解**:
- `@ApiResponse` - 响应说明
- `@SecurityRequirement` - 安全要求

#### 请求/响应注解

```java
@PostMapping
@Operation(summary = "创建用户", description = "创建新用户账户")
@io.swagger.v3.oas.annotations.parameters.RequestBody(
    description = "用户创建信息",
    required = true,
    content = @Content(
        schema = @Schema(implementation = UserCreateDTO.class),
        examples = @ExampleObject(
            name = "创建用户示例",
            summary = "正常创建用户的请求示例",
            value = """
                {
                  "username": "newuser",
                  "email": "user@example.com",
                  "phone": "13800138000",
                  "nickname": "新用户"
                }
                """
        )
    )
)
public Result<Long> createUser(@Valid @RequestBody UserCreateDTO userDTO) {

}
```

### 2. 数据模型注解

#### DTO/VO类注解

```java
@Schema(description = "用户信息视图对象")
public class UserVO {

    @Schema(description = "用户ID", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Schema(description = "用户名", example = "testuser", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @Schema(description = "邮箱地址", example = "user@example.com", format = "email")
    private String email;

    @Schema(description = "用户状态", example = "1", allowableValues = {"0", "1", "2"})
    private Integer status;

    @Schema(description = "创建时间", example = "2024-01-01T10:00:00", accessMode = Schema.AccessMode.READ_ONLY)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
```

#### 枚举注解

```java
@Schema(description = "用户状态枚举")
public enum UserStatus {
    @Schema(description = "禁用")
    DISABLED(0, "禁用"),

    @Schema(description = "正常")
    ACTIVE(1, "正常"),

    @Schema(description = "锁定")
    LOCKED(2, "锁定");

    private final Integer code;
    private final String desc;
}
```

### 3. 响应结构规范

#### 统一响应格式

```java
@Schema(description = "统一响应结果")
public class Result<T> {

    @Schema(description = "响应码", example = "200")
    private Integer code;

    @Schema(description = "响应消息", example = "操作成功")
    private String message;

    @Schema(description = "响应数据")
    private T data;

    @Schema(description = "时间戳", example = "1704067200000")
    private Long timestamp;
}
```

#### 分页响应格式

```java
@Schema(description = "分页响应结果")
public class PageResult<T> {

    @Schema(description = "数据列表")
    private List<T> records;

    @Schema(description = "总记录数", example = "100")
    private Long total;

    @Schema(description = "当前页码", example = "1")
    private Long current;

    @Schema(description = "每页大小", example = "20")
    private Long size;

    @Schema(description = "总页数", example = "5")
    private Long pages;
}
```

### 4. 错误响应规范

#### 错误码定义

```java
@Schema(description = "业务错误码")
public enum ErrorCode {
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_ALREADY_EXISTS(1002, "用户已存在"),
    INVALID_PARAMETER(1003, "参数无效"),
    SYSTEM_ERROR(9999, "系统错误");

    private final Integer code;
    private final String message;
}
```

#### 错误响应示例

```java
@ApiResponse(
    responseCode = "404",
    description = "用户不存在",
    content = @Content(
        schema = @Schema(implementation = Result.class),
        examples = @ExampleObject(
            name = "用户不存在错误",
            summary = "查询不存在的用户时返回",
            value = """
                {
                  "code": 1001,
                  "message": "用户不存在",
                  "data": null,
                  "timestamp": 1704067200000
                }
                """
        )
    )
)
```

## API设计规范

### 1. RESTful设计原则

#### URL设计

```
GET    /api/v1/users           # 获取用户列表
GET    /api/v1/users/{id}      # 获取特定用户
POST   /api/v1/users           # 创建用户
PUT    /api/v1/users/{id}      # 完整更新用户
PATCH  /api/v1/users/{id}      # 部分更新用户
DELETE /api/v1/users/{id}      # 删除用户

GET    /api/v1/users/{id}/orders     # 获取用户的订单
GET    /api/v1/users/{id}/avatar    # 获取用户头像
```

#### HTTP状态码使用

```
200 OK                 # 请求成功
201 Created            # 资源创建成功
204 No Content         # 删除成功
400 Bad Request        # 请求参数错误
401 Unauthorized       # 未认证
403 Forbidden          # 无权限
404 Not Found          # 资源不存在
409 Conflict           # 资源冲突
422 Unprocessable Entity # 请求格式正确但语义错误
500 Internal Server Error # 服务器错误
```

### 2. 版本控制

#### URL版本控制

```
/api/v1/users  # 版本1
/api/v2/users  # 版本2
```

#### Header版本控制（可选）

```
Accept: application/vnd.api+json;version=1
```

### 3. 分页规范

#### 请求参数

```
GET /api/v1/users?page=1&size=20&sort=createdAt,desc
```

#### 响应格式

```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "records": [...],
    "total": 100,
    "current": 1,
    "size": 20,
    "pages": 5
  },
  "timestamp": 1704067200000
}
```

### 4. 过滤和搜索

#### 查询参数

```
GET /api/v1/users?status=1&keyword=test&startDate=2024-01-01&endDate=2024-12-31
```

#### 复杂搜索

```
POST /api/v1/users/search
Content-Type: application/json

{
  "filters": {
    "status": [1, 2],
    "ageRange": { "min": 18, "max": 65 },
    "keywords": ["active", "premium"]
  },
  "sort": [
    { "field": "createdAt", "direction": "desc" },
    { "field": "username", "direction": "asc" }
  ],
  "page": 1,
  "size": 20
}
```

## 安全规范

### 1. 认证授权

#### OAuth2.1安全配置

```java
@Operation(summary = "获取用户信息",
          security = @SecurityRequirement(name = "OAuth2"))
@PreAuthorize("hasAuthority('SCOPE_user:read')")
public Result<UserVO> getUserInfo() {

}
```

#### 安全定义

```java
@Configuration
@OpenAPIDefinition(
    info = @Info(title = "Cloud API", version = "v1.0"),
    security = {
        @SecurityRequirement(name = "OAuth2")
    },
    components = @Components(
        securitySchemes = @SecurityScheme(
            name = "OAuth2",
            type = SecurityScheme.Type.OAUTH2,
            flows = @OAuthFlows(
                password = @OAuthFlow(
                    tokenUrl = "/oauth2/token",
                    scopes = {
                        @OAuthScope(name = "user:read", description = "读取用户信息"),
                        @OAuthScope(name = "user:write", description = "写入用户信息")
                    }
                )
            )
        )
    )
)
public class OpenApiConfig {

}
```

### 2. 参数验证

#### Bean Validation注解

```java
public class UserCreateDTO {

    @Schema(description = "用户名", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度必须在3-20字符之间")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
    private String username;

    @Schema(description = "邮箱地址", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @Schema(description = "手机号码")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
}
```

## 文档生成配置

### 1. 全局配置

```yaml
# application.yml
springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /doc.html
    operationsSorter: method
    tagsSorter: alpha
  group-configs:
    - group: public
      paths-to-match: /api/public/**
    - group: admin
      paths-to-match: /api/admin/**
```

### 2. 自定义配置

```java
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Cloud API Documentation")
                        .version("1.0.0")
                        .description("Spring Cloud微服务API文档")
                        .contact(new Contact()
                                .name("Cloud Team")
                                .email("cloud@example.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .externalDocs(new ExternalDocumentation()
                        .description("项目文档")
                        .url("https://github.com/cloud/docs"))
                .addSecurityItem(new SecurityRequirement().addList("OAuth2"));
    }
}
```

### 3. Gateway聚合配置

```yaml
# gateway application.yml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: lb://user-service
          predicates:
            - Path=/api/v1/users/**
          filters:
            - StripPrefix=2

springdoc:
  swagger-ui:
    urls:
      - name: user-service
        url: /user-service/v3/api-docs
      - name: product-service
        url: /product-service/v3/api-docs
      - name: order-service
        url: /order-service/v3/api-docs
```

## 文档维护规范

### 1. 文档更新流程

1. **API变更时同步更新文档**
2. **代码Review时检查文档完整性**
3. **定期检查文档准确性**
4. **版本发布时更新文档版本**

### 2. 文档质量检查

#### 检查清单

- [ ] 所有API都有@Operation注解
- [ ] 所有参数都有@Parameter注解
- [ ] 所有DTO/VO都有@Schema注解
- [ ] 错误响应有完整说明
- [ ] 请求/响应示例完整
- [ ] 安全要求明确标注
- [ ] 版本信息正确

### 3. 文档测试

#### 文档可访问性测试

```bash
# 检查API规范生成
curl http://localhost:8081/v3/api-docs

# 检查Swagger UI
curl http://localhost:8081/doc.html

# 检查Gateway聚合文档
curl http://localhost:80/doc.html
```

## 示例文档

### 完整的API接口示例

```java
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "用户管理", description = "用户信息的增删改查操作")
@Validated
public class UserController {

    @GetMapping("/{id}")
    @Operation(
        summary = "获取用户详情",
        description = "根据用户ID获取用户的详细信息，包括基本信息、状态、创建时间等"
    )
    @Parameter(name = "id", description = "用户ID", required = true, example = "1")
    @ApiResponse(
        responseCode = "200",
        description = "获取成功",
        content = @Content(
            schema = @Schema(implementation = Result.class),
            examples = @ExampleObject(
                name = "成功响应",
                value = """
                    {
                      "code": 200,
                      "message": "获取成功",
                      "data": {
                        "id": 1,
                        "username": "testuser",
                        "email": "test@example.com",
                        "status": 1,
                        "createdAt": "2024-01-01T10:00:00"
                      },
                      "timestamp": 1704067200000
                    }
                    """
            )
        )
    )
    @ApiResponse(
        responseCode = "404",
        description = "用户不存在",
        content = @Content(
            examples = @ExampleObject(
                value = """
                    {
                      "code": 1001,
                      "message": "用户不存在",
                      "data": null,
                      "timestamp": 1704067200000
                    }
                    """
            )
        )
    )
    @PreAuthorize("hasAuthority('SCOPE_user:read')")
    public Result<UserVO> getUserById(@PathVariable @Min(1) Long id) {
        // 实现逻辑
    }
}
```

## 总结

本文档定义了API文档的完整规范，包括：

1. **注解规范** - 标准化的文档注解使用
2. **设计规范** - RESTful API设计原则
3. **安全规范** - OAuth2.1安全集成
4. **维护规范** - 文档质量和更新流程

遵循这些规范可以确保API文档的完整性、准确性和可维护性，为开发团队和API使用者提供良好的文档体验。