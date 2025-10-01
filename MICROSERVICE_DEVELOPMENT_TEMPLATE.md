# 🏗️ 微服务标准化开发模板

## 📋 概述

本文档基于User服务的成熟架构模式，提供微服务开发的标准模板和最佳实践指南，确保所有微服务遵循统一的架构标准。

## 🎯 设计原则

### 1. 单一职责原则
- 每个服务只负责一个业务域
- 控制器按功能职责明确划分
- 避免业务逻辑泄露到控制器层

### 2. 统一架构标准
- 相同的分层架构模式
- 统一的API设计规范
- 标准化的错误处理机制

### 3. 可扩展性设计
- 支持水平扩展
- 缓存策略优化
- 异步处理能力

## 📁 标准项目结构

```
{service-name}/
├── src/main/java/com/cloud/{service}/
│   ├── {ServiceName}Application.java        # 启动类
│   ├── config/                              # 配置类
│   │   ├── CacheConfig.java                # 缓存配置
│   │   ├── SecurityConfig.java             # 安全配置
│   │   └── WebConfig.java                  # Web配置
│   ├── controller/                         # 控制器层
│   │   ├── {Entity}Controller.java         # RESTful API控制器
│   │   └── {Entity}FeignController.java    # 内部服务调用
│   ├── converter/                          # 对象转换器
│   │   └── {Entity}Converter.java          # MapStruct转换器
│   ├── service/                            # 业务逻辑层
│   │   ├── {Entity}Service.java            # 服务接口
│   │   └── impl/
│   │       └── {Entity}ServiceImpl.java    # 服务实现
│   ├── mapper/                             # 数据访问层
│   │   └── {Entity}Mapper.java             # MyBatis-Plus Mapper
│   ├── module/entity/                      # 实体类
│   │   └── {Entity}.java                   # 数据库实体
│   ├── exception/                          # 异常处理
│   │   └── {Service}Exception.java         # 自定义异常
│   └── event/                              # 事件处理
│       ├── publisher/                      # 事件发布
│       └── listener/                       # 事件监听
├── src/main/resources/
│   ├── mapper/                             # MyBatis XML映射
│   ├── application.yml                     # 主配置文件
│   └── application-{env}.yml               # 环境配置
└── pom.xml                                 # Maven配置
```

## 🎮 控制器层标准模板

### 1. RESTful API控制器

