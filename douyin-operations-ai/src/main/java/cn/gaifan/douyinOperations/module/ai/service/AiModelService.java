package cn.gaifan.douyinOperations.module.ai.service;

import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.vo.AiModelAdminVO;
import cn.gaifan.douyinOperations.module.ai.vo.AiModelSaveVO;

import java.util.List;
import java.util.Map;

public interface AiModelService {
    List<AiModel> listAll();

    /** 管理端列表：掩码 apiKey，附带 resolvedBaseUrl */
    List<AiModelAdminVO> listAllForAdmin();

    void save(AiModelSaveVO vo);
    void delete(Long id);
    void setDefault(Long id);

    /** 极简对话探测连通性 */
    Map<String, Object> testConnection(Long id);
}
