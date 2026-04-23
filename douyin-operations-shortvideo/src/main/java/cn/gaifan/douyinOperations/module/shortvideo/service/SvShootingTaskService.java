package cn.gaifan.douyinOperations.module.shortvideo.service;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvShootingTaskSaveVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvShootingTaskSearchVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvShootingTaskVO;

import java.util.List;

public interface SvShootingTaskService {

    PageResultVO<SvShootingTaskVO> search(SvShootingTaskSearchVO vo, Long currentUserId, List<Long> visibleOwnerIds);

    SvShootingTaskVO get(Long id, Long currentUserId, List<Long> visibleOwnerIds);

    Long save(SvShootingTaskSaveVO vo, Long ownerId);

    void delete(Long id, Long ownerId);
}
