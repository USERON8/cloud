package com.cloud.user.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cloud.common.domain.dto.user.MerchantDTO;
import com.cloud.user.exception.MerchantException;
import com.cloud.user.module.entity.Merchant;

import java.util.List;








public interface MerchantService extends IService<Merchant> {

    

    






    MerchantDTO getMerchantById(Long id) throws MerchantException.MerchantNotFoundException;

    






    MerchantDTO getMerchantByUsername(String username) throws MerchantException.MerchantNotFoundException;

    






    MerchantDTO getMerchantByName(String merchantName) throws MerchantException.MerchantNotFoundException;

    





    List<MerchantDTO> getMerchantsByIds(List<Long> ids);

    







    Page<MerchantDTO> getMerchantsPage(Integer page, Integer size, Integer status);

    

    






    MerchantDTO createMerchant(MerchantDTO merchantDTO) throws MerchantException.MerchantAlreadyExistsException;

    






    boolean updateMerchant(MerchantDTO merchantDTO) throws MerchantException.MerchantNotFoundException;

    






    boolean deleteMerchant(Long id) throws MerchantException.MerchantNotFoundException;

    





    boolean batchDeleteMerchants(List<Long> ids);

    

    







    boolean updateMerchantStatus(Long id, Integer status) throws MerchantException.MerchantNotFoundException;

    






    boolean enableMerchant(Long id) throws MerchantException.MerchantNotFoundException;

    






    boolean disableMerchant(Long id) throws MerchantException.MerchantNotFoundException;

    

    








    boolean approveMerchant(Long id, String remark)
            throws MerchantException.MerchantNotFoundException, MerchantException.MerchantAuditException;

    








    boolean rejectMerchant(Long id, String reason)
            throws MerchantException.MerchantNotFoundException, MerchantException.MerchantAuditException;

    

    






    Object getMerchantStatistics(Long id) throws MerchantException.MerchantNotFoundException;

    

    




    void evictMerchantCache(Long id);

    


    void evictAllMerchantCache();
}
