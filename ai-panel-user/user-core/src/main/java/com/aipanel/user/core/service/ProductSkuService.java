package com.aipanel.user.core.service;

import com.aipanel.user.api.dto.request.ProductSkuRequest;
import com.aipanel.user.api.dto.response.ProductSkuResponse;

import java.util.List;

public interface ProductSkuService {

    ProductSkuResponse create(ProductSkuRequest request);

    ProductSkuResponse update(Long id, ProductSkuRequest request);

    void delete(Long id);

    ProductSkuResponse getById(Long id);

    ProductSkuResponse getBySku(String sku);

    ProductSkuResponse getByPlatformAndSku(String platform, String sku);

    List<ProductSkuResponse> listAll();

    List<ProductSkuResponse> listByPlatform(String platform);

    List<ProductSkuResponse> listEnabled();
}
