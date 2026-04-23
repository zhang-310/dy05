package cn.gaifan.douyinOperations.module.live.service;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveCompetitorScriptSaveVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveCompetitorScriptSearchVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveCompetitorScriptVO;

public interface LiveCompetitorScriptService {

    PageResultVO<LiveCompetitorScriptVO> search(LiveCompetitorScriptSearchVO vo, Long userId);

    LiveCompetitorScriptVO getById(Long id, Long userId);

    long save(LiveCompetitorScriptSaveVO vo, Long userId);

    void delete(Long id, Long userId);
}
