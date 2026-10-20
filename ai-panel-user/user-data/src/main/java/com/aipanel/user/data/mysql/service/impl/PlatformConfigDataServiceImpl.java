package com.aipanel.user.data.mysql.service.impl;

import com.aipanel.user.data.mysql.mapper.PlatformConfigMapper;
import com.aipanel.user.data.mysql.po.PlatformConfigPO;
import com.aipanel.user.data.mysql.service.PlatformConfigDataService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlatformConfigDataServiceImpl extends ServiceImpl<PlatformConfigMapper, PlatformConfigPO> implements PlatformConfigDataService {

    @Override
    public PlatformConfigPO findByPlatform(String platform) {
        return getOne(new LambdaQueryWrapper<PlatformConfigPO>()
                .eq(PlatformConfigPO::getPlatform, platform));
    }

    @Override
    public List<PlatformConfigPO> findAllEnabled() {
        return list(new LambdaQueryWrapper<PlatformConfigPO>()
                .eq(PlatformConfigPO::getEnabled, true)
                .orderByAsc(PlatformConfigPO::getId));
    }

    @Override
    public boolean existsByPlatform(String platform) {
        return count(new LambdaQueryWrapper<PlatformConfigPO>()
                .eq(PlatformConfigPO::getPlatform, platform)) > 0;
    }
}
