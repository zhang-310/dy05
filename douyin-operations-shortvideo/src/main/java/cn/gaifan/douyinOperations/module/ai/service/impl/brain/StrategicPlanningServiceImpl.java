package cn.gaifan.douyinOperations.module.ai.service.impl.brain;

import cn.gaifan.douyinOperations.module.ai.service.brain.HostPersonaService;
import cn.gaifan.douyinOperations.module.ai.service.brain.StrategicPlanningService;
import cn.gaifan.douyinOperations.module.ai.service.brain.TrendMonitorService;
import cn.gaifan.douyinOperations.module.douyin.entity.DouyinAccount;
import cn.gaifan.douyinOperations.module.douyin.repository.DouyinAccountRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveScriptEffectivenessRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveSessionRepository;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvVideo;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvProjectRepository;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvVideoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import cn.gaifan.douyinOperations.module.ai.service.LlmClient;
import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.repository.AiModelRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 战略规划服务实现（Phase2）
 * 支持模板：generic 通用增长 | skincare 护肤品转型三步走
 */
@Service
public class StrategicPlanningServiceImpl implements StrategicPlanningService {

    private static final Logger log = LoggerFactory.getLogger(StrategicPlanningServiceImpl.class);

    @Value("${app.ai.brain.strategic-planning.enabled:true}")
    private boolean enabled;

    @Autowired(required = false)
    private LlmClient llmClient;

    @Autowired(required = false)
    private AiModelRepository aiModelRepository;

    @Autowired(required = false)
    private HostPersonaService hostPersonaService;

    @Autowired(required = false)
    private DouyinAccountRepository accountRepository;

    @Autowired(required = false)
    private LiveScriptEffectivenessRepository effectivenessRepository;

    @Autowired(required = false)
    private SvProjectRepository projectRepository;

    @Autowired(required = false)
    private LiveSessionRepository liveSessionRepository;

    @Autowired(required = false)
    private SvVideoRepository videoRepository;

    @Autowired(required = false)
    private TrendMonitorService trendMonitorService;

    @Override
    public StrategicPlan generate(Long accountId, Long userId, List<String> goals) {
        return generate(accountId, userId, goals, null);
    }

    @Override
    public StrategicPlan generate(Long accountId, Long userId, List<String> goals, Map<String, Object> context) {
        if (!enabled) {
            return new StrategicPlan(accountId, "", "", List.of(), Map.of(), List.of(), List.of(), List.of());
        }
        List<StrategicPlanningService.Diagnosis> diagnoses = collectDiagnoses(userId);
        String template = resolveTemplate(context);
        StrategicPlan base;
        if ("skincare".equals(template)) {
            base = buildSkincarePlan(accountId, resolveStrategyPhase(context));
        } else {
            base = buildGenericPlan(accountId, goals);
        }
        if (!diagnoses.isEmpty() && llmClient != null) {
            AiModel model = findModel();
            if (model != null) {
                try {
                    String prompt = buildStrategicPrompt(userId, diagnoses, base);
                    var resp = llmClient.chat(model, "你是抖音运营战略规划专家，基于数据给出个性化建议。", prompt);
                    if (resp != null && resp.success() && resp.content() != null && !resp.content().isBlank()) {
                        return mergeLlmAdvice(base, resp.content(), diagnoses);
                    }
                } catch (Exception e) {
                    log.debug("战略规划 LLM 生成失败: {}", e.getMessage());
                }
            }
        }
        return new StrategicPlan(base.accountId(), base.industryAnalysis(), base.competitorAnalysis(),
                base.opportunityPoints(), base.swotScores(), base.contentMatrix(), base.growthPhases(), diagnoses);
    }

    private List<StrategicPlanningService.Diagnosis> collectDiagnoses(Long userId) {
        List<StrategicPlanningService.Diagnosis> list = new ArrayList<>();
        if (userId == null) return list;
        double growthRate = 0.05;
        long fanCount = 0;
        if (accountRepository != null) {
            var page = accountRepository.findByOwnerIdAndDeleted(userId, 0, PageRequest.of(0, 1));
            if (page.hasContent()) {
                DouyinAccount acc = page.getContent().get(0);
                fanCount = acc.getFanCount() != null ? acc.getFanCount() : 0;
            }
        }
        if (growthRate < 0.05) list.add(new StrategicPlanningService.Diagnosis("增长停滞", "月增长率仅" + String.format("%.1f%%", growthRate * 100)));
        if (effectivenessRepository != null) {
            Double avgConv = effectivenessRepository.findAvgConversionByUserId(userId);
            if (avgConv != null && avgConv < 2.0) list.add(new StrategicPlanningService.Diagnosis("转化率低", "平均转化率" + String.format("%.2f%%", avgConv)));
        }
        double postFreq = 0;
        if (projectRepository != null) {
            long count = projectRepository.countByOwnerIdAndDeleted(userId, 0);
            postFreq = count / 4.0;
        }
        if (postFreq < 3 && postFreq > 0) list.add(new StrategicPlanningService.Diagnosis("更新频率低", "周均发布约" + String.format("%.1f", postFreq) + "条"));
        return list;
    }

