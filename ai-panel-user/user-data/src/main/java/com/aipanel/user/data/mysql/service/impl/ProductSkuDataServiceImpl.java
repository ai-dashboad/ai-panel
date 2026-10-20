package com.aipanel.user.data.mysql.service.impl;

import com.aipanel.user.data.mysql.mapper.ProductSkuMapper;
import com.aipanel.user.data.mysql.po.ProductSkuPO;
import com.aipanel.user.data.mysql.service.ProductSkuDataService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductSkuDataServiceImpl extends ServiceImpl<ProductSkuMapper, ProductSkuPO> implements ProductSkuDataService {

    @Override
    public ProductSkuPO findBySku(String sku) {
        return getOne(new LambdaQueryWrapper<ProductSkuPO>()
                .eq(ProductSkuPO::getSku, sku)
                .isNull(ProductSkuPO::getPlatform));
    }

    @Override
    public ProductSkuPO findByPlatformAndSku(String platform, String sku) {
        // First try to find platform-specific SKU
        ProductSkuPO platformSku = getOne(new LambdaQueryWrapper<ProductSkuPO>()
                .eq(ProductSkuPO::getPlatform, platform)
                .eq(ProductSkuPO::getSku, sku));

        if (platformSku != null) {
            return platformSku;
        }

        // Fall back to global SKU (null platform)
        return getOne(new LambdaQueryWrapper<ProductSkuPO>()
                .isNull(ProductSkuPO::getPlatform)
                .eq(ProductSkuPO::getSku, sku));
    }

    @Override
    public List<ProductSkuPO> findByPlatform(String platform) {
        return list(new LambdaQueryWrapper<ProductSkuPO>()
                .and(wrapper -> wrapper
                        .eq(ProductSkuPO::getPlatform, platform)
                        .or()
                        .isNull(ProductSkuPO::getPlatform))
                .eq(ProductSkuPO::getEnabled, true)
                .orderByAsc(ProductSkuPO::getId));
    }

    @Override
    public List<ProductSkuPO> findAllEnabled() {
        return list(new LambdaQueryWrapper<ProductSkuPO>()
                .eq(ProductSkuPO::getEnabled, true)
                .orderByAsc(ProductSkuPO::getId));
    }

    @Override
    public boolean existsBySku(String sku) {
        return count(new LambdaQueryWrapper<ProductSkuPO>()
                .eq(ProductSkuPO::getSku, sku)) > 0;
    }
}
