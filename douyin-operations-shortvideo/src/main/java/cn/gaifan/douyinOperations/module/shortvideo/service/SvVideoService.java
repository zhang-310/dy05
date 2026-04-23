package cn.gaifan.douyinOperations.module.shortvideo.service;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.*;

import java.util.List;
import java.util.Map;

public interface SvVideoService {

    PageResultVO<SvVideoVO> search(SvVideoSearchVO vo);

    SvVideoVO getById(Long id, List<Long> visibleOwnerIds);

    long save(SvVideoSaveVO vo, List<Long> visibleOwnerIds);

    void delete(Long id, List<Long> visibleOwnerIds);

    void incrementViewCount(Long id, List<Long> visibleOwnerIds);

    /** 视频数据趋势（折线图），按日期返回 viewCount/likeCount 等 */
    List<Map<String, Object>> getDataTrend(Long videoId, List<Long> visibleOwnerIds);
}
