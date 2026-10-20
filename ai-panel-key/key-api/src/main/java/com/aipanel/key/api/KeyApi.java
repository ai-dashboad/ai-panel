package com.aipanel.key.api;

import com.aipanel.common.domain.PageResult;
import com.aipanel.common.domain.R;
import com.aipanel.key.api.dto.request.CreateApiKeyRequest;
import com.aipanel.key.api.dto.request.UpdateApiKeyRequest;
import com.aipanel.key.api.dto.request.ValidateApiKeyRequest;
import com.aipanel.key.api.dto.response.ApiKeyResponse;
import com.aipanel.key.api.dto.response.ApiKeyValidationResponse;
import com.aipanel.key.api.dto.response.QuotaUsageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * API Key Service Feign Client
 */
@Tag(name = "API Key Management", description = "API Key CRUD and validation")
@FeignClient(name = "ai-panel-key")
public interface KeyApi {

    @PostMapping("/api/v1/keys")
    @Operation(summary = "Create API Key", description = "Create a new API Key for the authenticated user")
    R<ApiKeyResponse> createApiKey(@RequestBody @Valid CreateApiKeyRequest request);

    @GetMapping("/api/v1/keys")
    @Operation(summary = "List API Keys", description = "List all API Keys for the authenticated user")
    R<PageResult<ApiKeyResponse>> listApiKeys(
        @Parameter(description = "Page number") @RequestParam(defaultValue = "1") Integer pageNum,
        @Parameter(description = "Page size") @RequestParam(defaultValue = "20") Integer pageSize);

    @GetMapping("/api/v1/keys/{keyId}")
    @Operation(summary = "Get API Key", description = "Get API Key details by ID")
    R<ApiKeyResponse> getApiKey(
        @Parameter(description = "API Key ID", required = true) @PathVariable String keyId);

    @PutMapping("/api/v1/keys/{keyId}")
    @Operation(summary = "Update API Key", description = "Update API Key settings")
    R<ApiKeyResponse> updateApiKey(
        @Parameter(description = "API Key ID", required = true) @PathVariable String keyId,
        @RequestBody @Valid UpdateApiKeyRequest request);

    @DeleteMapping("/api/v1/keys/{keyId}")
    @Operation(summary = "Delete API Key", description = "Delete an API Key")
    R<Void> deleteApiKey(
        @Parameter(description = "API Key ID", required = true) @PathVariable String keyId);

    @PostMapping("/api/v1/keys/{keyId}/regenerate")
    @Operation(summary = "Regenerate API Key", description = "Regenerate the API Key secret")
    R<ApiKeyResponse> regenerateApiKey(
        @Parameter(description = "API Key ID", required = true) @PathVariable String keyId);

    @GetMapping("/api/v1/keys/{keyId}/usage")
    @Operation(summary = "Get Key Usage", description = "Get usage statistics for an API Key")
    R<QuotaUsageResponse> getKeyUsage(
        @Parameter(description = "API Key ID", required = true) @PathVariable String keyId);

    // Internal APIs for other services
    @PostMapping("/api/v1/keys/internal/validate")
    @Operation(summary = "Validate API Key (Internal)", description = "Internal API for validating API Keys")
    R<ApiKeyValidationResponse> validateApiKey(@RequestBody @Valid ValidateApiKeyRequest request);
}
