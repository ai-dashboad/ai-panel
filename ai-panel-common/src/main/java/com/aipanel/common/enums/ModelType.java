package com.aipanel.common.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/**
 * AI Model capability types
 */
@Getter
@Schema(description = "AI Model Types")
public enum ModelType {

    CHAT("chat", "Chat/Conversation", "Conversational AI models"),
    COMPLETION("completion", "Text Completion", "Text completion models"),
    EMBEDDING("embedding", "Embedding", "Text embedding/vectorization models"),
    IMAGE_GENERATION("image_generation", "Image Generation", "Image generation models like DALL-E"),
    IMAGE_EDIT("image_edit", "Image Editing", "Image editing and manipulation models"),
    AUDIO_TRANSCRIPTION("audio_transcription", "Audio Transcription", "Speech-to-text models like Whisper"),
    AUDIO_SPEECH("audio_speech", "Text-to-Speech", "Text-to-speech models"),
    VISION("vision", "Vision/Multimodal", "Multimodal models with image understanding"),
    CODE("code", "Code Generation", "Specialized code generation models"),
    MODERATION("moderation", "Content Moderation", "Content moderation models");

    private final String code;
    private final String displayName;
    private final String description;

    ModelType(String code, String displayName, String description) {
        this.code = code;
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * Get ModelType by code
     */
    public static ModelType fromCode(String code) {
        if (code == null) {
            return CHAT;
        }
        for (ModelType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        return CHAT;
    }

    /**
     * Check if model type supports streaming
     */
    public boolean supportsStreaming() {
        return this == CHAT || this == COMPLETION || this == CODE;
    }

    /**
     * Check if model type produces text output
     */
    public boolean producesTextOutput() {
        return this == CHAT || this == COMPLETION || this == CODE ||
               this == AUDIO_TRANSCRIPTION;
    }
}
