package com.cloud.user.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cloud.common.domain.dto.user.AdminDTO;
import com.cloud.user.exception.AdminException;
import com.cloud.user.module.entity.Admin;

import java.util.List;








public interface AdminService extends IService<Admin> {

    

    






    AdminDTO getAdminById(Long id) throws AdminException.AdminNotFoundException;

    






    AdminDTO getAdminByUsername(String username) throws AdminException.AdminNotFoundException;

    





    List<AdminDTO> getAdminsByIds(List<Long> ids);

    







    Page<AdminDTO> getMerchantsPage(Integer page, Integer size, Integer status);

    






    Page<AdminDTO> getAdminsPage(Integer page, Integer size);

    

    






    AdminDTO createAdmin(AdminDTO adminDTO) throws AdminException.AdminAlreadyExistsException;

    






    boolean updateAdmin(AdminDTO adminDTO) throws AdminException.AdminNotFoundException;

    






    boolean deleteAdmin(Long id) throws AdminException.AdminNotFoundException;

    





    boolean batchDeleteAdmins(List<Long> ids);

    

    







    boolean updateAdminStatus(Long id, Integer status) throws AdminException.AdminNotFoundException;

    






    boolean enableAdmin(Long id) throws AdminException.AdminNotFoundException;

    






    boolean disableAdmin(Long id) throws AdminException.AdminNotFoundException;

    

    







    boolean resetPassword(Long id, String newPassword) throws AdminException.AdminNotFoundException;

    









    boolean changePassword(Long id, String oldPassword, String newPassword)
            throws AdminException.AdminNotFoundException, AdminException.AdminPasswordException;

    

    




    void evictAdminCache(Long id);

    


    void evictAllAdminCache();
}
