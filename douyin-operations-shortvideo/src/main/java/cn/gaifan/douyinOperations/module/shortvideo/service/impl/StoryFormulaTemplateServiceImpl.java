package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvStoryFormulaTemplate;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvStoryFormulaTemplateRepository;
import cn.gaifan.douyinOperations.module.shortvideo.service.StoryFormulaTemplateService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class StoryFormulaTemplateServiceImpl implements StoryFormulaTemplateService {

    @Resource
    private SvStoryFormulaTemplateRepository repository;

    @Override
    public List<Map<String, Object>> listUserTemplatesAsMaps(Long ownerId) {
        if (ownerId == null) {
            return List.of();
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (SvStoryFormulaTemplate t : repository.findByOwnerIdAndDeletedOrderByUpdateTimeDesc(ownerId, 0)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", t.getId());
            m.put("code", t.getCode());
            m.put("title", t.getName());
            m.put("name", t.getName());
            m.put("description", t.getDescription());
            m.put("beats", t.getDescription());
            m.put("formulaJson", t.getFormulaJson());
            m.put("source", "db");
            out.add(m);
        }
        return out;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long save(Long ownerId, Map<String, Object> body) {
        if (ownerId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        Long id = body != null && body.get("id") instanceof Number n ? n.longValue() : null;
        String code = body != null && body.get("code") instanceof String s ? s.trim() : "";
        String name = body != null && body.get("name") instanceof String s ? s.trim() : "";
        if (!StringUtils.hasText(code)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "code 必填");
        }
        if (!StringUtils.hasText(name) && body != null && body.get("title") instanceof String t) {
            name = t.trim();
        }
        if (!StringUtils.hasText(name)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "name/title 必填");
        }
        String description = body != null && body.get("description") instanceof String s ? s : null;
        if (!StringUtils.hasText(description) && body != null && body.get("beats") instanceof String b) {
            description = b;
        }
        String formulaJson = body != null && body.get("formulaJson") instanceof String s ? s : null;

        SvStoryFormulaTemplate entity;
        if (id != null && id > 0) {
            entity = repository.findByIdAndOwnerIdAndDeleted(id, ownerId, 0)
                    .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "模板不存在"));
        } else {
            entity = new SvStoryFormulaTemplate();
            entity.setOwnerId(ownerId);
            entity.setCode(code);
        }
        entity.setName(name);
        entity.setDescription(description);
        entity.setFormulaJson(formulaJson);
        repository.save(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long ownerId, Long id) {
        if (ownerId == null || id == null) {
            return;
        }
        SvStoryFormulaTemplate t = repository.findByIdAndOwnerIdAndDeleted(id, ownerId, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "模板不存在"));
        t.setDeleted(1);
        repository.save(t);
    }
}
