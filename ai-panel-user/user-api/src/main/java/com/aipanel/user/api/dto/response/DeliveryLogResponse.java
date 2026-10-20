package com.aipanel.user.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Delivery Log Response")
public class DeliveryLogResponse {

    @Schema(description = "Log ID", example = "1")
    private Long id;

    @Schema(description = "Associated order ID", example = "1")
    private Long orderId;

    @Schema(description = "Action performed: create_user, generate_key, send_delivery, retry")
    private String action;

    @Schema(description = "Action status: success, failed")
    private String status;

    @Schema(description = "Log message or error details")
    private String message;

    @Schema(description = "Log creation time")
    private LocalDateTime createdAt;
}
