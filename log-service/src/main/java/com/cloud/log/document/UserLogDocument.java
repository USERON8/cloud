package com.cloud.log.document;

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
 * 用户日志ES文档模型
 *
 * @author cloud
 * @date 2025/1/15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "user_logs")
public class UserLogDocument {

    @Id
    private String id;

    /**
     * 追踪ID，用于关联请求链路
     */
    @Field(type = FieldType.Keyword)
    private String traceId;

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
     * 用户昵称
     */
    @Field(type = FieldType.Text, analyzer = "ik_smart")
    private String nickname;

    /**
     * 用户手机号（脱敏）
     */
    @Field(type = FieldType.Keyword)
    private String phone;

    /**
     * 用户类型
     */
    @Field(type = FieldType.Keyword)
    private String userType;

    /**
     * 变更前状态
     */
    @Field(type = FieldType.Integer)
    private Integer beforeStatus;

    /**
     * 变更后状态
     */
    @Field(type = FieldType.Integer)
    private Integer afterStatus;

    /**
     * 变更类型：1-创建用户，2-更新用户，3-删除用户，4-状态变更
     */
    @Field(type = FieldType.Integer)
    private Integer changeType;

    /**
     * 变更类型描述
     */
    @Field(type = FieldType.Keyword)
    private String changeTypeDesc;

    /**
     * 操作人
     */
    @Field(type = FieldType.Keyword)
    private String operator;

    /**
     * 操作时间
     */
    @Field(type = FieldType.Date, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime operateTime;

    /**
     * 日志创建时间
     */
    @Field(type = FieldType.Date, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime logCreateTime;

    /**
     * 消息来源服务
     */
    @Field(type = FieldType.Keyword)
    private String sourceService;

    /**
     * 事件类型
     */
    @Field(type = FieldType.Keyword)
    private String eventType;
}
