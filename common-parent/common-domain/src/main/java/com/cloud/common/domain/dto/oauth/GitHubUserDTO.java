package com.cloud.common.domain.dto.oauth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "GitHub OAuth user DTO")
public class GitHubUserDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "GitHub user id")
    @NotNull(message = "githubId cannot be null")
    private Long githubId;

    @Schema(description = "GitHub login")
    @NotBlank(message = "login cannot be blank")
    private String login;

    @Schema(description = "Display name")
    private String name;

    @Schema(description = "Email")
    private String email;

    @Schema(description = "Avatar url")
    private String avatarUrl;

    @Schema(description = "Profile url")
    private String htmlUrl;

    @Schema(description = "Bio")
    private String bio;

    @Schema(description = "Location")
    private String location;

    @Schema(description = "Blog")
    private String blog;

    @Schema(description = "Company")
    private String company;

    @Schema(description = "Public repository count")
    private Integer publicRepos;

    @Schema(description = "Followers count")
    private Integer followers;

    @Schema(description = "Following count")
    private Integer following;

    @Schema(description = "Created at")
    private LocalDateTime createdAt;

    @Schema(description = "Updated at")
    private LocalDateTime updatedAt;

    public String buildSystemUsername() {
        return "github_" + this.login;
    }

    public String getDisplayName() {
        return (name != null && !name.trim().isEmpty()) ? name : login;
    }
}
