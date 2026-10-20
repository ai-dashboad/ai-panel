package com.aipanel.user.web.controller;

import com.aipanel.common.domain.R;
import com.aipanel.user.api.dto.request.ProductSkuRequest;
import com.aipanel.user.api.dto.response.ProductSkuResponse;
import com.aipanel.user.core.service.ProductSkuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/skus")
@RequiredArgsConstructor
@Tag(name = "Product SKU Management", description = "Admin endpoints for managing product SKUs")
@Slf4j
public class ProductSkuController {

    private final ProductSkuService productSkuService;

    @GetMapping
    @Operation(summary = "List all product SKUs")
    public R<List<ProductSkuResponse>> listSkus(
            @Parameter(description = "Filter by platform")
            @RequestParam(required = false) String platform) {
        List<ProductSkuResponse> skus;
        if (platform != null) {
            skus = productSkuService.listByPlatform(platform);
        } else {
            skus = productSkuService.listAll();
        }
        return R.ok(skus);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product SKU by ID")
    public R<ProductSkuResponse> getSku(
            @Parameter(description = "SKU ID", required = true)
            @PathVariable Long id) {
        ProductSkuResponse sku = productSkuService.getById(id);
        return R.ok(sku);
    }

    @PostMapping
    @Operation(summary = "Create product SKU")
    public R<ProductSkuResponse> createSku(
            @RequestBody @Valid ProductSkuRequest request) {
        ProductSkuResponse sku = productSkuService.create(request);
        return R.ok(sku);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update product SKU")
    public R<ProductSkuResponse> updateSku(
            @Parameter(description = "SKU ID", required = true)
            @PathVariable Long id,
            @RequestBody @Valid ProductSkuRequest request) {
        ProductSkuResponse sku = productSkuService.update(id, request);
        return R.ok(sku);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete product SKU")
    public R<Void> deleteSku(
            @Parameter(description = "SKU ID", required = true)
            @PathVariable Long id) {
        productSkuService.delete(id);
        return R.ok();
    }
}
