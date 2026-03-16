package com.cloud.common.domain.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;



@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterResponseDTO {



    private Long id;




    private String username;





    private String phone;




    private String nickname;




    private List<String> roles;
}
