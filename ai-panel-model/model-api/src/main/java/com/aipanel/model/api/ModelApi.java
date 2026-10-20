package com.aipanel.model.api;

import com.aipanel.common.domain.R;
import com.aipanel.model.api.dto.request.CreateModelRequest;
import com.aipanel.model.api.dto.request.UpdateModelRequest;
import com.aipanel.model.api.dto.response.ModelDetailResponse;
import com.aipanel.model.api.dto.response.ProviderResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Model Service Feign API
 */
@FeignClient(name = "model-service", path = "/api/v1/models")
public interface ModelApi {

    @GetMapping
    R<List<ModelDetailResponse>> listModels(
        @RequestParam(required = false) String provider,
        @RequestParam(required = false) String type
    );

    @GetMapping("/{modelId}")
    R<ModelDetailResponse> getModel(@PathVariable String modelId);

    @PostMapping
    R<ModelDetailResponse> createModel(@RequestBody CreateModelRequest request);

    @PutMapping("/{modelId}")
    R<ModelDetailResponse> updateModel(@PathVariable String modelId, @RequestBody UpdateModelRequest request);

    @DeleteMapping("/{modelId}")
    R<Void> deleteModel(@PathVariable String modelId);

    @GetMapping("/providers")
    R<List<ProviderResponse>> listProviders();

    @GetMapping("/providers/{providerCode}")
    R<ProviderResponse> getProvider(@PathVariable String providerCode);
}
