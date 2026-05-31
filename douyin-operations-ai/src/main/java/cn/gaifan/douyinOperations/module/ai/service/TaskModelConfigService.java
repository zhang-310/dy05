package cn.gaifan.douyinOperations.module.ai.service;

import cn.gaifan.douyinOperations.module.ai.entity.AiTaskModelConfig;
import cn.gaifan.douyinOperations.module.ai.vo.TaskModelConfigRowVO;

import java.util.List;

/**
 * 任务-模型配置服务
 */
public interface TaskModelConfigService {

    List<TaskModelConfigRowVO> listAll();

    TaskModelConfigRowVO getRowById(Long id);

    AiTaskModelConfig getById(Long id);

    Long save(AiTaskModelConfig config);

    void delete(Long id);
}
