package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.common.util.ContentSubstringPolicy;
import cn.gaifan.douyinOperations.module.shortvideo.config.ShortVideoAutoRemakeContentPolicyProperties;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvViralVideo;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvViralVideoRepository;
import cn.gaifan.douyinOperations.module.shortvideo.service.PersonaViralFusionService;
import cn.gaifan.douyinOperations.module.shortvideo.service.SvContentCalendarService;
import cn.gaifan.douyinOperations.module.shortvideo.service.ViralRemakeService;
import cn.gaifan.douyinOperations.module.shortvideo.integration.VideoInsightIntegrationBridge;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * LF-06：爆款二创全自动编排——将采集→深度分析→人设匹配→脚本生成→排入日历串联为流水线。
 * <p>
 * 每步失败独立隔离，不影响后续记录处理。系统用户（userId=0）驱动全部操作。
 */
@Service
public class ViralAutoOrchestrationService {

    private static final Logger log = LoggerFactory.getLogger(ViralAutoOrchestrationService.class);

    private static final long SYSTEM_USER_ID = 0L;

    @Autowired
    private SvViralVideoRepository viralVideoRepository;

    @Autowired(required = false)
    private VideoInsightIntegrationBridge videoInsightIntegrationBridge;

    @Autowired(required = false)
    private PersonaViralFusionService personaFusionService;

    @Autowired(required = false)
    private ViralRemakeService viralRemakeService;

    @Autowired(required = false)
    private SvContentCalendarService contentCalendarService;

    @Value("${app.shortvideo.auto-orchestration.min-viral-score:70}")
    private int minViralScore;

    @Value("${app.shortvideo.auto-orchestration.batch-limit:20}")
    private int batchLimit;

    @Value("${app.shortvideo.auto-orchestration.calendar-days-ahead:3}")
    private int calendarDaysAhead;

    @Value("${app.shortvideo.auto-orchestration.default-persona-id:0}")
    private long defaultPersonaId;

    /** sop | persona_fusion，与 ViralRemakeController generate-script 一致 */
    @Value("${app.shortvideo.auto-orchestration.remake-script-mode:sop}")
    private String remakeScriptMode;

    @Resource
    private ShortVideoAutoRemakeContentPolicyProperties autoRemakeContentPolicy;

    /**
     * 执行全自动编排流水线。
     *
     * @return 各阶段计数汇总
     */
    public Map<String, Object> runPipeline() {
        log.info("[自动编排] 开始全自动编排流水线");
        int analyzed = 0;
        int recommended = 0;
        int scripted = 0;
        int scheduled = 0;
        int errors = 0;
        int skippedPolicy = 0;

        // 1. 筛选待处理的高分爆款（remakeStatus=0 且 viralScore >= 阈值）
        List<SvViralVideo> candidates = findCandidates();
        log.info("[自动编排] 筛选到 {} 条待处理爆款（阈值≥{}）", candidates.size(), minViralScore);

        for (SvViralVideo viral : candidates) {
            try {
                if (autoRemakeContentPolicy != null && autoRemakeContentPolicy.isEnabled()) {
                    String blob = viralTextBlob(viral);
                    String hit = ContentSubstringPolicy.firstHit(blob, autoRemakeContentPolicy.getSkipAutoRemakeSubstrings());
                    if (hit != null) {
                        skippedPolicy++;
                        log.info("[自动编排] 跳过高风险/不宜自动二创题材，节省资源 viralId={} hit={}", viral.getId(), hit);
                        continue;
                    }
                }
                // 2. 深度分析：仅当 deep_analyze_status=completed 后才进入推荐，避免「已提交异步深度却立刻 recommend」竞态
                if (videoInsightIntegrationBridge != null) {
                    SvViralVideo deepState = viralVideoRepository.findById(viral.getId()).orElse(null);
                    if (deepState == null) {
                        continue;
                    }
                    String ds = deepState.getDeepAnalyzeStatus();
                    if (!"completed".equals(ds)) {
                        if ("processing".equals(ds)) {
                            log.debug("[自动编排] viralId={} 深度分析进行中，本周期跳过推荐及后续步骤", viral.getId());
                            continue;
                        }
                        try {
                            videoInsightIntegrationBridge.requestDeepAnalyzeFromShortvideo(
                                    viral.getId(), SYSTEM_USER_ID, null, "auto-orch-" + viral.getId());
                            analyzed++;
                            log.debug("[自动编排] viralId={} 已提交/重试深度分析（status={}）", viral.getId(), ds);
                        } catch (Exception e) {
                            log.warn("[自动编排] viralId={} 深度分析提交失败: {}", viral.getId(), e.getMessage());
                            errors++;
                        }
                        continue;
                    }
                }

                // 深度门禁后重新加载，避免 candidates 快照与 DB 状态不一致
                SvViralVideo viralFresh = viralVideoRepository.findById(viral.getId()).orElse(null);
                if (viralFresh == null) {
                    continue;
                }
                viral = viralFresh;

                // 3. AI 推荐二创（状态 0→1），此时深度结果已就绪（若启用深度服务）
                if (viralRemakeService != null && viral.getRemakeStatus() != null && viral.getRemakeStatus() == 0) {
                    try {
                        viralRemakeService.recommendRemake(viral.getId(), SYSTEM_USER_ID);
                        recommended++;
                        log.debug("[自动编排] viralId={} 已生成 AI 推荐", viral.getId());
                    } catch (Exception e) {
                        log.warn("[自动编排] viralId={} AI 推荐失败: {}", viral.getId(), e.getMessage());
                        errors++;
                        continue;
                    }
                }

                // 4. 人设匹配 + 自动确认（状态 1→2）
                Long matchedPersonaId = autoMatchAndConfirm(viral);
                if (matchedPersonaId == null) {
                    continue;
                }

                // 5. 生成二创脚本（状态 2→3）
                if (viralRemakeService != null) {
                    try {
                        // 重新加载以获取最新状态
                        SvViralVideo refreshed = viralVideoRepository.findById(viral.getId()).orElse(null);
                        if (refreshed != null && refreshed.getRemakeStatus() != null && refreshed.getRemakeStatus() == 2) {
                            viralRemakeService.generateRemakeScript(viral.getId(), SYSTEM_USER_ID, remakeScriptMode);
                            scripted++;
                            log.debug("[自动编排] viralId={} 脚本已生成", viral.getId());
                        }
                    } catch (Exception e) {
                        log.warn("[自动编排] viralId={} 脚本生成失败: {}", viral.getId(), e.getMessage());
                        errors++;
                        continue;
                    }
                }

                // 6. 排入内容日历
                if (contentCalendarService != null && matchedPersonaId > 0) {
                    try {
                        String from = LocalDate.now().plusDays(1).format(DateTimeFormatter.ISO_DATE);
                        String to = LocalDate.now().plusDays(calendarDaysAhead).format(DateTimeFormatter.ISO_DATE);
                        int created = contentCalendarService.autoGenerate(matchedPersonaId, from, to, SYSTEM_USER_ID);
                        if (created > 0) {
                            scheduled++;
                            log.debug("[自动编排] viralId={} 已排入日历（{}条）", viral.getId(), created);
                        }
                    } catch (Exception e) {
                        log.warn("[自动编排] viralId={} 排入日历失败: {}", viral.getId(), e.getMessage());
                        errors++;
                    }
                }
            } catch (Exception e) {
                log.error("[自动编排] viralId={} 意外异常: {}", viral.getId(), e.getMessage(), e);
                errors++;
            }
        }

        log.info("[自动编排] 流水线完成: 候选={}, 策略跳过={}, 深度分析={}, AI推荐={}, 脚本生成={}, 排入日历={}, 失败={}",
                candidates.size(), skippedPolicy, analyzed, recommended, scripted, scheduled, errors);

        return Map.of(
                "candidates", candidates.size(),
                "skippedContentPolicy", skippedPolicy,
                "analyzed", analyzed,
                "recommended", recommended,
                "scripted", scripted,
                "scheduled", scheduled,
                "errors", errors
        );
    }

