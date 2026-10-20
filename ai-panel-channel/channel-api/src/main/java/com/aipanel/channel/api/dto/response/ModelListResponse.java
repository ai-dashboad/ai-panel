package com.aipanel.channel.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * OpenAI-compatible model list response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Model list response (OpenAI-compatible)")
public class ModelListResponse {

    @Schema(description = "Object type", example = "list")
    private String object;

    @Schema(description = "List of available models")
    private List<ModelData> data;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Model information")
    public static class ModelData {
        @Schema(description = "Model ID")
        private String id;

        @Schema(description = "Object type", example = "model")
        private String object;

        @Schema(description = "Unix timestamp of creation")
        private Long created;

        @Schema(description = "Model owner")
        @Builder.Default
        private String ownedBy = "ai-panel";
    }
}
