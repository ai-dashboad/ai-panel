package com.aipanel.common.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/**
 * AI Model Provider Types
 * Supports 100+ providers similar to LiteLLM
 */
@Getter
@Schema(description = "AI Model Provider Types")
public enum ProviderType {

    // Commercial Cloud Services
    OPENAI("openai", "OpenAI", "https://api.openai.com/v1"),
    ANTHROPIC("anthropic", "Anthropic", "https://api.anthropic.com"),
    GOOGLE("google", "Google AI", "https://generativelanguage.googleapis.com/v1beta"),
    AZURE_OPENAI("azure", "Azure OpenAI", null),
    AWS_BEDROCK("bedrock", "AWS Bedrock", null),
    GOOGLE_VERTEX("vertex", "Google Vertex AI", null),
    MISTRAL("mistral", "Mistral AI", "https://api.mistral.ai/v1"),
    GROQ("groq", "Groq", "https://api.groq.com/openai/v1"),
    COHERE("cohere", "Cohere", "https://api.cohere.ai/v1"),
    AI21("ai21", "AI21 Labs", "https://api.ai21.com/studio/v1"),
    TOGETHER("together", "Together AI", "https://api.together.xyz/v1"),
    REPLICATE("replicate", "Replicate", "https://api.replicate.com/v1"),
    PERPLEXITY("perplexity", "Perplexity", "https://api.perplexity.ai"),
    ANYSCALE("anyscale", "Anyscale", "https://api.endpoints.anyscale.com/v1"),
    FIREWORKS("fireworks", "Fireworks AI", "https://api.fireworks.ai/inference/v1"),

    // Chinese Providers
    ZHIPU("zhipu", "Zhipu AI (GLM)", "https://open.bigmodel.cn/api/paas/v4"),
    BAICHUAN("baichuan", "Baichuan", "https://api.baichuan-ai.com/v1"),
    MOONSHOT("moonshot", "Moonshot (Kimi)", "https://api.moonshot.cn/v1"),
    DEEPSEEK("deepseek", "DeepSeek", "https://api.deepseek.com/v1"),
    QWEN("qwen", "Qwen (Tongyi)", "https://dashscope.aliyuncs.com/api/v1"),
    SPARK("spark", "iFlytek Spark", "https://spark-api.xf-yun.com/v1"),
    HUNYUAN("hunyuan", "Tencent Hunyuan", "https://hunyuan.tencentcloudapi.com"),
    DOUBAO("doubao", "ByteDance Doubao", "https://ark.cn-beijing.volces.com/api/v3"),
    MINIMAX("minimax", "MiniMax", "https://api.minimax.chat/v1"),
    STEPFUN("stepfun", "StepFun", "https://api.stepfun.com/v1"),

    // Local/Private Deployment
    OLLAMA("ollama", "Ollama", "http://localhost:11434/api"),
    VLLM("vllm", "vLLM", "http://localhost:8000/v1"),
    HUGGINGFACE("huggingface", "HuggingFace TGI", null),
    LOCALAI("localai", "LocalAI", "http://localhost:8080/v1"),
    LM_STUDIO("lmstudio", "LM Studio", "http://localhost:1234/v1"),

    // OpenAI Compatible Interface
    OPENAI_COMPATIBLE("openai_compatible", "OpenAI Compatible", null);

    private final String code;
    private final String displayName;
    private final String defaultBaseUrl;

    ProviderType(String code, String displayName, String defaultBaseUrl) {
        this.code = code;
        this.displayName = displayName;
        this.defaultBaseUrl = defaultBaseUrl;
    }

    /**
     * Get ProviderType by code
     */
    public static ProviderType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (ProviderType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown provider: " + code);
    }

    /**
     * Check if provider has a default base URL
     */
    public boolean hasDefaultBaseUrl() {
        return this.defaultBaseUrl != null;
    }

    /**
     * Check if this is a Chinese provider
     */
    public boolean isChineseProvider() {
        return this == ZHIPU || this == BAICHUAN || this == MOONSHOT ||
               this == DEEPSEEK || this == QWEN || this == SPARK ||
               this == HUNYUAN || this == DOUBAO || this == MINIMAX ||
               this == STEPFUN;
    }

    /**
     * Check if this is a local/private deployment provider
     */
    public boolean isLocalProvider() {
        return this == OLLAMA || this == VLLM || this == HUGGINGFACE ||
               this == LOCALAI || this == LM_STUDIO;
    }
}
