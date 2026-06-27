package com.cloud.user.config;

import com.cloud.common.security.PasswordEncoderFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordEncoderConfig {

  @Bean
  public PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactory.createDelegatingPasswordEncoder();
  }
}
