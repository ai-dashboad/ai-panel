package com.aipanel.channel.core.upstream;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BaseUrlsTest {

    @Test
    void normalisesOpenAiStyleBaseUrls() {
        assertThat(BaseUrls.chatCompletionsUrl("http://gpu-host:8000", "vllm")).isEqualTo("http://gpu-host:8000/v1/chat/completions");
        assertThat(BaseUrls.chatCompletionsUrl("http://gpu-host:8000/v1", "vllm")).isEqualTo("http://gpu-host:8000/v1/chat/completions");
        assertThat(BaseUrls.chatCompletionsUrl("http://host:11434/v1/", "ollama")).isEqualTo("http://host:11434/v1/chat/completions");
        assertThat(BaseUrls.chatCompletionsUrl("https://open.bigmodel.cn/api/paas/v4", "zhipu"))
            .isEqualTo("https://open.bigmodel.cn/api/paas/v4/chat/completions");
        assertThat(BaseUrls.chatCompletionsUrl("http://h:1/v1/chat/completions", "openai_compatible"))
            .isEqualTo("http://h:1/v1/chat/completions");
        assertThat(BaseUrls.chatCompletionsUrl(null, "openai")).isEqualTo("https://api.openai.com/v1/chat/completions");
        assertThat(BaseUrls.chatCompletionsUrl("https://api.openai.com", "openai")).isEqualTo("https://api.openai.com/v1/chat/completions");
        assertThat(BaseUrls.modelsUrl("http://gpu-host:8000", "vllm")).isEqualTo("http://gpu-host:8000/v1/models");
    }

    @Test
    void normalisesAnthropicBaseUrls() {
        assertThat(BaseUrls.anthropicMessagesUrl(null)).isEqualTo("https://api.anthropic.com/v1/messages");
        assertThat(BaseUrls.anthropicMessagesUrl("https://api.anthropic.com/")).isEqualTo("https://api.anthropic.com/v1/messages");
        assertThat(BaseUrls.anthropicMessagesUrl("https://proxy.example.com/v1")).isEqualTo("https://proxy.example.com/v1/messages");
        assertThat(BaseUrls.anthropicModelsUrl("https://proxy.example.com")).isEqualTo("https://proxy.example.com/v1/models");
    }

    @Test
    void rejectsMissingOrInvalidBaseUrls() {
        assertThatThrownBy(() -> BaseUrls.chatCompletionsUrl("", "openai_compatible")).isInstanceOf(UpstreamException.class);
        assertThatThrownBy(() -> BaseUrls.chatCompletionsUrl("ftp://x", "vllm")).isInstanceOf(UpstreamException.class);
        assertThatThrownBy(() -> BaseUrls.chatCompletionsUrl("not a url", "vllm")).isInstanceOf(UpstreamException.class);
    }

    @Test
    void infersProviderTypeOnlyForKnownPrefixes() {
        assertThat(ProviderTypes.inferFromModel("gpt-4o")).isEqualTo("openai");
        assertThat(ProviderTypes.inferFromModel("claude-sonnet-4-5")).isEqualTo("anthropic");
        assertThat(ProviderTypes.inferFromModel("deepseek-chat")).isEqualTo("deepseek");
        assertThat(ProviderTypes.inferFromModel("glm-4.5")).isEqualTo("zhipu");
        assertThat(ProviderTypes.inferFromModel("my-finetune")).isNull();
        assertThat(ProviderTypes.isKeyOptional("vllm")).isTrue();
        assertThat(ProviderTypes.isKeyOptional("OLLAMA")).isTrue();
        assertThat(ProviderTypes.isKeyOptional("openai")).isFalse();
        assertThat(ProviderTypes.isSupported("azure")).isFalse();
    }
}
