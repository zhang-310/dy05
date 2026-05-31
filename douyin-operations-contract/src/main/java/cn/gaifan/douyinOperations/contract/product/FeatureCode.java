package cn.gaifan.douyinOperations.contract.product;

/**
 * 功能编码常量
 *
 * 每个产品内的可授权功能。格式：{productCode}.{featureCode}
 */
public final class FeatureCode {

    private FeatureCode() {}

    // -- douyin-ops --
    public static final String DOUYIN_ACCOUNT_MGMT = "douyin-ops.account-mgmt";
    public static final String DOUYIN_VIDEO_ANALYSIS = "douyin-ops.video-analysis";
    public static final String DOUYIN_CONTENT_PLANNING = "douyin-ops.content-planning";
    public static final String DOUYIN_LIVE_SCRIPT = "douyin-ops.live-script";

    // -- video-insight --
    public static final String VIDEO_BREAKDOWN = "video-insight.breakdown";
    public static final String VIDEO_VIRAL_ANALYSIS = "video-insight.viral-analysis";

    // -- knowledge-base --
    public static final String KB_RAG = "knowledge-base.rag";
    public static final String KB_DOCUMENT = "knowledge-base.document";

    // -- digital-human --
    public static final String DIGITAL_HUMAN_GENERATE = "digital-human.generate";

    // -- drama-ai --
    public static final String DRAMA_SCRIPT = "drama-ai.script";
    public static final String DRAMA_STORYBOARD = "drama-ai.storyboard";

    // -- photo-avatar-video --
    public static final String PHOTO_AVATAR_GENERATE = "photo-avatar-video.generate";

    // -- shortvideo-maker --
    public static final String SHORTVIDEO_EXPORT = "shortvideo-maker.export";
    public static final String SHORTVIDEO_SCRIPT_GENERATE = "shortvideo-maker.script.generate";

    // -- AI (cross-product) --
    public static final String AI_CHAT = "ai.chat";
    public static final String AI_GENERATION = "ai.generation";
    public static final String AI_MCP_TOOL = "ai.mcp-tool";
    public static final String AI_EVOLUTION = "ai.evolution.run";
}
