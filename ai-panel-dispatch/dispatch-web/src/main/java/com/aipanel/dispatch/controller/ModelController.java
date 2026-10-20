package com.aipanel.dispatch.controller;

import com.aipanel.common.domain.R;
import com.aipanel.dispatch.api.dto.response.ModelInfoResponse;
import com.aipanel.dispatch.data.po.ModelGroupPO;
import com.aipanel.dispatch.data.po.ModelInfoPO;
import com.aipanel.dispatch.data.service.ModelCacheService;
import com.aipanel.dispatch.data.service.ModelDataService;
import com.aipanel.dispatch.data.service.ModelGroupDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Model Information Controller
 */
@RestController
@RequestMapping("/api/v1/models")
@Tag(name = "Models", description = "AI Model Information API")
@Slf4j
@RequiredArgsConstructor
public class ModelController {

    private final ModelDataService modelDataService;
    private final ModelCacheService modelCacheService;
    private final ModelGroupDataService modelGroupDataService;

    @GetMapping
    @Operation(summary = "List models", description = "Lists all available AI models with sell pricing")
    public R<List<ModelInfoResponse>> listModels(
            @Parameter(description = "Filter by provider") @RequestParam(required = false) String provider,
            @Parameter(description = "Filter by model type") @RequestParam(required = false) String type,
            @Parameter(description = "Search keyword") @RequestParam(required = false) String keyword,
            @Parameter(description = "Filter by group code") @RequestParam(required = false) String group
    ) {
        List<ModelInfoPO> models;

        if (provider == null && type == null && keyword == null) {
            models = modelCacheService.getCachedAllModels();
            if (models.isEmpty()) {
                models = modelDataService.getAllEnabledModels();
                modelCacheService.cacheAllModels(models);
            }
        } else if (provider != null) {
            models = modelDataService.getModelsByProvider(provider);
        } else if (type != null) {
            models = modelDataService.getModelsByType(type);
        } else {
            models = modelDataService.searchModels(keyword);
        }

        Map<String, ModelGroupPO> groupByModel = modelGroupDataService.mapPrimaryGroupByModelId();

        List<ModelInfoResponse> responses = models.stream()
            .map(po -> convertToResponse(po, groupByModel.get(po.getModelId())))
            .filter(r -> group == null || group.equals(r.getGroupCode()))
            .collect(Collectors.toList());

        return R.ok(responses);
    }

    @GetMapping("/{modelId}")
    @Operation(summary = "Get model", description = "Get model information by ID")
    public R<ModelInfoResponse> getModel(
            @Parameter(description = "Model ID") @PathVariable String modelId
    ) {
        ModelInfoPO model = modelCacheService.getCachedModel(modelId);
        if (model == null) {
            model = modelDataService.getModelInfo(modelId);
            if (model != null) {
                modelCacheService.cacheModel(model);
            }
        }

        if (model == null) {
            return R.fail(404, "Model not found: " + modelId);
        }

        Map<String, ModelGroupPO> groupByModel = modelGroupDataService.mapPrimaryGroupByModelId();
        return R.ok(convertToResponse(model, groupByModel.get(modelId)));
    }

    @PostMapping("/cache/refresh")
    @Operation(summary = "Refresh cache", description = "Refresh model cache from database")
    public R<String> refreshCache() {
        modelCacheService.invalidateAll();
        List<ModelInfoPO> models = modelDataService.getAllEnabledModels();
        modelCacheService.cacheAllModels(models);
        log.info("Model cache refreshed with {} models", models.size());
        return R.ok("Cache refreshed");
    }

    private ModelInfoResponse convertToResponse(ModelInfoPO po, ModelGroupPO group) {
        BigDecimal multiplier = group != null && group.getPriceMultiplier() != null
            ? group.getPriceMultiplier() : BigDecimal.ONE;

        BigDecimal costIn = po.getInputPrice() != null ? po.getInputPrice() : BigDecimal.ZERO;
        BigDecimal costOut = po.getOutputPrice() != null ? po.getOutputPrice() : BigDecimal.ZERO;

        // DB stores per-1K; expose employee sell price per 1M
        BigDecimal sellInPerM = costIn.multiply(multiplier).multiply(BigDecimal.valueOf(1000))
            .setScale(4, RoundingMode.HALF_UP);
        BigDecimal sellOutPerM = costOut.multiply(multiplier).multiply(BigDecimal.valueOf(1000))
            .setScale(4, RoundingMode.HALF_UP);

        List<String> labels = new ArrayList<>();
        labels.add("chat");
        if (po.getProviderCode() != null) {
            labels.add(po.getProviderCode());
        }

        return ModelInfoResponse.builder()
            .id(po.getModelId())
            .displayName(po.getDisplayName())
            .provider(po.getProviderCode())
            .modelType(po.getModelType())
            .contextWindow(po.getContextWindow())
            .maxOutputTokens(po.getMaxOutputTokens())
            .inputPrice(po.getInputPrice())
            .outputPrice(po.getOutputPrice())
            .features(parseFeatures(po.getFeatures()))
            .deprecated(po.getDeprecated())
            .description(po.getDescription())
            .groupCode(group != null ? group.getGroupCode() : null)
            .groupName(group != null ? group.getGroupName() : null)
            .priceMultiplier(multiplier)
            .sellInputPricePerMillion(sellInPerM)
            .sellOutputPricePerMillion(sellOutPerM)
            .labels(labels)
            .pricingType("usage")
            .build();
    }

    private List<String> parseFeatures(String featuresJson) {
        if (featuresJson == null || featuresJson.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            String cleaned = featuresJson.replace("[", "").replace("]", "").replace("\"", "");
            if (cleaned.isBlank()) {
                return Collections.emptyList();
            }
            return Arrays.stream(cleaned.split(",\\s*"))
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
