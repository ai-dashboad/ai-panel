package com.aipanel.channel.web.controller;

import com.aipanel.channel.api.dto.request.ProviderApiKeyRequest;
import com.aipanel.channel.api.dto.request.ProviderConfigRequest;
import com.aipanel.channel.api.dto.response.ProviderApiKeyResponse;
import com.aipanel.channel.api.dto.response.ProviderConfigResponse;
import com.aipanel.channel.core.pool.ApiKeyPool;
import com.aipanel.channel.core.pool.DynamicProviderFactory;
import com.aipanel.channel.core.service.ProviderConfigService;
import com.aipanel.common.domain.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Provider Configuration Management API
 */
@Tag(name = "Provider Configuration", description = "Manage AI providers and API keys")
@RestController
@RequestMapping("/api/v1/providers")
@RequiredArgsConstructor
public class ProviderConfigController {

    private final ProviderConfigService providerConfigService;
    private final DynamicProviderFactory dynamicProviderFactory;
    private final ApiKeyPool apiKeyPool;

    // ==================== Provider Config ====================

    @Operation(summary = "List all providers")
    @GetMapping
    public R<List<ProviderConfigResponse>> listProviders() {
        return R.ok(providerConfigService.listProviders());
    }

    @Operation(summary = "Get provider by ID")
    @GetMapping("/{id}")
    public R<ProviderConfigResponse> getProvider(@PathVariable Long id) {
        return R.ok(providerConfigService.getProvider(id));
    }

    @Operation(summary = "Create a new provider")
    @PostMapping
    public R<ProviderConfigResponse> createProvider(@Valid @RequestBody ProviderConfigRequest request) {
        return R.ok(providerConfigService.createProvider(request));
    }

    @Operation(summary = "Update a provider")
    @PutMapping("/{id}")
    public R<ProviderConfigResponse> updateProvider(
            @PathVariable Long id,
            @Valid @RequestBody ProviderConfigRequest request) {
        return R.ok(providerConfigService.updateProvider(id, request));
    }

    @Operation(summary = "Delete a provider")
    @DeleteMapping("/{id}")
    public R<Void> deleteProvider(@PathVariable Long id) {
        providerConfigService.deleteProvider(id);
        return R.ok();
    }

    // ==================== API Keys ====================

    @Operation(summary = "List API keys")
    @GetMapping("/keys")
    public R<List<ProviderApiKeyResponse>> listApiKeys(
            @RequestParam(required = false) Long providerId) {
        return R.ok(providerConfigService.listApiKeys(providerId));
    }

    @Operation(summary = "Get API key by ID")
    @GetMapping("/keys/{id}")
    public R<ProviderApiKeyResponse> getApiKey(@PathVariable Long id) {
        return R.ok(providerConfigService.getApiKey(id));
    }

    @Operation(summary = "Create a new API key")
    @PostMapping("/keys")
    public R<ProviderApiKeyResponse> createApiKey(@Valid @RequestBody ProviderApiKeyRequest request) {
        return R.ok(providerConfigService.createApiKey(request));
    }

    @Operation(summary = "Update an API key")
    @PutMapping("/keys/{id}")
    public R<ProviderApiKeyResponse> updateApiKey(
            @PathVariable Long id,
            @Valid @RequestBody ProviderApiKeyRequest request) {
        return R.ok(providerConfigService.updateApiKey(id, request));
    }

    @Operation(summary = "Delete an API key")
    @DeleteMapping("/keys/{id}")
    public R<Void> deleteApiKey(@PathVariable Long id) {
        providerConfigService.deleteApiKey(id);
        return R.ok();
    }

    @Operation(summary = "Reset API key status (clear errors)")
    @PostMapping("/keys/{id}/reset")
    public R<Void> resetApiKeyStatus(@PathVariable Long id) {
        providerConfigService.resetApiKeyStatus(id);
        return R.ok();
    }

    // ==================== Status & Statistics ====================

    @Operation(summary = "Get providers status and statistics")
    @GetMapping("/status")
    public R<Map<String, Object>> getProvidersStatus() {
        return R.ok(dynamicProviderFactory.getProvidersStatus());
    }

    @Operation(summary = "Reload key pool from database")
    @PostMapping("/reload")
    public R<Void> reloadKeyPool() {
        apiKeyPool.reload();
        return R.ok();
    }

    // ==================== Quick Setup (傻瓜式配置) ====================

    @Operation(summary = "Validate a provider API key")
    @PostMapping("/validate-key")
    public R<ProviderConfigService.ValidateKeyResult> validateKey(@RequestBody Map<String, String> request) {
        String type = request.get("type");
        String apiKey = request.get("apiKey");
        return R.ok(providerConfigService.validateKey(type, apiKey));
    }

    @Operation(summary = "Quick setup: create provider + key with preset config")
    @PostMapping("/quick-setup")
    public R<ProviderConfigResponse> quickSetup(@RequestBody Map<String, String> request) {
        String type = request.get("type");
        String apiKey = request.get("apiKey");
        return R.ok(providerConfigService.quickSetup(type, apiKey));
    }

    @Operation(summary = "Batch setup multiple providers")
    @PostMapping("/batch-setup")
    public R<List<ProviderConfigResponse>> batchSetup(@RequestBody Map<String, List<ProviderConfigService.ProviderSetupRequest>> request) {
        List<ProviderConfigService.ProviderSetupRequest> providers = request.get("providers");
        return R.ok(providerConfigService.batchSetup(providers));
    }
}
