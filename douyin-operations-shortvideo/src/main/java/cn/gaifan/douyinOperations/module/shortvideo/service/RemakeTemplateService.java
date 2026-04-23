package cn.gaifan.douyinOperations.module.shortvideo.service;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvRemakeTemplate;
import cn.gaifan.douyinOperations.module.shortvideo.vo.RemakeTemplateSearchVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.RemakeTemplateSaveVO;

import java.util.Map;

/**
 * 二创模板服务（Phase 5）
 */
public interface RemakeTemplateService {

    SvRemakeTemplate save(RemakeTemplateSaveVO vo, Long ownerId);

    void delete(Long id, Long ownerId);

    PageResultVO<SvRemakeTemplate> search(RemakeTemplateSearchVO vo, Long ownerId);

    SvRemakeTemplate getById(Long id);

    SvRemakeTemplate createFromViralAnalysis(Long viralVideoId, String remakeType, Long ownerId);

    String generateFromTemplate(Long templateId, Map<String, String> variables, Long ownerId);
}
