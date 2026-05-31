package cn.gaifan.douyinOperations.module.drama.service;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.drama.vo.*;

import java.util.Map;

public interface DramaService {

    Map<String, Object> overview(Long userId);

    PageResultVO<DramaProjectVO> search(DramaSearchVO searchVO, Long userId);

    Long createProject(DramaSaveVO saveVO, Long userId);

    DramaProjectVO getDetail(Long projectId, Long userId);

    Long update(DramaUpdateVO updateVO, Long userId);

    void delete(Long projectId, Long userId);

    void changeStatus(Long projectId, String newStatus, Long userId);

    Map<String, Object> exportToShortvideoMaker(Long projectId, Long userId, String traceId);
}
