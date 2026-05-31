package cn.gaifan.douyinOperations.module.shortvideo.service;

import java.util.List;

/**
 * 抖音 SEO 服务 (Phase 8)
 * 智能标签/最佳封面/发布时间/A/B 测试
 */
public interface DouyinSeoService {

    /**
     * 智能标签建议
     *
     * @param title       视频标题
     * @param description 视频描述
     * @param industry    行业
     * @return 推荐标签列表
     */
    List<String> suggestTags(String title, String description, String industry);

    /**
     * 最佳发布时间建议
     *
     * @param accountId 账号 ID
     * @return 推荐时段列表 (如 "18:00-20:00")
     */
    List<String> suggestPublishTime(Long accountId, List<Long> visibleOwnerIds);

    /**
     * 封面建议（从关键帧中选择）
     *
     * @param frameUrls 关键帧 URL 列表
     * @return 推荐封面 URL
     */
    String suggestCover(List<String> frameUrls);

    /**
     * A/B 测试标题建议
     *
     * @param baseTitle 原标题
     * @return 备选标题列表
     */
    List<String> suggestAbTestTitles(String baseTitle);
}
