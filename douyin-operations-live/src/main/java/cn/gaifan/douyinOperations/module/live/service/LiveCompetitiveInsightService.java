package cn.gaifan.douyinOperations.module.live.service;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveCompetitiveInsightSaveVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveCompetitiveInsightSearchVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveCompetitiveInsightVO;

public interface LiveCompetitiveInsightService {

    PageResultVO<LiveCompetitiveInsightVO> search(LiveCompetitiveInsightSearchVO vo, Long ownerId);

    LiveCompetitiveInsightVO getById(Long id, Long ownerId);

    long save(LiveCompetitiveInsightSaveVO vo, Long ownerId);

    void delete(Long id, Long ownerId);
}
