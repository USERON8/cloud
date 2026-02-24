package com.cloud.common.domain.dto.user;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.io.Serial;
import java.io.Serializable;




@Data
public class MerchantAuthRequestDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    


    @NotBlank(message = "Business license number cannot be blank")
    @Size(max = 50, message = "Business license number length must be less than or equal to 50")
    private String businessLicenseNumber;
    


    @NotBlank(message = "Business license URL cannot be blank")
    @Size(max = 255, message = "Business license URL length must be less than or equal to 255")
    private String businessLicenseUrl;
    


    @NotBlank(message = "ID card front URL cannot be blank")
    @Size(max = 255, message = "ID card front URL length must be less than or equal to 255")
    private String idCardFrontUrl;
    


    @NotBlank(message = "ID card back URL cannot be blank")
    @Size(max = 255, message = "ID card back URL length must be less than or equal to 255")
    private String idCardBackUrl;
    


    @NotBlank(message = "Contact phone cannot be blank")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "Invalid contact phone format")
    private String contactPhone;
    


    @NotBlank(message = "Contact address cannot be blank")
    @Size(max = 255, message = "Contact address length must be less than or equal to 255")
    private String contactAddress;
}
