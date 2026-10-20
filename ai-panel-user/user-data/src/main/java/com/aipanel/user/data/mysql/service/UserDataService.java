package com.aipanel.user.data.mysql.service;

import com.aipanel.user.data.mysql.po.UserPO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.time.LocalDateTime;

/**
 * User data service interface
 */
public interface UserDataService extends IService<UserPO> {

    UserPO findByUsername(String username);

    UserPO findByEmail(String email);

    UserPO findByUsernameOrEmail(String usernameOrEmail);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    void updateLastLogin(Long userId, LocalDateTime lastLoginAt);
}
