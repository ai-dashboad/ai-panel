package com.aipanel.channel.web.controller;

import com.aipanel.channel.api.ChannelApi;
import com.aipanel.channel.api.dto.request.CreateChannelRequest;
import com.aipanel.channel.api.dto.request.GenerateApiKeysRequest;
import com.aipanel.channel.api.dto.response.ChannelResponse;
import com.aipanel.channel.api.dto.response.GeneratedKeyResponse;
import com.aipanel.channel.core.service.ApiKeyGeneratorService;
import com.aipanel.channel.core.service.ChannelService;
import com.aipanel.channel.data.mysql.po.ChannelApiKeyPO;
import com.aipanel.channel.data.mysql.service.ChannelApiKeyDataService;
import com.aipanel.common.domain.PageResult;
import com.aipanel.common.domain.R;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Channel management controller
 */
@RestController
@RequestMapping("/api/v1/channels")
@RequiredArgsConstructor
@Tag(name = "Channel Management", description = "Sales channel and API key management")
public class ChannelController implements ChannelApi {

    private final ChannelService channelService;
    private final ApiKeyGeneratorService apiKeyGeneratorService;
    private final ChannelApiKeyDataService keyDataService;

    @Override
    @PostMapping
    @Operation(summary = "Create Channel", description = "Create a new sales channel")
    public R<ChannelResponse> createChannel(@Valid @RequestBody CreateChannelRequest request) {
        return R.ok(channelService.create(request));
    }

    @Override
    @GetMapping
    @Operation(summary = "List Channels", description = "List all sales channels")
    public R<PageResult<ChannelResponse>> listChannels(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return R.ok(channelService.list(pageNum, pageSize));
    }

    @Override
    @GetMapping("/{channelId}")
    @Operation(summary = "Get Channel", description = "Get channel details by ID")
    public R<ChannelResponse> getChannel(@PathVariable Long channelId) {
        return R.ok(channelService.getById(channelId));
    }

    @Override
    @PutMapping("/{channelId}")
    @Operation(summary = "Update Channel", description = "Update channel settings")
    public R<ChannelResponse> updateChannel(@PathVariable Long channelId,
                                             @Valid @RequestBody CreateChannelRequest request) {
        return R.ok(channelService.update(channelId, request));
    }

    @Override
    @DeleteMapping("/{channelId}")
    @Operation(summary = "Delete Channel", description = "Delete a sales channel")
    public R<Void> deleteChannel(@PathVariable Long channelId) {
        channelService.delete(channelId);
        return R.ok();
    }

    @Override
    @PostMapping("/{channelId}/enable")
    @Operation(summary = "Enable Channel", description = "Enable a sales channel")
    public R<Void> enableChannel(@PathVariable Long channelId) {
        channelService.enable(channelId);
        return R.ok();
    }

    @Override
    @PostMapping("/{channelId}/disable")
    @Operation(summary = "Disable Channel", description = "Disable a sales channel")
    public R<Void> disableChannel(@PathVariable Long channelId) {
        channelService.disable(channelId);
        return R.ok();
    }

    @Override
    @PostMapping("/keys/generate")
    @Operation(summary = "Generate API Keys", description = "Batch generate API keys for a channel")
    public R<List<GeneratedKeyResponse>> generateApiKeys(@Valid @RequestBody GenerateApiKeysRequest request) {
        return R.ok(apiKeyGeneratorService.generateKeys(request));
    }

    @Override
    @GetMapping("/{channelId}/keys")
    @Operation(summary = "List Channel Keys", description = "List all API keys for a channel")
    public R<PageResult<GeneratedKeyResponse>> listChannelKeys(
            @PathVariable Long channelId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {

        Page<ChannelApiKeyPO> page = new Page<>(pageNum, pageSize);
        IPage<ChannelApiKeyPO> result = keyDataService.listByChannel(channelId, page);

        PageResult<GeneratedKeyResponse> pageResult = new PageResult<>();
        pageResult.setList(result.getRecords().stream()
            .map(this::toKeyResponse)
            .collect(Collectors.toList()));
        pageResult.setTotal(result.getTotal());
        pageResult.setPageNum((int) result.getCurrent());
        pageResult.setPageSize((int) result.getSize());

        return R.ok(pageResult);
    }

    private GeneratedKeyResponse toKeyResponse(ChannelApiKeyPO key) {
        GeneratedKeyResponse response = new GeneratedKeyResponse();
        BeanUtils.copyProperties(key, response);
        // Don't expose the full API key - only prefix
        response.setApiKey(null);
        return response;
    }
}
