package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.entity.AiTaskModelConfig;
import cn.gaifan.douyinOperations.module.ai.repository.AiModelRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiTaskModelConfigRepository;
import cn.gaifan.douyinOperations.module.ai.service.LlmClient;
import cn.gaifan.douyinOperations.module.douyin.entity.DyPersona;
import cn.gaifan.douyinOperations.module.douyin.service.DouyinPersonaService;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvProject;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvScript;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvShot;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvShotList;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvProjectRepository;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvScriptRepository;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvShotListRepository;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvShotRepository;
import cn.gaifan.douyinOperations.module.shortvideo.service.DailyShootService;
import cn.gaifan.douyinOperations.module.shortvideo.service.SvShotListService;
import cn.gaifan.douyinOperations.contract.product.FeatureCode;
import cn.gaifan.douyinOperations.contract.product.ProductCode;
import cn.gaifan.douyinOperations.common.config.RequestIdentityHolder;
import cn.gaifan.douyinOperations.contract.credit.CreditReservationActionRequest;
import cn.gaifan.douyinOperations.contract.credit.CreditReserveRequest;
import cn.gaifan.douyinOperations.module.platform.credit.CommercialCreditHelper;
import cn.gaifan.douyinOperations.module.platform.credit.CommercialProductChargeService;
import cn.gaifan.douyinOperations.module.platform.identity.CommercialIdentityBridge;
import cn.gaifan.douyinOperations.module.platform.product.DeliveryLedgerService;
import cn.gaifan.douyinOperations.module.platform.product.DeliveryProduct;
import java.math.BigDecimal;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 每日拍摄脚本 Service 实现
 */
@Service
public class DailyShootServiceImpl implements DailyShootService {

    private static final String PROJECT_TYPE_DAILY = "daily";
    private static final int MAX_DAILY_QUOTA = 3;
    private static final String TASK_CODE = "short_video_script";

    @Resource
    private DouyinPersonaService personaService;
    @Resource
    private SvProjectRepository projectRepository;
    @Resource
    private SvScriptRepository scriptRepository;
    @Resource
    private SvShotListRepository shotListRepository;
    @Resource
    private SvShotRepository shotRepository;
    @Resource
    private SvShotListService shotListService;
    @Resource
    private LlmClient llmClient;
    @Resource
    private AiTaskModelConfigRepository taskModelConfigRepository;
    @Resource
    private AiModelRepository modelRepository;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private CommercialProductChargeService commercialProductChargeService;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private CommercialCreditHelper commercialCreditHelper;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private DeliveryLedgerService deliveryLedgerService;
    @Resource
    private ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> generateDaily(Long userId, Long personaId, String scheduleDateStr,
                                              Integer count, String style, String duration, String topic) {
        if (userId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        DyPersona persona = personaService.getPersona(personaId, userId);
        if (persona == null) throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "人设不存在或无权访问");

        Date scheduleDate = parseDate(scheduleDateStr);
        if (scheduleDate == null) throw new BusinessException(ErrorCode.VALIDATION_FAIL, "计划日期格式错误，请使用 yyyy-MM-dd");
        Date today = Date.valueOf(LocalDate.now());
        if (scheduleDate.before(today))
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "计划拍摄日期不可早于当天");

