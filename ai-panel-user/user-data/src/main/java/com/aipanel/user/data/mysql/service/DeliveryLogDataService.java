package com.aipanel.user.data.mysql.service;

import com.aipanel.user.data.mysql.po.DeliveryLogPO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface DeliveryLogDataService extends IService<DeliveryLogPO> {

    List<DeliveryLogPO> findByOrderId(Long orderId);
}
