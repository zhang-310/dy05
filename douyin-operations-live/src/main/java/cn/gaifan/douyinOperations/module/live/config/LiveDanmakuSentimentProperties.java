package cn.gaifan.douyinOperations.module.live.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * 直播弹幕情绪快路径（LIVE-01）：滚动窗口 + 可配置词表
 */
@Configuration
@ConfigurationProperties(prefix = "app.live.danmaku-sentiment")
public class LiveDanmakuSentimentProperties {

    private boolean enabled = true;

    /** 聚合窗口（秒），与建议 SSE 调度周期对齐时体验较一致 */
    private int windowSeconds = 30;

    /** 窗口内负向条数 ≥ 此值时，实时建议追加一条 */
    private int suggestWhenNegativeCountGte = 3;

    /** 单次 bulk ingest 最大条数（Webhook/中间件推送上限，防 DoS） */
    private int batchMaxLines = 200;

    private List<String> positiveWords = new ArrayList<>(List.of(
            "好", "棒", "赞", "爱了", "种草", "冲", "买", "划算", "真香", "支持", "喜欢",
            "不错", "可以", "推荐", "回购", "干货", "用心", "专业"
    ));

    private List<String> negativeWords = new ArrayList<>(List.of(
            "差", "假", "坑", "骗", "贵", "别买", "退货", "拉黑", "水军", "无聊", "难看",
            "骗人", "垃圾", "没用", "踩雷", "翻车"
    ));

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getWindowSeconds() {
        return windowSeconds;
    }

    public void setWindowSeconds(int windowSeconds) {
        this.windowSeconds = Math.max(5, windowSeconds);
    }

    public int getSuggestWhenNegativeCountGte() {
        return suggestWhenNegativeCountGte;
    }

    public void setSuggestWhenNegativeCountGte(int suggestWhenNegativeCountGte) {
        this.suggestWhenNegativeCountGte = Math.max(1, suggestWhenNegativeCountGte);
    }

    public int getBatchMaxLines() {
        return batchMaxLines;
    }

    public void setBatchMaxLines(int batchMaxLines) {
        this.batchMaxLines = Math.max(1, Math.min(batchMaxLines, 2000));
    }

    public List<String> getPositiveWords() {
        return positiveWords;
    }

    public void setPositiveWords(List<String> positiveWords) {
        this.positiveWords = positiveWords != null ? positiveWords : new ArrayList<>();
    }

    public List<String> getNegativeWords() {
        return negativeWords;
    }

    public void setNegativeWords(List<String> negativeWords) {
        this.negativeWords = negativeWords != null ? negativeWords : new ArrayList<>();
    }
}
