package com.cloud.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cloud.common.domain.dto.user.UserDTO;
import com.cloud.common.domain.dto.user.UserPageDTO;
import com.cloud.common.domain.vo.user.UserVO;
import com.cloud.common.result.PageResult;
import com.cloud.user.module.entity.User;

import java.util.Collection;
import java.util.List;






public interface UserService extends IService<User> {

    





    UserDTO findByUsername(String username);

    





    PageResult<UserVO> pageQuery(UserPageDTO pageDTO);

    





    boolean deleteUserById(Long id);

    





    boolean deleteUsersByIds(Collection<Long> userIds);

    





    UserDTO getUserById(Long id);

    





    UserDTO getUserByUsername(String username);

    





    List<UserDTO> getUsersByIds(Collection<Long> userIds);

    






    UserDTO registerUser(com.cloud.common.domain.dto.auth.RegisterRequestDTO registerRequest);

    





    String getUserPassword(String username);

    





    UserDTO findByGitHubId(Long githubId);

    





    UserDTO findByGitHubUsername(String githubUsername);

    






    UserDTO findByOAuthProvider(String oauthProvider, String oauthProviderId);

    






    UserDTO createGitHubUser(com.cloud.common.domain.dto.oauth.GitHubUserDTO githubUserDTO);

    







    boolean updateGitHubUserInfo(Long userId, com.cloud.common.domain.dto.oauth.GitHubUserDTO githubUserDTO);

    






    com.baomidou.mybatisplus.extension.plugins.pagination.Page<UserDTO> getUsersPage(Integer page, Integer size);

    





    Long createUser(UserDTO userDTO);

    





    Boolean updateUser(UserDTO userDTO);

    





    Boolean deleteUser(Long id);

    






    Boolean updateUserStatus(Long id, Integer status);

    





    String resetPassword(Long id);

    







    Boolean changePassword(Long id, String oldPassword, String newPassword);

    






    Integer batchUpdateUserStatus(Collection<Long> userIds, Integer status);
}
