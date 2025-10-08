# 开发规范文档

本文档定义了项目开发过程中必须遵循的各项规范和最佳实践。

## 📋 目录

1. [代码规范](#1-代码规范)
2. [Git 提交规范](#2-git-提交规范)
3. [接口设计规范](#3-接口设计规范)
4. [数据库设计规范](#4-数据库设计规范)
5. [异常处理规范](#5-异常处理规范)
6. [日志规范](#6-日志规范)
7. [测试规范](#7-测试规范)
8. [安全规范](#8-安全规范)
9. [Docker 容器化与部署规范](#9-docker-容器化与部署规范)
10. [代码审查清单](#10-代码审查清单)
11. [附录](#11-附录)

---

## 1. 代码规范

### 1.1 命名规范

#### 包命名

- 全部小写
- 使用域名倒序
- 示例: `com.cloud.user.service`

#### 类命名

- 使用大驼峰命名法 (PascalCase)
- 类名应该是名词
- 示例: `UserService`, `OrderController`

#### 方法命名

- 使用小驼峰命名法 (camelCase)
- 方法名应该是动词或动词短语
- 示例: `getUserById()`, `createOrder()`

#### 变量命名

- 使用小驼峰命名法
- 变量名应该具有描述性
- 避免单字母变量名（除了循环变量）
- 示例: `userId`, `orderList`

#### 常量命名

- 全部大写，单词间用下划线分隔
- 示例: `MAX_RETRY_COUNT`, `DEFAULT_PAGE_SIZE`

### 1.2 代码结构

#### Controller 层

```java
@RestController
@RequestMapping("/api/user")
@Tag(name = "用户管理", description = "用户相关接口")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    
    @GetMapping("/{id}")
    @Operation(summary = "获取用户信息")
    public Result<UserVO> getUserById(@PathVariable Long id) {
        return Result.success(userService.getUserById(id));
    }
}
```

#### Service 层

```java
@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    
    private final UserMapper userMapper;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO createUser(UserDTO userDTO) {
        // 业务逻辑
        return userVO;
    }
}
```

#### Mapper 层

```java
@Mapper
public interface UserMapper extends BaseMapper<User> {
    
    /**
     * 根据用户名查询用户
     * @param username 用户名
     * @return 用户信息
     */
    User selectByUsername(@Param("username") String username);
}
```

### 1.3 注释规范

#### 类注释

```java
/**
 * 用户服务实现类
 * 提供用户相关的业务逻辑处理
 *
 * @author cloud
 * @date 2025-01-20
 * @since 1.0.0
 */
public class UserServiceImpl implements UserService {
}
```

#### 方法注释

```java
/**
 * 创建用户
 *
 * @param userDTO 用户信息
 * @return 创建成功的用户信息
 * @throws BusinessException 业务异常
 */
public UserVO createUser(UserDTO userDTO) {
}
```

#### 复杂逻辑注释

```java
// 1. 验证用户名是否重复
// 2. 加密用户密码
// 3. 保存用户信息
// 4. 发送欢迎邮件
```

### 1.4 代码格式

- **缩进**: 使用 4 个空格，不使用 Tab
- **行宽**: 不超过 120 字符
- **空行**: 合理使用空行分隔代码块
- **导入**: 按字母顺序排列，移除未使用的导入

### 1.5 最佳实践

#### 使用 Lombok 简化代码

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private Long id;
    private String username;
    private String email;
}
```

#### 使用 Optional 处理空值

```java
public Optional<User> findUserById(Long id) {
    return Optional.ofNullable(userMapper.selectById(id));
}
```

#### 使用 Stream API 处理集合

```java
List<Long> userIds = users.stream()
    .map(User::getId)
    .collect(Collectors.toList());
```

---

## 2. Git 提交规范

### 2.1 分支命名

- `master/main`: 主分支，始终保持可部署状态
- `develop`: 开发分支
- `feature/功能名`: 功能分支
- `bugfix/问题描述`: 修复分支
- `hotfix/紧急修复`: 紧急修复分支
- `release/版本号`: 发布分支

### 2.2 提交信息格式

```
<type>(<scope>): <subject>

<body>

<footer>
```

#### Type 类型

- `feat`: 新功能
- `fix`: Bug 修复
- `docs`: 文档更新
- `style`: 代码格式调整
- `refactor`: 重构
- `perf`: 性能优化
- `test`: 测试相关
- `chore`: 构建/工具变动

#### 示例

```
feat(user): 添加用户注册功能

- 实现用户注册接口
- 添加邮箱验证
- 添加用户名重复检查

Closes #123
```

### 2.3 提交频率

- 每完成一个小功能点就提交
- 提交前确保代码可以编译通过
- 提交前运行单元测试

---

## 3. 接口设计规范

### 3.1 RESTful API 设计

#### URL 设计

```
GET    /api/users          # 获取用户列表
GET    /api/users/{id}     # 获取单个用户
POST   /api/users          # 创建用户
PUT    /api/users/{id}     # 更新用户
DELETE /api/users/{id}     # 删除用户
```

#### 版本控制

```
/api/v1/users
/api/v2/users
```

### 3.2 请求参数

#### 路径参数

```java
@GetMapping("/users/{id}")
public Result<UserVO> getUser(@PathVariable Long id) {
}
```

#### 查询参数

```java
@GetMapping("/users")
public Result<PageResult<UserVO>> listUsers(
    @RequestParam(defaultValue = "1") Integer page,
    @RequestParam(defaultValue = "10") Integer size) {
}
```

#### 请求体

```java
@PostMapping("/users")
public Result<UserVO> createUser(@RequestBody @Valid UserDTO userDTO) {
}
```

### 3.3 响应格式

#### 统一响应结构

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "username": "admin"
  },
  "timestamp": 1674201600000
}
```

#### 分页响应

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [...],
    "total": 100,
    "page": 1,
    "size": 10
  }
}
```

#### 错误响应

```json
{
  "code": 400,
  "message": "用户名已存在",
  "timestamp": 1674201600000
}
```

### 3.4 HTTP 状态码

- `200 OK`: 请求成功
- `201 Created`: 创建成功
- `204 No Content`: 删除成功
- `400 Bad Request`: 请求参数错误
- `401 Unauthorized`: 未认证
- `403 Forbidden`: 无权限
- `404 Not Found`: 资源不存在
- `500 Internal Server Error`: 服务器错误

---

## 4. 数据库设计规范

### 4.1 表命名

- 使用小写字母和下划线
- 表名使用复数形式或业务名称
- 示例: `users`, `orders`, `order_items`

### 4.2 字段命名

- 使用小写字母和下划线
- 字段名应具有描述性
- 示例: `user_id`, `created_at`, `order_status`

### 4.3 字段类型

| 数据类型     | 使用场景     |
|----------|----------|
| BIGINT   | ID、金额（分） |
| VARCHAR  | 字符串      |
| TEXT     | 长文本      |
| DATETIME | 时间戳      |
| DECIMAL  | 精确小数     |
| TINYINT  | 状态、标志位   |

### 4.4 必备字段

每个表必须包含以下字段：

```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标志 0-未删除 1-已删除',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
```

### 4.5 索引规范

- 主键使用 `id`
- 唯一索引命名: `uk_字段名`
- 普通索引命名: `idx_字段名`
- 组合索引命名: `idx_字段1_字段2`

```sql
CREATE UNIQUE INDEX uk_username ON users(username);
CREATE INDEX idx_created_at ON users(created_at);
CREATE INDEX idx_user_id_status ON orders(user_id, status);
```

### 4.6 SQL 编写规范

#### 查询优化

```sql
-- 好的示例
SELECT id, username, email FROM users WHERE status = 1 LIMIT 100;

-- 避免 SELECT *
-- 避免 SELECT id, username, email FROM users WHERE status = 1;

-- 使用 LIMIT 限制返回数量
```

#### 事务处理

```java
@Transactional(rollbackFor = Exception.class)
public void createOrder(OrderDTO orderDTO) {
    // 事务操作
}
```

---

## 5. 异常处理规范

### 5.1 异常分类

#### 业务异常

```java
public class BusinessException extends RuntimeException {
    private final Integer code;
    private final String message;
}
```

#### 系统异常

```java
public class SystemException extends RuntimeException {
    private final Integer code;
    private final String message;
}
```

### 5.2 异常处理

#### 全局异常处理器

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }
    
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.fail(500, "系统异常");
    }
}
```

### 5.3 异常抛出

```java
// 参数校验失败
if (StringUtils.isBlank(username)) {
    throw new BusinessException(ErrorCode.INVALID_PARAM, "用户名不能为空");
}

// 资源不存在
User user = userMapper.selectById(id);
if (user == null) {
    throw new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在");
}
```

---

## 6. 日志规范

### 6.1 日志级别

- `ERROR`: 错误信息，需要立即处理
- `WARN`: 警告信息，可能存在问题
- `INFO`: 重要的业务流程信息
- `DEBUG`: 调试信息
- `TRACE`: 详细的调试信息

### 6.2 日志格式

```java
// 业务流程日志
log.info("用户注册成功, userId: {}, username: {}", userId, username);

// 异常日志
log.error("用户注册失败, username: {}", username, e);

// 性能日志
long startTime = System.currentTimeMillis();
// ... 业务逻辑
log.info("查询用户列表耗时: {}ms", System.currentTimeMillis() - startTime);
```

### 6.3 日志注意事项

- 不要记录敏感信息（密码、Token等）
- 使用占位符而不是字符串拼接
- 异常日志必须包含堆栈信息
- 生产环境避免使用 DEBUG 级别

---

## 7. 测试规范

### 7.1 单元测试

```java
@SpringBootTest
class UserServiceTest {
    
    @Autowired
    private UserService userService;
    
    @Test
    @DisplayName("创建用户-成功")
    void testCreateUserSuccess() {
        // Given
        UserDTO userDTO = UserDTO.builder()
            .username("test")
            .email("test@example.com")
            .build();
        
        // When
        UserVO userVO = userService.createUser(userDTO);
        
        // Then
        assertNotNull(userVO);
        assertEquals("test", userVO.getUsername());
    }
    
    @Test
    @DisplayName("创建用户-用户名重复")
    void testCreateUserDuplicateUsername() {
        // Given
        UserDTO userDTO = UserDTO.builder()
            .username("admin")
            .build();
        
        // When & Then
        assertThrows(BusinessException.class, () -> {
            userService.createUser(userDTO);
        });
    }
}
```

### 7.2 测试覆盖率

- 单元测试覆盖率要求: 70% 以上
- 核心业务逻辑覆盖率: 90% 以上

### 7.3 集成测试

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class UserControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void testGetUser() throws Exception {
        mockMvc.perform(get("/api/users/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.username").value("admin"));
    }
}
```

---

## 8. 安全规范

### 8.1 认证授权

- 所有对外接口必须进行认证
- 敏感操作需要权限校验
- 使用 OAuth2.1 + JWT 进行认证

### 8.2 数据加密

- 密码使用 BCrypt 加密
- 敏感数据传输使用 HTTPS
- 数据库敏感字段加密存储

### 8.3 SQL 注入防护

```java
// 使用参数化查询
@Select("SELECT * FROM users WHERE username = #{username}")
User selectByUsername(@Param("username") String username);

// 避免字符串拼接
// 错误示例: SELECT * FROM users WHERE username = '" + username + "'"
```

### 8.4 XSS 防护

- 对用户输入进行 HTML 转义
- 使用内容安全策略 (CSP)
- 设置正确的 Content-Type

### 8.5 CSRF 防护

- 使用 CSRF Token
- 验证 Referer 头
- SameSite Cookie 属性

---

## 9. Docker 容器化与部署规范

### 9.1 基础设施架构

项目采用 Docker Compose 进行本地开发环境和测试环境的容器化部署，包含以下核心组件：

#### 核心服务 (docker-compose.yml)

| 服务                  | 版本             | 端口                | IP地址         | 说明        |
|---------------------|----------------|-------------------|--------------|-----------|
| MySQL               | 9.3.0          | 3306              | 172.28.0.10  | 关系型数据库    |
| Redis               | 7.4.5-bookworm | 6379              | 172.28.0.20  | 缓存数据库     |
| Nacos               | v3.0.2         | 8848/9090/9848    | 172.28.0.30  | 配置中心/服务注册 |
| RocketMQ NameServer | 5.3.2          | 39876             | 172.28.0.40  | 消息队列命名服务  |
| RocketMQ Broker     | 5.3.2          | 30909/30911/30912 | 172.28.0.50  | 消息队列代理    |
| RocketMQ Proxy      | 5.3.2          | 38080/38081       | 172.28.0.60  | 消息队列代理    |
| RocketMQ Console    | 2.1.0          | 38082             | 172.28.0.65  | 消息队列管控台   |
| Nginx               | stable-perl    | 80/443            | 172.28.0.70  | 反向代理/负载均衡 |
| MinIO               | 2025-07-23     | 9000/9001         | 172.28.0.80  | 对象存储      |
| Elasticsearch       | 9.1.2          | 9200/9300         | 172.28.0.90  | 搜索引擎      |
| Kibana              | 9.1.2          | 5601              | 172.28.0.100 | ES 可视化工具  |

#### 监控服务 (monitoring-compose.yml)

| 服务            | 版本     | 端口        | 说明      |
|---------------|--------|-----------|---------|
| Prometheus    | 3.5.0  | 9099      | 指标采集与监控 |
| Grafana       | 12.2.0 | 3000      | 监控数据可视化 |
| Elasticsearch | 9.1.2  | 9201/9301 | 日志存储    |
| Logstash      | 9.1.2  | 5044/9600 | 日志收集处理  |
| Kibana        | 9.1.2  | 5601      | 日志可视化   |

### 9.2 环境启动

#### 启动核心服务

```bash
# 进入 docker 目录
cd docker

# 启动所有核心服务
docker-compose up -d

# 启动指定服务
docker-compose up -d mysql redis nacos

# 查看服务状态
docker-compose ps

# 查看服务日志
docker-compose logs -f [service-name]
```

#### 启动监控服务

```bash
# 启动监控栈
docker-compose -f monitoring-compose.yml up -d

# 查看监控服务状态
docker-compose -f monitoring-compose.yml ps
```

#### 停止服务

```bash
# 停止所有服务
docker-compose down

# 停止并删除数据卷（谨慎使用）
docker-compose down -v
```

### 9.3 服务配置说明

#### MySQL 配置

```yaml
数据库: nacos_config
管理员账号: root / root
应用账号: nacos / nacos
数据目录: D:\docker\mysql\data
配置目录: D:\docker\mysql\conf
日志目录: D:\docker\mysql\logs
初始化脚本: D:\docker\mysql\init
```

#### Redis 配置

```yaml
配置文件: D:\docker\redis\conf\redis.conf
持久化: AOF 模式 (appendonly yes)
数据目录: D:\docker\redis\data
```

#### Nacos 配置

```yaml
访问地址: http://localhost:8848/nacos
管理员账号: root / root
运行模式: standalone (单机)
数据库: MySQL (nacos_config)
鉴权: 已启用
Token密钥: VGhpc0lzTXlDdXN0b21TZWNyZXRLZXkwMTIzNDU2Nzg=
JVM 内存: -Xms512m -Xmx512m
```

#### RocketMQ 配置

```yaml
NameServer: 172.28.0.40:9876
Broker 配置: /home/rocketmq/broker/conf/broker.conf
控制台地址: http://localhost:38082
VIP通道: 已禁用
```

#### MinIO 配置

```yaml
API端口: 9000
控制台端口: 9001
访问地址: http://localhost:9001
账号密码: minioadmin / minioadmin
数据目录: D:\docker\minio\data
```

#### Elasticsearch 配置

```yaml
HTTP端口: 9200
集群通信端口: 9300
JVM内存: -Xms1g -Xmx1g
安全认证: 已禁用
运行模式: single-node
```

#### Prometheus & Grafana

```yaml
Prometheus地址: http://localhost:9099
Grafana地址: http://localhost:3000
Grafana账号: admin / admin
配置文件: docker/prometheus/prometheus.yml
数据保留: 200小时
```

### 9.4 网络配置

#### 核心服务网络

```yaml
网络名称: service_net
网络驱动: bridge
子网: 172.28.0.0/24
网关: 172.28.0.1
IP范围: 172.28.0.10 - 172.28.0.100
```

#### 监控服务网络

```yaml
网络名称: monitoring
网络驱动: bridge
```

### 9.5 数据持久化

所有服务数据统一存储在 `D:\docker` 目录下，按服务名称分类：

```
D:\docker\
├── mysql\          # MySQL 数据和配置
│   ├── data\       # 数据文件
│   ├── conf\       # 配置文件
│   ├── logs\       # 日志文件
│   └── init\       # 初始化脚本
├── redis\          # Redis 数据和配置
├── nacos\          # Nacos 数据和日志
├── rocketmq\       # RocketMQ 数据
│   ├── namesrv\    # NameServer
│   ├── broker\     # Broker
│   └── proxy\      # Proxy
├── nginx\          # Nginx 配置
├── minio\          # MinIO 对象存储
├── es\             # Elasticsearch
├── kibana\         # Kibana
├── prometheus\     # Prometheus 数据
└── grafana\        # Grafana 数据
```

### 9.6 健康检查

以下服务配置了健康检查机制：

```yaml
MySQL: mysqladmin ping (5秒间隔)
Redis: redis-cli ping (5秒间隔)
Nacos: curl http://localhost:9090/nacos/ (10秒间隔)
Elasticsearch: curl http://localhost:9200 (10秒间隔)
Kibana: curl http://localhost:5601/api/status (30秒间隔)
```

### 9.7 服务依赖关系

```
Nacos → MySQL (必须先启动 MySQL)
Broker → NameServer
Proxy → Broker
RocketMQ Console → NameServer
Nginx → Nacos + Proxy
Kibana → Elasticsearch
Grafana → Prometheus
```

### 9.8 常见问题处理

#### 问题1: Nacos 启动失败

```bash
# 检查 MySQL 是否正常运行
docker-compose ps mysql

# 查看 Nacos 日志
docker-compose logs nacos

# 确认数据库连接
docker exec -it mysql_db mysql -unacos -pnacos -e "show databases;"
```

#### 问题2: RocketMQ Broker 连接失败

```bash
# 检查 NameServer 状态
docker-compose ps namesrv

# 查看 Broker 配置
docker exec -it rmqbroker cat /home/rocketmq/broker/conf/broker.conf

# 验证网络连通性
docker exec -it rmqbroker ping 172.28.0.40
```

#### 问题3: Elasticsearch 内存不足

```bash
# Windows 需要增加 WSL2 内存限制
# 编辑 %USERPROFILE%\.wslconfig
[wsl2]
memory=8GB

# 重启 WSL
wsl --shutdown
```

### 9.9 部署清单

#### 开发环境部署检查

- [ ] Docker 和 Docker Compose 已安装
- [ ] 端口无冲突 (3306, 6379, 8848, 9000 等)
- [ ] 数据目录已创建 (`D:\docker`)
- [ ] 配置文件已准备 (broker.conf, redis.conf 等)
- [ ] 启动核心服务并验证健康状态
- [ ] 访问各服务管理界面确认正常

#### 生产环境部署注意事项

- [ ] 修改默认密码和密钥
- [ ] 启用 HTTPS 和 SSL 证书
- [ ] 配置防火墙规则
- [ ] 调整 JVM 参数和资源限制
- [ ] 配置数据备份策略
- [ ] 设置日志轮转和清理
- [ ] 启用服务监控和告警
- [ ] 准备容灾和高可用方案

---

## 10. 代码审查清单

### 10.1 功能性

- [ ] 代码实现了需求文档中的所有功能
- [ ] 边界条件处理正确
- [ ] 错误处理完善

### 10.2 可读性

- [ ] 命名规范清晰
- [ ] 注释充分
- [ ] 代码结构清晰

### 10.3 性能

- [ ] 没有明显的性能问题
- [ ] 数据库查询优化
- [ ] 合理使用缓存

### 10.4 安全性

- [ ] 输入验证完整
- [ ] 权限校验正确
- [ ] 敏感信息保护

### 10.5 测试

- [ ] 单元测试覆盖率达标
- [ ] 测试用例充分
- [ ] 测试通过

---

## 11. 附录

### 11.1 推荐工具

#### 开发工具

- **IDE**: IntelliJ IDEA Ultimate
- **代码质量**: SonarQube
- **代码格式化**: Checkstyle / Alibaba Java Coding Guidelines
- **API 测试**: Postman / Apifox
- **数据库工具**: Navicat / DBeaver / DataGrip

#### 运维工具

- **容器管理**: Docker Desktop / Portainer
- **服务监控**: Prometheus + Grafana
- **日志分析**: ELK Stack (Elasticsearch + Logstash + Kibana)
- **API 网关**: Nginx / Kong
- **负载测试**: JMeter / Gatling

#### DevOps 工具

- **版本控制**: Git / GitLab
- **CI/CD**: Jenkins / GitLab CI / GitHub Actions
- **镜像仓库**: Docker Hub / Harbor
- **配置管理**: Nacos / Apollo
- **服务网格**: Istio (可选)

### 11.2 推荐插件

#### IntelliJ IDEA 插件

- **Lombok**: 简化 Java 代码
- **MyBatisX**: MyBatis 增强
- **Alibaba Java Coding Guidelines**: 阿里巴巴代码规范检查
- **SonarLint**: 实时代码质量检查
- **GitToolBox**: Git 增强工具
- **RestfulTool**: RESTful 接口导航
- **Rainbow Brackets**: 彩虹括号
- **Translation**: 翻译插件
- **Docker**: Docker 集成
- **Database Navigator**: 数据库工具

#### VSCode 插件（可选）

- **Java Extension Pack**
- **Spring Boot Extension Pack**
- **Docker**
- **YAML**
- **GitLens**

### 11.3 技术栈版本说明

#### 后端技术栈

```yaml
Java: 17 LTS
Spring Boot: 3.x
Spring Cloud: 2023.x
Spring Cloud Alibaba: 2023.x
MyBatis Plus: 3.5.x
Lombok: 1.18.x
```

#### 中间件版本

```yaml
MySQL: 9.3.0
Redis: 7.4.5
Nacos: 3.0.2
RocketMQ: 5.3.2
Elasticsearch: 9.1.2
Nginx: stable-perl
MinIO: 2025-07-23
```

#### 监控工具版本

```yaml
Prometheus: 3.5.0
Grafana: 12.2.0
Kibana: 9.1.2
Logstash: 9.1.2
```

### 11.4 开发环境要求

#### 硬件要求

```
CPU: 4核及以上
内存: 16GB 及以上（推荐 32GB）
硬盘: SSD 256GB 及以上
```

#### 软件要求

```
操作系统: Windows 10/11, macOS, Linux
JDK: OpenJDK 17 或 Oracle JDK 17
Maven: 3.9.x
Docker: 20.10.x 及以上
Docker Compose: 2.x
Git: 2.x
```

### 11.5 项目目录结构

```
cloud/
├── common-module/           # 公共模块
│   ├── common-core/         # 核心组件
│   ├── common-auth/         # 认证授权
│   ├── common-message/      # 消息组件
│   └── common-redis/        # Redis 组件
├── gateway-service/         # 网关服务
├── auth-service/            # 认证服务
├── user-service/            # 用户服务
├── order-service/           # 订单服务
├── docker/                  # Docker 配置
│   ├── docker-compose.yml   # 核心服务编排
│   ├── monitoring-compose.yml # 监控服务编排
│   ├── prometheus/          # Prometheus 配置
│   └── logstash/            # Logstash 配置
├── docs/                    # 文档目录
│   ├── api/                 # API 文档
│   ├── design/              # 设计文档
│   └── deployment/          # 部署文档
├── scripts/                 # 脚本文件
├── pom.xml                  # 父POM
├── README.md                # 项目说明
└── RULE.md                  # 开发规范（本文档）
```

### 11.6 参考资料

#### 官方文档

- [Spring Boot 官方文档](https://spring.io/projects/spring-boot)
- [Spring Cloud 官方文档](https://spring.io/projects/spring-cloud)
- [Spring Cloud Alibaba 文档](https://spring-cloud-alibaba-group.github.io/github-pages/2023/zh-cn/)
- [MyBatis Plus 文档](https://baomidou.com/)
- [Nacos 文档](https://nacos.io/zh-cn/docs/)
- [RocketMQ 文档](https://rocketmq.apache.org/zh/docs/)

#### 编码规范

- [阿里巴巴 Java 开发手册](https://github.com/alibaba/p3c)
- [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- [Effective Java](https://www.oracle.com/java/technologies/effective-java.html)

#### 架构设计

- [微服务架构设计模式](https://microservices.io/)
- [领域驱动设计 (DDD)](https://domainlanguage.com/ddd/)
- [12-Factor App](https://12factor.net/zh_cn/)

#### Docker 与 Kubernetes

- [Docker 官方文档](https://docs.docker.com/)
- [Docker Compose 文档](https://docs.docker.com/compose/)
- [Kubernetes 文档](https://kubernetes.io/zh-cn/docs/)

---

## 📞 联系方式

- **技术支持**: tech-support@example.com
- **问题反馈**: [GitHub Issues](https://github.com/your-org/cloud/issues)
- **团队Wiki**: [内部Wiki链接]

---

**文档版本**: 2.0.0  
**最后更新**: 2025-01-20  
**维护团队**: 云架构团队  
**审核人**: 技术架构师

