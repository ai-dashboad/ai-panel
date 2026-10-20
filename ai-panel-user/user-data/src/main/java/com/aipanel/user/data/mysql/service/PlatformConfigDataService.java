package com.aipanel.user.data.mysql.service;

import com.aipanel.user.data.mysql.po.PlatformConfigPO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface PlatformConfigDataService extends IService<PlatformConfigPO> {

    PlatformConfigPO findByPlatform(String platform);

    List<PlatformConfigPO> findAllEnabled();

    boolean existsByPlatform(String platform);
}
