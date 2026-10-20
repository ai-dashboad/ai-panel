package com.aipanel.common.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * User subscription plan types
 */
@Getter
@Schema(description = "User Plan Types")
public enum PlanType {

    FREE("free", "Free", 10, 100, 100_000L, BigDecimal.ZERO),
    BASIC("basic", "Basic", 60, 1000, 1_000_000L, new BigDecimal("9.90")),
    PRO("pro", "Pro", 300, 10000, 10_000_000L, new BigDecimal("49.00")),
    ENTERPRISE("enterprise", "Enterprise", 1000, -1, -1L, new BigDecimal("199.00"));

    private final String code;
    private final String displayName;
    private final int requestsPerMinute;
    private final int requestsPerDay;      // -1 means unlimited
    private final long tokensPerMonth;     // -1 means unlimited
    private final BigDecimal monthlyPrice; // USD

    PlanType(String code, String displayName, int requestsPerMinute,
             int requestsPerDay, long tokensPerMonth, BigDecimal monthlyPrice) {
        this.code = code;
        this.displayName = displayName;
        this.requestsPerMinute = requestsPerMinute;
        this.requestsPerDay = requestsPerDay;
        this.tokensPerMonth = tokensPerMonth;
        this.monthlyPrice = monthlyPrice;
    }

    /**
     * Get PlanType by code
     */
    public static PlanType fromCode(String code) {
        if (code == null) {
            return FREE;
        }
        for (PlanType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        return FREE;
    }

    /**
     * Check if plan has unlimited requests per day
     */
    public boolean hasUnlimitedDailyRequests() {
        return this.requestsPerDay < 0;
    }

    /**
     * Check if plan has unlimited tokens per month
     */
    public boolean hasUnlimitedMonthlyTokens() {
        return this.tokensPerMonth < 0;
    }

    /**
     * Check if this is a paid plan
     */
    public boolean isPaid() {
        return this.monthlyPrice.compareTo(BigDecimal.ZERO) > 0;
    }
}
