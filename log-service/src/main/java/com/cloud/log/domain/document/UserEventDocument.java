package com.cloud.log.domain.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;

/**
 * 用户事件Elasticsearch文档
 * 用于存储用户生命周期事件到ES
 * 基于阿里巴巴官方示例标准设计
 *
 * @author cloud
 * @date 2025/1/15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "user-events", createIndex = true)
public class UserEventDocument {

    /**
     * 文档ID
     */
    @Id
    private String id;

    /**
     * 用户ID
     */
    @Field(type = FieldType.Long)
    private Long userId;

    /**
     * 用户名
     */
    @Field(type = FieldType.Keyword)
    private String username;

    /**
     * 昵称
     */
    @Field(type = FieldType.Keyword)
    private String nickname;

    /**
     * 事件类型
     */
    @Field(type = FieldType.Keyword)
    private String eventType;

    /**
     * 消息标签
     */
    @Field(type = FieldType.Keyword)
    private String tag;

    /**
     * 追踪ID
     */
    @Field(type = FieldType.Keyword)
    private String traceId;

    /**
     * 邮箱（脱敏后）
     */
    @Field(type = FieldType.Keyword)
    private String email;

    /**
     * 手机号（脱敏后）
     */
    @Field(type = FieldType.Keyword)
    private String phone;

    /**
     * 用户状态
     */
    @Field(type = FieldType.Integer)
    private Integer userStatus;

    /**
     * 原用户状态
     */
    @Field(type = FieldType.Integer)
    private Integer oldUserStatus;

    /**
     * 头像URL（脱敏后）
     */
    @Field(type = FieldType.Keyword)
    private String avatarUrl;

    /**
     * 性别
     */
    @Field(type = FieldType.Integer)
    private Integer gender;

    /**
     * 生日
     */
    @Field(type = FieldType.Date, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime birthday;

    /**
     * 地区编码
     */
    @Field(type = FieldType.Keyword)
    private String regionCode;

    /**
     * 地址（脱敏后）
     */
    @Field(type = FieldType.Text)
    private String address;

    /**
     * 个人简介
     */
    @Field(type = FieldType.Text)
    private String bio;

    /**
     * 注册来源
     */
    @Field(type = FieldType.Integer)
    private Integer registerSource;

    /**
     * 注册IP（脱敏后）
     */
    @Field(type = FieldType.Keyword)
    private String registerIp;

    /**
     * 注册设备信息（脱敏后）
     */
    @Field(type = FieldType.Text)
    private String registerDevice;

    /**
     * 最后登录时间
     */
    @Field(type = FieldType.Date, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastLoginTime;

    /**
     * 最后登录IP（脱敏后）
     */
    @Field(type = FieldType.Keyword)
    private String lastLoginIp;

    /**
     * 最后登录设备（脱敏后）
     */
    @Field(type = FieldType.Text)
    private String lastLoginDevice;

    /**
     * 登录次数
     */
    @Field(type = FieldType.Integer)
    private Integer loginCount;

    /**
     * 实名认证状态
     */
    @Field(type = FieldType.Integer)
    private Integer verificationStatus;

    /**
     * 实名姓名（脱敏后）
     */
    @Field(type = FieldType.Keyword)
    private String realName;

    /**
     * 身份证号（脱敏后）
     */
    @Field(type = FieldType.Keyword)
    private String idCard;

    /**
     * VIP等级
     */
    @Field(type = FieldType.Integer)
    private Integer vipLevel;

    /**
     * 积分
     */
    @Field(type = FieldType.Integer)
    private Integer points;

    /**
     * 用户创建时间
     */
    @Field(type = FieldType.Date, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime userCreateTime;

    /**
     * 用户更新时间
     */
    @Field(type = FieldType.Date, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime userUpdateTime;

    /**
     * 事件发生时间
     */
    @Field(type = FieldType.Date, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime eventTime;

    /**
     * 操作人ID
     */
    @Field(type = FieldType.Long)
    private Long operatorId;

    /**
     * 操作人名称
     */
    @Field(type = FieldType.Keyword)
    private String operatorName;

    /**
     * 操作类型
     */
    @Field(type = FieldType.Integer)
    private Integer operationType;

    /**
     * 操作IP（脱敏后）
     */
    @Field(type = FieldType.Keyword)
    private String operationIp;

    /**
     * 操作设备信息（脱敏后）
     */
    @Field(type = FieldType.Text)
    private String operationDevice;

    /**
     * 操作原因
     */
    @Field(type = FieldType.Text)
    private String operationReason;

    /**
     * 扩展字段
     */
    @Field(type = FieldType.Text)
    private String extendData;

    /**
     * 消息时间戳
     */
    @Field(type = FieldType.Long)
    private Long messageTimestamp;

    /**
     * 处理时间
     */
    @Field(type = FieldType.Date, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime processTime;
}
