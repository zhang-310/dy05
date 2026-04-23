package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvOpsReportTemplate;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvOpsReportTemplateRepository;
import cn.gaifan.douyinOperations.module.shortvideo.service.OpsReportTemplateService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class OpsReportTemplateServiceImpl implements OpsReportTemplateService {

    @Resource
    private SvOpsReportTemplateRepository repository;

    @Override
    public List<Map<String, Object>> list(Long ownerId) {
        if (ownerId == null) {
            return List.of();
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (SvOpsReportTemplate t : repository.findByOwnerIdAndDeletedOrderByUpdateTimeDesc(ownerId, 0)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", t.getId());
            m.put("name", t.getName());
            m.put("definitionJson", t.getDefinitionJson());
            m.put("updateTime", t.getUpdateTime());
            out.add(m);
        }
        return out;
    }

    @Override
    public Map<String, Object> findOne(Long ownerId, Long id) {
        if (ownerId == null || id == null) {
            return null;
        }
        return repository.findByIdAndOwnerIdAndDeleted(id, ownerId, 0)
                .map(t -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", t.getId());
                    m.put("name", t.getName());
                    m.put("definitionJson", t.getDefinitionJson());
                    m.put("updateTime", t.getUpdateTime());
                    return m;
                })
                .orElse(null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long save(Long ownerId, Map<String, Object> body) {
        if (ownerId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        Long id = body != null && body.get("id") instanceof Number n ? n.longValue() : null;
        String name = body != null && body.get("name") instanceof String s ? s.trim() : "";
        if (!StringUtils.hasText(name)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "name 必填");
        }
        String def = body != null && body.get("definitionJson") instanceof String s ? s : null;
        SvOpsReportTemplate e;
        if (id != null && id > 0) {
            e = repository.findByIdAndOwnerIdAndDeleted(id, ownerId, 0)
                    .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "模板不存在"));
        } else {
            e = new SvOpsReportTemplate();
            e.setOwnerId(ownerId);
        }
        e.setName(name);
        e.setDefinitionJson(def);
        repository.save(e);
        return e.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long ownerId, Long id) {
        if (ownerId == null || id == null) {
            return;
        }
        SvOpsReportTemplate t = repository.findByIdAndOwnerIdAndDeleted(id, ownerId, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "模板不存在"));
        t.setDeleted(1);
        repository.save(t);
    }
}
