package cn.gaifan.douyinOperations.module.ai.service.impl.brain;

import cn.gaifan.douyinOperations.module.ai.service.brain.GrowthPathService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import cn.gaifan.douyinOperations.module.ai.service.brain.StrategicPlanningService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 增长路径服务实现（Phase2）
 * 委托 StrategicPlanningService 生成
 */
@Service
public class GrowthPathServiceImpl implements GrowthPathService {

    private static final Logger log = LoggerFactory.getLogger(GrowthPathServiceImpl.class);

    @Value("${app.ai.brain.strategic-planning.enabled:true}")
    private boolean enabled;

    /** B-1：阶段粉丝里程碑，逗号分隔，与战略阶段序号对齐；不足时用最后一档或 targetFans */
    @Value("${app.ai.brain.growth-path.phase-milestone-fans:10000,50000,100000,500000}")
    private String phaseMilestoneFans;

    @Autowired(required = false)
    private StrategicPlanningService strategicPlanningService;

    private long[] milestoneFansArray() {
        if (phaseMilestoneFans == null || phaseMilestoneFans.isBlank()) {
            return new long[] {10000L, 50000L, 100000L, 500000L};
        }
        try {
            long[] arr = Arrays.stream(phaseMilestoneFans.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .mapToLong(Long::parseLong)
                    .toArray();
            return arr.length > 0 ? arr : new long[] {10000L, 50000L, 100000L, 500000L};
        } catch (Exception e) {
            log.warn("解析 growth-path.phase-milestone-fans 失败，使用默认: {}", e.getMessage());
            return new long[] {10000L, 50000L, 100000L, 500000L};
        }
    }

    private long milestoneAt(int phaseIndexOneBased, long targetFans) {
        long[] m = milestoneFansArray();
        if (phaseIndexOneBased <= 0) {
            return m.length > 0 ? m[0] : targetFans;
        }
        if (phaseIndexOneBased <= m.length) {
            return m[phaseIndexOneBased - 1];
        }
        return targetFans;
    }

    @Override
    public GrowthPathResult generate(Long accountId, Long userId, Map<String, Object> currentState, long targetFans) {
        if (!enabled) {
            return new GrowthPathResult(List.of(), "服务未启用", List.of());
        }
        if (strategicPlanningService != null && strategicPlanningService.isAvailable()) {
            var gp = strategicPlanningService.generateGrowthPath(accountId, userId, currentState);
            List<PhasePlan> plans = new ArrayList<>();
            int i = 1;
            for (var p : gp.phases()) {
                long target = milestoneAt(i, targetFans);
                plans.add(new PhasePlan(i, p.phase(), target, p.strategies(), List.of("粉丝数", "互动率"), "约4-8周"));
                i++;
            }
            return new GrowthPathResult(plans, gp.summary(), List.of("内容质量", "更新频率", "人设一致性"));
        }
        long[] m = milestoneFansArray();
        long t1 = m.length > 0 ? m[0] : 10_000L;
        long t2 = m.length > 1 ? m[1] : 50_000L;
        return new GrowthPathResult(
                List.of(
                        new PhasePlan(1, "冷启动", t1, List.of("爆款跟拍", "热点内容"), List.of("粉丝数"), "4-6周"),
                        new PhasePlan(2, "成长", t2, List.of("系列化", "IP打造"), List.of("粉丝数", "完播率"), "8-12周"),
                        new PhasePlan(3, "成熟", targetFans, List.of("商业化", "矩阵"), List.of("粉丝数", "转化率"), "视目标而定")
                ),
                "分三阶段推进，优先保证内容质量。",
                List.of("内容质量", "更新频率", "人设一致性")
        );
    }

    @Override
    public boolean isAvailable() {
        return enabled;
    }
}
