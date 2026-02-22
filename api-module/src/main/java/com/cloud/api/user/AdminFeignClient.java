package com.cloud.api.user;

import com.cloud.common.domain.dto.user.AdminDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;







@FeignClient(name = "user-service", path = "/admin", contextId = "adminFeignClient")
public interface AdminFeignClient {

    





    @GetMapping("/query/getById/{id}")
    AdminDTO findById(@PathVariable("id") Long id);

    




    @GetMapping("/query/getAll")
    List<AdminDTO> findAll();

    





    @PostMapping("/manage/create")
    AdminDTO create(@RequestBody AdminDTO adminDTO);

    






    @PutMapping("/manage/update/{id}")
    AdminDTO update(@PathVariable("id") Long id, @RequestBody AdminDTO adminDTO);

    





    @DeleteMapping("/manage/delete/{id}")
    Boolean delete(@PathVariable("id") Long id);
}
