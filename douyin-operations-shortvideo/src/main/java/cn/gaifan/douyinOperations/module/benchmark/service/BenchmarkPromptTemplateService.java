package cn.gaifan.douyinOperations.module.benchmark.service;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.benchmark.vo.BenchmarkPromptTemplateSearchVO;
import cn.gaifan.douyinOperations.module.benchmark.vo.BenchmarkPromptTemplateSaveVO;
import cn.gaifan.douyinOperations.module.benchmark.vo.BenchmarkPromptTemplateVO;

import java.util.List;

/**
 * Prompt 模板管理服务
 */
public interface BenchmarkPromptTemplateService {

    /**
     * 分页查询模板列表
     */
    PageResultVO<BenchmarkPromptTemplateVO> search(BenchmarkPromptTemplateSearchVO searchVO, Long ownerId);

    /**
     * 根据 ID 查询模板详情
     */
    BenchmarkPromptTemplateVO getById(Long id, Long ownerId);

    /**
     * 保存模板（新增或更新）
     */
    BenchmarkPromptTemplateVO save(BenchmarkPromptTemplateSaveVO saveVO, Long ownerId);

    /**
     * 删除模板
     */
    void delete(Long id, Long ownerId);

    /**
     * 激活/停用模板
     */
    void toggleActive(Long id, Boolean isActive, Long ownerId);

    /**
     * 获取指定场景类型的激活模板列表
     */
    List<BenchmarkPromptTemplateVO> getActiveTemplatesByScene(String sceneType, Long ownerId);

    /**
     * 获取指定行业的激活模板列表
     */
    List<BenchmarkPromptTemplateVO> getActiveTemplatesByIndustry(String industry, Long ownerId);

    /**
     * 根据模板编码获取模板
     */
    BenchmarkPromptTemplateVO getByTemplateCode(String templateCode, Long ownerId);

    /**
     * 更新模板使用统计
     */
    void updateUsageStats(Long templateId, Double rating);
}
