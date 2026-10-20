package com.aipanel.model.core.service.impl;

import com.aipanel.common.exception.BusinessException;
import com.aipanel.common.enums.ErrorCode;
import com.aipanel.model.api.dto.request.CreateModelRequest;
import com.aipanel.model.api.dto.request.UpdateModelRequest;
import com.aipanel.model.api.dto.response.ModelDetailResponse;
import com.aipanel.model.api.dto.response.ProviderResponse;
import com.aipanel.model.core.service.ModelManagementService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Model Management Service Implementation
 * Note: Data layer will be injected when available
 */
@Service
@Slf4j
public class ModelManagementServiceImpl implements ModelManagementService {

    private final ObjectMapper objectMapper;

    public ModelManagementServiceImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public List<ModelDetailResponse> listModels(String provider, String type) {
        log.info("Listing models: provider={}, type={}", provider, type);
        // Implementation will use data layer
        throw new UnsupportedOperationException("Data layer not yet injected");
    }

    @Override
    public ModelDetailResponse getModel(String modelId) {
        log.info("Getting model: {}", modelId);
        throw new UnsupportedOperationException("Data layer not yet injected");
    }

    @Override
    public ModelDetailResponse createModel(CreateModelRequest request) {
        log.info("Creating model: {}", request.getModelId());
        throw new UnsupportedOperationException("Data layer not yet injected");
    }

    @Override
    public ModelDetailResponse updateModel(String modelId, UpdateModelRequest request) {
        log.info("Updating model: {}", modelId);
        throw new UnsupportedOperationException("Data layer not yet injected");
    }

    @Override
    public void deleteModel(String modelId) {
        log.info("Deleting model: {}", modelId);
        throw new UnsupportedOperationException("Data layer not yet injected");
    }

    @Override
    public List<ProviderResponse> listProviders() {
        log.info("Listing providers");
        throw new UnsupportedOperationException("Data layer not yet injected");
    }

    @Override
    public ProviderResponse getProvider(String providerCode) {
        log.info("Getting provider: {}", providerCode);
        throw new UnsupportedOperationException("Data layer not yet injected");
    }

    @Override
    public void syncModelsFromProvider(String providerCode) {
        log.info("Syncing models from provider: {}", providerCode);
        throw new UnsupportedOperationException("Data layer not yet injected");
    }

    /**
     * Convert features list to JSON string
     */
    protected String featuresToJson(List<String> features) {
        if (features == null || features.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(features);
        } catch (JsonProcessingException e) {
            log.error("Failed to convert features to JSON", e);
            return "[]";
        }
    }

    /**
     * Convert JSON string to features list
     */
    protected List<String> jsonToFeatures(String json) {
        if (json == null || json.isEmpty()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (JsonProcessingException e) {
            log.error("Failed to parse features JSON", e);
            return List.of();
        }
    }
}
