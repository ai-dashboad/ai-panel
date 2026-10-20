package com.aipanel.user.core.service.impl;

import com.aipanel.common.enums.ErrorCode;
import com.aipanel.common.exception.BusinessException;
import com.aipanel.user.api.dto.request.ProductSkuRequest;
import com.aipanel.user.api.dto.response.ProductSkuResponse;
import com.aipanel.user.core.service.ProductSkuService;
import com.aipanel.user.data.mysql.po.ProductSkuPO;
import com.aipanel.user.data.mysql.service.ProductSkuDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductSkuServiceImpl implements ProductSkuService {

    private final ProductSkuDataService productSkuDataService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductSkuResponse create(ProductSkuRequest request) {
        // Check for duplicate SKU
        ProductSkuPO existing = productSkuDataService.findByPlatformAndSku(request.getPlatform(), request.getSku());
        if (existing != null) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "SKU already exists: " + request.getSku());
        }

        ProductSkuPO sku = new ProductSkuPO();
        copyRequestToEntity(request, sku);
        sku.setCreatedAt(LocalDateTime.now());
        sku.setUpdatedAt(LocalDateTime.now());

        productSkuDataService.save(sku);
        log.info("Product SKU created: sku={}, id={}", sku.getSku(), sku.getId());

        return convertToResponse(sku);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductSkuResponse update(Long id, ProductSkuRequest request) {
        ProductSkuPO sku = productSkuDataService.getById(id);
        if (sku == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "Product SKU not found");
        }

        // Check for duplicate if SKU code is being changed
        if (!sku.getSku().equals(request.getSku())) {
            ProductSkuPO existing = productSkuDataService.findByPlatformAndSku(request.getPlatform(), request.getSku());
            if (existing != null) {
                throw BusinessException.of(ErrorCode.PARAM_ERROR, "SKU already exists: " + request.getSku());
            }
        }

        copyRequestToEntity(request, sku);
        sku.setUpdatedAt(LocalDateTime.now());

        productSkuDataService.updateById(sku);
        log.info("Product SKU updated: sku={}, id={}", sku.getSku(), sku.getId());

        return convertToResponse(sku);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ProductSkuPO sku = productSkuDataService.getById(id);
        if (sku == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "Product SKU not found");
        }

        productSkuDataService.removeById(id);
        log.info("Product SKU deleted: sku={}, id={}", sku.getSku(), id);
    }

    @Override
    public ProductSkuResponse getById(Long id) {
        ProductSkuPO sku = productSkuDataService.getById(id);
        if (sku == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "Product SKU not found");
        }
        return convertToResponse(sku);
    }

    @Override
    public ProductSkuResponse getBySku(String skuCode) {
        ProductSkuPO sku = productSkuDataService.findBySku(skuCode);
        if (sku == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "Product SKU not found: " + skuCode);
        }
        return convertToResponse(sku);
    }

    @Override
    public ProductSkuResponse getByPlatformAndSku(String platform, String skuCode) {
        ProductSkuPO sku = productSkuDataService.findByPlatformAndSku(platform, skuCode);
        if (sku == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "Product SKU not found: " + skuCode);
        }
        return convertToResponse(sku);
    }

    @Override
    public List<ProductSkuResponse> listAll() {
        return productSkuDataService.list().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductSkuResponse> listByPlatform(String platform) {
        return productSkuDataService.findByPlatform(platform).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductSkuResponse> listEnabled() {
        return productSkuDataService.findAllEnabled().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    private void copyRequestToEntity(ProductSkuRequest request, ProductSkuPO sku) {
        sku.setPlatform(request.getPlatform());
        sku.setSku(request.getSku());
        sku.setProductName(request.getName());
        sku.setDescription(request.getDescription());
        sku.setQuotaType(request.getQuotaType() != null ? request.getQuotaType() : "tokens");
        sku.setQuotaTokens(request.getQuotaTokens());
        sku.setValidityDays(request.getValidityDays() != null ? request.getValidityDays() : 30);
        sku.setRateLimitPerMinute(request.getRateLimit());
        sku.setEnabled(request.getEnabled() != null ? request.getEnabled() : true);
    }

    private ProductSkuResponse convertToResponse(ProductSkuPO sku) {
        return ProductSkuResponse.builder()
                .id(sku.getId())
                .platform(sku.getPlatform())
                .sku(sku.getSku())
                .name(sku.getProductName())
                .description(sku.getDescription())
                .quotaType(sku.getQuotaType())
                .quotaTokens(sku.getQuotaTokens())
                .quotaRequests(null)
                .validityDays(sku.getValidityDays())
                .allowedModels(null)
                .rateLimit(sku.getRateLimitPerMinute())
                .enabled(sku.getEnabled())
                .createdAt(sku.getCreatedAt())
                .updatedAt(sku.getUpdatedAt())
                .build();
    }
}
