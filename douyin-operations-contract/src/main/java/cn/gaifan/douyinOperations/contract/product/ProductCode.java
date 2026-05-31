package cn.gaifan.douyinOperations.contract.product;

/**
 * 产品编码常量
 *
 * 每个独立可售产品在此注册。所有授权、计费、功能开关均以此编码为 key。
 */
public final class ProductCode {

    private ProductCode() {}

    /** 抖音运营（首发产品） */
    public static final String DOUYIN_OPS = "douyin-ops";

    /** 短视频拆解分析 */
    public static final String VIDEO_INSIGHT = "video-insight";

    /** AI 知识库 */
    public static final String KNOWLEDGE_BASE = "knowledge-base";

    /** 短剧 AI 制作 */
    public static final String DRAMA_AI = "drama-ai";

    /** AI 数字人 */
    public static final String DIGITAL_HUMAN = "digital-human";

    /** 照片生成头像视频 */
    public static final String PHOTO_AVATAR_VIDEO = "photo-avatar-video";

    /** 短视频成片创作 */
    public static final String SHORTVIDEO_MAKER = "shortvideo-maker";
}
