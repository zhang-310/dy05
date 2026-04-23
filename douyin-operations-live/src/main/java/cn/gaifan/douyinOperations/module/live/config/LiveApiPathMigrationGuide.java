package cn.gaifan.douyinOperations.module.live.config;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * P4-01：直播 AI API 路径重构过渡层。
 * <p>
 * 旧路径 {@code /api/v1/live/ai/*} 下的端点超过 40 个，不便于维护和文档化。
 * 新路径按职责拆分为：
 * <ul>
 *   <li>{@code /api/v1/live/generation/*} — AI 话术生成</li>
 *   <li>{@code /api/v1/live/refine/*} — 话术精修</li>
 *   <li>{@code /api/v1/live/recommend/*} — 话术推荐</li>
 *   <li>{@code /api/v1/live/compliance/*} — 话术合规检测</li>
 * </ul>
 * 本 Controller 为过渡期兼容层，将新路径 307 转发到旧路径（由 Nginx/网关处理更优，此处为后端保底兜底）。
 * <p>
 * 实际执行：推荐在 Nginx 层配置转发规则，本 Controller 仅作为路径说明文档。
 * 完整迁移后，旧路径 Controller 将被移除。
 *
 * @since Phase 5 (P4-01)
 */
@Hidden
@RestController
@RequestMapping("/api/v1/live")
public class LiveApiPathMigrationGuide {

    /**
     * 路径迁移说明（仅文档用，不处理实际请求）：
     *
     * <pre>
     * 生成类 (generation):
     *   /api/v1/live/generation/full-sse          → /api/v1/live/ai/generate-full-sse
     *   /api/v1/live/generation/full-pipelined-sse→ /api/v1/live/ai/generate-full-pipelined-sse
     *   /api/v1/live/generation/slot-sse          → /api/v1/live/ai/generate-slot-sse
     *   /api/v1/live/generation/skeleton-sse      → /api/v1/live/ai/generate-skeleton-sse
     *   /api/v1/live/generation/full-async        → /api/v1/live/ai/generate-full-async
     *   /api/v1/live/generation/parallel          → /api/v1/live/ai/generate-parallel
     *
     * 精修类 (refine):
     *   /api/v1/live/refine/script                → /api/v1/live/ai/refine-script
     *   /api/v1/live/refine/script-sse            → /api/v1/live/ai/refine-script-sse
     *   /api/v1/live/refine/segment               → /api/v1/live/ai/refine-segment
     *   /api/v1/live/refine/chat                  → /api/v1/live/ai/chat-for-script
     *   /api/v1/live/refine/chat-sse              → /api/v1/live/ai/chat-for-script-sse
     *   /api/v1/live/refine/batch-chat            → /api/v1/live/ai/batch-chat-for-script
     *   /api/v1/live/refine/suggest               → /api/v1/live/ai/suggest-improvement
     *   /api/v1/live/refine/similarity            → /api/v1/live/ai/check-similarity
     *
     * 推荐类 (recommend):
     *   /api/v1/live/recommend/scripts            → /api/v1/live/ai/recommend-scripts
     *   /api/v1/live/recommend/chat-2h-strategy   → /api/v1/live/ai/chat-2h-strategy
     *
     * 合规类 (compliance):
     *   /api/v1/live/compliance/check             → /api/v1/live/ai/check-violation
     *   /api/v1/live/compliance/check-enhanced    → /api/v1/live/ai/check-violation-enhanced
     *   /api/v1/live/compliance/save-to-copy      → /api/v1/live/ai/save-to-copy-if-passed
     * </pre>
     *
     * Nginx 配置示例：
     * <pre>
     * location ~ ^/api/v1/live/generation/(.*)$ {
     *     return 307 /api/v1/live/ai/generate-$1;
     * }
     * location ~ ^/api/v1/live/refine/(.*)$ {
     *     return 307 /api/v1/live/ai/$1;
     * }
     * location ~ ^/api/v1/live/recommend/(.*)$ {
     *     return 307 /api/v1/live/ai/$1;
     * }
     * location ~ ^/api/v1/live/compliance/(.*)$ {
     *     return 307 /api/v1/live/ai/$1;
     * }
     * </pre>
     */
    @RequestMapping("/migration-guide")
    public Object migrationGuide(HttpServletRequest request, HttpServletResponse response) {
        return java.util.Map.of(
                "message", "P4-01 API 路径重构迁移说明",
                "oldPrefix", "/api/v1/live/ai",
                "newPrefixes", java.util.List.of(
                        "/api/v1/live/generation (生成类)",
                        "/api/v1/live/refine (精修类)",
                        "/api/v1/live/recommend (推荐类)",
                        "/api/v1/live/compliance (合规类)"
                ),
                "status", "in-progress",
                "note", "旧路径保留兼容，请优先使用新路径。完整迁移计划见 docs/upgrade-plan-20260321/"
        );
    }
}
