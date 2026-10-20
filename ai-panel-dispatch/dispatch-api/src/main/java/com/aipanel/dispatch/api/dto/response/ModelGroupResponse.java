package com.aipanel.dispatch.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Model group (pricing pool)")
public class ModelGroupResponse {

    private Long id;
    private String code;
    private String name;
    private BigDecimal priceMultiplier;
    private String description;
    private Boolean enabled;
    private Integer sortOrder;
    private Integer modelCount;
    private List<String> modelIds;
}
