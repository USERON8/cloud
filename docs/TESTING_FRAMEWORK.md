# 测试框架配置指南

## 概述

本项目采用Spring Boot 3.5.3自带的测试框架，遵循Spring Boot测试最佳实践，避免重复引入测试依赖。

## 测试依赖体系

### 核心测试依赖（Spring Boot自带）

`spring-boot-starter-test` 已包含以下测试组件：

- **JUnit 5** - 现代测试框架
- **Mockito** - Mock框架
- **AssertJ** - 流畅断言库
- **Hamcrest** - 匹配器
- **Spring Test** - Spring测试集成
- **Spring Boot Test** - Spring Boot测试特性

### 增强测试依赖

仅配置Spring Boot未包含的增强组件：

```xml
<!-- TestContainers - 集成测试支持 -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>1.19.8</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>mysql</artifactId>
    <version>1.19.8</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>redis</artifactId>
    <version>1.19.8</version>
    <scope>test</scope>
</dependency>
```

## 测试配置

### 基础��试配置

每个服务模块包含：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

### 测试插件配置

**Maven Surefire Plugin** - 执行单元测试：
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>3.5.4</version>
    <configuration>
        <forkCount>1</forkCount>
        <reuseForks>true</reuseForks>
        <systemPropertyVariables>
            <spring.profiles.active>test</spring.profiles.active>
        </systemPropertyVariables>
    </configuration>
</plugin>
```

**JaCoCo Plugin** - 代码覆盖率：
```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.11</version>
    <configuration>
        <excludes>
            <exclude>**/dto/**</exclude>
            <exclude>**/vo/**</exclude>
            <exclude>**/entity/**</exclude>
            <exclude>**/config/**</exclude>
            <exclude>**/Application.*</exclude>
        </excludes>
        <rules>
            <rule>
                <element>BUNDLE</element>
                <limits>
                    <limit>
                        <counter>INSTRUCTION</counter>
                        <value>COVEREDRATIO</value>
                        <minimum>0.80</minimum>
                    </limit>
                </limits>
            </rule>
        </rules>
    </configuration>
</plugin>
```

## 测试结构

### 测试包结构

```
src/test/java/
└── com/cloud/{service}/
    ├── controller/     # 控制器测试
    ├── service/        # 服务层测试
    ├── mapper/         # 数据访问层测试
    └── config/         # 配置测试
```

### 基础测试类

**BaseTestConfig** - 通用测试配置：
```java
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
public abstract class BaseTestConfig {

    @BeforeEach
    void setUp() {
        setupMockSecurityContext();
    }

    @AfterEach
    void tearDown() {
        cleanupMocks();
    }

    protected void setupMockSecurityContext(String userId, String username, String role, List<String> authorities) {
        // Mock Security Context
    }

    protected void cleanupMocks() {
        // 清理Mock对象
    }
}
```

## 测试类型

### 1. 单元测试

**服务层测试示例**：
```java
@ExtendWith(MockitoExtension.class)
@DisplayName("用户���务单元测试")
class UserServiceTest extends BaseTestConfig {

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    @DisplayName("根据ID获取用户 - 成功")
    void testGetUserById_Success() {
        // Given - 设置测试数据
        when(userMapper.selectById(1L)).thenReturn(testUser);

        // When - 执行测试方法
        UserVO result = userService.getUserById(1L);

        // Then - 验证结果
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        verify(userMapper, times(1)).selectById(1L);
    }
}
```

### 2. 集成测试

**控制器集成测试**：
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1"
})
class UserControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @MockBean
    private UserService userService; // Mock服务层

    @Test
    void testGetUserById() {
        when(userService.getUserById(1L)).thenReturn(testUser);

        ResponseEntity<Result<UserVO>> response = restTemplate.getForEntity(
            "/api/users/1",
            new ParameterizedTypeReference<Result<UserVO>>() {}
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody().getData());
    }
}
```

### 3. TestContainers集成测试

**数据库集成测试**：
```java
@Testcontainers
@SpringBootTest
class UserRepositoryIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @Autowired
    private UserMapper userMapper;

    @Test
    void testDatabaseOperations() {
        // 测试真实数据库操作
    }
}
```

## Mock策略

### 1. 外部依赖Mock

```java
@MockBean
private RedisTemplate<String, Object> redisTemplate;

@MockBean
private RestTemplate restTemplate;
```

