package cn.gaifan.douyinOperations.module.shortvideo.service;

import cn.gaifan.douyinOperations.module.shortvideo.entity.SvViralVideo;
import cn.gaifan.douyinOperations.module.shortvideo.vo.ViralCollectVO;

import java.util.List;

/**
 * 爆款库服务
 */
public interface ViralVideoService {

    /**
     * 获取爆款列表
     * @param mode platform=平台爆款(owner_id=0) / my=我的收藏(sv_viral_favorite)+自建(owner_id=userId)
     */
    List<SvViralVideo> listViralVideos(Long userId, String mode, String category, String sortBy, Integer page, Integer rows);

    /**
     * 收藏爆款视频
     */
    Long collectViralVideo(ViralCollectVO vo, Long userId);

    /**
     * 获取爆款详情
     */
    SvViralVideo getViralVideo(Long id, Long userId);

    /**
     * 删除收藏
     */
    void deleteViralVideo(Long id, Long userId);

    /**
     * 触发AI分析
     */
    void triggerAnalysis(Long id, Long userId);

    /**
     * 爆款复刻（生成创作方案）
     */
    String replicateViral(Long id, Long userId);

    /**
     * 获取推荐爆款（系统推荐）
     */
    List<SvViralVideo> getRecommendedVirals(Long userId, Integer limit);
}
