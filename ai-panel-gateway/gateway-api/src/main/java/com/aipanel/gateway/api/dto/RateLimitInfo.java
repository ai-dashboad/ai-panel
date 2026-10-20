package com.aipanel.gateway.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Rate limit information DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Rate Limit Information")
public class RateLimitInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Rate limit value (requests per period)", example = "100")
    private long limit;

    @Schema(description = "Remaining requests in current period", example = "95")
    private long remaining;

    @Schema(description = "Timestamp when the limit resets (epoch seconds)", example = "1704067200")
    private long resetAt;

    @Schema(description = "Rate limit type", example = "user")
    private String limitType;

    /**
     * Check if request is allowed
     */
    public boolean isAllowed() {
        return remaining > 0;
    }

    /**
     * Get time until reset in seconds
     */
    public long getSecondsUntilReset() {
        long now = System.currentTimeMillis() / 1000;
        return Math.max(0, resetAt - now);
    }
}