### 2. 数据层Mock

```java
@Mock
private UserMapper userMapper;

when(userMapper.selectById(anyLong()))
    .thenReturn(mockUser);
```

### 3. 安全上下文Mock

```java
@BeforeEach
void setupSecurityContext() {
    Authentication authentication = UsernamePasswordAuthenticationToken
        .authenticated("testuser", null,
            AuthorityUtils.createAuthorityList("ROLE_USER", "read", "write"));

    SecurityContextHolder.getContext().setAuthentication(authentication);
}
```

## 测试数据管理

### 1. 测试数据构建器

```java
public class UserTestDataBuilder {

    public static UserVO buildUserVO() {
        return UserVO.builder()
            .id(1L)
            .username("testuser")
            .email("test@example.com")
            .status(1)
            .createdAt(LocalDateTime.now())
            .build();
    }

    public static UserDTO buildUserDTO() {
        return UserDTO.builder()
            .username("newuser")
            .email("new@example.com")
            .build();
    }
}
```

### 2. 测试数据清理

```java
@AfterEach
void cleanupTestData() {
    // 清理测试数据
    userRepository.deleteAll();
    redisTemplate.getConnectionFactory().getConnection().flushAll();
}
```

## 测试命令

### 运行测试

```bash
# 运行所有测试
mvn test

# 运行特定模块测试
mvn test -pl user-service

# 运行特定测试类
mvn test -Dtest=UserServiceTest

# 运行特定测试方法
mvn test -Dtest=UserServiceTest#testGetUserById_Success

# 跳过测试编译
mvn compile -DskipTests

# 生成测试报告
mvn clean test jacoco:report
```

### 查看测试报告

- **测试报告**: `target/surefire-reports/index.html`
- **覆盖率报告**: `target/site/jacoco/index.html`

## 测试最佳实践

### 1. 命名规范

- **测试类**: `{ClassName}Test`
- **测试方法**: `test{MethodName}_{Scenario}`
- **显示名称**: 使用`@DisplayName`注解

### 2. 测试结构 (Given-When-Then)

```java
@Test
@DisplayName("创建用户 - 成功")
void testCreateUser_Success() {
    // Given - 准备测试数据
    UserDTO userDTO = UserTestDataBuilder.buildUserDTO();
    when(userMapper.insert(any())).thenReturn(1);

    // When - 执行测试方法
    UserVO result = userService.createUser(userDTO);

    // Then - 验证结果
    assertNotNull(result);
    assertEquals("newuser", result.getUsername());
    verify(userMapper, times(1)).insert(any());
}
```

### 3. 断言策略

- 使用AssertJ进行流畅断言
- 验证所有重要属性
- 包含边界条件测试

### 4. Mock验证

- 验证Mock调用次数
- 验证调用参数
- 使用`verifyNoMoreInteractions()`检查多余调用

## 代码覆盖率要求

### 覆盖率目标

- **指令覆盖率**: ≥ 80%
- **分支覆盖率**: ≥ 70%
- **行覆盖率**: ≥ 85%

### 排除范围

- DTO/VO类
- Entity类
- 配置类
- Application启动类
- 自动生成的代码

### 覆盖率检查

构建时会自动检查覆盖率，不达标则构建失败。

## 常见问题解决

### 1. 测试依赖冲突

**问题**: 重复引入JUnit或Mockito
**解决**: 移除显式依赖，使用Spring Boot Test自带版本

### 2. Mock失效

**问题**: @MockBean不生效
**解决**: 确保使用@SpringBootTest或@WebMvcTest

### 3. 数据库连接问题

**问题**: 测试数据库连接失败
**解决**: 使用TestContainers或H2内存数据库

### 4. 安全上下文问题

**问题**: 测试中权限验证失败
**解决**: 使用@WithMockUser或手动设置SecurityContext

## 总结

本项目测试框架配置遵循以下原则：

1. **依赖简化**: 优先使用Spring Boot自带测试依赖
2. **分层测试**: 单元测试 + 集成测试 + 端到端测试
3. **Mock策略**: 合理Mock外部依赖
4. **覆盖率**: 确保80%以上的代码覆盖率
5. **可维护性**: 清晰的测试结构和命名规范

这套配置为项目提供了完整的测试保障，确保代码质量和系统稳定性。