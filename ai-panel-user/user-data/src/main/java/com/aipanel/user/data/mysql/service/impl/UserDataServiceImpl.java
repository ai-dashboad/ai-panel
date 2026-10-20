package com.aipanel.user.data.mysql.service.impl;

import com.aipanel.user.data.mysql.mapper.UserMapper;
import com.aipanel.user.data.mysql.po.UserPO;
import com.aipanel.user.data.mysql.service.UserDataService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * User data service implementation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserDataServiceImpl extends ServiceImpl<UserMapper, UserPO> implements UserDataService {

    private final UserMapper userMapper;

    @Override
    public UserPO findByUsername(String username) {
        return userMapper.findByUsername(username);
    }

    @Override
    public UserPO findByEmail(String email) {
        return userMapper.findByEmail(email);
    }

    @Override
    public UserPO findByUsernameOrEmail(String usernameOrEmail) {
        return userMapper.findByUsernameOrEmail(usernameOrEmail);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userMapper.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userMapper.existsByEmail(email);
    }

    @Override
    public void updateLastLogin(Long userId, LocalDateTime lastLoginAt) {
        userMapper.updateLastLogin(userId, lastLoginAt);
    }
}
