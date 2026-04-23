package cn.gaifan.douyinOperations.module.shortvideo.schedule;

import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.live.entity.LiveSession;
import cn.gaifan.douyinOperations.module.live.repository.LiveSessionRepository;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvProject;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvScript;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvProjectRepository;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvScriptRepository;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvShootingTaskRepository;
import cn.gaifan.douyinOperations.module.shortvideo.service.DailyShootService;
import cn.gaifan.douyinOperations.module.shortvideo.service.SvShootingTaskService;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvShootingTaskSaveVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 明日存在直播计划时，按人设生成一条日更脚本并创建拍摄工单（幂等、失败隔离）。
 * 默认关闭，由 {@code app.shortvideo.daily-script-generation.enabled} 控制。
 */
@Component
@ConditionalOnProperty(prefix = "app.shortvideo.daily-script-generation", name = "enabled", havingValue = "true")
public class DailyScriptGenerationScheduler {

    private static final Logger log = LoggerFactory.getLogger(DailyScriptGenerationScheduler.class);

    static final String DESCRIPTION_MARKER = "[daily-scheduler]";

    private final LiveSessionRepository liveSessionRepository;
    private final SvShootingTaskRepository shootingTaskRepository;
    private final DailyShootService dailyShootService;
    private final SvShootingTaskService shootingTaskService;
    private final SvProjectRepository projectRepository;
    private final SvScriptRepository scriptRepository;

    @Value("${app.shortvideo.daily-script-generation.zone:Asia/Shanghai}")
    private String zoneId;

    @Value("${app.shortvideo.daily-script-generation.max-targets-per-run:50}")
    private int maxTargetsPerRun;

    @Value("${app.shortvideo.daily-script-generation.persona-ids:}")
    private String personaIdsRaw;

    public DailyScriptGenerationScheduler(LiveSessionRepository liveSessionRepository,
                                         SvShootingTaskRepository shootingTaskRepository,
                                         DailyShootService dailyShootService,
                                         SvShootingTaskService shootingTaskService,
                                         SvProjectRepository projectRepository,
                                         SvScriptRepository scriptRepository) {
        this.liveSessionRepository = liveSessionRepository;
        this.shootingTaskRepository = shootingTaskRepository;
        this.dailyShootService = dailyShootService;
        this.shootingTaskService = shootingTaskService;
        this.projectRepository = projectRepository;
        this.scriptRepository = scriptRepository;
    }

