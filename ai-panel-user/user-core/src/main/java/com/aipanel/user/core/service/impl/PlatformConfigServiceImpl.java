package com.aipanel.user.core.service.impl;

import com.aipanel.common.enums.ErrorCode;
import com.aipanel.common.exception.BusinessException;
import com.aipanel.user.api.dto.request.PlatformConfigRequest;
import com.aipanel.user.api.dto.response.PlatformConfigResponse;
import com.aipanel.user.core.service.PlatformConfigService;
import com.aipanel.user.data.mysql.po.PlatformConfigPO;
import com.aipanel.user.data.mysql.service.PlatformConfigDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlatformConfigServiceImpl implements PlatformConfigService {

    private final PlatformConfigDataService platformConfigDataService;

    @Value("${app.webhook.base-url:http://localhost:8080}")
    private String webhookBaseUrl;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlatformConfigResponse create(PlatformConfigRequest request) {
        if (platformConfigDataService.existsByPlatform(request.getPlatform())) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "Platform already exists: " + request.getPlatform());
        }

        PlatformConfigPO config = new PlatformConfigPO();
        copyRequestToEntity(request, config);
        config.setCreatedAt(LocalDateTime.now());
        config.setUpdatedAt(LocalDateTime.now());

        platformConfigDataService.save(config);
        log.info("Platform config created: platform={}, id={}", config.getPlatform(), config.getId());

        return convertToResponse(config);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlatformConfigResponse update(Long id, PlatformConfigRequest request) {
        PlatformConfigPO config = platformConfigDataService.getById(id);
        if (config == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "Platform config not found");
        }

        // Check if platform name is being changed and conflicts
        if (!config.getPlatform().equals(request.getPlatform())) {
            if (platformConfigDataService.existsByPlatform(request.getPlatform())) {
                throw BusinessException.of(ErrorCode.PARAM_ERROR, "Platform already exists: " + request.getPlatform());
            }
        }

        copyRequestToEntity(request, config);
        config.setUpdatedAt(LocalDateTime.now());

        platformConfigDataService.updateById(config);
        log.info("Platform config updated: platform={}, id={}", config.getPlatform(), config.getId());

        return convertToResponse(config);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        PlatformConfigPO config = platformConfigDataService.getById(id);
        if (config == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "Platform config not found");
        }

        platformConfigDataService.removeById(id);
        log.info("Platform config deleted: platform={}, id={}", config.getPlatform(), id);
    }

    @Override
    public PlatformConfigResponse getById(Long id) {
        PlatformConfigPO config = platformConfigDataService.getById(id);
        if (config == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "Platform config not found");
        }
        return convertToResponse(config);
    }

    @Override
    public PlatformConfigResponse getByPlatform(String platform) {
        PlatformConfigPO config = platformConfigDataService.findByPlatform(platform);
        if (config == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "Platform config not found: " + platform);
        }
        return convertToResponse(config);
    }

    @Override
    public List<PlatformConfigResponse> listAll() {
        return platformConfigDataService.list().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<PlatformConfigResponse> listEnabled() {
        return platformConfigDataService.findAllEnabled().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public String getWebhookUrl(String platform) {
        return webhookBaseUrl + "/api/v1/webhook/" + platform;
    }

    private void copyRequestToEntity(PlatformConfigRequest request, PlatformConfigPO config) {
        config.setPlatform(request.getPlatform());
        config.setName(request.getName());
        config.setAppKey(request.getAppKey());
        config.setAppSecret(request.getAppSecret());
        config.setWebhookSecret(request.getWebhookSecret());
        config.setDeliveryTemplate(request.getDeliveryTemplate());
        config.setAutoCreateUser(request.getAutoCreateUser() != null ? request.getAutoCreateUser() : true);
        config.setAutoDelivery(request.getAutoDelivery() != null ? request.getAutoDelivery() : true);
        config.setEnabled(request.getEnabled() != null ? request.getEnabled() : true);
        config.setConfig(request.getConfig());
    }

    private PlatformConfigResponse convertToResponse(PlatformConfigPO config) {
        return PlatformConfigResponse.builder()
                .id(config.getId())
                .platform(config.getPlatform())
                .name(config.getName())
                .appKey(maskSensitive(config.getAppKey()))
                .webhookUrl(getWebhookUrl(config.getPlatform()))
                .deliveryTemplate(config.getDeliveryTemplate())
                .autoCreateUser(config.getAutoCreateUser())
                .autoDelivery(config.getAutoDelivery())
                .enabled(config.getEnabled())
                .config(config.getConfig())
                .createdAt(config.getCreatedAt())
                .updatedAt(config.getUpdatedAt())
                .build();
    }

    private String maskSensitive(String value) {
        if (value == null || value.length() <= 6) {
            return "***";
        }
        return value.substring(0, 3) + "***";
    }
}
