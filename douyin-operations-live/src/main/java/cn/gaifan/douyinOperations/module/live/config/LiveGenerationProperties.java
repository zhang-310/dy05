package cn.gaifan.douyinOperations.module.live.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * 直播生成侧可配置项：聊家常话题池（M-8）、历史话术粗查重（Q-2）、并行槽位生成（G-1 parallel 元数据，可与 @Value 并存）。
 */
@Configuration
@ConfigurationProperties(prefix = "app.live.generation")
public class LiveGenerationProperties {

    /**
     * 结构化聊家常话题方向；为空时 {@link cn.gaifan.douyinOperations.module.live.service.LivePromptBuilder} 使用内置默认池。
     */
    private List<String> chatTopicPool = new ArrayList<>();

    private CorpusDuplicateCheck corpusDuplicateCheck = new CorpusDuplicateCheck();

    /** G-1：与 {@code app.live.generation.parallel.*} 对齐，供需要统一读配置处使用 */
    private Parallel parallel = new Parallel();

    /**
     * P2 Q-5：在正则提取【2-30字】之外，用轻量启发式提示长段缺标注、括号不成对（非 LLM，可关）。
     */
    private PerformanceCueHeuristics performanceCueHeuristics = new PerformanceCueHeuristics();

    /** P2 Q-3：词典粗情感（非模型） */
    private SentimentLexicon sentimentLexicon = new SentimentLexicon();

    /** P2 Q-5：可选 LLM 补充表演建议（成本高，默认关） */
    private PerformanceCueLlm performanceCueLlm = new PerformanceCueLlm();

    public List<String> getChatTopicPool() {
        return chatTopicPool;
    }

    public void setChatTopicPool(List<String> chatTopicPool) {
        this.chatTopicPool = chatTopicPool != null ? chatTopicPool : new ArrayList<>();
    }

    public CorpusDuplicateCheck getCorpusDuplicateCheck() {
        return corpusDuplicateCheck;
    }

    public void setCorpusDuplicateCheck(CorpusDuplicateCheck corpusDuplicateCheck) {
        this.corpusDuplicateCheck = corpusDuplicateCheck != null ? corpusDuplicateCheck : new CorpusDuplicateCheck();
    }

    public Parallel getParallel() {
        return parallel;
    }

    public void setParallel(Parallel parallel) {
        this.parallel = parallel != null ? parallel : new Parallel();
    }

    public PerformanceCueHeuristics getPerformanceCueHeuristics() {
        return performanceCueHeuristics;
    }

    public void setPerformanceCueHeuristics(PerformanceCueHeuristics performanceCueHeuristics) {
        this.performanceCueHeuristics = performanceCueHeuristics != null ? performanceCueHeuristics : new PerformanceCueHeuristics();
    }

    public SentimentLexicon getSentimentLexicon() {
        return sentimentLexicon;
    }

    public void setSentimentLexicon(SentimentLexicon sentimentLexicon) {
        this.sentimentLexicon = sentimentLexicon != null ? sentimentLexicon : new SentimentLexicon();
    }

    public PerformanceCueLlm getPerformanceCueLlm() {
        return performanceCueLlm;
    }

    public void setPerformanceCueLlm(PerformanceCueLlm performanceCueLlm) {
        this.performanceCueLlm = performanceCueLlm != null ? performanceCueLlm : new PerformanceCueLlm();
    }

    public static class Parallel {
        private int maxConcurrency = 5;
        private int timeoutSeconds = 300;

        public int getMaxConcurrency() {
            return maxConcurrency;
        }

