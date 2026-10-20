package com.aipanel.user.data.mysql.service.impl;

import com.aipanel.user.data.mysql.mapper.UserPlanMapper;
import com.aipanel.user.data.mysql.po.UserPlanPO;
import com.aipanel.user.data.mysql.service.UserPlanDataService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * User plan data service implementation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserPlanDataServiceImpl extends ServiceImpl<UserPlanMapper, UserPlanPO> implements UserPlanDataService {

    private final UserPlanMapper userPlanMapper;

    @Override
    public UserPlanPO findActiveByUserId(Long userId) {
        return userPlanMapper.findActiveByUserId(userId);
    }

    @Override
    public List<UserPlanPO> findAllByUserId(Long userId) {
        return userPlanMapper.findAllByUserId(userId);
    }
}
