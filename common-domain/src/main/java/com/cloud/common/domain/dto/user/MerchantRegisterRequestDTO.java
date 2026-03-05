package com.cloud.common.domain.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class MerchantRegisterRequestDTO {

    @NotBlank(message = "Username cannot be blank")
    private String username;

    @NotBlank(message = "Password cannot be blank")
    private String password;

    @NotBlank(message = "Email cannot be blank")
    private String email;

    @NotBlank(message = "Phone number cannot be blank")
    private String phone;

    @NotBlank(message = "Store name cannot be blank")
    private String nickname;

    @NotBlank(message = "Business license number cannot be blank")
    private String businessLicenseNumber;

    @NotNull(message = "Business license file cannot be null")
    private MultipartFile businessLicenseFile;

    @NotNull(message = "ID card front file cannot be null")
    private MultipartFile idCardFrontFile;

    @NotNull(message = "ID card back file cannot be null")
    private MultipartFile idCardBackFile;

    @NotBlank(message = "Contact phone cannot be blank")
    private String contactPhone;

    @NotBlank(message = "Contact address cannot be blank")
    private String contactAddress;
}
