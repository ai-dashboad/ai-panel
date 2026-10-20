package com.aipanel.channel.api;

import com.aipanel.channel.api.dto.request.CreateChannelRequest;
import com.aipanel.channel.api.dto.request.GenerateApiKeysRequest;
import com.aipanel.channel.api.dto.response.ChannelResponse;
import com.aipanel.channel.api.dto.response.GeneratedKeyResponse;
import com.aipanel.common.domain.PageResult;
import com.aipanel.common.domain.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Channel Service Feign Client
 */
@Tag(name = "Channel Management", description = "Sales channel and API key management")
@FeignClient(name = "ai-panel-channel")
public interface ChannelApi {

    @PostMapping("/api/v1/channels")
    @Operation(summary = "Create Channel", description = "Create a new sales channel")
    R<ChannelResponse> createChannel(@RequestBody @Valid CreateChannelRequest request);

    @GetMapping("/api/v1/channels")
    @Operation(summary = "List Channels", description = "List all sales channels")
    R<PageResult<ChannelResponse>> listChannels(
        @Parameter(description = "Page number") @RequestParam(defaultValue = "1") Integer pageNum,
        @Parameter(description = "Page size") @RequestParam(defaultValue = "20") Integer pageSize);

    @GetMapping("/api/v1/channels/{channelId}")
    @Operation(summary = "Get Channel", description = "Get channel details by ID")
    R<ChannelResponse> getChannel(
        @Parameter(description = "Channel ID", required = true) @PathVariable Long channelId);

    @PutMapping("/api/v1/channels/{channelId}")
    @Operation(summary = "Update Channel", description = "Update channel settings")
    R<ChannelResponse> updateChannel(
        @Parameter(description = "Channel ID", required = true) @PathVariable Long channelId,
        @RequestBody @Valid CreateChannelRequest request);

    @DeleteMapping("/api/v1/channels/{channelId}")
    @Operation(summary = "Delete Channel", description = "Delete a sales channel")
    R<Void> deleteChannel(
        @Parameter(description = "Channel ID", required = true) @PathVariable Long channelId);

    @PostMapping("/api/v1/channels/{channelId}/enable")
    @Operation(summary = "Enable Channel", description = "Enable a sales channel")
    R<Void> enableChannel(
        @Parameter(description = "Channel ID", required = true) @PathVariable Long channelId);

    @PostMapping("/api/v1/channels/{channelId}/disable")
    @Operation(summary = "Disable Channel", description = "Disable a sales channel")
    R<Void> disableChannel(
        @Parameter(description = "Channel ID", required = true) @PathVariable Long channelId);

    @PostMapping("/api/v1/channels/keys/generate")
    @Operation(summary = "Generate API Keys", description = "Batch generate API keys for a channel")
    R<List<GeneratedKeyResponse>> generateApiKeys(@RequestBody @Valid GenerateApiKeysRequest request);

    @GetMapping("/api/v1/channels/{channelId}/keys")
    @Operation(summary = "List Channel Keys", description = "List all API keys for a channel")
    R<PageResult<GeneratedKeyResponse>> listChannelKeys(
        @Parameter(description = "Channel ID", required = true) @PathVariable Long channelId,
        @Parameter(description = "Page number") @RequestParam(defaultValue = "1") Integer pageNum,
        @Parameter(description = "Page size") @RequestParam(defaultValue = "20") Integer pageSize);
}