```java
package com.cloud.{service}.controller;

import com.cloud.common.result.Result;
import com.cloud.common.result.PageResult;
import com.cloud.{service}.service.{Entity}Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * {Entity}RESTful API控制器
 * 提供{entity}资源的CRUD操作，参考User服务标准架构
 *
 * @author {author}
 */
@Slf4j
@RestController
@RequestMapping("/{entities}")
@RequiredArgsConstructor
@Tag(name = "{Entity}服务", description = "{Entity}资源的RESTful API接口")
public class {Entity}Controller {

    private final {Entity}Service {entity}Service;

    /**
     * 获取{entity}列表（支持查询参数）
     */
    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_{entity}:read')")
    @Operation(summary = "获取{entity}列表", description = "获取{entity}列表，支持分页和查询参数")
    public Result<PageResult<{Entity}VO>> get{Entities}(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") Integer size,
            // 添加其他查询参数
            ) {
        
        // 实现逻辑
        return Result.success(pageResult);
    }

    /**
     * 根据ID获取{entity}详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_{entity}:read')")
    @Operation(summary = "获取{entity}详情", description = "根据{entity}ID获取{entity}详细信息")
    public Result<{Entity}DTO> get{Entity}ById(
            @Parameter(description = "{Entity}ID") @PathVariable
            @Positive(message = "{Entity}ID必须为正整数") Long id) {

        // 实现逻辑
        return Result.success("查询成功", {entity});
    }

    /**
     * 创建{entity}
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_{entity}:create')")
    @Operation(summary = "创建{entity}", description = "创建新{entity}")
    public Result<Long> create{Entity}(
            @Parameter(description = "{Entity}信息") @RequestBody
            @Valid @NotNull(message = "{Entity}信息不能为空") {Entity}RequestDTO requestDTO) {

        try {
            Long {entity}Id = {entity}Service.create{Entity}(requestDTO);
            return Result.success("{Entity}创建成功", {entity}Id);
        } catch (Exception e) {
            log.error("创建{entity}失败", e);
            return Result.error("创建{entity}失败: " + e.getMessage());
        }
    }

    /**
     * 更新{entity}信息
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_{entity}:write')")
    @Operation(summary = "更新{entity}信息", description = "更新{entity}信息")
    public Result<Boolean> update{Entity}(
            @Parameter(description = "{Entity}ID") @PathVariable Long id,
            @Parameter(description = "{Entity}信息") @RequestBody
            @Valid @NotNull(message = "{Entity}信息不能为空") {Entity}RequestDTO requestDTO,
            Authentication authentication) {

        try {
            boolean result = {entity}Service.update{Entity}(id, requestDTO);
            return Result.success("{Entity}更新成功", result);
        } catch (Exception e) {
            log.error("更新{entity}信息失败，{entity}ID: {}", id, e);
            return Result.error("更新{entity}信息失败: " + e.getMessage());
        }
    }

    /**
     * 删除{entity}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_{entity}:write')")
    @Operation(summary = "删除{entity}", description = "删除{entity}")
    public Result<Boolean> delete{Entity}(
            @Parameter(description = "{Entity}ID") @PathVariable
            @Positive(message = "{Entity}ID必须为正整数") Long id) {

        try {
            boolean result = {entity}Service.delete{Entity}(id);
            return Result.success("{Entity}删除成功", result);
        } catch (Exception e) {
            log.error("删除{entity}失败，{entity}ID: {}", id, e);
            return Result.error("删除{entity}失败: " + e.getMessage());
        }
    }

    // 添加其他业务操作方法
}
```

### 2. Feign接口控制器

```java
package com.cloud.{service}.controller;

import com.cloud.api.{service}.{Entity}FeignClient;
import com.cloud.{service}.service.{Entity}Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

/**
 * {Entity}服务Feign客户端接口实现控制器
 * 实现{entity}服务对外提供的Feign接口
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class {Entity}FeignController implements {Entity}FeignClient {

    private final {Entity}Service {entity}Service;

    @Override
    public {Entity}VO get{Entity}ById(Long id) {
        log.debug("[{Entity}Feign控制器] 根据ID查询{entity}，ID: {}", id);
        // 直接委托给Service层，享受缓存和事务管理
        return {entity}Service.get{Entity}ById(id);
    }

    @Override
    public {Entity}VO create{Entity}({Entity}DTO {entity}DTO) {
        log.info("[{Entity}Feign控制器] 创建{entity}，{entity}: {}", {entity}DTO.getName());
        // 直接委托给Service层处理
        return {entity}Service.create{Entity}ForFeign({entity}DTO);
    }

    // 实现其他Feign接口方法
}
```

## 🏢 服务层标准模板

### 1. 服务接口

```java
package com.cloud.{service}.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cloud.{service}.module.entity.{Entity};

/**
 * {Entity}服务接口
 */
public interface {Entity}Service extends IService<{Entity}> {

    /**
     * 根据ID获取{entity}信息
     */
    {Entity}DTO get{Entity}ById(Long id);

    /**
     * 创建{entity}
     */
    Long create{Entity}({Entity}RequestDTO requestDTO);

    /**
     * 更新{entity}
     */
    Boolean update{Entity}(Long id, {Entity}RequestDTO requestDTO);

    /**
     * 删除{entity}
     */
    Boolean delete{Entity}(Long id);

    // 其他业务方法
}
```

### 2. 服务实现

