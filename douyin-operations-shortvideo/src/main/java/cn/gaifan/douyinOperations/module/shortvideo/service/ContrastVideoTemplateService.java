package cn.gaifan.douyinOperations.module.shortvideo.service;

import java.util.List;
import java.util.Map;

/**
 * 反差变装短视频模板服务。
 * 支持社会身份反差和个人状态反差两种核心模板，
 * 提供三段式笑点配置和 BGM 策略推荐。
 */
public interface ContrastVideoTemplateService {

    /**
     * 获取反差变装分镜模板
     * @param contrastType 反差类型：social_identity_contrast / personal_state_contrast
     * @param duration 视频时长（秒）
     * @return 分镜模板
     */
    Map<String, Object> getShotTemplate(String contrastType, int duration);

    /**
     * 获取三段式笑点配置
     * @param contrastType 反差类型
     * @return 笑点配置
     */
    Map<String, Object> getComedyConfig(String contrastType);

    /**
     * 获取 BGM 策略推荐
     * @param contrastType 反差类型
     * @return BGM 策略
     */
    Map<String, Object> getBgmStrategy(String contrastType);

    /**
     * 获取所有可用的反差模板列表
     * @return 模板列表
     */
    List<Map<String, Object>> listTemplates();

    /**
     * 获取固定时长分镜预设（覆盖 15s/25s/30s/45s/60s/90s）
     * @param contrastType 反差类型
     * @param preset 时长预设，如 "15s"、"30s"、"60s"
     * @return 预设模板
     */
    Map<String, Object> getPresetTemplate(String contrastType, String preset);
}