        int genCount = count != null && count >= 1 && count <= 3 ? count : 1;
        long existing = projectRepository.countByOwnerIdAndPersonaIdAndProjectTypeAndScheduleDateAndDeleted(
                userId, personaId, PROJECT_TYPE_DAILY, scheduleDate, 0);
        if (existing + genCount > MAX_DAILY_QUOTA)
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "该人设该日期最多 3 条脚本，当前已有 " + existing + " 条");

        String dur = StringUtils.hasText(duration) ? duration : "30秒";
        String sty = StringUtils.hasText(style) ? style : "口播";
        String top = StringUtils.hasText(topic) ? topic : "";

        List<Map<String, Object>> plans = new ArrayList<>();
        for (int i = 0; i < genCount; i++) {
            Map<String, Object> plan = createOneDailyScript(userId, persona, scheduleDate, dur, sty, top);
            if (plan != null) plans.add(plan);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("plans", plans);
        result.put("quotaRemaining", MAX_DAILY_QUOTA - (int) existing - plans.size());
        return result;
    }

    private Map<String, Object> createOneDailyScript(Long userId, DyPersona persona, Date scheduleDate,
                                                     String duration, String style, String topic) {
        String scriptPrompt = buildScriptPrompt(persona, duration, style, topic);
        List<AiModel> models = resolveModels();
        if (models.isEmpty())
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "请先在「AI 模型配置」中配置可用模型");

        LlmClient.LlmResponse resp = llmClient.chatWithFallback(models,
                "你是专业的短视频脚本创作师，输出严格为 JSON 格式，不要其他说明。", scriptPrompt);
        if (resp == null || !resp.success())
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "AI 生成脚本失败: " + (resp != null ? resp.errorMsg() : "无可用模型"));

        String[] parsed = parseTitleAndContentFromLlm(resp.content());
        String title = parsed[0];
        String scriptContent = parsed[1];

        SvScript script = new SvScript();
        script.setOwnerId(userId);
        script.setTitle(title != null ? title : "每日拍摄 " + scheduleDate);
        script.setContent(scriptContent);
        script.setScriptType(PROJECT_TYPE_DAILY);
        script.setGenerationType("ai");
        script.setTheme(topic);
        script.setStyle(style);
        script = scriptRepository.save(script);

        SvShotListService.GenerateResult genResult = shotListService.generateWithResult(
                script.getId(), scriptContent, 6, style, userId);
        if (genResult.shotListId() == null) return null;

        SvShotList shotList = shotListRepository.findById(genResult.shotListId()).orElseThrow();
        List<SvShot> shots = shotRepository.findByShotListIdAndDeletedOrderByShotNumberAsc(shotList.getId(), 0);
        for (SvShot s : shots) {
            s.setReviewStatus("pending");
            shotRepository.save(s);
        }

        SvProject project = new SvProject();
        project.setOwnerId(userId);
        project.setAccountId(persona.getAccountId());
        project.setTitle(script.getTitle());
        project.setProjectType(PROJECT_TYPE_DAILY);
        project.setPersonaId(persona.getId());
        project.setScheduleDate(scheduleDate);
        project.setShootStatus("not_started");
        project.setStatus("draft");
        project.setScriptId(script.getId());
        project.setShotListId(shotList.getId());
        project = projectRepository.save(project);

        Map<String, Object> plan = new HashMap<>();
        plan.put("planId", project.getId());
        plan.put("title", script.getTitle());
        plan.put("sceneCount", shots.size());
        plan.put("scheduleDate", scheduleDate.toString());
        return plan;
    }

    private String buildScriptPrompt(DyPersona persona, String duration, String style, String topic) {
        StringBuilder sb = new StringBuilder();
        sb.append("请为人设「").append(persona.getPersonaName()).append("」创作一条 ");
        sb.append(duration).append(" 的 ").append(style).append(" 风格短视频脚本。");
        if (StringUtils.hasText(topic)) sb.append(" 主题：").append(topic).append("。");
        if (StringUtils.hasText(persona.getContentStyle())) sb.append(" 人设风格：").append(persona.getContentStyle()).append("。");
        sb.append(" 输出格式：{\"title\":\"脚本标题\",\"content\":\"完整脚本正文\"}");
        return sb.toString();
    }

    /** @return [title, content] */
    private String[] parseTitleAndContentFromLlm(String raw) {
        if (!StringUtils.hasText(raw)) return new String[]{null, ""};
        try {
            int start = raw.indexOf('{');
            int end = raw.lastIndexOf('}');
            if (start >= 0 && end > start) {
                Map<String, Object> m = objectMapper.readValue(raw.substring(start, end + 1), new TypeReference<>() {});
                String t = m.get("title") != null ? m.get("title").toString().trim() : null;
                String c = m.get("content") != null ? m.get("content").toString().trim() : raw;
                return new String[]{t, c};
            }
        } catch (Exception ignored) {
            // JSON解析失败，返回原始内容
        }
        return new String[]{null, raw};
    }

    private List<AiModel> resolveModels() {
        var config = taskModelConfigRepository.findByTaskCodeAndStatusAndDeleted(TASK_CODE, 1, 0);
        if (config.isPresent()) {
            AiTaskModelConfig tc = config.get();
            List<AiModel> result = new ArrayList<>();
            for (Long modelId : Arrays.asList(tc.getPrimaryModelId(), tc.getFallbackModelId(), tc.getFallback2ModelId())) {
                if (modelId == null) continue;
                modelRepository.findById(modelId).filter(m -> m.getStatus() == 1 && m.getDeleted() == 0)
                        .ifPresent(result::add);
            }
            if (!result.isEmpty()) return result;
        }
        return modelRepository.findByStatusAndDeleted(1, 0).stream().limit(3).toList();
    }

    @Override
    public List<Map<String, Object>> dailyList(Long userId, String scheduleDateStr, Long personaId) {
        if (userId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        List<SvProject> all = projectRepository.findByOwnerIdAndProjectTypeAndDeletedOrderByScheduleDateDescCreateTimeDesc(
                userId, PROJECT_TYPE_DAILY, 0);

        Date filterDate = parseDate(scheduleDateStr);
        return all.stream()
                .filter(p -> (filterDate == null || (p.getScheduleDate() != null && p.getScheduleDate().equals(filterDate))))
                .filter(p -> (personaId == null || (p.getPersonaId() != null && p.getPersonaId().equals(personaId))))
                .map(this::projectToDailyItem)
                .collect(Collectors.toList());
    }

    private Map<String, Object> projectToDailyItem(SvProject p) {
        Map<String, Object> m = new HashMap<>();
        m.put("planId", p.getId());
        m.put("title", p.getTitle());
        m.put("scheduleDate", p.getScheduleDate() != null ? p.getScheduleDate().toString() : null);
        m.put("personaId", p.getPersonaId());
        m.put("shootStatus", p.getShootStatus() != null ? p.getShootStatus() : "not_started");
        m.put("status", p.getStatus());
        m.put("scriptId", p.getScriptId());
        m.put("shotListId", p.getShotListId());
        m.put("createTime", p.getCreateTime());
        return m;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> reviewShot(Long userId, Long shotId, String reviewStatus, String reviewerNote) {
        if (userId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        if (!"approved".equals(reviewStatus) && !"needs_revision".equals(reviewStatus))
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "reviewStatus 需为 approved 或 needs_revision");

        SvShot shot = shotRepository.findById(shotId).orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "分镜不存在"));
        SvShotList list = shotListRepository.findById(shot.getShotListId()).orElseThrow();
        if (!list.getOwnerId().equals(userId)) throw new BusinessException(ErrorCode.FORBIDDEN, "无权限");

        SvProject project = projectRepository.findByShotListIdAndDeleted(list.getId(), 0)
                .filter(p -> PROJECT_TYPE_DAILY.equals(p.getProjectType()))
                .filter(p -> p.getOwnerId().equals(userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "未找到关联的每日拍摄项目"));

        shot.setReviewStatus(reviewStatus);
        shot.setReviewerNote(reviewerNote);
        shotRepository.save(shot);

        List<SvShot> shots = shotRepository.findByShotListIdAndDeletedOrderByShotNumberAsc(list.getId(), 0);
        boolean allApproved = shots.stream().allMatch(s -> "approved".equals(s.getReviewStatus()));
        if (allApproved) {
            project.setShootStatus("ready");
            projectRepository.save(project);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("sceneId", shotId);
        result.put("reviewStatus", reviewStatus);
        result.put("allApproved", allApproved);
        result.put("shootStatus", allApproved ? "ready" : project.getShootStatus());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateShootStatus(Long userId, Long projectId, String shootStatus) {
        if (userId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        if (!StringUtils.hasText(shootStatus)) throw new BusinessException(ErrorCode.VALIDATION_FAIL, "shootStatus 不能为空");
        Set<String> allowed = Set.of("not_started", "ready", "shooting", "shot_done");
        if (!allowed.contains(shootStatus)) throw new BusinessException(ErrorCode.VALIDATION_FAIL, "shootStatus 无效");

        SvProject project = projectRepository.findById(projectId).orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "项目不存在"));
        if (!project.getOwnerId().equals(userId)) throw new BusinessException(ErrorCode.FORBIDDEN, "无权限");
        if (!PROJECT_TYPE_DAILY.equals(project.getProjectType()))
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "仅每日拍摄类型可更新拍摄状态");

        project.setShootStatus(shootStatus);
        projectRepository.save(project);
    }

    @Override
    public String exportScript(Long userId, Long projectId) {
        if (userId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        String traceId = "sv-export-" + projectId + "-" + System.currentTimeMillis();
        String reservationId = null;
        if (commercialCreditHelper != null && commercialCreditHelper.isEnforced()) {
            var ctx = RequestIdentityHolder.current();
            String tenantId = CommercialIdentityBridge.resolveTenantId(ctx);
            if ("default".equals(tenantId) || tenantId == null || tenantId.isBlank()) {
                tenantId = "demo-tenant";
            }
            String gfUser = CommercialIdentityBridge.resolveUserId(ctx);
            var reserved = commercialCreditHelper.reserve(new CreditReserveRequest(
                    tenantId,
                    gfUser,
                    null,
                    ProductCode.SHORTVIDEO_MAKER,
                    FeatureCode.SHORTVIDEO_EXPORT,
                    ctx != null && ctx.channel() != null ? ctx.channel() : "WEB",
                    BigDecimal.ONE,
                    "standard",
                    traceId,
                    "project-" + projectId,
                    "成片脚本导出冻结 projectId=" + projectId
            ));
            reservationId = reserved.reservationId();
        } else if (commercialProductChargeService != null) {
            commercialProductChargeService.charge(
                    CommercialProductChargeService.CommercialProductChargeCommand.of(
                            ProductCode.SHORTVIDEO_MAKER,
                            FeatureCode.SHORTVIDEO_EXPORT,
                            "成片脚本导出 projectId=" + projectId,
                            DeliveryProduct.SHORTVIDEO_MAKER
                    ));
        }
        SvProject project = projectRepository.findById(projectId).orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "项目不存在"));
        if (!project.getOwnerId().equals(userId)) throw new BusinessException(ErrorCode.FORBIDDEN, "无权限");

        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════════════\n");
        sb.append("  拍摄脚本 - ").append(project.getTitle()).append("\n");
        sb.append("  计划日期：").append(project.getScheduleDate() != null ? project.getScheduleDate() : "-").append("\n");
        sb.append("═══════════════════════════════════════\n\n");

        if (project.getShotListId() != null) {
            List<SvShot> shots = shotRepository.findByShotListIdAndDeletedOrderByShotNumberAsc(project.getShotListId(), 0);
            for (SvShot s : shots) {
                sb.append("【分镜 ").append(s.getShotNumber()).append("】");
                if (StringUtils.hasText(s.getTimeRange())) sb.append(" ").append(s.getTimeRange());
                sb.append("\n");
                if (StringUtils.hasText(s.getSceneDescription())) sb.append("  场景：").append(s.getSceneDescription()).append("\n");
                if (StringUtils.hasText(s.getCameraAngle())) sb.append("  机位：").append(s.getCameraAngle()).append("\n");
                if (StringUtils.hasText(s.getCameraType())) sb.append("  运镜：").append(s.getCameraType()).append("\n");
                if (StringUtils.hasText(s.getDialogue())) sb.append("  台词：").append(s.getDialogue()).append("\n");
                if (StringUtils.hasText(s.getReviewerNote())) sb.append("  备注：").append(s.getReviewerNote()).append("\n");
                sb.append("\n");
            }
        }
        String result = sb.toString();
        finalizeExportCommercial(reservationId, traceId, projectId);
        return result;
    }

    private void finalizeExportCommercial(String reservationId, String traceId, Long projectId) {
        if (reservationId == null || commercialCreditHelper == null) {
            return;
        }
        var ctx = RequestIdentityHolder.current();
        String tenantId = CommercialIdentityBridge.resolveTenantId(ctx);
        if ("default".equals(tenantId) || tenantId == null || tenantId.isBlank()) {
            tenantId = "demo-tenant";
        }
        commercialCreditHelper.commit(reservationId, new CreditReservationActionRequest(
                tenantId,
                CommercialIdentityBridge.resolveUserId(ctx),
                null,
                ctx != null && ctx.channel() != null ? ctx.channel() : "WEB",
                traceId,
                "成片脚本导出 commit projectId=" + projectId
        ));
        if (deliveryLedgerService != null) {
            deliveryLedgerService.recordShortvideoMakerDelivery(tenantId, traceId, "EXPORTED");
        }
    }

    private static Date parseDate(String s) {
        if (!StringUtils.hasText(s)) return null;
        try {
            return Date.valueOf(LocalDate.parse(s.trim(), DateTimeFormatter.ISO_LOCAL_DATE));
        } catch (Exception e) {
            return null;
        }
    }
}