```java
package com.cloud.{service}.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.common.annotation.DistributedLock;
import com.cloud.common.exception.BusinessException;
import com.cloud.common.exception.EntityNotFoundException;
import com.cloud.common.messaging.AsyncLogProducer;
import com.cloud.{service}.converter.{Entity}Converter;
import com.cloud.{service}.mapper.{Entity}Mapper;
import com.cloud.{service}.module.entity.{Entity};
import com.cloud.{service}.service.{Entity}Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {Entity}服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class {Entity}ServiceImpl extends ServiceImpl<{Entity}Mapper, {Entity}>
        implements {Entity}Service {
    
    private final {Entity}Converter {entity}Converter;
    private final AsyncLogProducer asyncLogProducer;

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('SCOPE_{entity}:read')")
    @Cacheable(cacheNames = "{entity}Cache", key = "#id", unless = "#result == null")
    public {Entity}DTO get{Entity}ById(Long id) {
        if (id == null) {
            throw new BusinessException("{Entity}ID不能为空");
        }

        try {
            log.info("根据ID查找{entity}: {}", id);
            {Entity} {entity} = getById(id);
            if ({entity} == null) {
                throw new EntityNotFoundException("{Entity}", id);
            }
            return {entity}Converter.toDTO({entity});
        } catch (Exception e) {
            log.error("根据ID查找{entity}失败，{entity}ID: {}", id, e);
            throw new BusinessException("获取{entity}信息失败", e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @PreAuthorize("hasRole('ADMIN')")
    @DistributedLock(
            key = "'{entity}:create:' + #requestDTO.name",
            waitTime = 3,
            leaseTime = 15,
            failMessage = "{Entity}创建操作获取锁失败"
    )
    public Long create{Entity}({Entity}RequestDTO requestDTO) {
        try {
            log.info("创建{entity}：{}", requestDTO.getName());
            
            {Entity} {entity} = {entity}Converter.toEntity(requestDTO);
            boolean saved = save({entity});
            
            if (!saved) {
                throw new BusinessException("{Entity}创建失败");
            }

            // 发送异步日志
            asyncLogProducer.send{Entity}OperationLogAsync(
                    "{service}-service",
                    "CREATE",
                    {entity}.getId(),
                    {entity}.getName(),
                    null,
                    "创建{entity}成功",
                    "SYSTEM"
            );

            return {entity}.getId();
        } catch (Exception e) {
            log.error("创建{entity}失败", e);
            throw new BusinessException("创建{entity}失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @PreAuthorize("hasRole('ADMIN')")
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "{entity}Cache", key = "#id"),
                    @CacheEvict(cacheNames = "{entity}ListCache", allEntries = true)
            }
    )
    public Boolean update{Entity}(Long id, {Entity}RequestDTO requestDTO) {
        try {
            log.info("更新{entity}：ID={}", id);
            
            {Entity} existing{Entity} = getById(id);
            if (existing{Entity} == null) {
                throw new EntityNotFoundException("{Entity}", id);
            }

            {Entity} {entity} = {entity}Converter.toEntity(requestDTO);
            {entity}.setId(id);
            
            boolean updated = updateById({entity});
            
            if (updated) {
                // 发送异步日志
                asyncLogProducer.send{Entity}OperationLogAsync(
                        "{service}-service",
                        "UPDATE",
                        id,
                        {entity}.getName(),
                        "更新前数据",
                        "更新后数据",
                        "SYSTEM"
                );
            }

            return updated;
        } catch (Exception e) {
            log.error("更新{entity}失败，ID: {}", id, e);
            throw new BusinessException("更新{entity}失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @PreAuthorize("hasRole('ADMIN')")
    @CacheEvict(cacheNames = "{entity}Cache", key = "#id")
    public Boolean delete{Entity}(Long id) {
        try {
            log.info("删除{entity}：ID={}", id);
            
            {Entity} {entity} = getById(id);
            if ({entity} == null) {
                throw new EntityNotFoundException("{Entity}", id);
            }

            boolean deleted = removeById(id);
            
            if (deleted) {
                // 发送异步日志
                asyncLogProducer.send{Entity}OperationLogAsync(
                        "{service}-service",
                        "DELETE",
                        id,
                        {entity}.getName(),
                        "删除的{entity}数据",
                        null,
                        "SYSTEM"
                );
            }

            return deleted;
        } catch (Exception e) {
            log.error("删除{entity}失败，ID: {}", id, e);
            throw new BusinessException("删除{entity}失败: " + e.getMessage(), e);
        }
    }
}
```

