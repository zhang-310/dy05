package cn.gaifan.douyinOperations.module.agent.skill.impl;

import cn.gaifan.douyinOperations.module.agent.skill.Skill;
import cn.gaifan.douyinOperations.module.live.entity.LiveSession;
import cn.gaifan.douyinOperations.module.live.repository.LiveSessionRepository;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 直播场次查询技能 - 从 live_session 表查询真实场次数据
 */
@Component
public class LiveSessionQuerySkill implements Skill {

    private static final Logger log = LoggerFactory.getLogger(LiveSessionQuerySkill.class);

    /** 场次状态：0=未开始 1=直播中 2=已结束 */
    private static final String[] STATUS_NAMES = {"未开始", "直播中", "已结束"};

    @Resource
    private LiveSessionRepository liveSessionRepository;

    @Override
    public String getName() {
        return "live_session_query";
    }

    @Override
    public String getDescription() {
        return "查询直播场次信息，包括场次数据、商品、话术等";
    }

    @Override
    public boolean matches(String input) {
        if (input == null) return false;
        String lower = input.toLowerCase();
        return lower.contains("场次") || lower.contains("直播") || lower.contains("开播")
                || lower.contains("gmv") || lower.contains("观看");
    }

    @Override
    public String execute(SkillContext ctx) {
        try {
            String keyword = (String) ctx.params().get("keyword");
            log.info("[LiveSessionQuery] 执行查询: keyword={}, userId={}", keyword, ctx.userId());

            // 按标题关键词搜索当前用户的场次，最多 10 条，按最近排
            List<LiveSession> sessions = liveSessionRepository
                    .searchByTitleKeyword(ctx.userId(), keyword != null ? keyword : "",
                            PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "scheduledTime")))
                    .getContent();

            if (sessions == null || sessions.isEmpty()) {
                return "未找到匹配的直播场次。请检查关键词，或先创建直播场次。\n" +
                       "💡 提示：可通过「直播场次」页面创建和管理场次。";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("找到 ").append(sessions.size()).append(" 个直播场次：\n\n");

            for (int i = 0; i < sessions.size(); i++) {
                LiveSession s = sessions.get(i);
                String statusName = (s.getStatus() != null && s.getStatus() < STATUS_NAMES.length)
                        ? STATUS_NAMES[s.getStatus()] : "未知";

                sb.append(i + 1).append(". ").append(s.getLiveTitle() != null ? s.getLiveTitle() : "未命名场次").append("\n");
                sb.append("   状态: ").append(statusName).append("\n");
                if (s.getScheduledTime() != null) {
                    sb.append("   计划时间: ").append(formatTimestamp(s.getScheduledTime())).append("\n");
                }
                if (s.getViewers() != null) {
                    sb.append("   观看人数: ").append(formatNumber(s.getViewers())).append("\n");
                }
                if (s.getLikes() != null) {
                    sb.append("   点赞数: ").append(formatLong(s.getLikes())).append("\n");
                }
                if (s.getSessionType() != null) {
                    sb.append("   场次类型: ").append(s.getSessionType()).append("\n");
                }
                if (s.getScriptStyle() != null) {
                    sb.append("   话术风格: ").append(s.getScriptStyle()).append("\n");
                }
                sb.append("\n");
            }

            sb.append("💡 共 ").append(sessions.size()).append(" 条结果。\n");
            return sb.toString();

        } catch (Exception e) {
            log.error("[LiveSessionQuery] 执行失败", e);
            return "场次查询失败: " + e.getMessage();
        }
    }

    private String formatTimestamp(java.sql.Timestamp ts) {
        if (ts == null) return "—";
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");
        return sdf.format(ts);
    }

    private String formatNumber(Integer n) {
        if (n == null) return "0";
        if (n >= 10000) return String.format("%.1f万", n / 10000.0);
        return String.valueOf(n);
    }

    private String formatLong(Long n) {
        if (n == null) return "0";
        if (n >= 10000) return String.format("%.1f万", n / 10000.0);
        return String.valueOf(n);
    }
}