    private static String viralTextBlob(SvViralVideo v) {
        String t = v.getTitle() != null ? v.getTitle() : "";
        String tags = v.getTags() != null ? v.getTags() : "";
        String ind = v.getIndustryTags() != null ? v.getIndustryTags() : "";
        return t + "\n" + tags + "\n" + ind;
    }

    /**
     * 筛选待处理的高分爆款（remakeStatus=0, viralScore >= 阈值, 自动采集, ownerId=0）
     */
    private List<SvViralVideo> findCandidates() {
        Specification<SvViralVideo> spec = (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            preds.add(cb.equal(root.get("ownerId"), 0L));
            preds.add(cb.equal(root.get("deleted"), 0));
            preds.add(cb.equal(root.get("autoCollected"), true));
            preds.add(cb.or(cb.isNull(root.get("remakeStatus")), cb.equal(root.get("remakeStatus"), 0)));
            preds.add(cb.greaterThanOrEqualTo(root.get("viralScore"), minViralScore));
            return cb.and(preds.toArray(new Predicate[0]));
        };
        return viralVideoRepository.findAll(spec,
                PageRequest.of(0, batchLimit, Sort.by(Sort.Direction.DESC, "viralScore"))).getContent();
    }

    /**
     * 人设匹配 + 自动确认，返回匹配到的 personaId（无匹配时回退到默认人设）
     */
    private Long autoMatchAndConfirm(SvViralVideo viral) {
        if (viralRemakeService == null) {
            return null;
        }

        // 重新加载获取最新状态
        SvViralVideo refreshed = viralVideoRepository.findById(viral.getId()).orElse(null);
        if (refreshed == null || refreshed.getRemakeStatus() == null || refreshed.getRemakeStatus() != 1) {
            return null;
        }

        Long personaId = defaultPersonaId;

        // 尝试通过 PersonaFusion 匹配最佳人设
        if (personaFusionService != null) {
            try {
                List<Map<String, Object>> matches = personaFusionService.matchPersonas(viral.getId(), SYSTEM_USER_ID);
                if (matches != null && !matches.isEmpty()) {
                    Object pid = matches.get(0).get("personaId");
                    if (pid instanceof Number) {
                        personaId = ((Number) pid).longValue();
                    }
                }
            } catch (Exception e) {
                log.warn("[自动编排] viralId={} 人设匹配失败，使用默认人设: {}", viral.getId(), e.getMessage());
            }
        }

        // 自动确认
        try {
            viralRemakeService.confirmRemake(viral.getId(), "form_imitation", personaId > 0 ? personaId : null, SYSTEM_USER_ID);
            log.debug("[自动编排] viralId={} 已自动确认，personaId={}", viral.getId(), personaId);
            return personaId;
        } catch (Exception e) {
            log.warn("[自动编排] viralId={} 自动确认失败: {}", viral.getId(), e.getMessage());
            return null;
        }
    }
}
