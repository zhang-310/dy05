package cn.gaifan.douyinOperations.module.live.service;

import java.util.List;
import java.util.Map;

/**
 * 内容素材库服务：段子库、鸡汤库、名言库、互动游戏库、BGM库、表演配合库、风险管控库。
 * 为直播话术和短视频脚本提供素材注入能力。
 */
public interface ContentMaterialService {

    /**
     * 获取随机素材
     * @param materialType 素材类型：joke/chicken_soup/quote/interactive_game/bgm/performance/risk_control
     * @param category 子分类（可选）
     * @param count 数量
     * @return 素材列表
     */
    List<Map<String, Object>> getRandomMaterials(String materialType, String category, int count);

    /**
     * 根据人设获取匹配素材
     * @param materialType 素材类型
     * @param personaType 人设类型
     * @param ageRange 年龄段
     * @param count 数量
     * @return 匹配的素材列表
     */
    List<Map<String, Object>> getMaterialsByPersona(String materialType, String personaType, String ageRange, int count);

    /**
     * 获取素材注入 Prompt
     * @param materialType 素材类型
     * @param category 子分类
     * @return 可直接注入 Prompt 的素材文本
     */
    String buildMaterialPrompt(String materialType, String category);

    /**
     * 获取所有素材分类
     * @return 分类树
     */
    Map<String, List<String>> getMaterialCategories();

    /**
     * 获取表演指导提示
     * @param category 表演类型（expression_love/expression_funny/gesture_emphasis/gesture_emotion/voice_rhythm/voice_emotion）
     * @return Prompt 文本
     */
    String buildPerformancePrompt(String category);

    /**
     * 风险话术匹配器：按风险类型查询应对话术
     * @param riskType 风险类型（sensitive_topic/cold_scene/negative_comment/platform_rule/emotional_outbreak）
     * @return 应对话术列表
     */
    List<String> matchRiskScripts(String riskType);

    /**
     * 获取用于 huashu 知识库导入的素材（夫妻/婆媳/励志/名言等，与 chat 话题、ChunkLabeler type 对齐）
     * @return 每项含 type、category、content、typeHint（type:家庭爱情/名言/搞笑幽默/正能量）
     */
    List<Map<String, Object>> getAllMaterialsForHuashuExport();
}
