package cn.gaifan.douyinOperations.module.benchmark.service;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.benchmark.vo.*;

import java.util.List;

/**
 * 对标视频管理服务
 */
public interface BenchmarkVideoService {

    /**
     * 分页查询视频
     */
    PageResultVO<BenchmarkVideoVO> search(BenchmarkVideoSearchVO searchVO, Long ownerId);

    /**
     * 根据ID获取视频
     */
    BenchmarkVideoVO getById(Long id, Long ownerId);

    /**
     * 保存视频（新增或更新）
     */
    BenchmarkVideoVO save(BenchmarkVideoSaveVO saveVO, Long ownerId);

    /**
     * 删除视频
     */
    void delete(Long id, Long ownerId);

    /**
     * 采集账号视频
     * @param collectVO 采集参数
     * @param ownerId 用户ID
     * @return 采集到的视频列表
     */
    List<BenchmarkVideoVO> collectAccountVideos(CollectAccountVideosVO collectVO, Long ownerId);

    /**
     * 更新视频分析状态
     */
    void updateAnalysisStatus(Long videoId, String status);
}
