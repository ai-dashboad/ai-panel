package com.aipanel.model.core.service;

import com.aipanel.model.api.dto.request.CreateModelRequest;
import com.aipanel.model.api.dto.request.UpdateModelRequest;
import com.aipanel.model.api.dto.response.ModelDetailResponse;
import com.aipanel.model.api.dto.response.ProviderResponse;

import java.util.List;

/**
 * Model Management Service Interface
 */
public interface ModelManagementService {

    /**
     * List all models
     */
    List<ModelDetailResponse> listModels(String provider, String type);

    /**
     * Get model by ID
     */
    ModelDetailResponse getModel(String modelId);

    /**
     * Create a new model
     */
    ModelDetailResponse createModel(CreateModelRequest request);

    /**
     * Update model
     */
    ModelDetailResponse updateModel(String modelId, UpdateModelRequest request);

    /**
     * Delete model
     */
    void deleteModel(String modelId);

    /**
     * List all providers
     */
    List<ProviderResponse> listProviders();

    /**
     * Get provider by code
     */
    ProviderResponse getProvider(String providerCode);

    /**
     * Sync models from provider
     */
    void syncModelsFromProvider(String providerCode);
}
