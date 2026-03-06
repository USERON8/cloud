package com.cloud.common.domain.dto.auth;

import com.cloud.common.domain.dto.user.UserDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;







@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponseDTO {
    


    private String access_token;

    


    private String token_type;

    


    private long expires_in;

    


    private String refresh_token;

    


    private String scope;

    


    private String userType;

    


    private String nickname;

    


    private UserDTO user;
}
