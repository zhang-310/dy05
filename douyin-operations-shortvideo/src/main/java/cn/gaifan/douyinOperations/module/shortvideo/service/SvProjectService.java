package cn.gaifan.douyinOperations.module.shortvideo.service;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvProjectSaveVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvProjectSearchVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvProjectVO;

/**
 * 短视频项目 Service
 */
public interface SvProjectService {

    PageResultVO<SvProjectVO> search(SvProjectSearchVO vo, Long ownerId, java.util.List<Long> visibleOwnerIds);

    SvProjectVO get(Long id, Long ownerId, java.util.List<Long> visibleOwnerIds);

    Long save(SvProjectSaveVO vo, Long ownerId);

    void delete(Long id, Long ownerId);
}
