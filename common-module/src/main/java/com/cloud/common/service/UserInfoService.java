package com.cloud.common.service;

import com.cloud.common.utils.UserContextUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;







@Slf4j
@Service
public class UserInfoService {

    




    public Map<String, Object> getCurrentUserBasicInfo() {
        log.debug("Start loading current user basic info");

        Map<String, Object> userInfo = new HashMap<>();

        try {
            
            userInfo.put("userId", UserContextUtils.getCurrentUserId());
            userInfo.put("username", UserContextUtils.getCurrentUsername());
            userInfo.put("userType", UserContextUtils.getCurrentUserType());
            userInfo.put("nickname", UserContextUtils.getCurrentUserNickname());
            userInfo.put("status", UserContextUtils.getCurrentUserStatus());

            
            userInfo.put("clientId", UserContextUtils.getClientId());
            userInfo.put("tokenVersion", UserContextUtils.getTokenVersion());
            userInfo.put("isAuthenticated", UserContextUtils.isAuthenticated());

            
            Set<String> scopes = UserContextUtils.getCurrentUserScopes();
            userInfo.put("scopes", scopes);
            userInfo.put("scopeCount", scopes.size());

            
            userInfo.put("isRegularUser", UserContextUtils.isRegularUser());
            userInfo.put("isMerchant", UserContextUtils.isMerchant());
            userInfo.put("isAdmin", UserContextUtils.isAdmin());

            log.debug("鎴愬姛鑾峰彇鐢ㄦ埛鍩烘湰淇℃伅锛岀敤鎴稩D: {}", userInfo.get("userId"));

        } catch (Exception e) {
            log.error("鑾峰彇鐢ㄦ埛鍩烘湰淇℃伅澶辫触", e);
            userInfo.put("error", "鑾峰彇鐢ㄦ埛淇℃伅澶辫触: " + e.getMessage());
        }

        return userInfo;
    }

    





    public Map<String, Object> getCurrentUserSensitiveInfo() {
        log.debug("Start loading current user sensitive info");

        Map<String, Object> sensitiveInfo = new HashMap<>();

        try {
            
            if (!UserContextUtils.isAuthenticated()) {
                sensitiveInfo.put("error", "鐢ㄦ埛鏈璇侊紝鏃犳硶鑾峰彇鏁忔劅淇℃伅");
                return sensitiveInfo;
            }

            
            String phone = UserContextUtils.getCurrentUserPhone();
            if (StringUtils.hasText(phone)) {
                
                String maskedPhone = maskPhoneNumber(phone);
                sensitiveInfo.put("phone", phone);  
                sensitiveInfo.put("maskedPhone", maskedPhone);  
            } else {
                sensitiveInfo.put("phone", null);
                sensitiveInfo.put("maskedPhone", null);
            }

            log.debug("鎴愬姛鑾峰彇鐢ㄦ埛鏁忔劅淇℃伅锛岀敤鎴稩D: {}", UserContextUtils.getCurrentUserId());

        } catch (Exception e) {
            log.error("鑾峰彇鐢ㄦ埛鏁忔劅淇℃伅澶辫触", e);
            sensitiveInfo.put("error", "鑾峰彇鏁忔劅淇℃伅澶辫触: " + e.getMessage());
        }

        return sensitiveInfo;
    }

    





    public Map<String, Object> getCurrentUserFullInfo() {
        log.debug("Start loading current user full info");

        Map<String, Object> fullInfo = new HashMap<>();

        
        Map<String, Object> basicInfo = getCurrentUserBasicInfo();
        fullInfo.putAll(basicInfo);

        
        Map<String, Object> sensitiveInfo = getCurrentUserSensitiveInfo();
        fullInfo.putAll(sensitiveInfo);

        
        fullInfo.put("timestamp", System.currentTimeMillis());
        fullInfo.put("source", "UserInfoService");

        log.debug("鎴愬姛鑾峰彇鐢ㄦ埛瀹屾暣淇℃伅锛岀敤鎴稩D: {}", fullInfo.get("userId"));

        return fullInfo;
    }

    




