package com.cloud.user.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloud.common.domain.Result;
import com.cloud.common.domain.dto.MerchantAuthDTO;
import com.cloud.user.module.entity.MerchantAuth;
import org.springframework.web.multipart.MultipartFile;

/**
 * 商家认证服务接口
 */
public interface MerchantAuthService {

    /**
     * 提交商家认证申请
     *
     * @param userId                  用户ID
     * @param shopName                店铺名称
     * @param businessLicenseNumber   营业执照号码
     * @param businessLicenseFile     营业执照文件
     * @param idCardFrontFile         身份证正面文件
     * @param idCardBackFile          身份证反面文件
     * @param contactPhone            联系电话
     * @param contactAddress          联系地址
     * @return 认证信息
     */
    MerchantAuth submitAuthApplication(Long userId, String shopName, String businessLicenseNumber,
                                       MultipartFile businessLicenseFile, MultipartFile idCardFrontFile,
                                       MultipartFile idCardBackFile, String contactPhone, String contactAddress);

    /**
     * 根据用户ID获取认证信息
     *
     * @param userId 用户ID
     * @return 认证信息
     */
    MerchantAuth getByUserId(Long userId);

    /**
     * 分页查询待审核的商家认证申请
     *
     * @param page 页码
     * @param size 每页数量
     * @return 认证信息分页结果
     */
    IPage<MerchantAuth> getPendingAuthApplications(int page, int size);

    /**
     * 审核商家认证申请
     *
     * @param authId 认证ID
     * @param status 审核状态 1-通过，2-拒绝
     * @param remark 审核备注
     * @return 是否成功
     */
    boolean auditMerchantAuth(Long authId, Integer status, String remark);

    /**
     * 将实体转换为DTO
     *
     * @param merchantAuth 认证实体
     * @return 认证DTO
     */
    MerchantAuthDTO convertToDTO(MerchantAuth merchantAuth);
}