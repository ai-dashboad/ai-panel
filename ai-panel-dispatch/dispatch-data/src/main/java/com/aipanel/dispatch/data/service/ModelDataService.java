package com.aipanel.dispatch.data.service;

import com.aipanel.dispatch.data.mapper.ModelInfoMapper;
import com.aipanel.dispatch.data.mapper.ModelProviderMapper;
import com.aipanel.dispatch.data.po.ModelInfoPO;
import com.aipanel.dispatch.data.po.ModelProviderPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Model Data Service
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ModelDataService {

    private final ModelInfoMapper modelInfoMapper;
    private final ModelProviderMapper modelProviderMapper;

    public ModelInfoPO getModelInfo(String modelId) {
        return modelInfoMapper.findByModelId(modelId);
    }

    public List<ModelInfoPO> getAllEnabledModels() {
        return modelInfoMapper.findAllEnabled();
    }

    public List<ModelInfoPO> getModelsByProvider(String providerCode) {
        return modelInfoMapper.findByProviderCode(providerCode);
    }

    public List<ModelInfoPO> getModelsByType(String modelType) {
        return modelInfoMapper.findByModelType(modelType);
    }

    public List<ModelInfoPO> searchModels(String keyword) {
        return modelInfoMapper.searchModels(keyword);
    }

    public ModelProviderPO getProvider(String providerCode) {
        return modelProviderMapper.findByProviderCode(providerCode);
    }

    public List<ModelProviderPO> getAllEnabledProviders() {
        return modelProviderMapper.findAllEnabled();
    }

    public List<ModelProviderPO> getHealthyProviders() {
        return modelProviderMapper.findHealthyProviders();
    }

    public void updateProviderHealth(Long providerId, int healthStatus) {
        modelProviderMapper.updateHealthStatus(providerId, healthStatus);
    }

    public int saveModel(ModelInfoPO model) {
        if (model.getId() == null) {
            return modelInfoMapper.insert(model);
        }
        return modelInfoMapper.updateById(model);
    }

    public int saveProvider(ModelProviderPO provider) {
        if (provider.getId() == null) {
            return modelProviderMapper.insert(provider);
        }
        return modelProviderMapper.updateById(provider);
    }
}
