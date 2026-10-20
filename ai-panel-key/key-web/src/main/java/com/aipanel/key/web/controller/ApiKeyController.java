package com.aipanel.key.web.controller;

import com.aipanel.common.constant.CommonConstants;
import com.aipanel.common.domain.PageResult;
import com.aipanel.common.domain.R;
import com.aipanel.key.api.dto.request.CreateApiKeyRequest;
import com.aipanel.key.api.dto.request.UpdateApiKeyRequest;
import com.aipanel.key.api.dto.request.ValidateApiKeyRequest;
import com.aipanel.key.api.dto.response.ApiKeyResponse;
import com.aipanel.key.api.dto.response.ApiKeyValidationResponse;
import com.aipanel.key.api.dto.response.QuotaUsageResponse;
import com.aipanel.key.core.service.ApiKeyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * API Key management controller
 */
@RestController
@RequestMapping("/api/v1/keys")
@RequiredArgsConstructor
@Tag(name = "API Key Management", description = "API Key CRUD operations")
@Slf4j
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    @PostMapping
    @Operation(summary = "Create API Key")
    public R<ApiKeyResponse> createApiKey(
            @RequestBody @Valid CreateApiKeyRequest request,
            @RequestHeader(value = CommonConstants.HEADER_USER_ID, required = false) Long userId) {
        if (userId == null) {
            return R.fail(401, "Not authenticated");
        }
        log.info("Creating API Key: userId={}, name={}", userId, request.getName());
        ApiKeyResponse response = apiKeyService.createApiKey(userId, request);
        return R.ok(response);
    }

    @GetMapping
    @Operation(summary = "List API Keys")
    public R<PageResult<ApiKeyResponse>> listApiKeys(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestHeader(value = CommonConstants.HEADER_USER_ID, required = false) Long userId) {
        if (userId == null) {
            return R.fail(401, "Not authenticated");
        }
        PageResult<ApiKeyResponse> result = apiKeyService.listApiKeys(userId, pageNum, pageSize);
        return R.ok(result);
    }

    @GetMapping("/{keyId}")
    @Operation(summary = "Get API Key")
    public R<ApiKeyResponse> getApiKey(
            @Parameter(description = "API Key ID", required = true) @PathVariable String keyId,
            @RequestHeader(value = CommonConstants.HEADER_USER_ID, required = false) Long userId) {
        if (userId == null) {
            return R.fail(401, "Not authenticated");
        }
        ApiKeyResponse response = apiKeyService.getApiKey(userId, keyId);
        return R.ok(response);
    }

    @PutMapping("/{keyId}")
    @Operation(summary = "Update API Key")
    public R<ApiKeyResponse> updateApiKey(
            @Parameter(description = "API Key ID", required = true) @PathVariable String keyId,
            @RequestBody @Valid UpdateApiKeyRequest request,
            @RequestHeader(value = CommonConstants.HEADER_USER_ID, required = false) Long userId) {
        if (userId == null) {
            return R.fail(401, "Not authenticated");
        }
        log.info("Updating API Key: userId={}, keyId={}", userId, keyId);
        ApiKeyResponse response = apiKeyService.updateApiKey(userId, keyId, request);
        return R.ok(response);
    }

    @DeleteMapping("/{keyId}")
    @Operation(summary = "Delete API Key")
    public R<Void> deleteApiKey(
            @Parameter(description = "API Key ID", required = true) @PathVariable String keyId,
            @RequestHeader(value = CommonConstants.HEADER_USER_ID, required = false) Long userId) {
        if (userId == null) {
            return R.fail(401, "Not authenticated");
        }
        log.info("Deleting API Key: userId={}, keyId={}", userId, keyId);
        apiKeyService.deleteApiKey(userId, keyId);
        return R.ok();
    }

    @PostMapping("/{keyId}/regenerate")
    @Operation(summary = "Regenerate API Key")
    public R<ApiKeyResponse> regenerateApiKey(
            @Parameter(description = "API Key ID", required = true) @PathVariable String keyId,
            @RequestHeader(value = CommonConstants.HEADER_USER_ID, required = false) Long userId) {
        if (userId == null) {
            return R.fail(401, "Not authenticated");
        }
        log.info("Regenerating API Key: userId={}, keyId={}", userId, keyId);
        ApiKeyResponse response = apiKeyService.regenerateApiKey(userId, keyId);
        return R.ok(response);
    }

    @GetMapping("/{keyId}/usage")
    @Operation(summary = "Get Key Usage")
    public R<QuotaUsageResponse> getKeyUsage(
            @Parameter(description = "API Key ID", required = true) @PathVariable String keyId,
            @RequestHeader(value = CommonConstants.HEADER_USER_ID, required = false) Long userId) {
        if (userId == null) {
            return R.fail(401, "Not authenticated");
        }
        QuotaUsageResponse response = apiKeyService.getKeyUsage(userId, keyId);
        return R.ok(response);
    }

    @PostMapping("/internal/validate")
    @Operation(summary = "Validate API Key (Internal)")
    public R<ApiKeyValidationResponse> validateApiKeyInternal(@RequestBody @Valid ValidateApiKeyRequest request) {
        ApiKeyValidationResponse response = apiKeyService.validateApiKey(request.getApiKey(), request.getModel());
        return R.ok(response);
    }

    @GetMapping("/validate")
    @Operation(summary = "Validate API Key")
    public R<ApiKeyValidationResponse> validateApiKey(
            @Parameter(description = "API Key", required = true) @RequestParam String key,
            @Parameter(description = "Model to access") @RequestParam(required = false) String model) {
        ApiKeyValidationResponse response = apiKeyService.validateApiKey(key, model);
        return R.ok(response);
    }

    @PostMapping("/ensure-default")
    @Operation(summary = "Ensure user has default API Key", description = "Creates an API key for the user if they don't have one, returns existing key otherwise")
    public R<ApiKeyResponse> ensureDefaultApiKey(
            @RequestHeader(value = CommonConstants.HEADER_USER_ID, required = false) Long userId) {
        if (userId == null) {
            return R.fail(401, "Not authenticated");
        }
        log.info("Ensuring default API Key for user: {}", userId);
        ApiKeyResponse response = apiKeyService.ensureDefaultApiKey(userId);
        return R.ok(response);
    }
}
