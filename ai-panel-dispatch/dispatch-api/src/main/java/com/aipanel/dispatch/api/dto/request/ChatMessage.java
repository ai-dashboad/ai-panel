package com.aipanel.dispatch.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Chat message DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Chat Message")
public class ChatMessage {

    @NotBlank(message = "Role is required")
    @Pattern(regexp = "^(system|user|assistant|function|tool)$",
             message = "Role must be system/user/assistant/function/tool")
    @Schema(description = "Message role", example = "user",
            allowableValues = {"system", "user", "assistant", "function", "tool"},
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String role;

    @NotBlank(message = "Content is required")
    @Size(max = 100000, message = "Content max 100000 characters")
    @Schema(description = "Message content", example = "Hello, how are you?",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;

    @Size(max = 64, message = "Name max 64 characters")
    @Schema(description = "Sender name (optional)", example = "my_assistant")
    private String name;

    /**
     * Create a system message
     */
    public static ChatMessage system(String content) {
        return ChatMessage.builder().role("system").content(content).build();
    }

    /**
     * Create a user message
     */
    public static ChatMessage user(String content) {
        return ChatMessage.builder().role("user").content(content).build();
    }

    /**
     * Create an assistant message
     */
    public static ChatMessage assistant(String content) {
        return ChatMessage.builder().role("assistant").content(content).build();
    }
}
