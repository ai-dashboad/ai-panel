package com.aipanel.user.web.controller;

import com.aipanel.common.domain.R;
import com.aipanel.user.api.dto.request.PlatformConfigRequest;
import com.aipanel.user.api.dto.response.PlatformConfigResponse;
import com.aipanel.user.core.service.PlatformConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/platforms")
@RequiredArgsConstructor
@Tag(name = "Platform Config Management", description = "Admin endpoints for managing platform configurations")
@Slf4j
public class PlatformConfigController {

    private final PlatformConfigService platformConfigService;

    @GetMapping
    @Operation(summary = "List all platform configurations")
    public R<List<PlatformConfigResponse>> listPlatforms() {
        List<PlatformConfigResponse> platforms = platformConfigService.listAll();
        return R.ok(platforms);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get platform configuration by ID")
    public R<PlatformConfigResponse> getPlatform(
            @Parameter(description = "Platform config ID", required = true)
            @PathVariable Long id) {
        PlatformConfigResponse platform = platformConfigService.getById(id);
        return R.ok(platform);
    }

    @PostMapping
    @Operation(summary = "Create platform configuration")
    public R<PlatformConfigResponse> createPlatform(
            @RequestBody @Valid PlatformConfigRequest request) {
        PlatformConfigResponse platform = platformConfigService.create(request);
        return R.ok(platform);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update platform configuration")
    public R<PlatformConfigResponse> updatePlatform(
            @Parameter(description = "Platform config ID", required = true)
            @PathVariable Long id,
            @RequestBody @Valid PlatformConfigRequest request) {
        PlatformConfigResponse platform = platformConfigService.update(id, request);
        return R.ok(platform);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete platform configuration")
    public R<Void> deletePlatform(
            @Parameter(description = "Platform config ID", required = true)
            @PathVariable Long id) {
        platformConfigService.delete(id);
        return R.ok();
    }

    @GetMapping("/webhook-url/{platform}")
    @Operation(summary = "Get webhook URL for a platform")
    public R<String> getWebhookUrl(
            @Parameter(description = "Platform identifier", required = true)
            @PathVariable String platform) {
        String webhookUrl = platformConfigService.getWebhookUrl(platform);
        return R.ok(webhookUrl);
    }
}
