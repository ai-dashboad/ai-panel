package com.aipanel.dispatch.controller;

import com.aipanel.common.domain.R;
import com.aipanel.dispatch.api.dto.response.ModelGroupResponse;
import com.aipanel.dispatch.data.po.ModelGroupMemberPO;
import com.aipanel.dispatch.data.po.ModelGroupPO;
import com.aipanel.dispatch.data.service.ModelGroupDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/model-groups")
@Tag(name = "Model Groups", description = "Enterprise pricing pools / model groups")
@Slf4j
@RequiredArgsConstructor
public class ModelGroupController {

    private final ModelGroupDataService modelGroupDataService;

    @GetMapping
    @Operation(summary = "List model groups")
    public R<List<ModelGroupResponse>> listGroups(
            @RequestParam(defaultValue = "true") boolean enabledOnly
    ) {
        List<ModelGroupPO> groups = enabledOnly
            ? modelGroupDataService.listEnabledGroups()
            : modelGroupDataService.listAllGroups();

        List<ModelGroupResponse> responses = groups.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
        return R.ok(responses);
    }

    @GetMapping("/{code}")
    @Operation(summary = "Get model group by code")
    public R<ModelGroupResponse> getGroup(@PathVariable String code) {
        ModelGroupPO group = modelGroupDataService.getByCode(code);
        if (group == null) {
            return R.fail(404, "Group not found: " + code);
        }
        return R.ok(toResponse(group));
    }

    @PostMapping
    @Operation(summary = "Create or update model group")
    public R<ModelGroupResponse> saveGroup(@RequestBody Map<String, Object> body) {
        ModelGroupPO group = new ModelGroupPO();
        if (body.get("id") != null) {
            group.setId(Long.valueOf(body.get("id").toString()));
        }
        group.setGroupCode((String) body.get("code"));
        group.setGroupName((String) body.get("name"));
        if (body.get("priceMultiplier") != null) {
            group.setPriceMultiplier(new BigDecimal(body.get("priceMultiplier").toString()));
        } else {
            group.setPriceMultiplier(BigDecimal.ONE);
        }
        group.setDescription((String) body.get("description"));
        group.setEnabled(body.get("enabled") == null || Boolean.parseBoolean(body.get("enabled").toString()));
        group.setSortOrder(body.get("sortOrder") != null
            ? Integer.parseInt(body.get("sortOrder").toString()) : 100);
        group.setDeleted(false);

        ModelGroupPO saved = modelGroupDataService.save(group);
        return R.ok(toResponse(saved));
    }

    private ModelGroupResponse toResponse(ModelGroupPO po) {
        List<ModelGroupMemberPO> members = modelGroupDataService.listMembers(po.getId());
        return ModelGroupResponse.builder()
            .id(po.getId())
            .code(po.getGroupCode())
            .name(po.getGroupName())
            .priceMultiplier(po.getPriceMultiplier())
            .description(po.getDescription())
            .enabled(po.getEnabled())
            .sortOrder(po.getSortOrder())
            .modelCount(members.size())
            .modelIds(members.stream().map(ModelGroupMemberPO::getModelId).collect(Collectors.toList()))
            .build();
    }
}
