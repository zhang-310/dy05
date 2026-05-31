package cn.gaifan.douyinOperations.module.digitalhuman.service;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.digitalhuman.vo.DigitalHumanSaveVO;
import cn.gaifan.douyinOperations.module.digitalhuman.vo.DigitalHumanSearchVO;
import cn.gaifan.douyinOperations.module.digitalhuman.vo.DigitalHumanTaskVO;

import java.util.Map;

public interface DigitalHumanService {

    Map<String, Object> overview(Long userId);

    PageResultVO<DigitalHumanTaskVO> search(DigitalHumanSearchVO searchVO, Long userId);

    Long createTask(DigitalHumanSaveVO saveVO, Long userId);

    DigitalHumanTaskVO getStatus(Long taskId, Long userId);

    void delete(Long taskId, Long userId);
}
