package cn.gaifan.douyinOperations.module.photoavatar.service;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.photoavatar.vo.PhotoAvatarSaveVO;
import cn.gaifan.douyinOperations.module.photoavatar.vo.PhotoAvatarSearchVO;
import cn.gaifan.douyinOperations.module.photoavatar.vo.PhotoAvatarTaskVO;

import java.util.Map;

public interface PhotoAvatarService {

    Map<String, Object> overview(Long userId);

    PageResultVO<PhotoAvatarTaskVO> search(PhotoAvatarSearchVO searchVO, Long userId);

    Long createVideo(PhotoAvatarSaveVO saveVO, Long userId);

    PhotoAvatarTaskVO getStatus(Long taskId, Long userId);

    void delete(Long taskId, Long userId);

    Long retry(Long taskId, Long userId);
}
