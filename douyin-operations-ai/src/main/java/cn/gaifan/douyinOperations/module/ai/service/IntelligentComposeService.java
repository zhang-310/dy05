package cn.gaifan.douyinOperations.module.ai.service;

import java.util.List;

/**
 * 智能合成服务 (Phase 8)
 * BeatSync 节奏卡点 + 智能转场 + 张力曲线 + 开头钩子
 */
public interface IntelligentComposeService {

    /**
     * 根据 BGM 节奏计算卡点时间
     *
     * @param bgmUrl  BGM 音频 URL
     * @param clipCount 片段数量
     * @return 每个片段的起止时间（秒）
     */
    List<BeatSegment> computeBeatSync(String bgmUrl, int clipCount);

    /**
     * 智能转场建议
     *
     * @param prevClip 上一片段描述
     * @param nextClip 下一片段描述
     * @return 推荐转场类型 (fade/cut/dissolve/wipe)
     */
    String suggestTransition(String prevClip, String nextClip);

    /**
     * 张力曲线建议（用于片段时长分配）
     *
     * @param totalDuration 总时长（秒）
     * @param clipCount     片段数
     * @return 各片段时长占比 (0-1)
     */
    List<Double> suggestTensionCurve(int totalDuration, int clipCount);

    record BeatSegment(double startSec, double endSec, double intensity) {}
}
