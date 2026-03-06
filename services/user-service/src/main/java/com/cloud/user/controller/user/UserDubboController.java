package com.cloud.user.controller.user;

import com.cloud.api.user.UserDubboApi;
import com.cloud.common.domain.dto.user.UserDTO;
import com.cloud.common.domain.dto.user.UserProfileUpsertDTO;
import com.cloud.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/internal/user")
@RequiredArgsConstructor
public class UserDubboController implements UserDubboApi {

    private final UserService userService;

    @Override
    @GetMapping("/id/{id}")
    public UserDTO findById(@PathVariable("id") Long id) {
        return userService.getProfileById(id);
    }

    @Override
    @PostMapping("/create")
    public Long create(@RequestBody UserProfileUpsertDTO profileUpsertDTO) {
        return userService.createProfile(profileUpsertDTO);
    }

    @Override
    @PutMapping("/update")
    public Boolean update(@RequestBody UserProfileUpsertDTO profileUpsertDTO) {
        return userService.updateProfile(profileUpsertDTO);
    }
}

