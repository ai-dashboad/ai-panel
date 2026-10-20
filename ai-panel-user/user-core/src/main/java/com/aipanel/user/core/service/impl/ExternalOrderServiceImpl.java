package com.aipanel.user.core.service.impl;

import com.aipanel.common.domain.PageResult;
import com.aipanel.common.enums.ErrorCode;
import com.aipanel.common.exception.BusinessException;
import com.aipanel.user.api.dto.response.DeliveryLogResponse;
import com.aipanel.user.api.dto.response.ExternalOrderResponse;
import com.aipanel.user.core.service.AutoDeliveryService;
import com.aipanel.user.core.service.ExternalOrderService;
import com.aipanel.user.data.mysql.po.DeliveryLogPO;
import com.aipanel.user.data.mysql.po.ExternalOrderPO;
import com.aipanel.user.data.mysql.service.DeliveryLogDataService;
import com.aipanel.user.data.mysql.service.ExternalOrderDataService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalOrderServiceImpl implements ExternalOrderService {

    private final ExternalOrderDataService externalOrderDataService;
    private final DeliveryLogDataService deliveryLogDataService;
    private final AutoDeliveryService autoDeliveryService;

    @Override
    public ExternalOrderResponse getById(Long id) {
        ExternalOrderPO order = externalOrderDataService.getById(id);
        if (order == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "Order not found");
        }
        return convertToResponse(order);
    }

    @Override
    public ExternalOrderResponse getByPlatformAndExternalOrderId(String platform, String externalOrderId) {
        ExternalOrderPO order = externalOrderDataService.findByPlatformAndExternalOrderId(platform, externalOrderId);
        if (order == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "Order not found");
        }
        return convertToResponse(order);
    }

    @Override
    public PageResult<ExternalOrderResponse> listPage(int pageNum, int pageSize, String platform, String status, String keyword) {
        Page<ExternalOrderPO> page = new Page<>(pageNum, pageSize);
        IPage<ExternalOrderPO> result = externalOrderDataService.findPage(page, platform, status, keyword);

        List<ExternalOrderResponse> records = result.getRecords().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        return PageResult.of(records, (int) result.getCurrent(), (int) result.getSize(), result.getTotal());
    }

    @Override
    public List<ExternalOrderResponse> listByUserId(Long userId) {
        return externalOrderDataService.findByUserId(userId).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<DeliveryLogResponse> getDeliveryLogs(Long orderId) {
        return deliveryLogDataService.findByOrderId(orderId).stream()
                .map(this::convertLogToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void retryDelivery(Long orderId) {
        ExternalOrderPO order = externalOrderDataService.getById(orderId);
        if (order == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "Order not found");
        }

        if (!ExternalOrderPO.STATUS_FAILED.equals(order.getStatus())) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "Only failed orders can be retried");
        }

        // Reset status to pending for retry
        order.setStatus(ExternalOrderPO.STATUS_PENDING);
        order.setFailReason(null);
        order.setUpdatedAt(LocalDateTime.now());
        externalOrderDataService.updateById(order);

        // Trigger delivery
        autoDeliveryService.processOrder(orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long orderId) {
        ExternalOrderPO order = externalOrderDataService.getById(orderId);
        if (order == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "Order not found");
        }

        if (ExternalOrderPO.STATUS_DELIVERED.equals(order.getStatus())) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "Cannot cancel delivered orders");
        }

        order.setStatus(ExternalOrderPO.STATUS_CANCELLED);
        order.setUpdatedAt(LocalDateTime.now());
        externalOrderDataService.updateById(order);

        log.info("Order cancelled: id={}, externalOrderId={}", orderId, order.getExternalOrderId());
    }

    private ExternalOrderResponse convertToResponse(ExternalOrderPO order) {
        return ExternalOrderResponse.builder()
                .id(order.getId())
                .platform(order.getPlatform())
                .externalOrderId(order.getExternalOrderId())
                .externalItemId(order.getExternalItemId())
                .sku(order.getSku())
                .buyerNick(order.getBuyerNick())
                .buyerPhone(maskPhone(order.getBuyerPhone()))
                .buyerEmail(order.getBuyerEmail())
                .buyerMessage(order.getBuyerMessage())
                .quantity(order.getQuantity())
                .amount(order.getAmount())
                .userId(order.getUserId())
                .apiKeyId(order.getApiKeyId())
                .status(order.getStatus())
                .deliveryMethod(order.getDeliveryMethod())
                .deliveryContent(order.getDeliveryContent())
                .deliveryTime(order.getDeliveryTime())
                .failReason(order.getFailReason())
                .retryCount(order.getRetryCount())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    private DeliveryLogResponse convertLogToResponse(DeliveryLogPO log) {
        return DeliveryLogResponse.builder()
                .id(log.getId())
                .orderId(log.getOrderId())
                .action(log.getAction())
                .status(log.getStatus())
                .message(log.getMessage())
                .createdAt(log.getCreatedAt())
                .build();
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
