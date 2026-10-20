package com.aipanel.channel.core.upstream;

import org.springframework.stereotype.Component;

/**
 * Adapter for OpenAI itself. Same wire protocol as {@link OpenAiCompatibleAdapter}, but an API key
 * is always required and the base URL defaults to {@code https://api.openai.com/v1}.
 */
@Component
public class OpenAiAdapter extends OpenAiCompatibleAdapter {

    public OpenAiAdapter() {
        super();
    }

    public OpenAiAdapter(UpstreamHttp http) {
        super(http);
    }

    @Override
    public boolean supports(String providerType) {
        return ProviderTypes.OPENAI.equals(ProviderTypes.normalize(providerType));
    }

    @Override
    protected boolean keyRequired(UpstreamTarget target) {
        return true;
    }
}
