package com.aipanel.dispatch.data.service;

import com.aipanel.dispatch.data.mapper.ModelGroupMapper;
import com.aipanel.dispatch.data.mapper.ModelGroupMemberMapper;
import com.aipanel.dispatch.data.po.ModelGroupMemberPO;
import com.aipanel.dispatch.data.po.ModelGroupPO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ModelGroupDataService {

    private final ModelGroupMapper modelGroupMapper;
    private final ModelGroupMemberMapper modelGroupMemberMapper;

    public List<ModelGroupPO> listEnabledGroups() {
        try {
            return modelGroupMapper.findAllEnabled();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public List<ModelGroupPO> listAllGroups() {
        try {
            return modelGroupMapper.selectList(new LambdaQueryWrapper<ModelGroupPO>()
                .eq(ModelGroupPO::getDeleted, false)
                .orderByAsc(ModelGroupPO::getSortOrder));
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public ModelGroupPO getByCode(String code) {
        return modelGroupMapper.selectOne(new LambdaQueryWrapper<ModelGroupPO>()
            .eq(ModelGroupPO::getGroupCode, code)
            .eq(ModelGroupPO::getDeleted, false)
            .last("LIMIT 1"));
    }

    public List<ModelGroupMemberPO> listMembers(Long groupId) {
        try {
            return modelGroupMemberMapper.findByGroupId(groupId);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public Map<String, ModelGroupPO> mapPrimaryGroupByModelId() {
        List<ModelGroupPO> groups = listEnabledGroups();
        if (groups.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, ModelGroupPO> byId = groups.stream()
            .collect(Collectors.toMap(ModelGroupPO::getId, g -> g, (a, b) -> a));

        return groups.stream()
            .flatMap(g -> listMembers(g.getId()).stream()
                .map(m -> Map.entry(m.getModelId(), byId.get(g.getId()))))
            .filter(e -> e.getValue() != null)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a));
    }

    public ModelGroupPO save(ModelGroupPO group) {
        if (group.getId() == null) {
            modelGroupMapper.insert(group);
        } else {
            modelGroupMapper.updateById(group);
        }
        return group;
    }
}
