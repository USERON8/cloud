package com.cloud.common.domain.event.product;

import com.cloud.common.domain.event.base.BaseBusinessLogEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 店铺变更日志事件
 * 
 * 记录店铺相关的操作日志，包括：
 * - 店铺注册、更新、删除
 * - 店铺状态变更
 * - 店铺认证信息变更
 * - 店铺经营类目调整
 * - 店铺权限变更等
 * 
 * @author CloudDevAgent
 * @since 2025-09-27
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ShopChangeLogEvent extends BaseBusinessLogEvent {

    private static final long serialVersionUID = 1L;

    /**
     * 店铺名称
     */
    private String shopName;

    /**
     * 店铺代码
     */
    private String shopCode;

    /**
     * 店铺所有者ID
     */
    private Long ownerId;

    /**
     * 店铺所有者姓名
     */
    private String ownerName;

    /**
     * 店铺类型
     * PERSONAL - 个人店铺
     * ENTERPRISE - 企业店铺
     * FLAGSHIP - 旗舰店
     */
    private String shopType;

    /**
     * 店铺状态变更
     * PENDING -> ACTIVE（待审核->营业中）
     * ACTIVE -> SUSPENDED（营业中->暂停营业）
     * SUSPENDED -> CLOSED（暂停营业->关闭）
     */
    private String statusChange;

    /**
     * 认证状态变更
     * UNVERIFIED -> VERIFIED（未认证->已认证）
     * VERIFIED -> REJECTED（已认证->认证失败）
     */
    private String verificationChange;

    /**
     * 经营类目变更
     */
    private String categoryChange;

    /**
     * 联系电话
     */
    private String contactPhone;

    /**
     * 店铺地址
     */
    private String shopAddress;

    @Override
    public String getLogType() {
        return "SHOP_CHANGE";
    }
}