    public Map<String, Object> getCurrentUserPermissionSummary() {
        log.debug("Start loading current user permission summary");

        Map<String, Object> permissionSummary = new HashMap<>();

        try {
            
            boolean isAuthenticated = UserContextUtils.isAuthenticated();
            permissionSummary.put("isAuthenticated", isAuthenticated);

            if (!isAuthenticated) {
                permissionSummary.put("message", "User is not authenticated");
                return permissionSummary;
            }

            
            String userId = UserContextUtils.getCurrentUserId();
            String userType = UserContextUtils.getCurrentUserType();

            permissionSummary.put("userId", userId);
            permissionSummary.put("userType", userType);

            
            permissionSummary.put("isRegularUser", UserContextUtils.isRegularUser());
            permissionSummary.put("isMerchant", UserContextUtils.isMerchant());
            permissionSummary.put("isAdmin", UserContextUtils.isAdmin());

            
            Set<String> scopes = UserContextUtils.getCurrentUserScopes();
            permissionSummary.put("scopes", scopes);
            permissionSummary.put("scopeCount", scopes.size());

            
            Map<String, Boolean> commonPermissions = new HashMap<>();
            commonPermissions.put("canRead", UserContextUtils.hasScope("read"));
            commonPermissions.put("canWrite", UserContextUtils.hasScope("write"));
            commonPermissions.put("canUserRead", UserContextUtils.hasScope("user:read"));
            commonPermissions.put("canUserWrite", UserContextUtils.hasScope("user:write"));
            commonPermissions.put("canAdminRead", UserContextUtils.hasScope("admin:read"));
            commonPermissions.put("canAdminWrite", UserContextUtils.hasScope("admin:write"));
            permissionSummary.put("commonPermissions", commonPermissions);

            log.debug("鎴愬姛鑾峰彇鐢ㄦ埛鏉冮檺鎽樿锛岀敤鎴稩D: {}, 鏉冮檺鏁伴噺: {}", userId, scopes.size());

        } catch (Exception e) {
            log.error("鑾峰彇鐢ㄦ埛鏉冮檺鎽樿澶辫触", e);
            permissionSummary.put("error", "鑾峰彇鏉冮檺淇℃伅澶辫触: " + e.getMessage());
        }

        return permissionSummary;
    }

    





    public boolean hasPermission(String permission) {
        try {
            if (!UserContextUtils.isAuthenticated()) {
                return false;
            }
            return UserContextUtils.hasScope(permission);
        } catch (Exception e) {
            log.error("妫€鏌ユ潈闄愬け璐ワ紝鏉冮檺: {}", permission, e);
            return false;
        }
    }

    





    public boolean hasAnyPermission(String... permissions) {
        try {
            if (!UserContextUtils.isAuthenticated()) {
                return false;
            }
            return UserContextUtils.hasAnyScope(permissions);
        } catch (Exception e) {
            log.error("妫€鏌ヤ换鎰忔潈闄愬け璐ワ紝鏉冮檺: {}", java.util.Arrays.toString(permissions), e);
            return false;
        }
    }

    





    public boolean isUserType(String userType) {
        try {
            return UserContextUtils.isUserType(userType);
        } catch (Exception e) {
            log.error("妫€鏌ョ敤鎴风被鍨嬪け璐ワ紝绫诲瀷: {}", userType, e);
            return false;
        }
    }

    





    public String getCurrentUserDebugInfo() {
        try {
            return UserContextUtils.getCurrentUserInfo();
        } catch (Exception e) {
            return "鑾峰彇璋冭瘯淇℃伅澶辫触: " + e.getMessage();
        }
    }

    





    private String maskPhoneNumber(String phone) {
        if (!StringUtils.hasText(phone) || phone.length() < 7) {
            return phone;
        }

        
        if (phone.length() == 11) {
            return phone.substring(0, 3) + "****" + phone.substring(7);
        } else {
            
            return phone.substring(0, 2) + "***" + phone.substring(phone.length() - 2);
        }
    }
}
