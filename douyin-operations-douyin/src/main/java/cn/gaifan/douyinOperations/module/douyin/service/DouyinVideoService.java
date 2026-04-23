package cn.gaifan.douyinOperations.module.douyin.service;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.douyin.vo.*;

/**
 * 抖音视频服务接口
 */
public interface DouyinVideoService {

    /**
     * 分页查询视频
     */
    PageResultVO<DouyinVideoVO> search(DouyinVideoSearchVO vo);

    /**
     * 获取视频详情
     */
    DouyinVideoVO getVideo(Long id);

    /**
     * 保存视频
     */
    long saveVideo(DouyinVideoSaveVO vo);

    /**
     * 同步视频（调用 API 获取并更新）
     */
    void syncVideos(Long accountId);
}
