package com.aipanel.channel.core.upstream;

import java.util.Locale;
import java.util.Set;

/**
 * Provider type catalogue and model-name based fallback inference.
 * <p>
 * Every type except {@code anthropic} is spoken to with the OpenAI chat
 * completions protocol. Self-hosted types do not require an API key.
 */
public final class ProviderTypes {

    public static final String OPENAI = "openai";
    public static final String ANTHROPIC = "anthropic";
    public static final String VLLM = "vllm";
    public static final String OLLAMA = "ollama";
    public static final String OPENAI_COMPATIBLE = "openai_compatible";

    /** Types where the API key is optional (typically self-hosted inference servers). */
    public static final Set<String> KEY_OPTIONAL = Set.of(
        VLLM, OLLAMA, OPENAI_COMPATIBLE, "lmstudio", "sglang", "localai", "llamacpp", "xinference"
    );

    /** Types known to speak the OpenAI chat completions protocol at {base}/chat/completions. */
    public static final Set<String> OPENAI_PROTOCOL = Set.of(
        OPENAI, VLLM, OLLAMA, OPENAI_COMPATIBLE, "lmstudio", "sglang", "localai", "llamacpp", "xinference",
        "deepseek", "moonshot", "zhipu", "qwen", "minimax", "openrouter", "siliconflow", "stepfun",
        "doubao", "google", "mistral", "groq", "together", "xai"
    );

    /** Types whose wire protocol is not implemented yet. */
    public static final Set<String> UNSUPPORTED = Set.of("azure");

    private ProviderTypes() {
    }

    public static String normalize(String type) {
        return type == null ? "" : type.trim().toLowerCase(Locale.ROOT);
    }

    public static boolean isAnthropic(String type) {
        return ANTHROPIC.equals(normalize(type));
    }

    public static boolean isKeyOptional(String type) {
        return KEY_OPTIONAL.contains(normalize(type));
    }

    public static boolean isSupported(String type) {
        String t = normalize(type);
        return !t.isEmpty() && !UNSUPPORTED.contains(t);
    }

    /**
     * Default base URL for well-known hosted providers (used when a provider has no base URL).
     */
    public static String defaultBaseUrl(String type) {
        return switch (normalize(type)) {
            case OPENAI -> "https://api.openai.com/v1";
            case ANTHROPIC -> "https://api.anthropic.com";
            case "deepseek" -> "https://api.deepseek.com/v1";
            case "moonshot" -> "https://api.moonshot.cn/v1";
            case "zhipu" -> "https://open.bigmodel.cn/api/paas/v4";
            case "qwen" -> "https://dashscope.aliyuncs.com/compatible-mode/v1";
            case "openrouter" -> "https://openrouter.ai/api/v1";
            case "siliconflow" -> "https://api.siliconflow.cn/v1";
            case OLLAMA -> "http://localhost:11434/v1";
            case VLLM -> "http://localhost:8000/v1";
            default -> null;
        };
    }

    /**
     * Fallback provider type inferred from a model name. Only used when the model is not
     * explicitly configured on any enabled provider. Returns {@code null} when unknown.
     */
    public static String inferFromModel(String model) {
        if (model == null || model.isBlank()) {
            return null;
        }
        String m = model.toLowerCase(Locale.ROOT);
        if (m.startsWith("gpt") || m.startsWith("o1") || m.startsWith("o3") || m.startsWith("o4")
                || m.startsWith("chatgpt")) {
            return OPENAI;
        } else if (m.startsWith("claude")) {
            return ANTHROPIC;
        } else if (m.startsWith("deepseek")) {
            return "deepseek";
        } else if (m.startsWith("moonshot") || m.startsWith("kimi")) {
            return "moonshot";
        } else if (m.startsWith("glm")) {
            return "zhipu";
        } else if (m.startsWith("qwen")) {
            return "qwen";
        } else if (m.startsWith("gemini")) {
            return "google";
        } else if (m.startsWith("minimax") || m.startsWith("abab")) {
            return "minimax";
        } else if (m.startsWith("step-")) {
            return "stepfun";
        } else if (m.startsWith("doubao")) {
            return "doubao";
        } else if (m.contains("/")) {
            return "openrouter";
        }
        return null;
    }
}