## 🔧 配置类标准模板

### 1. 缓存配置

```java
package com.cloud.{service}.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * {Service}缓存配置
 */
@Configuration
@EnableCaching
public class {Service}CacheConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        // JSON序列化配置
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = 
                new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        om.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, 
                ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        jackson2JsonRedisSerializer.setObjectMapper(om);

        // 缓存配置
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30)) // 默认30分钟过期
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(jackson2JsonRedisSerializer))
                .disableCachingNullValues();

        return RedisCacheManager.builder(factory)
                .cacheDefaults(config)
                .build();
    }
}
```

### 2. 安全配置

```java
package com.cloud.{service}.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * {Service}安全配置
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class {Service}SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> 
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/internal/**").permitAll() // 内部接口
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                );

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        // 配置权限转换逻辑
        return converter;
    }
}
```

## 📋 API设计规范

### 1. RESTful API标准

```
# 资源操作
GET    /{entities}                 # 获取资源列表（支持分页和查询）
GET    /{entities}/{id}            # 获取单个资源
POST   /{entities}                 # 创建资源
PUT    /{entities}/{id}            # 更新资源（完整）
PATCH  /{entities}/{id}            # 部分更新资源
DELETE /{entities}/{id}            # 删除资源

# 子资源操作
GET    /{entities}/{id}/profile    # 获取资源档案
PUT    /{entities}/{id}/profile    # 更新资源档案
PATCH  /{entities}/{id}/status     # 更新资源状态

# 批量操作
GET    /{entities}/batch           # 批量获取资源
DELETE /{entities}/batch           # 批量删除资源
PUT    /{entities}/batch/enable    # 批量启用资源

# 内部服务接口
GET    /internal/{entities}/{id}   # 内部服务获取资源
POST   /internal/{entities}        # 内部服务创建资源
```

### 2. 权限控制标准

```java
// 查询权限
@PreAuthorize("hasAuthority('SCOPE_{entity}:read')")

// 创建权限
@PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_{entity}:create')")

// 管理权限
@PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_{entity}:write')")

// 数据隔离权限
@PreAuthorize("hasRole('ADMIN') or @securityPermissionUtils.isOwner(authentication, #id)")
```

### 3. 返回结果标准

```java
// 成功返回
Result<T> result = Result.success("操作成功", data);
Result<Boolean> result = Result.success("操作成功", true);
Result<PageResult<T>> result = Result.success(pageResult);

// 错误返回
Result<Void> result = Result.error("错误信息");
```

## 📊 监控和日志标准

### 1. 日志记录

```java
// 业务日志
log.info("开始处理业务操作 - 参数: {}", params);
log.info("✅ 业务操作成功 - 结果: {}", result);
log.error("❌ 业务操作失败 - 错误: {}", e.getMessage(), e);

// 异步日志发送
asyncLogProducer.sendOperationLogAsync(
    "service-name",
    "OPERATION_TYPE",
    entityId,
    entityName,
    beforeData,
    afterData,
    operator
);
```

### 2. 性能监控

```java
// 方法性能监控
@Operation(summary = "业务方法", description = "详细描述")
@Cacheable(cacheNames = "cache", key = "#id")
@DistributedLock(key = "'lock:' + #id")
public Result<T> businessMethod(Long id) {
    // 业务逻辑
}
```

## ✅ 检查清单

### 开发阶段
- [ ] 遵循标准项目结构
- [ ] 实现标准控制器模板
- [ ] 使用统一的权限控制
- [ ] 添加完整的Swagger注解
- [ ] 实现缓存策略
- [ ] 添加分布式锁保护
- [ ] 集成异步日志记录

### 测试阶段
- [ ] 单元测试覆盖率 > 80%
- [ ] 集成测试通过
- [ ] API文档完整
- [ ] 性能测试合格

### 部署阶段
- [ ] 配置文件完整
- [ ] 健康检查正常
- [ ] 监控指标配置
- [ ] 日志收集配置

---

**模板版本**: v1.0  
**更新时间**: 2025-10-01  
**适用范围**: 所有新开发的微服务  
**维护人员**: 架构团队
