package com.cloud.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.common.domain.dto.MerchantAuthDTO;
import com.cloud.user.converter.MerchantAuthConverter;
import com.cloud.user.mapper.MerchantAuthMapper;
import com.cloud.user.module.entity.MerchantAuth;
import com.cloud.user.module.entity.User;
import com.cloud.user.service.FileUploadService;
import com.cloud.user.service.MerchantAuthService;
import com.cloud.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 商家认证服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantAuthServiceImpl extends ServiceImpl<MerchantAuthMapper, MerchantAuth> implements MerchantAuthService {

    private final MerchantAuthMapper merchantAuthMapper;
    private final FileUploadService fileUploadService;
    private final UserService userService;
    private final MerchantAuthConverter merchantAuthConverter;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MerchantAuth submitAuthApplication(Long userId, String shopName, String businessLicenseNumber,
                                              MultipartFile businessLicenseFile, MultipartFile idCardFrontFile,
                                              MultipartFile idCardBackFile, String contactPhone, String contactAddress) {
        log.info("提交商家认证申请, userId: {}", userId);

        // 检查是否已提交过认证申请
        MerchantAuth existingAuth = getByUserId(userId);
        if (existingAuth != null) {
            throw new RuntimeException("您已提交过认证申请，请耐心等待审核结果");
        }

        // 上传文件
        String businessLicenseUrl = fileUploadService.uploadAvatar(businessLicenseFile, userId);
        String idCardFrontUrl = fileUploadService.uploadAvatar(idCardFrontFile, userId);
        String idCardBackUrl = fileUploadService.uploadAvatar(idCardBackFile, userId);

        // 创建认证信息
        MerchantAuth merchantAuth = new MerchantAuth();
        merchantAuth.setUserId(userId);
        merchantAuth.setShopName(shopName);
        merchantAuth.setBusinessLicenseNumber(businessLicenseNumber);
        merchantAuth.setBusinessLicenseUrl(businessLicenseUrl);
        merchantAuth.setIdCardFrontUrl(idCardFrontUrl);
        merchantAuth.setIdCardBackUrl(idCardBackUrl);
        merchantAuth.setContactPhone(contactPhone);
        merchantAuth.setContactAddress(contactAddress);
        merchantAuth.setStatus(0); // 待审核状态
        merchantAuth.setCreatedAt(LocalDateTime.now());
        merchantAuth.setUpdatedAt(LocalDateTime.now());

        // 保存认证信息
        merchantAuthMapper.insert(merchantAuth);

        log.info("商家认证申请提交成功, authId: {}, userId: {}", merchantAuth.getId(), userId);
        return merchantAuth;
    }

    @Override
    public MerchantAuth getByUserId(Long userId) {
        return merchantAuthMapper.selectOne(new QueryWrapper<MerchantAuth>().eq("user_id", userId));
    }

    @Override
    public IPage<MerchantAuth> getPendingAuthApplications(int page, int size) {
        QueryWrapper<MerchantAuth> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", 0); // 待审核状态
        queryWrapper.orderByDesc("created_at");
        return merchantAuthMapper.selectPage(new Page<>(page, size), queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean auditMerchantAuth(Long authId, Integer status, String remark) {
        log.info("审核商家认证申请, authId: {}, status: {}", authId, status);

        // 获取认证信息
        MerchantAuth merchantAuth = merchantAuthMapper.selectById(authId);
        if (merchantAuth == null) {
            log.warn("商家认证申请不存在, authId: {}", authId);
            return false;
        }

        // 更新认证状态
        merchantAuth.setStatus(status);
        merchantAuth.setAuditRemark(remark);
        merchantAuth.setUpdatedAt(LocalDateTime.now());
        merchantAuthMapper.updateById(merchantAuth);

        // 如果审核通过，更新用户状态为启用并设置为商家类型
        if (Objects.equals(status, 1)) {
            User user = userService.getById(merchantAuth.getUserId());
            if (user != null) {
                user.setStatus(1); // 启用
                user.setUserType("MERCHANT"); // 设置为商家类型
                // 不再设置merchantApproved字段，通过MerchantAuth表来管理认证状态
                userService.updateById(user);
                log.info("商家认证审核通过，用户已启用, userId: {}", merchantAuth.getUserId());
            }
        }

        log.info("商家认证申请审核完成, authId: {}, status: {}", authId, status);
        return true;
    }

    @Override
    public MerchantAuthDTO convertToDTO(MerchantAuth merchantAuth) {
        return merchantAuthConverter.toDTO(merchantAuth);
    }
}