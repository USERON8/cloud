package com.cloud.user.service;

import com.cloud.common.domain.dto.user.UserDTO;
import com.cloud.common.domain.vo.user.UserVO;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;







public interface UserAsyncService {

    






    CompletableFuture<List<UserDTO>> getUsersByIdsAsync(Collection<Long> userIds);

    





    CompletableFuture<List<UserVO>> getUserVOsByIdsAsync(Collection<Long> userIds);

    





    CompletableFuture<UserDTO> getUserByIdAsync(Long userId);

    





    CompletableFuture<Map<String, Boolean>> checkUsernamesExistAsync(List<String> usernames);

    





    CompletableFuture<Map<Long, Boolean>> checkUsersActiveAsync(Collection<Long> userIds);

    





    CompletableFuture<Boolean> updateLastLoginTimeAsync(Collection<Long> userIds);

    





    CompletableFuture<Void> refreshUserCacheAsync(Long userId);

    





    CompletableFuture<Void> refreshUserCacheAsync(Collection<Long> userIds);

    





    CompletableFuture<Integer> preloadPopularUsersAsync(Integer limit);

    




    CompletableFuture<Long> countUsersAsync();

    





    CompletableFuture<Long> countActiveUsersAsync(Integer days);

    





    CompletableFuture<Map<String, Long>> getUserGrowthTrendAsync(Integer days);
}
