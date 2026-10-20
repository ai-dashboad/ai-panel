package com.aipanel.dispatch.api;

import com.aipanel.common.domain.R;
import com.aipanel.dispatch.api.dto.request.ChatRequest;
import com.aipanel.dispatch.api.dto.response.ChatResponse;
import com.aipanel.dispatch.api.dto.response.ModelInfoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * Model Dispatch Service Feign Client
 */
@Tag(name = "Model Dispatch", description = "AI model dispatch and routing")
@FeignClient(name = "ai-panel-dispatch")
public interface DispatchApi {

    @PostMapping("/api/v1/chat/completions")
    @Operation(summary = "Chat Completions", description = "Send chat request to AI model (OpenAI compatible)")
    R<ChatResponse> chat(@RequestBody @Valid ChatRequest request);

    @GetMapping("/api/v1/models")
    @Operation(summary = "List Models", description = "Get available AI models")
    R<List<ModelInfoResponse>> listModels();
}
