package com.aipanel.user.data.mysql.service.impl;

import com.aipanel.user.data.mysql.mapper.DeliveryLogMapper;
import com.aipanel.user.data.mysql.po.DeliveryLogPO;
import com.aipanel.user.data.mysql.service.DeliveryLogDataService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryLogDataServiceImpl extends ServiceImpl<DeliveryLogMapper, DeliveryLogPO> implements DeliveryLogDataService {

    @Override
    public List<DeliveryLogPO> findByOrderId(Long orderId) {
        return list(new LambdaQueryWrapper<DeliveryLogPO>()
                .eq(DeliveryLogPO::getOrderId, orderId)
                .orderByAsc(DeliveryLogPO::getCreatedAt));
    }
}
