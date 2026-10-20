package com.aipanel.user.data.mysql.service;

import com.aipanel.user.data.mysql.po.UserPlanPO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * User plan data service interface
 */
public interface UserPlanDataService extends IService<UserPlanPO> {

    UserPlanPO findActiveByUserId(Long userId);

    List<UserPlanPO> findAllByUserId(Long userId);
}
