package com.cloud.auth.config;

import com.cloud.auth.service.AuthUserService;
import com.cloud.common.domain.dto.UserDTO;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AuthUserDetailService implements UserDetailsService {

    private final AuthUserService authUserService;

    public AuthUserDetailService(AuthUserService authUserService) {
        this.authUserService = authUserService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserDTO user = authUserService.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }

        // 根据用户类型添加角色
        List<String> authorities = new ArrayList<>();
        authorities.add("ROLE_" + user.getUserType());
        
        // 添加基础用户角色
        authorities.add("ROLE_USER");

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                AuthorityUtils.createAuthorityList(authorities.toArray(new String[0]))
        );
    }
}
