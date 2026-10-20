package com.aipanel.user.core.service;

import com.aipanel.user.api.dto.request.PlatformConfigRequest;
import com.aipanel.user.api.dto.response.PlatformConfigResponse;

import java.util.List;

public interface PlatformConfigService {

    PlatformConfigResponse create(PlatformConfigRequest request);

    PlatformConfigResponse update(Long id, PlatformConfigRequest request);

    void delete(Long id);

    PlatformConfigResponse getById(Long id);

    PlatformConfigResponse getByPlatform(String platform);

    List<PlatformConfigResponse> listAll();

    List<PlatformConfigResponse> listEnabled();

    String getWebhookUrl(String platform);
}
