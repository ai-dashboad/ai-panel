package com.aipanel.user.core.service;

import com.aipanel.common.domain.PageResult;
import com.aipanel.user.api.dto.response.DeliveryLogResponse;
import com.aipanel.user.api.dto.response.ExternalOrderResponse;

import java.util.List;

public interface ExternalOrderService {

    ExternalOrderResponse getById(Long id);

    ExternalOrderResponse getByPlatformAndExternalOrderId(String platform, String externalOrderId);

    PageResult<ExternalOrderResponse> listPage(int pageNum, int pageSize, String platform, String status, String keyword);

    List<ExternalOrderResponse> listByUserId(Long userId);

    List<DeliveryLogResponse> getDeliveryLogs(Long orderId);

    void retryDelivery(Long orderId);

    void cancelOrder(Long orderId);
}
