package com.aipanel.user.data.mysql.service;

import com.aipanel.user.data.mysql.po.ProductSkuPO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface ProductSkuDataService extends IService<ProductSkuPO> {

    ProductSkuPO findBySku(String sku);

    ProductSkuPO findByPlatformAndSku(String platform, String sku);

    List<ProductSkuPO> findByPlatform(String platform);

    List<ProductSkuPO> findAllEnabled();

    boolean existsBySku(String sku);
}
