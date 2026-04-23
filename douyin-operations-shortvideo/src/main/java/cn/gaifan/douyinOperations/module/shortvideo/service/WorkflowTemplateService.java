package cn.gaifan.douyinOperations.module.shortvideo.service;

import cn.gaifan.douyinOperations.module.shortvideo.entity.SvWorkflowTemplate;

import java.util.List;
import java.util.Map;

/**
 * 工作流模板服务（Phase 4.4）
 */
public interface WorkflowTemplateService {

    /**
     * 查询用户可用模板（含系统预设 owner_id=0）
     */
    List<Map<String, Object>> listForOwner(Long ownerId);

    /**
     * 根据 ID 获取模板
     */
    SvWorkflowTemplate getById(Long id, Long ownerId);
}
