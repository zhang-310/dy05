package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvWorkflowTemplate;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvWorkflowTemplateRepository;
import cn.gaifan.douyinOperations.module.shortvideo.service.WorkflowTemplateService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工作流模板服务实现（Phase 4.4）
 */
@Service
public class WorkflowTemplateServiceImpl implements WorkflowTemplateService {

    @Resource
    private SvWorkflowTemplateRepository workflowTemplateRepository;

    @Override
    public List<Map<String, Object>> listForOwner(Long ownerId) {
        if (ownerId == null) return List.of();
        List<SvWorkflowTemplate> list = workflowTemplateRepository.findAvailableForOwner(ownerId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (SvWorkflowTemplate t : list) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", t.getId());
            m.put("templateName", t.getTemplateName());
            m.put("description", t.getDescription());
            m.put("steps", t.getSteps());
            m.put("isSystem", t.getIsSystem());
            m.put("createTime", t.getCreateTime());
            result.add(m);
        }
        return result;
    }

    @Override
    public SvWorkflowTemplate getById(Long id, Long ownerId) {
        if (id == null) throw new BusinessException(ErrorCode.VALIDATION_FAIL, "id不能为空");
        SvWorkflowTemplate t = workflowTemplateRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "模板不存在"));
        if (t.getOwnerId() != 0 && !t.getOwnerId().equals(ownerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权限访问该模板");
        }
        return t;
    }
}
