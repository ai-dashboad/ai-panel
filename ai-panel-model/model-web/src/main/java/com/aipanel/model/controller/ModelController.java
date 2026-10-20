package com.aipanel.model.controller;

import com.aipanel.common.domain.R;
import com.aipanel.model.api.dto.request.CreateModelRequest;
import com.aipanel.model.api.dto.request.UpdateModelRequest;
import com.aipanel.model.api.dto.response.ModelDetailResponse;
import com.aipanel.model.api.dto.response.ProviderResponse;
import com.aipanel.model.core.service.ModelManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Model Management Controller
 */
@RestController
@RequestMapping("/api/v1/models")
@Tag(name = "Model Management", description = "AI Model Management API")
@Slf4j
@RequiredArgsConstructor
public class ModelController {

    private final ModelManagementService modelManagementService;

    @GetMapping
    @Operation(summary = "List models", description = "List all available models")
    public R<List<ModelDetailResponse>> listModels(
            @Parameter(description = "Filter by provider") @RequestParam(required = false) String provider,
            @Parameter(description = "Filter by type") @RequestParam(required = false) String type
    ) {
        return R.ok(modelManagementService.listModels(provider, type));
    }

    @GetMapping("/{modelId}")
    @Operation(summary = "Get model", description = "Get model by ID")
    public R<ModelDetailResponse> getModel(
            @Parameter(description = "Model ID") @PathVariable String modelId
    ) {
        return R.ok(modelManagementService.getModel(modelId));
    }

    @PostMapping
    @Operation(summary = "Create model", description = "Create a new model")
    public R<ModelDetailResponse> createModel(
            @Valid @RequestBody CreateModelRequest request
    ) {
        return R.ok(modelManagementService.createModel(request));
    }

    @PutMapping("/{modelId}")
    @Operation(summary = "Update model", description = "Update model configuration")
    public R<ModelDetailResponse> updateModel(
            @Parameter(description = "Model ID") @PathVariable String modelId,
            @Valid @RequestBody UpdateModelRequest request
    ) {
        return R.ok(modelManagementService.updateModel(modelId, request));
    }

    @DeleteMapping("/{modelId}")
    @Operation(summary = "Delete model", description = "Delete a model")
    public R<Void> deleteModel(
            @Parameter(description = "Model ID") @PathVariable String modelId
    ) {
        modelManagementService.deleteModel(modelId);
        return R.ok();
    }

    @GetMapping("/providers")
    @Operation(summary = "List providers", description = "List all providers")
    public R<List<ProviderResponse>> listProviders() {
        return R.ok(modelManagementService.listProviders());
    }

    @GetMapping("/providers/{providerCode}")
    @Operation(summary = "Get provider", description = "Get provider by code")
    public R<ProviderResponse> getProvider(
            @Parameter(description = "Provider code") @PathVariable String providerCode
    ) {
        return R.ok(modelManagementService.getProvider(providerCode));
    }

    @PostMapping("/providers/{providerCode}/sync")
    @Operation(summary = "Sync models", description = "Sync models from provider")
    public R<String> syncModels(
            @Parameter(description = "Provider code") @PathVariable String providerCode
    ) {
        modelManagementService.syncModelsFromProvider(providerCode);
        return R.ok("Sync started");
    }
}
