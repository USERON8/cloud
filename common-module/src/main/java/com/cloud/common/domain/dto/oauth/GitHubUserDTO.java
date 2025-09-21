package com.cloud.common.domain.dto.oauth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * GitHub OAuth用户信息DTO
 * 用于存储从GitHub API获取的用户信息
 *
 * @author what's up
 * @since 2025-09-20
 */
@Data
@NoArgsConstructor 
@AllArgsConstructor
@Builder
@Schema(description = "GitHub OAuth用户信息DTO")
public class GitHubUserDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "GitHub用户ID")
    @NotNull(message = "GitHub用户ID不能为空")
    private Long githubId;

    @Schema(description = "GitHub用户名(login)")
    @NotBlank(message = "GitHub用户名不能为空")
    private String login;

    @Schema(description = "用户显示名称")
    private String name;

    @Schema(description = "邮箱地址")
    private String email;

    @Schema(description = "GitHub头像URL")
    private String avatarUrl;

    @Schema(description = "GitHub个人页面URL")
    private String htmlUrl;

    @Schema(description = "个人简介")
    private String bio;

    @Schema(description = "所在位置")
    private String location;

    @Schema(description = "博客/个人网站")
    private String blog;

    @Schema(description = "所在公司")
    private String company;

    @Schema(description = "公开仓库数量")
    private Integer publicRepos;

    @Schema(description = "粉丝数")
    private Integer followers;

    @Schema(description = "关注数")
    private Integer following;

    @Schema(description = "GitHub账户创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "GitHub账户更新时间")
    private LocalDateTime updatedAt;

    /**
     * 构建系统用户名
     * @return github_login格式的用户名
     */
    public String buildSystemUsername() {
        return "github_" + this.login;
    }

    /**
     * 获取显示名称，优先使用name，其次使用login
     * @return 显示名称
     */
    public String getDisplayName() {
        return (name != null && !name.trim().isEmpty()) ? name : login;
    }
}
