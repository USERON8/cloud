package com.cloud.common.domain.dto.user;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class UserAddressDTO implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  private Long id;

  @NotNull(message = "User ID cannot be null")
  private Long userId;

  @Size(max = 32, message = "Address tag length must be less than or equal to 32")
  private String addressTag;

  @NotBlank(message = "Receiver name cannot be blank")
  @Size(max = 50, message = "Receiver name length must be less than or equal to 50")
  private String receiverName;

  @NotBlank(message = "Receiver phone cannot be blank")
  @Pattern(regexp = "^1[3-9]\\d{9}$", message = "Invalid phone format")
  private String receiverPhone;

  @Size(max = 64, message = "Country length must be less than or equal to 64")
  private String country;

  @NotBlank(message = "Province cannot be blank")
  @Size(max = 64, message = "Province length must be less than or equal to 64")
  private String province;

  @NotBlank(message = "City cannot be blank")
  @Size(max = 64, message = "City length must be less than or equal to 64")
  private String city;

  @NotBlank(message = "District cannot be blank")
  @Size(max = 64, message = "District length must be less than or equal to 64")
  private String district;

  @NotBlank(message = "Street cannot be blank")
  @Size(max = 100, message = "Street length must be less than or equal to 100")
  private String street;

  @NotBlank(message = "Detail address cannot be blank")
  @Size(max = 255, message = "Detail address length must be less than or equal to 255")
  private String detailAddress;

  @Size(max = 16, message = "Postal code length must be less than or equal to 16")
  private String postalCode;

  private BigDecimal longitude;

  private BigDecimal latitude;

  @Min(value = 0, message = "Default flag must be greater than or equal to 0")
  @Max(value = 1, message = "Default flag must be less than or equal to 1")
  private Integer isDefault;

  private LocalDateTime createdAt;

  private LocalDateTime updatedAt;

  private Integer deleted;
}
