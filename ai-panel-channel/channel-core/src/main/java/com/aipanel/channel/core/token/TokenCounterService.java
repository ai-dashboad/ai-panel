package com.aipanel.channel.core.token;

import com.aipanel.channel.api.dto.request.ChatCompletionRequest;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import com.knuddels.jtokkit.api.ModelType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Token counter service using jtokkit library
 */
@Slf4j
@Service
public class TokenCounterService {

    private final EncodingRegistry registry;

    public TokenCounterService() {
        this.registry = Encodings.newDefaultEncodingRegistry();
    }

    /**
     * Count tokens in a list of messages
     */
    public int countMessages(List<ChatCompletionRequest.Message> messages) {
        int totalTokens = 0;
        Encoding encoding = registry.getEncodingForModel(ModelType.GPT_4);

        for (ChatCompletionRequest.Message message : messages) {
            // Add tokens for role
            totalTokens += 4; // <|im_start|>{role}\n ... \n<|im_end|>

            // Add tokens for content
            if (message.getContent() instanceof String) {
                totalTokens += encoding.countTokens((String) message.getContent());
            } else if (message.getContent() instanceof List<?> parts) {
                // Content parts: count text, use a flat estimate per image instead of the (base64) URL
                for (Object part : parts) {
                    if (part instanceof java.util.Map<?, ?> p && "text".equals(p.get("type")) && p.get("text") != null) {
                        totalTokens += encoding.countTokens(String.valueOf(p.get("text")));
                    } else {
                        totalTokens += 85;
                    }
                }
            } else if (message.getContent() != null) {
                totalTokens += encoding.countTokens(message.getContent().toString());
            }

            if (message.getToolCalls() != null) {
                for (ChatCompletionRequest.ToolCall call : message.getToolCalls()) {
                    if (call.getFunction() != null) {
                        totalTokens += encoding.countTokens(String.valueOf(call.getFunction().getName()))
                            + encoding.countTokens(String.valueOf(call.getFunction().getArguments()));
                    }
                }
            }

            // Add tokens for name if present
            if (message.getName() != null) {
                totalTokens += encoding.countTokens(message.getName()) + 1;
            }
        }

        // Add base tokens for formatting
        totalTokens += 3; // Every reply is primed with <|im_start|>assistant

        return totalTokens;
    }

    /**
     * Count tokens in a string
     */
    public int countTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        Encoding encoding = registry.getEncodingForModel(ModelType.GPT_4);
        return encoding.countTokens(text);
    }

    /**
     * Count tokens for a specific model
     */
    public int countTokens(String text, String model) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        Encoding encoding;
        try {
            // Try to get encoding for specific model
            ModelType modelType = getModelType(model);
            encoding = registry.getEncodingForModel(modelType);
        } catch (Exception e) {
            // Fall back to cl100k_base (GPT-4 encoding)
            encoding = registry.getEncoding(EncodingType.CL100K_BASE);
        }

        return encoding.countTokens(text);
    }

    private ModelType getModelType(String model) {
        if (model.startsWith("gpt-4")) {
            return ModelType.GPT_4;
        } else if (model.startsWith("gpt-3.5")) {
            return ModelType.GPT_3_5_TURBO;
        } else {
            // Default to GPT-4 encoding for other models
            return ModelType.GPT_4;
        }
    }
}
