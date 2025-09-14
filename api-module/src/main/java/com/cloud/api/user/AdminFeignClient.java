package com.cloud.api.user;

import com.cloud.common.domain.dto.user.AdminDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理员服务Feign客户端
 * 用于服务间调用管理员服务的接口
 */
@FeignClient(name = "user-service", path = "/admin", contextId = "admin-feign-client")
public interface AdminFeignClient {

    /**
     * 根据ID查找管理员
     *
     * @param id 管理员ID
     * @return 管理员信息
     */
    @GetMapping("/query/getById/{id}")
    AdminDTO findById(@PathVariable("id") Long id);

    /**
     * 获取所有管理员
     *
     * @return 管理员列表
     */
    @GetMapping("/query/getAll")
    List<AdminDTO> findAll();

    /**
     * 保存管理员信息
     *
     * @param adminDTO 管理员信息
     * @return 保存后的管理员信息
     */
    @PostMapping("/manage/create")
    AdminDTO create(@RequestBody AdminDTO adminDTO);

    /**
     * 更新管理员信息
     *
     * @param id       管理员ID
     * @param adminDTO 管理员信息
     * @return 更新后的管理员信息
     */
    @PutMapping("/manage/update/{id}")
    AdminDTO update(@PathVariable("id") Long id, @RequestBody AdminDTO adminDTO);

    /**
     * 删除管理员
     *
     * @param id 管理员ID
     * @return 是否删除成功
     */
    @DeleteMapping("/manage/delete/{id}")
    Boolean delete(@PathVariable("id") Long id);
}