package cn.gaifan.douyinOperations.module.live.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 直播实时面板：弹幕建议与自动执行相关配置（Phase 2 定界）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.live.realtime")
public class LiveRealtimeProperties {

    private Suggestion suggestion = new Suggestion();

    @Data
    public static class Suggestion {
        /** 是否允许服务端自动调用 execute-suggestion（默认关闭，需审计与风控就绪后开启） */
        private boolean autoExecutionEnabled = false;
        /** 两次自动执行之间的最短间隔（秒） */
        private int minCooldownSeconds = 30;
        /** 单场次每小时自动跳转类操作上限 */
        private int maxAutoJumpsPerHour = 20;
        /**
         * 自动执行时允许的 actionType 白名单（不含 jump_slot，避免无监督跳转）。
         * YAML：{@code allowed-auto-action-types: [next_slot, inject_interaction]}
         */
        private List<String> allowedAutoActionTypes = new ArrayList<>(List.of("next_slot", "inject_interaction"));
        /** 仅 urgency ≥ 此值的建议才参与自动执行（1–5） */
        private int minUrgencyForAuto = 4;
    }
}
