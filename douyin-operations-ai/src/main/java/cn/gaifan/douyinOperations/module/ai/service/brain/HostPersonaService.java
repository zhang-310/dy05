package cn.gaifan.douyinOperations.module.ai.service.brain;

import cn.gaifan.douyinOperations.module.ai.entity.AiHostPersona;

import java.util.List;
import java.util.Map;

/**
 * 五位主播人设服务
 * 提供人设配置、风格向量、因果因子等
 */
public interface HostPersonaService {

    /** 获取所有人设（按 sort_order） */
    List<AiHostPersona> listAll();

    /** 按 hostCode 获取 */
    AiHostPersona getByCode(String hostCode);

    /** 按 ID 获取 */
    AiHostPersona getById(Long id);

    /** 获取主播的因果推理因子（用于 IndustryCausalEngine） */
    Map<String, Double> getBayesFactors(String hostCode);

    /** 获取主播的风格向量（用于多模态一致性） */
    Map<String, String> getStyleVector(String hostCode);

    /** 获取主播的 AI 优先级列表 */
    List<String> getAiPriorities(String hostCode);

    /** 获取流量路径：入口→转化→B端 */
    List<AiHostPersona> getFlowPath();

    boolean isAvailable();
}
