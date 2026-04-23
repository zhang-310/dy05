package cn.gaifan.douyinOperations.module.shortvideo.service;

/**
 * 内容审核服务（图像/视频/文本）
 * 可接入百度云内容审核 API，配置 ai.content-audit.* 后启用
 */
public interface ContentAuditService {

    /**
     * 审核图像 URL
     *
     * @param imageUrl 公网可访问的图片 URL
     * @return 审核结果，pass 表示通过
     */
    AuditResult auditImage(String imageUrl);

    /**
     * 审核视频 URL
     *
     * @param videoUrl 公网可访问的视频 URL
     * @return 审核结果
     */
    AuditResult auditVideo(String videoUrl);

    /**
     * 审核文本
     *
     * @param text 待审核文本
     * @return 审核结果
     */
    AuditResult auditText(String text);

    /** 是否已配置（有 API Key 等） */
    boolean isConfigured();

    record AuditResult(boolean passed, String conclusion, java.util.List<String> issues, java.util.List<String> suggestions) {
        public static AuditResult pass() {
            return new AuditResult(true, "pass", java.util.List.of(), java.util.List.of("建议增加字幕，提升观看体验"));
        }
        public static AuditResult fail(String conclusion, java.util.List<String> issues) {
            return new AuditResult(false, conclusion, issues, java.util.List.of());
        }
    }
}