    @Scheduled(cron = "${app.shortvideo.daily-script-generation.cron:0 0 5 * * *}",
            zone = "${app.shortvideo.daily-script-generation.zone:Asia/Shanghai}")
    public void run() {
        ZoneId zone = ZoneId.of(zoneId != null && !zoneId.isBlank() ? zoneId : "Asia/Shanghai");
        LocalDate tomorrow = LocalDate.now(zone).plusDays(1);
        Timestamp start = Timestamp.from(tomorrow.atStartOfDay(zone).toInstant());
        Timestamp end = Timestamp.from(tomorrow.plusDays(1).atStartOfDay(zone).toInstant());

        List<LiveSession> sessions = liveSessionRepository.findScheduledBetween(start, end);
        if (sessions.isEmpty()) {
            log.debug("daily-script-generation: no live_session with persona in window {} .. {}", start, end);
            return;
        }

        Set<Long> personaWhitelist = parsePersonaWhitelist();
        Map<String, LiveSession> dedup = new LinkedHashMap<>();
        for (LiveSession s : sessions) {
            if (s.getPersonaId() == null || s.getUserId() == null) {
                continue;
            }
            if (!personaWhitelist.isEmpty() && !personaWhitelist.contains(s.getPersonaId())) {
                continue;
            }
            String key = s.getUserId() + ":" + s.getPersonaId();
            dedup.putIfAbsent(key, s);
        }

        List<LiveSession> targets = new ArrayList<>(dedup.values());
        if (targets.size() > maxTargetsPerRun) {
            targets = targets.subList(0, Math.max(0, maxTargetsPerRun));
        }

        String scheduleDateStr = tomorrow.toString();
        Date shootDate = Date.valueOf(tomorrow);
        int ok = 0;
        int skipped = 0;
        int failed = 0;

        for (LiveSession session : targets) {
            Long userId = session.getUserId();
            Long personaId = session.getPersonaId();
            try {
                if (shootingTaskRepository.existsByOwnerIdAndPersonaIdAndShootDateAndDescriptionContainingAndDeleted(
                        userId, personaId, shootDate, DESCRIPTION_MARKER, 0)) {
                    skipped++;
                    continue;
                }

                String topic = StringUtils.hasText(session.getLiveTitle())
                        ? session.getLiveTitle().trim()
                        : "";
                if (topic.length() > 200) {
                    topic = topic.substring(0, 200);
                }

                Map<String, Object> result = dailyShootService.generateDaily(
                        userId, personaId, scheduleDateStr, 1, null, null,
                        StringUtils.hasText(topic) ? topic : null);

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> plans = result != null && result.get("plans") instanceof List<?> pl
                        ? (List<Map<String, Object>>) pl
                        : List.of();
                if (plans.isEmpty()) {
                    log.warn("daily-script-generation: generateDaily returned no plan userId={} personaId={}", userId, personaId);
                    failed++;
                    continue;
                }

                Object planIdObj = plans.get(0).get("planId");
                if (!(planIdObj instanceof Number)) {
                    failed++;
                    continue;
                }
                long planId = ((Number) planIdObj).longValue();
                SvProject project = projectRepository.findById(planId).orElse(null);
                if (project == null || project.getDeleted() != null && project.getDeleted() != 0) {
                    failed++;
                    continue;
                }

                String scriptContent = null;
                if (project.getScriptId() != null) {
                    SvScript sc = scriptRepository.findById(project.getScriptId()).orElse(null);
                    if (sc != null && StringUtils.hasText(sc.getContent())) {
                        scriptContent = sc.getContent();
                    }
                }

                SvShootingTaskSaveVO vo = new SvShootingTaskSaveVO();
                vo.setTitle(trimTitle("日更调度 · " + (StringUtils.hasText(session.getLiveTitle()) ? session.getLiveTitle() : "拍摄"), 256));
                vo.setShootDate(scheduleDateStr);
                vo.setPersonaId(personaId);
                vo.setProjectId(project.getId());
                vo.setScriptId(project.getScriptId());
                vo.setScriptContent(scriptContent);
                vo.setDescription(DESCRIPTION_MARKER + " liveSessionId=" + session.getId() + " personaId=" + personaId);
                vo.setShootingBrief("关联场次计划开播，已预生成日更脚本与分镜，请按工单拍摄。");
                vo.setStatus(0);

                shootingTaskService.save(vo, userId);
                ok++;
            } catch (BusinessException ex) {
                log.warn("daily-script-generation: skip userId={} personaId={} — {}", userId, personaId, ex.getMessage());
                failed++;
            } catch (Exception ex) {
                log.error("daily-script-generation: error userId={} personaId={}", userId, personaId, ex);
                failed++;
            }
        }

        log.info("daily-script-generation: date={} sessionsRaw={} targets={} ok={} skippedIdempotent={} failed={}",
                scheduleDateStr, sessions.size(), targets.size(), ok, skipped, failed);
    }

    private Set<Long> parsePersonaWhitelist() {
        if (!StringUtils.hasText(personaIdsRaw)) {
            return Set.of();
        }
        return java.util.Arrays.stream(personaIdsRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> {
                    try {
                        return Long.parseLong(s);
                    } catch (NumberFormatException e) {
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private static String trimTitle(String title, int maxLen) {
        if (title == null) {
            return "";
        }
        String t = title.trim();
        if (t.length() <= maxLen) {
            return t;
        }
        return t.substring(0, maxLen);
    }
}