    private String buildStrategicPrompt(Long userId, List<StrategicPlanningService.Diagnosis> diagnoses, StrategicPlan base) {
        long fanCount = 0;
        double growthRate = 0.05;
        int postCount = 0;
        long avgViews = 0;
        int liveCount = 0;
        double avgConversion = 0;
        long benchmarkViews = 5000;
        double benchmarkConversion = 0.02;

        if (accountRepository != null && userId != null) {
            var page = accountRepository.findByOwnerIdAndDeleted(userId, 0, PageRequest.of(0, 1));
            if (page.hasContent()) {
                DouyinAccount acc = page.getContent().get(0);
                fanCount = acc.getFanCount() != null ? acc.getFanCount() : 0;
            }
        }
        if (projectRepository != null && userId != null) {
            long count = projectRepository.countByOwnerIdAndDeleted(userId, 0);
            postCount = (int) (count / 4.0); // 月均约 count/4
        }
        if (videoRepository != null && userId != null) {
            var videos = videoRepository.findByOwnerIdAndDeleted(userId, 0);
            if (!videos.isEmpty()) {
                avgViews = (long) videos.stream().limit(30)
                        .filter(v -> v.getViewCount() != null && v.getViewCount() > 0)
                        .mapToLong(v -> v.getViewCount())
                        .average().orElse(0);
            }
        }
        if (liveSessionRepository != null && userId != null) {
            liveCount = (int) liveSessionRepository.countByUserIdAndDeleted(userId, 0);
        }
        if (effectivenessRepository != null && userId != null) {
            Double conv = effectivenessRepository.findAvgConversionByUserId(userId);
            avgConversion = conv != null ? conv : 0;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("请为以下账号制定运营战略规划：\n");
        sb.append(String.format("账号信息：粉丝数 %d，月增长率 %.1f%%\n", fanCount, growthRate * 100));
        sb.append(String.format("内容数据：月均发布约 %d 条，平均播放量 %d\n", postCount, avgViews));
        sb.append(String.format("直播数据：累计直播 %d 场，平均转化率 %.2f%%\n", liveCount, avgConversion));
        sb.append(String.format("行业基准：平均播放量 %d，平均转化率 %.1f%%\n", benchmarkViews, benchmarkConversion * 100));
        sb.append("\n诊断：");
        for (var d : diagnoses) sb.append(d.title()).append("(").append(d.detail()).append("); ");
        sb.append("\n当前规划要点：").append(base.industryAnalysis());
        sb.append("\n请输出：1.诊断 2.短期目标(1月) 3.中期目标(3月) 4.长期目标(6月) 5.具体行动项");
        return sb.toString();
    }

    private StrategicPlan mergeLlmAdvice(StrategicPlan base, String llmContent, List<StrategicPlanningService.Diagnosis> diagnoses) {
        List<PhaseStrategy> phases = new ArrayList<>(base.growthPhases());
        if (phases.isEmpty() && llmContent.length() > 20) {
            phases.add(new PhaseStrategy("数据驱动", "基于诊断优化", List.of(llmContent.substring(0, Math.min(200, llmContent.length())))));
        }
        return new StrategicPlan(base.accountId(), base.industryAnalysis(), base.competitorAnalysis(),
                base.opportunityPoints(), base.swotScores(), base.contentMatrix(), phases, diagnoses);
    }

    private AiModel findModel() {
        if (aiModelRepository == null) return null;
        var all = aiModelRepository.findByStatusAndDeleted(1, 0);
        return all.isEmpty() ? null : all.get(0);
    }

    private String resolveTemplate(Map<String, Object> context) {
        if (context == null) return "generic";
        Object t = context.get("template");
        if (t != null && "skincare".equals(String.valueOf(t).trim())) return "skincare";
        Object phase = context.get("strategyPhase");
        if (phase != null) {
            int p = phase instanceof Number ? ((Number) phase).intValue() : 0;
            if (p >= 1 && p <= 3) return "skincare";
        }
        Object hostCode = context.get("hostCode");
        if (hostCode != null && hostPersonaService != null) {
            var persona = hostPersonaService.getByCode(String.valueOf(hostCode));
            if (persona != null && persona.getStrategyPhase() != null && persona.getStrategyPhase() >= 1) {
                return "skincare";
            }
        }
        return "generic";
    }

    private int resolveStrategyPhase(Map<String, Object> context) {
        if (context != null) {
            Object p = context.get("strategyPhase");
            if (p instanceof Number) return ((Number) p).intValue();
            Object hc = context.get("hostCode");
            if (hc != null && hostPersonaService != null) {
                var persona = hostPersonaService.getByCode(String.valueOf(hc));
                if (persona != null && persona.getStrategyPhase() != null) return persona.getStrategyPhase();
            }
        }
        return 1;
    }

    private StrategicPlan buildSkincarePlan(Long accountId, int phase) {
        List<PhaseStrategy> phases = List.of(
                new PhaseStrategy("阶段1", "品类测试与形象建立（62w→200w）", List.of("引入2-3个中端护肤品牌", "护肤知识内容体系", "粉丝教育")),
                new PhaseStrategy("阶段2", "品牌深度合作（200w→800w）", List.of("签约3-5个头部护肤品牌", "专业认证", "供应链优化")),
                new PhaseStrategy("阶段3", "头部竞争与生态（800w→2000w+）", List.of("独家品牌/联名产品", "自有品牌开发", "护肤产业生态"))
        );
        return new StrategicPlan(
                accountId,
                "护肤品赛道头部单场3000w+，腰部500w-1000w。品类、客单价、供应链、粉丝购买力是关键差距。",
                "可对标护肤垂类头部主播的内容结构、品牌合作深度与专业形象。",
                List.of("成分科普", "功效宣称合规", "专业形象打造", "高客单价价值感"),
                Map.of("strength", 0.7, "weakness", 0.3, "opportunity", 0.65, "threat", 0.4),
                List.of(
                        new ContentMatrixItem("护肤成分科普", "成分科学+功效合规", 1),
                        new ContentMatrixItem("专业形象", "护肤达人认证、专家合作", 2),
                        new ContentMatrixItem("品牌合作", "中高端护肤品牌深度合作", 3)
                ),
                phases,
                List.of()
        );
    }

    private StrategicPlan buildGenericPlan(Long accountId, List<String> goals) {
        return new StrategicPlan(
                accountId,
                "护肤彩妆行业竞争激烈，需差异化定位。",
                "可对标同类目头部账号的内容结构与更新频率。",
                List.of("成分科普", "使用体验分享", "场景化种草"),
                Map.of("strength", 0.7, "weakness", 0.3, "opportunity", 0.6, "threat", 0.4),
                List.of(
                        new ContentMatrixItem("爆款跟拍", "紧跟热榜，48h内产出", 1),
                        new ContentMatrixItem("系列化", "打造专属IP系列", 2),
                        new ContentMatrixItem("种草", "产品+场景结合", 3)
                ),
                List.of(
                        new PhaseStrategy("阶段1", "万粉突破", List.of("爆款内容", "热点跟拍")),
                        new PhaseStrategy("阶段2", "5万粉", List.of("系列化", "IP打造")),
                        new PhaseStrategy("阶段3", "10万粉", List.of("商业化", "矩阵扩展"))
                ),
                List.of()
        );
    }

    @Override
    public GrowthPath generateGrowthPath(Long accountId, Long userId, Map<String, Object> currentState) {
        if (!enabled) return new GrowthPath(List.of(), "");
        List<PhaseStrategy> phases = List.of(
                new PhaseStrategy("阶段1", "万粉突破", List.of("爆款内容", "热点跟拍")),
                new PhaseStrategy("阶段2", "5万粉", List.of("系列化", "IP打造")),
                new PhaseStrategy("阶段3", "10万粉", List.of("商业化", "矩阵扩展"))
        );
        return new GrowthPath(phases, "分三阶段推进，优先保证内容质量与更新频率。");
    }

    @Override
    public boolean isAvailable() {
        return enabled;
    }
}