        public void setMaxConcurrency(int maxConcurrency) {
            this.maxConcurrency = maxConcurrency;
        }

        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }
    }

    public static class CorpusDuplicateCheck {
        /** 是否启用与同用户历史 live_script 正文的 bigram Jaccard 粗查重 */
        private boolean enabled = true;
        /** 相似度达到阈值则 suspected=true，并合并 ai_suggestion */
        private double similarityThreshold = 0.82;
        /** 最多比对最近 N 条非空话术 */
        private int maxCorpusScripts = 40;
        /** 正文短于此长度跳过查重 */
        private int minChars = 50;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public double getSimilarityThreshold() {
            return similarityThreshold;
        }

        public void setSimilarityThreshold(double similarityThreshold) {
            this.similarityThreshold = similarityThreshold;
        }

        public int getMaxCorpusScripts() {
            return maxCorpusScripts;
        }

        public void setMaxCorpusScripts(int maxCorpusScripts) {
            this.maxCorpusScripts = maxCorpusScripts;
        }

        public int getMinChars() {
            return minChars;
        }

        public void setMinChars(int minChars) {
            this.minChars = minChars;
        }
    }

    public static class PerformanceCueHeuristics {
        private boolean enabled = true;
        /** 正文达到此长度且无任何合法【2-30字】表演标注时，向 ai_suggestion 追加提示 */
        private int minCharsForMissingCueHint = 200;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getMinCharsForMissingCueHint() {
            return minCharsForMissingCueHint;
        }

        public void setMinCharsForMissingCueHint(int minCharsForMissingCueHint) {
            this.minCharsForMissingCueHint = Math.max(0, minCharsForMissingCueHint);
        }
    }

    public static class SentimentLexicon {
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class PerformanceCueLlm {
        private boolean enabled = false;
        private int maxScriptChars = 3500;
        /** Redis 结果缓存 TTL（秒），0 表示关闭缓存 */
        private int cacheTtlSeconds = 86400;
        private boolean cacheEnabled = true;
        /** 每用户每日最多调用 LLM 次数，0 表示不限制 */
        private int maxInvocationsPerDayPerOwner = 30;
        /** 同一用户两次调用最小间隔（毫秒），0 表示不限制 */
        private long minIntervalMs = 0;
        /** 与话术内容一并参与缓存键哈希，变更可令旧缓存失效 */
        private String promptVersion = "v1";
        /** true 时要求模型输出单行 JSON 数组，如 ["提示1","提示2"]；解析失败回退逐行文本 */
        private boolean structuredJsonEnabled = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getMaxScriptChars() {
            return maxScriptChars;
        }

        public void setMaxScriptChars(int maxScriptChars) {
            this.maxScriptChars = Math.max(400, maxScriptChars);
        }

        public int getCacheTtlSeconds() {
            return cacheTtlSeconds;
        }

        public void setCacheTtlSeconds(int cacheTtlSeconds) {
            this.cacheTtlSeconds = Math.max(0, cacheTtlSeconds);
        }

        public boolean isCacheEnabled() {
            return cacheEnabled;
        }

        public void setCacheEnabled(boolean cacheEnabled) {
            this.cacheEnabled = cacheEnabled;
        }

        public int getMaxInvocationsPerDayPerOwner() {
            return maxInvocationsPerDayPerOwner;
        }

        public void setMaxInvocationsPerDayPerOwner(int maxInvocationsPerDayPerOwner) {
            this.maxInvocationsPerDayPerOwner = Math.max(0, maxInvocationsPerDayPerOwner);
        }

        public long getMinIntervalMs() {
            return minIntervalMs;
        }

        public void setMinIntervalMs(long minIntervalMs) {
            this.minIntervalMs = Math.max(0, minIntervalMs);
        }

        public String getPromptVersion() {
            return promptVersion != null ? promptVersion : "v1";
        }

        public void setPromptVersion(String promptVersion) {
            this.promptVersion = promptVersion;
        }

        public boolean isStructuredJsonEnabled() {
            return structuredJsonEnabled;
        }

        public void setStructuredJsonEnabled(boolean structuredJsonEnabled) {
            this.structuredJsonEnabled = structuredJsonEnabled;
        }
    }
}
