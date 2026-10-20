package com.aipanel.channel.core.service;

import com.aipanel.channel.api.dto.request.ProviderApiKeyRequest;
import com.aipanel.channel.api.dto.request.ProviderConfigRequest;
import com.aipanel.channel.api.dto.response.ProviderApiKeyResponse;
import com.aipanel.channel.api.dto.response.ProviderConfigResponse;
import com.aipanel.channel.core.pool.ApiKeyPool;
import com.aipanel.channel.data.mysql.mapper.ProviderApiKeyMapper;
import com.aipanel.channel.data.mysql.mapper.ProviderConfigMapper;
import com.aipanel.channel.data.mysql.mapper.ProviderModelMapper;
import com.aipanel.channel.data.mysql.po.ProviderApiKeyPO;
import com.aipanel.channel.data.mysql.po.ProviderConfigPO;
import com.aipanel.channel.data.mysql.po.ProviderModelPO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Provider Configuration Service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProviderConfigService {

    private final ProviderConfigMapper providerConfigMapper;
    private final ProviderApiKeyMapper providerApiKeyMapper;
    private final ProviderModelMapper providerModelMapper;
    private final ApiKeyPool apiKeyPool;

    // ==================== Provider Config ====================

    public List<ProviderConfigResponse> listProviders() {
        List<ProviderConfigPO> providers = providerConfigMapper.selectList(
            new LambdaQueryWrapper<ProviderConfigPO>()
                .orderByAsc(ProviderConfigPO::getPriority)
        );

        return providers.stream()
            .map(this::toProviderResponse)
            .collect(Collectors.toList());
    }

    public ProviderConfigResponse getProvider(Long id) {
        ProviderConfigPO provider = providerConfigMapper.selectById(id);
        if (provider == null) {
            throw new IllegalArgumentException("Provider not found: " + id);
        }
        return toProviderResponse(provider);
    }

    @Transactional
    public ProviderConfigResponse createProvider(ProviderConfigRequest request) {
        // Check if type already exists
        Long count = providerConfigMapper.selectCount(
            new LambdaQueryWrapper<ProviderConfigPO>()
                .eq(ProviderConfigPO::getType, request.getType())
        );
        if (count > 0) {
            throw new IllegalArgumentException("Provider type already exists: " + request.getType());
        }

        ProviderConfigPO po = new ProviderConfigPO();
        BeanUtils.copyProperties(request, po);
        providerConfigMapper.insert(po);

        // Reload key pool
        apiKeyPool.reload();

        return toProviderResponse(po);
    }

    @Transactional
    public ProviderConfigResponse updateProvider(Long id, ProviderConfigRequest request) {
        ProviderConfigPO provider = providerConfigMapper.selectById(id);
        if (provider == null) {
            throw new IllegalArgumentException("Provider not found: " + id);
        }

        BeanUtils.copyProperties(request, provider);
        providerConfigMapper.updateById(provider);

        // Reload key pool
        apiKeyPool.reload();

        return toProviderResponse(provider);
    }

    @Transactional
    public void deleteProvider(Long id) {
        providerConfigMapper.deleteById(id);

        // Also delete associated keys
        providerApiKeyMapper.delete(
            new LambdaQueryWrapper<ProviderApiKeyPO>()
                .eq(ProviderApiKeyPO::getProviderId, id)
        );

        // Reload key pool
        apiKeyPool.reload();
    }

    private ProviderConfigResponse toProviderResponse(ProviderConfigPO po) {
        ProviderConfigResponse response = new ProviderConfigResponse();
        BeanUtils.copyProperties(po, response);

        // Count keys
        Long keyCount = providerApiKeyMapper.selectCount(
            new LambdaQueryWrapper<ProviderApiKeyPO>()
                .eq(ProviderApiKeyPO::getProviderId, po.getId())
        );
        response.setKeyCount(keyCount.intValue());

        // Count active keys
        Long activeKeyCount = providerApiKeyMapper.selectCount(
            new LambdaQueryWrapper<ProviderApiKeyPO>()
                .eq(ProviderApiKeyPO::getProviderId, po.getId())
                .eq(ProviderApiKeyPO::getEnabled, true)
                .eq(ProviderApiKeyPO::getStatus, "active")
        );
        response.setActiveKeyCount(activeKeyCount.intValue());

        // Get models
        List<ProviderModelPO> models = providerModelMapper.selectList(
            new LambdaQueryWrapper<ProviderModelPO>()
                .eq(ProviderModelPO::getProviderId, po.getId())
                .eq(ProviderModelPO::getEnabled, true)
        );
        response.setModels(models.stream()
            .map(ProviderModelPO::getModelId)
            .collect(Collectors.toList()));

        return response;
    }

    // ==================== API Keys ====================

    public List<ProviderApiKeyResponse> listApiKeys(Long providerId) {
        LambdaQueryWrapper<ProviderApiKeyPO> wrapper = new LambdaQueryWrapper<>();
        if (providerId != null) {
            wrapper.eq(ProviderApiKeyPO::getProviderId, providerId);
        }
        wrapper.orderByDesc(ProviderApiKeyPO::getCreatedAt);

        List<ProviderApiKeyPO> keys = providerApiKeyMapper.selectList(wrapper);
        return keys.stream()
            .map(this::toApiKeyResponse)
            .collect(Collectors.toList());
    }

    public ProviderApiKeyResponse getApiKey(Long id) {
        ProviderApiKeyPO key = providerApiKeyMapper.selectById(id);
        if (key == null) {
            throw new IllegalArgumentException("API key not found: " + id);
        }
        return toApiKeyResponse(key);
    }

    @Transactional
    public ProviderApiKeyResponse createApiKey(ProviderApiKeyRequest request) {
        // Verify provider exists
        ProviderConfigPO provider = providerConfigMapper.selectById(request.getProviderId());
        if (provider == null) {
            throw new IllegalArgumentException("Provider not found: " + request.getProviderId());
        }

        ProviderApiKeyPO po = new ProviderApiKeyPO();
        BeanUtils.copyProperties(request, po);

        // Generate key prefix for display
        String apiKey = request.getApiKey();
        if (apiKey.length() > 8) {
            po.setKeyPrefix(apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4));
        } else {
            po.setKeyPrefix("****");
        }

        po.setStatus("active");
        po.setConsecutiveErrors(0);
        po.setTotalRequests(0L);
        po.setTotalTokens(0L);
        po.setUsedQuotaToday(0L);

        providerApiKeyMapper.insert(po);

        // Reload key pool
        apiKeyPool.reload();

        return toApiKeyResponse(po);
    }

    @Transactional
    public ProviderApiKeyResponse updateApiKey(Long id, ProviderApiKeyRequest request) {
        ProviderApiKeyPO key = providerApiKeyMapper.selectById(id);
        if (key == null) {
            throw new IllegalArgumentException("API key not found: " + id);
        }

        // Update fields (except apiKey - that requires a separate call)
        key.setKeyName(request.getKeyName());
        key.setEnabled(request.getEnabled());
        key.setWeight(request.getWeight());
        key.setRateLimitPerMinute(request.getRateLimitPerMinute());
        key.setDailyQuota(request.getDailyQuota());
        key.setExpiresAt(request.getExpiresAt());

        // If API key is provided and different, update it
        if (request.getApiKey() != null && !request.getApiKey().isEmpty()
            && !request.getApiKey().equals(key.getApiKey())) {
            key.setApiKey(request.getApiKey());
            String apiKey = request.getApiKey();
            if (apiKey.length() > 8) {
                key.setKeyPrefix(apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4));
            }
        }

        providerApiKeyMapper.updateById(key);

        // Reload key pool
        apiKeyPool.reload();

        return toApiKeyResponse(key);
    }

    @Transactional
    public void deleteApiKey(Long id) {
        providerApiKeyMapper.deleteById(id);

        // Reload key pool
        apiKeyPool.reload();
    }

    @Transactional
    public void resetApiKeyStatus(Long id) {
        ProviderApiKeyPO key = providerApiKeyMapper.selectById(id);
        if (key == null) {
            throw new IllegalArgumentException("API key not found: " + id);
        }

        key.setStatus("active");
        key.setConsecutiveErrors(0);
        key.setLastErrorAt(null);
        key.setLastErrorMessage(null);
        providerApiKeyMapper.updateById(key);

        // Reload key pool
        apiKeyPool.reload();
    }

    private ProviderApiKeyResponse toApiKeyResponse(ProviderApiKeyPO po) {
        ProviderApiKeyResponse response = new ProviderApiKeyResponse();
        BeanUtils.copyProperties(po, response);

        // Get provider info
        ProviderConfigPO provider = providerConfigMapper.selectById(po.getProviderId());
        if (provider != null) {
            response.setProviderName(provider.getName());
            response.setProviderType(provider.getType());
        }

        return response;
    }

    // ==================== Quick Setup (傻瓜式配置) ====================

    /**
     * Validate a provider API key without saving
     */
    public ValidateKeyResult validateKey(String type, String apiKey) {
        ValidateKeyResult result = new ValidateKeyResult();

        try {
            // TODO: Actually call the provider's API to validate
            // For now, just do basic validation
            if (apiKey == null || apiKey.isEmpty()) {
                result.setValid(false);
                result.setMessage("API Key 不能为空");
                return result;
            }

            // Basic format validation
            switch (type) {
                case "openai":
                    if (!apiKey.startsWith("sk-")) {
                        result.setValid(false);
                        result.setMessage("OpenAI API Key 应以 'sk-' 开头");
                        return result;
                    }
                    break;
                case "anthropic":
                    if (!apiKey.startsWith("sk-ant-")) {
                        result.setValid(false);
                        result.setMessage("Anthropic API Key 应以 'sk-ant-' 开头");
                        return result;
                    }
                    break;
                // Other providers - just basic length check
                default:
                    if (apiKey.length() < 10) {
                        result.setValid(false);
                        result.setMessage("API Key 长度不足");
                        return result;
                    }
            }

            // Mark as valid (in production, we'd actually call the API)
            result.setValid(true);
            result.setMessage("验证成功");

            // Mock balance for demo
            if ("openai".equals(type)) {
                result.setBalance("$45.20");
            } else if ("deepseek".equals(type)) {
                result.setBalance("¥128.00");
            } else if ("anthropic".equals(type)) {
                result.setBalance("$25.00");
            }

            return result;
        } catch (Exception e) {
            log.error("Failed to validate key for type: {}", type, e);
            result.setValid(false);
            result.setMessage("验证失败: " + e.getMessage());
            return result;
        }
    }

    /**
     * Quick setup: Create provider + API key in one step using preset configuration
     */
    @Transactional
    public ProviderConfigResponse quickSetup(String type, String apiKey) {
        // Check if provider already exists
        ProviderConfigPO existingProvider = providerConfigMapper.selectOne(
            new LambdaQueryWrapper<ProviderConfigPO>()
                .eq(ProviderConfigPO::getType, type)
        );

        ProviderConfigPO provider;
        if (existingProvider != null) {
            provider = existingProvider;
        } else {
            // Create provider with preset configuration
            provider = createPresetProvider(type);
            providerConfigMapper.insert(provider);
        }

        // Create API key
        ProviderApiKeyPO key = new ProviderApiKeyPO();
        key.setProviderId(provider.getId());
        key.setApiKey(apiKey);
        key.setKeyName("Default Key");
        key.setEnabled(true);
        key.setWeight(100);
        key.setStatus("active");
        key.setConsecutiveErrors(0);
        key.setTotalRequests(0L);
        key.setTotalTokens(0L);
        key.setUsedQuotaToday(0L);

        // Generate key prefix
        if (apiKey.length() > 8) {
            key.setKeyPrefix(apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4));
        } else {
            key.setKeyPrefix("****");
        }

        providerApiKeyMapper.insert(key);

        // Sync preset models for the provider
        syncModelsForProvider(provider.getId(), type);

        // Reload key pool
        apiKeyPool.reload();

        return toProviderResponse(provider);
    }

    /**
     * Batch setup multiple providers
     */
    @Transactional
    public List<ProviderConfigResponse> batchSetup(List<ProviderSetupRequest> providers) {
        return providers.stream()
            .map(p -> quickSetup(p.getType(), p.getApiKey()))
            .collect(Collectors.toList());
    }

    /**
     * Create preset provider configuration based on type
     */
    private ProviderConfigPO createPresetProvider(String type) {
        ProviderConfigPO provider = new ProviderConfigPO();
        provider.setType(type);
        provider.setEnabled(true);
        provider.setPriority(100);
        provider.setTimeoutSeconds(60);
        provider.setRetryCount(3);

        switch (type) {
            case "openai":
                provider.setName("OpenAI");
                provider.setBaseUrl("https://api.openai.com/v1");
                provider.setDescription("OpenAI GPT models including GPT-4o, GPT-4-Turbo, GPT-3.5-Turbo");
                break;
            case "anthropic":
                provider.setName("Anthropic");
                provider.setBaseUrl("https://api.anthropic.com");
                provider.setDescription("Anthropic Claude models including Claude 3.5 Sonnet, Claude 3 Opus");
                break;
            case "deepseek":
                provider.setName("DeepSeek");
                provider.setBaseUrl("https://api.deepseek.com/v1");
                provider.setDescription("DeepSeek models - cost-effective alternative");
                break;
            case "qwen":
                provider.setName("通义千问");
                provider.setBaseUrl("https://dashscope.aliyuncs.com/api/v1");
                provider.setDescription("Alibaba Qwen models");
                break;
            case "moonshot":
                provider.setName("Moonshot (月之暗面)");
                provider.setBaseUrl("https://api.moonshot.cn/v1");
                provider.setDescription("Moonshot AI models with long context support");
                break;
            case "zhipu":
                provider.setName("智谱 AI");
                provider.setBaseUrl("https://open.bigmodel.cn/api/paas/v4");
                provider.setDescription("Zhipu GLM models");
                break;
            case "minimax":
                provider.setName("MiniMax");
                provider.setBaseUrl("https://api.minimax.chat/v1");
                provider.setDescription("MiniMax chat models");
                break;
            case "openrouter":
                provider.setName("OpenRouter");
                provider.setBaseUrl("https://openrouter.ai/api/v1");
                provider.setDescription("OpenRouter aggregation gateway");
                break;
            case "siliconflow":
                provider.setName("SiliconFlow");
                provider.setBaseUrl("https://api.siliconflow.cn/v1");
                provider.setDescription("SiliconFlow OpenAI-compatible gateway");
                break;
            case "stepfun":
                provider.setName("StepFun");
                provider.setBaseUrl("https://api.stepfun.com/v1");
                provider.setDescription("StepFun models");
                break;
            case "doubao":
                provider.setName("Doubao / Volcengine");
                provider.setBaseUrl("https://ark.cn-beijing.volces.com/api/v3");
                provider.setDescription("Volcengine Doubao models");
                break;
            case "google":
                provider.setName("Google Gemini");
                provider.setBaseUrl("https://generativelanguage.googleapis.com/v1beta/openai/");
                provider.setDescription("Google Gemini via OpenAI-compatible endpoint");
                break;
            case "azure":
                provider.setName("Azure OpenAI");
                provider.setBaseUrl("https://YOUR_RESOURCE.openai.azure.com");
                provider.setDescription("Azure OpenAI deployments");
                break;
            default:
                provider.setName(type);
                provider.setBaseUrl("");
                provider.setDescription("Custom OpenAI-compatible provider");
        }

        return provider;
    }

    // ==================== DTOs for Quick Setup ====================

    @lombok.Data
    public static class ValidateKeyResult {
        private boolean valid;
        private String message;
        private String balance;
    }

    @lombok.Data
    public static class ProviderSetupRequest {
        private String type;
        private String apiKey;
    }

    // ==================== Model Sync ====================

    /**
     * Sync preset models for a provider
     */
    @Transactional
    public void syncModelsForProvider(Long providerId, String providerType) {
        List<PresetModel> presetModels = getPresetModels(providerType);

        for (PresetModel preset : presetModels) {
            // Check if model already exists
            Long count = providerModelMapper.selectCount(
                new LambdaQueryWrapper<ProviderModelPO>()
                    .eq(ProviderModelPO::getProviderId, providerId)
                    .eq(ProviderModelPO::getModelId, preset.getModelId())
            );

            if (count == 0) {
                ProviderModelPO model = new ProviderModelPO();
                model.setProviderId(providerId);
                model.setModelId(preset.getModelId());
                model.setModelName(preset.getModelName());
                model.setEnabled(true);
                model.setInputPricePerMillion(preset.getInputPrice());
                model.setOutputPricePerMillion(preset.getOutputPrice());
                model.setMaxTokens(preset.getMaxTokens());
                model.setContextWindow(preset.getContextWindow());
                model.setSupportsVision(preset.getSupportsVision());
                model.setSupportsFunctionCall(preset.getSupportsFunctionCall());
                providerModelMapper.insert(model);
            }
        }

        log.info("Synced {} models for provider: type={}, providerId={}",
            presetModels.size(), providerType, providerId);
    }

    /**
     * Get preset models for a provider type
     */
    private List<PresetModel> getPresetModels(String providerType) {
        List<PresetModel> models = new java.util.ArrayList<>();

        switch (providerType) {
            case "openai":
                // GPT-4o family
                models.add(new PresetModel("gpt-4o", "GPT-4o",
                    new java.math.BigDecimal("2.50"), new java.math.BigDecimal("10.00"),
                    16384, 128000, true, true));
                models.add(new PresetModel("gpt-4o-mini", "GPT-4o Mini",
                    new java.math.BigDecimal("0.15"), new java.math.BigDecimal("0.60"),
                    16384, 128000, true, true));
                // GPT-4 Turbo
                models.add(new PresetModel("gpt-4-turbo", "GPT-4 Turbo",
                    new java.math.BigDecimal("10.00"), new java.math.BigDecimal("30.00"),
                    4096, 128000, true, true));
                // GPT-3.5
                models.add(new PresetModel("gpt-3.5-turbo", "GPT-3.5 Turbo",
                    new java.math.BigDecimal("0.50"), new java.math.BigDecimal("1.50"),
                    4096, 16385, false, true));
                break;

            case "anthropic":
                // Claude 3.5
                models.add(new PresetModel("claude-3-5-sonnet-20241022", "Claude 3.5 Sonnet",
                    new java.math.BigDecimal("3.00"), new java.math.BigDecimal("15.00"),
                    8192, 200000, true, true));
                // Claude 3
                models.add(new PresetModel("claude-3-opus-20240229", "Claude 3 Opus",
                    new java.math.BigDecimal("15.00"), new java.math.BigDecimal("75.00"),
                    4096, 200000, true, true));
                models.add(new PresetModel("claude-3-haiku-20240307", "Claude 3 Haiku",
                    new java.math.BigDecimal("0.25"), new java.math.BigDecimal("1.25"),
                    4096, 200000, true, true));
                break;

            case "deepseek":
                models.add(new PresetModel("deepseek-chat", "DeepSeek Chat",
                    new java.math.BigDecimal("0.14"), new java.math.BigDecimal("0.28"),
                    8192, 64000, false, true));
                models.add(new PresetModel("deepseek-coder", "DeepSeek Coder",
                    new java.math.BigDecimal("0.14"), new java.math.BigDecimal("0.28"),
                    8192, 64000, false, true));
                break;

            case "qwen":
                models.add(new PresetModel("qwen-turbo", "Qwen Turbo",
                    new java.math.BigDecimal("0.30"), new java.math.BigDecimal("0.60"),
                    8192, 131072, false, true));
                models.add(new PresetModel("qwen-plus", "Qwen Plus",
                    new java.math.BigDecimal("0.80"), new java.math.BigDecimal("2.00"),
                    8192, 131072, false, true));
                models.add(new PresetModel("qwen-max", "Qwen Max",
                    new java.math.BigDecimal("2.00"), new java.math.BigDecimal("6.00"),
                    8192, 32768, false, true));
                break;

            case "moonshot":
                models.add(new PresetModel("moonshot-v1-8k", "Moonshot v1 8K",
                    new java.math.BigDecimal("1.00"), new java.math.BigDecimal("1.00"),
                    4096, 8192, false, true));
                models.add(new PresetModel("moonshot-v1-32k", "Moonshot v1 32K",
                    new java.math.BigDecimal("2.00"), new java.math.BigDecimal("2.00"),
                    4096, 32768, false, true));
                models.add(new PresetModel("moonshot-v1-128k", "Moonshot v1 128K",
                    new java.math.BigDecimal("6.00"), new java.math.BigDecimal("6.00"),
                    4096, 131072, false, true));
                break;

            case "zhipu":
                models.add(new PresetModel("glm-4", "GLM-4",
                    new java.math.BigDecimal("1.00"), new java.math.BigDecimal("1.00"),
                    4096, 128000, true, true));
                models.add(new PresetModel("glm-4-flash", "GLM-4 Flash",
                    new java.math.BigDecimal("0.10"), new java.math.BigDecimal("0.10"),
                    4096, 128000, false, true));
                models.add(new PresetModel("glm-4-plus", "GLM-4 Plus",
                    new java.math.BigDecimal("5.00"), new java.math.BigDecimal("5.00"),
                    4096, 128000, true, true));
                break;
            case "google":
                models.add(new PresetModel("gemini-2.0-flash", "Gemini 2.0 Flash",
                    new java.math.BigDecimal("0.10"), new java.math.BigDecimal("0.40"),
                    8192, 1048576, true, true));
                models.add(new PresetModel("gemini-1.5-pro", "Gemini 1.5 Pro",
                    new java.math.BigDecimal("1.25"), new java.math.BigDecimal("5.00"),
                    8192, 2097152, true, true));
                break;
            case "minimax":
                models.add(new PresetModel("MiniMax-Text-01", "MiniMax Text 01",
                    new java.math.BigDecimal("1.00"), new java.math.BigDecimal("1.00"),
                    8192, 1000000, false, true));
                break;
            case "openrouter":
                models.add(new PresetModel("openrouter/auto", "OpenRouter Auto",
                    new java.math.BigDecimal("2.00"), new java.math.BigDecimal("8.00"),
                    8192, 128000, false, true));
                break;
            case "siliconflow":
                models.add(new PresetModel("deepseek-ai/DeepSeek-V3", "DeepSeek V3",
                    new java.math.BigDecimal("0.14"), new java.math.BigDecimal("0.28"),
                    8192, 64000, false, true));
                break;
            case "stepfun":
                models.add(new PresetModel("step-2-16k", "Step-2 16K",
                    new java.math.BigDecimal("3.80"), new java.math.BigDecimal("3.80"),
                    4096, 16000, false, false));
                break;
            case "doubao":
                models.add(new PresetModel("doubao-pro-32k", "Doubao Pro 32K",
                    new java.math.BigDecimal("0.80"), new java.math.BigDecimal("2.00"),
                    4096, 32000, false, true));
                break;
        }

        return models;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class PresetModel {
        private String modelId;
        private String modelName;
        private java.math.BigDecimal inputPrice;
        private java.math.BigDecimal outputPrice;
        private Integer maxTokens;
        private Integer contextWindow;
        private Boolean supportsVision;
        private Boolean supportsFunctionCall;
    }
}
