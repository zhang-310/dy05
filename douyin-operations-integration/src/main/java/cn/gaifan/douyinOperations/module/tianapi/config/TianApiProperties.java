package cn.gaifan.douyinOperations.module.tianapi.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * TianAPI（天聚数行）配置
 * 文档：https://www.tianapi.com
 */
@Data
@Component
@ConfigurationProperties(prefix = "tianapi")
public class TianApiProperties {

    /** 是否启用 TianAPI 功能 */
    private boolean enabled = true;

    /** API Key，建议用环境变量 TIANAPI_API_KEY */
    private String apiKey = "";

    /** 接口 Base URL */
    private String baseUrl = "https://apis.tianapi.com";

    /** 抖音热搜缓存时间（分钟），减少调用次数 */
    private int hotCacheMinutes = 5;

    // ─── 素材自动入库（按高级会员 1万次/天、QPS20 设计） ─────────────────────────
    /** 是否启用素材自动入库到文案库 */
    private boolean materialImportEnabled = false;
    /** 定时任务 cron，默认每日 2:00 */
    private String materialImportCron = "0 0 2 * * ?";
    /** 入库目标用户 ID（copy_library.user_id），未配置则跳过定时任务 */
    private Long materialImportUserId;
    /** 每类素材每日拉取次数（默认 1000） */
    private int materialImportCallsPerCategory = 1000;
    /**
     * 每类 TianAPI 的 HTTP 请求次数上限（与天行「单接口日配额」对齐，如 10000）。
     * 非空且大于 0 时优先于 {@link #materialImportCallsPerCategory}：批量类每轮 1 次请求，单条类每轮 1 次请求，均最多执行本次数。
     */
    private Integer materialImportApiCallsPerCategory;
    /** 调用间隔毫秒（QPS20→55ms，约 18 次/秒） */
    private int materialImportDelayMs = 55;

    // ─── 同步入库到 AI 知识库 ────────────────────────────────────────────────────
    /** 是否同步入库到知识库（需 AI 模块启用且目标 KB 存在） */
    private boolean kbImportEnabled = false;
    /** 目标知识库名称，默认 huashu（话术库） */
    private String kbName = "huashu";

    // ─── 演示回退（API 未申请 code160 时） ────────────────────────────────────────
    /** 当 API 全部失败（imported=0,calls=0）时，是否用内置示例数据演示入库流程 */
    private boolean demoFallbackOnEmpty = true;
    /** 启动时执行一次入库（验证用，默认 false） */
    private boolean importOnStartup = false;

    public boolean isConfigured() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }
}
