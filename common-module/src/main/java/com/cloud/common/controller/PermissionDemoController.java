package com.cloud.common.controller;

import com.cloud.common.annotation.RequireAuthentication;
import com.cloud.common.annotation.RequireScope;
import com.cloud.common.annotation.RequireUserType;
import com.cloud.common.result.Result;
import com.cloud.common.security.PermissionManager;
import com.cloud.common.service.UserInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 权限校验示例控制器
 * 演示各种权限校验注解和功能的使用
 * 
 * @author what's up
 */
@Slf4j
@RestController
@RequestMapping("/demo/permission")
@RequiredArgsConstructor
@Tag(name = "权限校验示例", description = "演示权限校验功能的示例接口")
public class PermissionDemoController {
    
    private final PermissionManager permissionManager;
    private final UserInfoService userInfoService;
    
    /**
     * 公开接口 - 无需任何权限
     */
    @GetMapping("/public")
    @Operation(summary = "公开接口", description = "无需任何权限即可访问")
    public Result<String> publicEndpoint() {
        log.info("访问公开接口");
        return Result.success("这是公开接口，任何人都可以访问");
    }
    
    /**
     * 需要认证的接口
     */
    @GetMapping("/authenticated")
    @RequireAuthentication
    @Operation(summary = "认证接口", description = "需要用户认证后才能访问")
    public Result<Map<String, Object>> authenticatedEndpoint() {
        log.info("访问需认证接口");
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "您已通过认证验证");
        response.put("userInfo", userInfoService.getCurrentUserBasicInfo());
        
        return Result.success(response);
    }
    
    /**
     * 需要读权限的接口
     */
    @GetMapping("/require-read")
    @RequireScope("read")
    @Operation(summary = "读权限接口", description = "需要read权限才能访问")
    public Result<String> requireReadEndpoint() {
        log.info("访问需要读权限的接口");
        return Result.success("您拥有read权限，可以执行读操作");
    }
    
    /**
     * 需要写权限的接口
     */
    @PostMapping("/require-write")
    @RequireScope("write")
    @Operation(summary = "写权限接口", description = "需要write权限才能访问")
    public Result<String> requireWriteEndpoint(@RequestBody Map<String, Object> data) {
        log.info("访问需要写权限的接口，数据: {}", data);
        return Result.success("您拥有write权限，可以执行写操作");
    }
    
    /**
     * 需要多个权限之一的接口
     */
    @GetMapping("/require-any-scope")
    @RequireScope(value = {"user.read", "admin.read"}, mode = RequireScope.ScopeMode.ANY)
    @Operation(summary = "任意权限接口", description = "需要user.read或admin.read权限之一")
    public Result<String> requireAnyScopeEndpoint() {
        log.info("访问需要任意权限的接口");
        return Result.success("您拥有user.read或admin.read权限中的至少一个");
    }
    
    /**
     * 需要所有权限的接口
     */
    @PostMapping("/require-all-scope")
    @RequireScope(value = {"read", "write", "user.write"}, mode = RequireScope.ScopeMode.ALL)
    @Operation(summary = "全部权限接口", description = "需要同时拥有read、write、user.write权限")
    public Result<String> requireAllScopeEndpoint(@RequestBody Map<String, Object> data) {
        log.info("访问需要所有权限的接口，数据: {}", data);
        return Result.success("您拥有read、write、user.write所有必需权限");
    }
    
    /**
     * 仅普通用户可访问的接口
     */
    @GetMapping("/user-only")
    @RequireUserType(RequireUserType.UserType.USER)
    @Operation(summary = "普通用户接口", description = "仅普通用户可以访问")
    public Result<String> userOnlyEndpoint() {
        log.info("访问仅普通用户可访问的接口");
        return Result.success("欢迎普通用户！");
    }
    
    /**
     * 仅商户可访问的接口
     */
    @GetMapping("/merchant-only")
    @RequireUserType(RequireUserType.UserType.MERCHANT)
    @Operation(summary = "商户接口", description = "仅商户用户可以访问")
    public Result<String> merchantOnlyEndpoint() {
        log.info("访问仅商户可访问的接口");
        return Result.success("欢迎商户用户！您可以管理您的商品和订单");
    }
    
    /**
     * 仅管理员可访问的接口
     */
    @GetMapping("/admin-only")
    @RequireUserType(RequireUserType.UserType.ADMIN)
    @Operation(summary = "管理员接口", description = "仅管理员可以访问")
    public Result<Map<String, Object>> adminOnlyEndpoint() {
        log.info("访问仅管理员可访问的接口");
        
        Map<String, Object> adminInfo = new HashMap<>();
        adminInfo.put("message", "欢迎管理员！您拥有最高权限");
        adminInfo.put("userInfo", userInfoService.getCurrentUserFullInfo());
        adminInfo.put("permissionSummary", userInfoService.getCurrentUserPermissionSummary());
        
        return Result.success(adminInfo);
    }
    
    /**
     * 商户或管理员可访问的接口
     */
    @GetMapping("/merchant-or-admin")
    @RequireUserType({RequireUserType.UserType.MERCHANT, RequireUserType.UserType.ADMIN})
    @Operation(summary = "商户或管理员接口", description = "商户或管理员可以访问")
    public Result<String> merchantOrAdminEndpoint() {
        log.info("访问商户或管理员可访问的接口");
        return Result.success("欢迎商户或管理员！您拥有业务管理权限");
    }
    
    /**
     * 复合权限校验接口
     */
    @PostMapping("/complex-permission")
    @RequireAuthentication(message = "此接口需要登录后访问")
    @RequireUserType(value = {RequireUserType.UserType.MERCHANT, RequireUserType.UserType.ADMIN}, 
                    message = "此接口仅限商户和管理员访问")
    @RequireScope(value = {"write", "user.write"}, mode = RequireScope.ScopeMode.ALL, 
                  message = "此接口需要写权限和用户写权限")
    @Operation(summary = "复合权限接口", description = "需要认证+用户类型+权限范围的复合校验")
    public Result<Map<String, Object>> complexPermissionEndpoint(@RequestBody Map<String, Object> data) {
        log.info("访问复合权限校验接口，数据: {}", data);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "恭喜！您通过了复合权限校验");
        response.put("data", data);
        response.put("userInfo", userInfoService.getCurrentUserBasicInfo());
        response.put("timestamp", System.currentTimeMillis());
        
        return Result.success(response);
    }
    
    /**
     * 手动权限检查示例
     */
    @PostMapping("/manual-check")
    @RequireAuthentication
    @Operation(summary = "手动权限检查", description = "演示通过代码手动进行权限检查")
    public Result<String> manualPermissionCheck(
            @Parameter(description = "目标用户ID") @RequestParam(required = false) String targetUserId) {
        
        log.info("执行手动权限检查，目标用户ID: {}", targetUserId);
        
        // 手动检查管理员权限
        try {
            permissionManager.checkAdmin();
            
            if (targetUserId != null) {
                // 如果指定了目标用户ID，检查是否可以操作该用户数据
                permissionManager.checkSelfOrAdmin(targetUserId);
                return Result.success(String.format("您有权限操作用户[%s]的数据", targetUserId));
            } else {
                return Result.success("您拥有管理员权限");
            }
            
        } catch (Exception e) {
            log.info("管理员权限检查失败，尝试其他权限检查");
        }
        
        // 检查是否为自身操作
        if (targetUserId != null) {
            try {
                permissionManager.checkSelfOperation(targetUserId);
                return Result.success(String.format("您可以操作自己的数据[%s]", targetUserId));
            } catch (Exception e) {
                return Result.error("您只能操作自己的数据或需要管理员权限");
            }
        }
        
        return Result.success("手动权限检查完成");
    }
    
    /**
     * 获取当前用户完整信息
     */
    @GetMapping("/user-info")
    @RequireAuthentication
    @Operation(summary = "获取用户信息", description = "获取当前用户的完整信息")
    public Result<Map<String, Object>> getCurrentUserInfo() {
        log.info("获取当前用户完整信息");
        
        Map<String, Object> userInfo = userInfoService.getCurrentUserFullInfo();
        return Result.success(userInfo);
    }
    
    /**
     * 获取用户权限摘要
     */
    @GetMapping("/permission-summary")
    @RequireAuthentication
    @Operation(summary = "权限摘要", description = "获取当前用户的权限摘要信息")
    public Result<Map<String, Object>> getPermissionSummary() {
        log.info("获取当前用户权限摘要");
        
        Map<String, Object> summary = userInfoService.getCurrentUserPermissionSummary();
        return Result.success(summary);
    }
}
