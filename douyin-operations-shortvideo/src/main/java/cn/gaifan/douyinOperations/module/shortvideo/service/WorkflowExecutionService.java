package cn.gaifan.douyinOperations.module.shortvideo.service;

import cn.gaifan.douyinOperations.common.util.ByteArrayMultipartFile;
import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.entity.AiTaskModelConfig;
import cn.gaifan.douyinOperations.module.ai.repository.AiModelRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiTaskModelConfigRepository;
import cn.gaifan.douyinOperations.module.ai.service.AiMusicProvider;
import cn.gaifan.douyinOperations.module.ai.service.AiMusicService;
import cn.gaifan.douyinOperations.module.ai.service.LlmClient;
import cn.gaifan.douyinOperations.module.ai.service.SfxGenerationService;
import cn.gaifan.douyinOperations.module.ai.service.VideoEditService;
import cn.gaifan.douyinOperations.module.shortvideo.util.ShortVideoPathHelper;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvWorkflowTask;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvWorkflowTaskRepository;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvProjectSaveVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvProjectVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvScriptSaveVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvScriptVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvShotListVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvShotVO;
import cn.gaifan.douyinOperations.module.storage.service.BosStorageService;
import com.alibaba.fastjson2.JSON;
import jakarta.annotation.Resource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/**
 * 工作流执行引擎 (Phase 5.1)
 *
 * 将前端节点操作转化为后端异步任务:
 * 1. 接收前端的"从某节点开始执行"请求
 * 2. 异步执行: 脚本→分镜→关键帧→视频→后期→合成
 * 3. projectId > 0 时实际调用 script/shotList/media 服务
 * 4. 无效 projectId 或未配置供应商时明确失败，不返回占位完成
 */
@Service
public class WorkflowExecutionService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowExecutionService.class);

    /** 工作流步骤定义 */
    public enum WorkflowStep {
        SCRIPT("script", "脚本生成"),
        SHOT_LIST("shotList", "分镜设计"),
        KEYFRAME("keyframe", "关键帧生成"),
        VIDEO_GEN("videoGen", "视频生成"),
        POST_PROCESS("postProcess", "后期处理"),
        COMPOSE("compose", "智能合成"),
        PUBLISH("publish", "发布评估");

        public final String nodeId;
        public final String label;

        WorkflowStep(String nodeId, String label) {
            this.nodeId = nodeId;
            this.label = label;
        }
    }

    private static final String REDIS_KEY_PREFIX = "wf:task:";
    private static final long REDIS_TTL_HOURS = 24;

    /** Redis 不可用时的内存回退 */
    private final Map<String, Map<String, Object>> memoryFallback = new ConcurrentHashMap<>();

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Resource
    private SvProjectService projectService;
    @Resource
    private SvScriptService scriptService;
    @Resource
    private SvShotListService shotListService;
    @Resource
    private ShortVideoMaterialService materialService;
    @Resource
    private VideoEditService videoEditService;
    @Resource
    private BosStorageService bosStorageService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate;

    @Resource
    @Qualifier("workflowTaskExecutor")
    private Executor workflowTaskExecutor;

    @Resource
    private LlmClient llmClient;

    @Resource
    private AiTaskModelConfigRepository taskModelConfigRepository;

    @Resource
    private AiModelRepository modelRepository;

    @Resource
    private ShortVideoAiService shortVideoAiService;

    @Resource
    private ContentAuditService contentAuditService;

    @Resource
    private SvWorkflowTaskRepository workflowTaskRepository;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private AiMusicService aiMusicService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private SfxGenerationService sfxGenerationService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private DigitalHumanSynthesisService digitalHumanSynthesisService;

    @Resource
    private WorkflowCommercialGuard workflowCommercialGuard;

    private void saveTaskStatus(String taskId, String status, String currentStep, int progress,
                                Long projectId, Long ownerId) {
        Map<String, Object> data = loadTaskStatus(taskId);
        if (data == null) {
            data = new HashMap<>();
        } else {
            data = new HashMap<>(data);
        }
        data.put("status", status);
        data.put("currentStep", currentStep != null ? currentStep : "script");
        data.put("progress", progress);
        data.put("taskId", taskId);
        data.put("projectId", projectId);
        memoryFallback.put(taskId, data);
        if (stringRedisTemplate != null) {
            try {
                stringRedisTemplate.opsForValue().set(
                        REDIS_KEY_PREFIX + taskId,
                        JSON.toJSONString(data),
                        REDIS_TTL_HOURS,
                        TimeUnit.HOURS);
            } catch (Exception e) {
                log.warn("Redis 保存工作流状态失败: {}", e.getMessage());
            }
        }
        if (workflowTaskRepository != null && ownerId != null) {
            try {
                var opt = workflowTaskRepository.findByTaskId(taskId);
                if (opt.isPresent()) {
                    SvWorkflowTask t = opt.get();
                    t.setStatus(status);
                    t.setCurrentStep(currentStep != null ? currentStep : "script");
                    t.setProgress(progress);
                    workflowTaskRepository.save(t);
                } else {
                    SvWorkflowTask t = new SvWorkflowTask();
                    t.setTaskId(taskId);
                    t.setProjectId(projectId);
                    t.setOwnerId(ownerId);
                    t.setStatus(status);
                    t.setCurrentStep(currentStep != null ? currentStep : "script");
                    t.setProgress(progress);
                    workflowTaskRepository.save(t);
                }
            } catch (Exception e) {
                log.warn("DB 保存工作流状态失败: {}", e.getMessage());
            }
        }
    }

    private Map<String, Object> loadTaskStatus(String taskId) {
        if (stringRedisTemplate != null) {
            try {
                String json = stringRedisTemplate.opsForValue().get(REDIS_KEY_PREFIX + taskId);
                if (StringUtils.hasText(json)) {
                    return JSON.parseObject(json, Map.class);
                }
            } catch (Exception e) {
                log.warn("Redis 读取工作流状态失败: {}", e.getMessage());
            }
        }
        Map<String, Object> mem = memoryFallback.get(taskId);
        if (mem != null) return mem;
        if (workflowTaskRepository != null) {
            try {
                var opt = workflowTaskRepository.findByTaskId(taskId);
                if (opt.isPresent()) {
                    SvWorkflowTask t = opt.get();
                    Map<String, Object> data = new HashMap<>();
                    data.put("status", t.getStatus());
                    data.put("currentStep", t.getCurrentStep());
                    data.put("progress", t.getProgress() != null ? t.getProgress() : 0);
                    return data;
                }
            } catch (Exception e) {
                log.warn("DB 读取工作流状态失败: {}", e.getMessage());
            }
        }
        return null;
    }

    /**
     * 从指定步骤开始执行工作流 (异步)
     *
     * @param projectId 项目 ID
     * @param startStep 起始步骤
     * @param params    步骤参数
     * @param userId    用户 ID
     * @return 工作流任务 ID
     */
    public String executeFrom(Long projectId, String startStep, Map<String, Object> params, Long userId) {
        String taskId = "wf_" + System.currentTimeMillis() + "_" + projectId;

        saveTaskStatus(taskId, "processing", startStep, 10, projectId, userId);
        log.info("工作流任务已提交: taskId={}, startStep={}, projectId={}", taskId, startStep, projectId);
        if (workflowCommercialGuard != null) {
            workflowCommercialGuard.reserveForTask(taskId, projectId, userId);
        }

        workflowTaskExecutor.execute(() -> runStep(taskId, projectId, startStep, params, userId));

        return taskId;
    }

    /**
     * 数字人口播带货一键成片：串联脚本、分镜、数字人口播、产品 B-roll、配音、合成。
     */
    public String executeDigitalHumanCommercePipeline(Long projectId, Map<String, Object> params, Long userId) {
        String taskId = "wf_dh_commerce_" + System.currentTimeMillis() + "_" + projectId;
        Map<String, Object> normalized = params != null ? new HashMap<>(params) : new HashMap<>();
        normalized.putIfAbsent("style", "数字人口播带货 产品细节展示");
        normalized.putIfAbsent("type", "digital_human_commerce");
        normalized.putIfAbsent("duration", 45);
        normalized.putIfAbsent("shotCount", 8);
        normalized.putIfAbsent("quality", "premium-fhd");
        normalized.putIfAbsent("aspectRatio", "9:16");
        saveTaskStatus(taskId, "processing", "pipeline:init", 5, projectId, userId);
        Map<String, Object> status = loadTaskStatus(taskId);
        if (status != null) {
            status.put("pipeline", "digital_human_commerce");
            status.put("steps", commercePipelineSteps());
            memoryFallback.put(taskId, status);
            persistTaskStatus(taskId, status);
        }
        if (workflowCommercialGuard != null) {
            workflowCommercialGuard.reserveForTask(taskId, projectId, userId);
        }
        workflowTaskExecutor.execute(() -> runDigitalHumanCommercePipeline(taskId, projectId, normalized, userId));
        return taskId;
    }

    private void runStep(String taskId, Long projectId, String startStep, Map<String, Object> params, Long userId) {
        try {
            if (projectId != null && projectId > 0) {
                if ("script".equals(startStep)) {
                    runScriptStep(taskId, projectId, params, userId);
                    return;
                }
                if ("shotList".equals(startStep)) {
                    runShotListStep(taskId, projectId, params, userId);
                    return;
                }
                if ("keyframe".equals(startStep)) {
                    runKeyframeStep(taskId, projectId, params, userId);
                    return;
                }
                if ("videoGen".equals(startStep)) {
                    runVideoGenStep(taskId, projectId, params, userId);
                    return;
                }
                if ("postProcess".equals(startStep)) {
                    runPostProcessStep(taskId, projectId, params, userId);
                    return;
                }
                if ("musicGen".equals(startStep)) {
                    runMusicGenStep(taskId, projectId, params, userId);
                    return;
                }
                if ("sfxGen".equals(startStep)) {
                    runSfxGenStep(taskId, projectId, params, userId);
                    return;
                }
                if ("digitalHuman".equals(startStep)) {
                    runDigitalHumanStep(taskId, projectId, params, userId);
                    return;
                }
                if ("compose".equals(startStep)) {
                    runComposeStep(taskId, projectId, params, userId);
                    return;
                }
                if ("publish".equals(startStep)) {
                    runPublishStep(taskId, projectId, params, userId);
                    return;
                }
                if ("digitalHumanCommerce".equals(startStep)) {
                    runDigitalHumanCommercePipeline(taskId, projectId, params, userId);
                    return;
                }
            }
            throw new IllegalArgumentException("工作流必须绑定有效 projectId，当前步骤不会占位完成");
        } catch (Exception e) {
            log.warn("工作流步骤执行失败: taskId={}, step={}, error={}", taskId, startStep, e.getMessage());
            saveTaskStatus(taskId, "failed", startStep, 0, projectId, userId);
            if (workflowCommercialGuard != null) {
                workflowCommercialGuard.releaseTask(taskId, projectId, e.getMessage());
            }
        }
        finalizeWorkflowCommercial(taskId, projectId);
    }

    private void finalizeWorkflowCommercial(String taskId, Long projectId) {
        if (workflowCommercialGuard == null) {
            return;
        }
        Map<String, Object> st = loadTaskStatus(taskId);
        String status = st != null && st.get("status") instanceof String s ? s : "";
        if ("completed".equals(status)) {
            workflowCommercialGuard.commitTask(taskId, projectId);
        } else if ("failed".equals(status)) {
            workflowCommercialGuard.releaseTask(taskId, projectId, "workflow step failed");
        }
    }

    private void runScriptStep(String taskId, Long projectId, Map<String, Object> params, Long userId) {
        try {
            SvProjectVO project = projectService.get(projectId, userId, java.util.List.of(userId));
            if (project == null) {
                saveTaskStatus(taskId, "failed", "script", 0, projectId, userId);
                return;
            }
            if (project.getScriptId() != null && project.getScriptId() > 0) {
                log.info("项目已有脚本 scriptId={}, 跳过生成", project.getScriptId());
                saveTaskStatus(taskId, "completed", "script", 15, projectId, userId);
                return;
            }
            String theme = params.get("theme") instanceof String s ? s : (project.getTitle() != null ? project.getTitle() : "短视频");
            String style = params.get("style") instanceof String s ? s : "温馨";
            int duration = params.get("duration") instanceof Number n ? n.intValue() : 30;
            if (duration <= 0 || duration > 120) duration = 30;
            String scriptContent = scriptService.generate("daily", theme, null, null, style, duration, userId);

            SvScriptSaveVO scriptVo = new SvScriptSaveVO();
            scriptVo.setTitle(project.getTitle());
            scriptVo.setContent(scriptContent);
            scriptVo.setScriptType("daily");
            scriptVo.setTheme(theme);
            scriptVo.setStyle(style);
            Long scriptId = scriptService.save(scriptVo, userId);

            SvProjectSaveVO updateVo = new SvProjectSaveVO();
            updateVo.setId(projectId);
            updateVo.setTitle(project.getTitle());
            updateVo.setProjectType(StringUtils.hasText(project.getProjectType()) ? project.getProjectType() : "daily");
            updateVo.setScriptId(scriptId);
            projectService.save(updateVo, userId);

            log.info("工作流 script 步骤完成: projectId={}, scriptId={}", projectId, scriptId);
            saveTaskStatus(taskId, "completed", "script", 15, projectId, userId);
        } catch (Exception e) {
            log.error("工作流 script 步骤失败", e);
            saveTaskStatus(taskId, "failed", "script", 0, projectId, userId);
        }
    }

    private void runShotListStep(String taskId, Long projectId, Map<String, Object> params, Long userId) {
        try {
            SvProjectVO project = projectService.get(projectId, userId, java.util.List.of(userId));
            if (project == null) {
                saveTaskStatus(taskId, "failed", "shotList", 0, projectId, userId);
                return;
            }
            Long scriptId = project.getScriptId();
            if (scriptId == null || scriptId <= 0) {
                log.warn("项目无 scriptId，请先执行脚本生成");
                saveTaskStatus(taskId, "failed", "shotList", 0, projectId, userId);
                return;
            }
            if (project.getShotListId() != null && project.getShotListId() > 0) {
                log.info("项目已有分镜 shotListId={}, 跳过生成", project.getShotListId());
                saveTaskStatus(taskId, "completed", "shotList", 30, projectId, userId);
                return;
            }
            var scriptVo = scriptService.get(scriptId, userId);
            String scriptContent = scriptVo != null && scriptVo.getContent() != null ? scriptVo.getContent() : "";
            if (!StringUtils.hasText(scriptContent)) {
                log.warn("脚本内容为空, scriptId={}", scriptId);
                saveTaskStatus(taskId, "failed", "shotList", 0, projectId, userId);
                return;
            }
            String style = params.get("style") instanceof String s ? s : "温馨";
            int shotCount = params.get("shotCount") instanceof Number n ? n.intValue() : 6;
            if (shotCount <= 0 || shotCount > 20) shotCount = 6;

            var genResult = shotListService.generateWithResult(scriptId, scriptContent, shotCount, style, userId);
            Long shotListId = genResult.shotListId();

            SvProjectSaveVO updateVo = new SvProjectSaveVO();
            updateVo.setId(projectId);
            updateVo.setTitle(project.getTitle());
            updateVo.setProjectType(StringUtils.hasText(project.getProjectType()) ? project.getProjectType() : "daily");
            updateVo.setScriptId(scriptId);
            updateVo.setShotListId(shotListId);
            projectService.save(updateVo, userId);

            log.info("工作流 shotList 步骤完成: projectId={}, shotListId={}", projectId, shotListId);
            saveTaskStatus(taskId, "completed", "shotList", 30, projectId, userId);
        } catch (Exception e) {
            log.error("工作流 shotList 步骤失败", e);
            saveTaskStatus(taskId, "failed", "shotList", 0, projectId, userId);
        }
    }

    private void runKeyframeStep(String taskId, Long projectId, Map<String, Object> params, Long userId) {
        try {
            SvProjectVO project = projectService.get(projectId, userId, java.util.List.of(userId));
            if (project == null) {
                saveTaskStatus(taskId, "failed", "keyframe", 0, projectId, userId);
                return;
            }
            Long shotListId = project.getShotListId();
            if (shotListId == null || shotListId <= 0) {
                log.warn("项目无 shotListId，请先执行分镜设计");
                saveTaskStatus(taskId, "failed", "keyframe", 0, projectId, userId);
                return;
            }
            var shotListVo = shotListService.get(shotListId, userId);
            if (shotListVo == null || shotListVo.getShots() == null || shotListVo.getShots().isEmpty()) {
                log.warn("分镜列表为空, shotListId={}", shotListId);
                saveTaskStatus(taskId, "failed", "keyframe", 0, projectId, userId);
                return;
            }
            String style = params.get("style") instanceof String s ? s : "温馨";
            List<ShortVideoMaterialService.KeyframeInput> inputs = new ArrayList<>();
            for (SvShotVO shot : shotListVo.getShots()) {
                inputs.add(new ShortVideoMaterialService.KeyframeInput(
                        shot.getId(),
                        shot.getShotNumber(),
                        shot.getSceneDescription(),
                        style,
                        project.getCharacterReferenceUrl(),
                        project.getSceneReferenceUrl()
                ));
            }
            materialService.generateKeyframes(projectId, shotListId, inputs, userId);
            log.info("工作流 keyframe 步骤完成: projectId={}, shotCount={}", projectId, inputs.size());
            saveTaskStatus(taskId, "completed", "keyframe", 45, projectId, userId);
        } catch (Exception e) {
            log.error("工作流 keyframe 步骤失败", e);
            saveTaskStatus(taskId, "failed", "keyframe", 0, projectId, userId);
        }
    }

    private void runVideoGenStep(String taskId, Long projectId, Map<String, Object> params, Long userId) {
        try {
            SvProjectVO project = projectService.get(projectId, userId, java.util.List.of(userId));
            if (project == null) {
                saveTaskStatus(taskId, "failed", "videoGen", 0, projectId, userId);
                return;
            }
            Long shotListId = project.getShotListId();
            if (shotListId == null || shotListId <= 0) {
                log.warn("项目无 shotListId，请先执行分镜设计");
                saveTaskStatus(taskId, "failed", "videoGen", 0, projectId, userId);
                return;
            }
            var shotListVo = shotListService.get(shotListId, userId);
            if (shotListVo == null || shotListVo.getShots() == null || shotListVo.getShots().isEmpty()) {
                log.warn("分镜列表为空, shotListId={}", shotListId);
                saveTaskStatus(taskId, "failed", "videoGen", 0, projectId, userId);
                return;
            }
            int videoDuration = params.get("duration") instanceof Number n ? n.intValue() : 5;
            if (videoDuration <= 0 || videoDuration > 10) videoDuration = 5;
            String defaultMotion = params.get("motion") instanceof String s ? s : "zoom-in";
            if (!StringUtils.hasText(defaultMotion)) defaultMotion = "zoom-in";
            String quality = params.get("quality") instanceof String s ? s : "premium-fhd";
            if (!StringUtils.hasText(quality)) quality = "premium-fhd";
            String aspectRatio = params.get("aspectRatio") instanceof String s ? s : "9:16";
            if (!StringUtils.hasText(aspectRatio)) aspectRatio = "9:16";
            String defaultCamera = params.get("defaultCamera") instanceof String s ? s : null;

            List<ShortVideoMaterialService.Img2VideoInput> inputs = new ArrayList<>();
            for (SvShotVO shot : shotListVo.getShots()) {
                if (!StringUtils.hasText(shot.getKeyframeUrl())) {
                    log.warn("分镜 shotId={} 无关键帧，请先执行关键帧生成", shot.getId());
                    continue;
                }
                String cam = StringUtils.hasText(shot.getCameraType()) ? shot.getCameraType()
                        : (StringUtils.hasText(defaultCamera) ? defaultCamera : "zoom-in");
                inputs.add(new ShortVideoMaterialService.Img2VideoInput(
                        shot.getId(),
                        shot.getShotNumber(),
                        shot.getKeyframeUrl(),
                        shot.getEndFrameUrl(),
                        videoDuration,
                        defaultMotion,
                        shot.getSceneDescription(),
                        quality,
                        aspectRatio,
                        cam,
                        shot.getMood(),
                        shot.getAction()
                ));
            }
            if (inputs.isEmpty()) {
                log.warn("无有效关键帧可生成视频");
                saveTaskStatus(taskId, "failed", "videoGen", 0, projectId, userId);
                return;
            }
            materialService.img2videoBatch(projectId, shotListId, inputs, userId);
            log.info("工作流 videoGen 步骤完成: projectId={}, videoCount={}", projectId, inputs.size());
            saveTaskStatus(taskId, "completed", "videoGen", 60, projectId, userId);
        } catch (Exception e) {
            log.error("工作流 videoGen 步骤失败", e);
            saveTaskStatus(taskId, "failed", "videoGen", 0, projectId, userId);
        }
    }

    /**
     * 后期处理步骤：videoGen 已包含 FFmpeg 后期处理，此处直接通过
     */
    private void runPostProcessStep(String taskId, Long projectId, Map<String, Object> params, Long userId) {
        try {
            log.info("工作流 postProcess 步骤: videoGen 已包含后期处理，直接通过 projectId={}", projectId);
            saveTaskStatus(taskId, "completed", "postProcess", 75, projectId, userId);
        } catch (Exception e) {
            log.error("工作流 postProcess 步骤失败", e);
            saveTaskStatus(taskId, "failed", "postProcess", 0, projectId, userId);
        }
    }

    /** BGM 生成步骤：调用已配置的 AiMusicService */
    private void runMusicGenStep(String taskId, Long projectId, Map<String, Object> params, Long userId) {
        try {
            if (aiMusicService == null) {
                throw new IllegalStateException("BGM 服务未注册");
            }
            String style = params.get("style") instanceof String s ? s : "cinematic background music";
            int duration = params.get("duration") instanceof Number n ? n.intValue() : 30;
            AiMusicProvider.MusicGenerationResult result = aiMusicService.generateBgm(style, duration, true);
            saveTaskStatus(taskId, "completed", "musicGen", 72, projectId, userId);
            Map<String, Object> status = loadTaskStatus(taskId);
            if (status != null) {
                status.put("bgmUrl", result.musicUrl());
                status.put("provider", result.provider());
                memoryFallback.put(taskId, status);
            }
            log.info("工作流 musicGen 步骤完成: projectId={}, provider={}", projectId, result.provider());
        } catch (Exception e) {
            log.warn("工作流 musicGen 步骤失败: {}", e.getMessage());
            saveTaskStatus(taskId, "failed", "musicGen", 0, projectId, userId);
        }
    }

    private void runDigitalHumanCommercePipeline(String taskId, Long projectId, Map<String, Object> params, Long userId) {
        try {
            if (projectId == null || projectId <= 0) {
                throw new IllegalArgumentException("数字人带货成片必须绑定有效 projectId");
            }
            saveTaskStatus(taskId, "processing", "script", 10, projectId, userId);
            ensureCommerceScript(taskId, projectId, params, userId);
            saveTaskStatus(taskId, "processing", "shotList", 24, projectId, userId);
            ensureCommerceShotList(taskId, projectId, params, userId);
            saveTaskStatus(taskId, "processing", "digitalHuman", 36, projectId, userId);
            String digitalHumanUrl = synthesizeDigitalHumanSegment(taskId, projectId, params, userId);
            saveTaskStatus(taskId, "processing", "productBroll:keyframe", 50, projectId, userId);
            generateProductBrollKeyframes(taskId, projectId, params, userId);
            saveTaskStatus(taskId, "processing", "productBroll:video", 66, projectId, userId);
            generateProductBrollVideos(taskId, projectId, params, userId);
            saveTaskStatus(taskId, "processing", "voice", 76, projectId, userId);
            generateVoiceClips(taskId, projectId, params, userId);
            saveTaskStatus(taskId, "processing", "compose", 88, projectId, userId);
            String finalVideoUrl = composeDigitalHumanCommerce(taskId, projectId, params, userId, digitalHumanUrl);
            saveTaskStatus(taskId, "completed", "completed", 100, projectId, userId);
            Map<String, Object> status = loadTaskStatus(taskId);
            if (status != null) {
                status.put("finalVideoUrl", finalVideoUrl);
                status.put("message", "数字人口播带货成片已完成");
                memoryFallback.put(taskId, status);
                persistTaskStatus(taskId, status);
            }
            if (workflowCommercialGuard != null) {
                workflowCommercialGuard.commitTask(taskId, projectId);
            }
        } catch (Exception e) {
            log.error("数字人口播带货一键成片失败: taskId={}, projectId={}", taskId, projectId, e);
            if (workflowCommercialGuard != null) {
                workflowCommercialGuard.releaseTask(taskId, projectId, e.getMessage());
            }
            saveTaskStatus(taskId, "failed", "digitalHumanCommerce", 0, projectId, userId);
            Map<String, Object> status = loadTaskStatus(taskId);
            if (status != null) {
                status.put("errorMessage", e.getMessage());
                memoryFallback.put(taskId, status);
                persistTaskStatus(taskId, status);
            }
        }
    }

    private void ensureCommerceScript(String taskId, Long projectId, Map<String, Object> params, Long userId) {
        SvProjectVO project = requireProject(projectId, userId);
        if (project.getScriptId() != null && project.getScriptId() > 0) {
            putTaskValue(taskId, "scriptId", project.getScriptId());
            return;
        }
        String theme = params.get("theme") instanceof String s && StringUtils.hasText(s) ? s : project.getTitle();
        String style = params.get("style") instanceof String s ? s : "数字人口播带货 产品细节展示";
        int duration = readInt(params.get("duration"), project.getDuration() != null ? project.getDuration() : 45);
        String productInfo = params.get("productInfo") instanceof String s ? s : buildCommerceProductInfo(project);
        String scriptContent = scriptService.generate("digital_human_commerce", theme, null, productInfo, style, duration, userId);

        SvScriptSaveVO scriptVo = new SvScriptSaveVO();
        scriptVo.setTitle(project.getTitle());
        scriptVo.setContent(scriptContent);
        scriptVo.setScriptType("digital_human_commerce");
        scriptVo.setGenerationType("ai");
        scriptVo.setTheme(theme);
        scriptVo.setStyle(style);
        scriptVo.setDuration(duration);
        scriptVo.setWordCount(scriptContent.replaceAll("\\s+", "").length());
        Long scriptId = scriptService.save(scriptVo, userId);

        SvProjectSaveVO update = baseProjectSave(project);
        update.setScriptId(scriptId);
        update.setDuration(duration);
        update.setStatus("processing");
        projectService.save(update, userId);
        putTaskValue(taskId, "scriptId", scriptId);
    }

    private void ensureCommerceShotList(String taskId, Long projectId, Map<String, Object> params, Long userId) {
        SvProjectVO project = requireProject(projectId, userId);
        if (project.getShotListId() != null && project.getShotListId() > 0) {
            putTaskValue(taskId, "shotListId", project.getShotListId());
            return;
        }
        if (project.getScriptId() == null || project.getScriptId() <= 0) {
            throw new IllegalStateException("项目无脚本，无法生成分镜");
        }
        SvScriptVO script = scriptService.get(project.getScriptId(), userId);
        String style = params.get("style") instanceof String s ? s : "数字人口播带货 产品细节展示";
        int shotCount = readInt(params.get("shotCount"), 8);
        var result = shotListService.generateWithResult(project.getScriptId(), script.getContent(), shotCount, style, userId);
        Long shotListId = result.shotListId();
        if (shotListId == null || shotListId <= 0) {
            throw new IllegalStateException("分镜生成未返回 shotListId");
        }
        SvProjectSaveVO update = baseProjectSave(project);
        update.setShotListId(shotListId);
        update.setStatus("processing");
        projectService.save(update, userId);
        putTaskValue(taskId, "shotListId", shotListId);
    }

    private String synthesizeDigitalHumanSegment(String taskId, Long projectId, Map<String, Object> params, Long userId) {
        if (digitalHumanSynthesisService == null || !digitalHumanSynthesisService.isConfigured()) {
            putTaskValue(taskId, "digitalHumanSkipped", true);
            putTaskValue(taskId, "digitalHumanSkipReason", "数字人服务未配置，将仅使用产品 B-roll 和配音合成");
            return null;
        }
        Map<String, Object> request = new HashMap<>(params);
        request.putIfAbsent("scriptText", resolveDigitalHumanScriptText(projectId, userId));
        String videoUrl = digitalHumanSynthesisService.synthesize(request, userId, projectId);
        if (!StringUtils.hasText(videoUrl)) {
            throw new IllegalStateException("数字人服务未返回任务或视频地址");
        }
        putTaskValue(taskId, "digitalHumanVideoUrl", videoUrl);
        return videoUrl;
    }

    private void generateProductBrollKeyframes(String taskId, Long projectId, Map<String, Object> params, Long userId) {
        SvProjectVO project = requireProject(projectId, userId);
        SvShotListVO shotList = requireShotList(project, userId);
        String style = params.get("style") instanceof String s ? s : "数字人口播带货 产品细节展示";
        List<ShortVideoMaterialService.KeyframeInput> inputs = new ArrayList<>();
        for (SvShotVO shot : shotList.getShots()) {
            if (isAvatarOnlyShot(shot)) {
                continue;
            }
            inputs.add(new ShortVideoMaterialService.KeyframeInput(
                    shot.getId(),
                    shot.getShotNumber(),
                    enrichProductBrollPrompt(shot),
                    style,
                    project.getCharacterReferenceUrl(),
                    project.getSceneReferenceUrl()
            ));
        }
        if (inputs.isEmpty()) {
            throw new IllegalStateException("没有可生成产品 B-roll 的分镜");
        }
        List<ShortVideoMaterialService.KeyframeResult> results =
                materialService.generateKeyframes(projectId, project.getShotListId(), inputs, userId);
        putTaskValue(taskId, "productBrollKeyframeCount", results.size());
    }

    private void generateProductBrollVideos(String taskId, Long projectId, Map<String, Object> params, Long userId) {
        SvProjectVO project = requireProject(projectId, userId);
        SvShotListVO shotList = requireShotList(project, userId);
        int duration = readInt(params.get("clipDuration"), 5);
        String quality = params.get("quality") instanceof String s ? s : "premium-fhd";
        String aspectRatio = params.get("aspectRatio") instanceof String s ? s : "9:16";
        String motion = params.get("motion") instanceof String s ? s : "slow-push-in";
        List<ShortVideoMaterialService.Img2VideoInput> inputs = new ArrayList<>();
        for (SvShotVO shot : shotList.getShots()) {
            if (isAvatarOnlyShot(shot) || !StringUtils.hasText(shot.getKeyframeUrl())) {
                continue;
            }
            inputs.add(new ShortVideoMaterialService.Img2VideoInput(
                    shot.getId(),
                    shot.getShotNumber(),
                    shot.getKeyframeUrl(),
                    shot.getEndFrameUrl(),
                    duration,
                    motion,
                    shot.getSceneDescription(),
                    quality,
                    aspectRatio,
                    StringUtils.hasText(shot.getCameraType()) ? shot.getCameraType() : "push-in",
                    shot.getMood(),
                    shot.getAction()
            ));
        }
        if (inputs.isEmpty()) {
            throw new IllegalStateException("没有带关键帧的产品 B-roll 分镜，请先确认关键帧生成结果");
        }
        List<ShortVideoMaterialService.VideoResult> results =
                materialService.img2videoBatch(projectId, project.getShotListId(), inputs, userId);
        putTaskValue(taskId, "productBrollVideoCount", results.size());
    }

    private void generateVoiceClips(String taskId, Long projectId, Map<String, Object> params, Long userId) {
        SvProjectVO project = requireProject(projectId, userId);
        SvShotListVO shotList = requireShotList(project, userId);
        String voice = params.get("voice") instanceof String s ? s : "default";
        double speed = params.get("speed") instanceof Number n ? n.doubleValue() : 1.0;
        List<ShortVideoMaterialService.VoiceInput> inputs = new ArrayList<>();
        for (SvShotVO shot : shotList.getShots()) {
            if (StringUtils.hasText(shot.getDialogue())) {
                inputs.add(new ShortVideoMaterialService.VoiceInput(
                        shot.getId(),
                        shot.getShotNumber(),
                        shot.getDialogue(),
                        voice,
                        speed
                ));
            }
        }
        if (inputs.isEmpty()) {
            putTaskValue(taskId, "voiceSkipped", true);
            return;
        }
        List<ShortVideoMaterialService.VoiceResult> results =
                materialService.generateVoiceBatch(projectId, project.getShotListId(), inputs, userId);
        putTaskValue(taskId, "voiceClipCount", results.size());
    }

    private String composeDigitalHumanCommerce(String taskId, Long projectId, Map<String, Object> params,
                                               Long userId, String digitalHumanUrl) {
        SvProjectVO project = requireProject(projectId, userId);
        SvShotListVO shotList = requireShotList(project, userId);
        List<String> videoUrls = new ArrayList<>();
        if (StringUtils.hasText(digitalHumanUrl) && !isPendingProviderUrl(digitalHumanUrl)) {
            videoUrls.add(digitalHumanUrl);
        } else if (StringUtils.hasText(digitalHumanUrl)) {
            putTaskValue(taskId, "digitalHumanPendingUrl", digitalHumanUrl);
        }
        List<String> voiceClipUrls = new ArrayList<>();
        for (SvShotVO shot : shotList.getShots()) {
            if (StringUtils.hasText(shot.getVideoUrl())) {
                videoUrls.add(shot.getVideoUrl());
            }
            if (StringUtils.hasText(shot.getAudioUrl())) {
                voiceClipUrls.add(shot.getAudioUrl());
            }
        }
        if (videoUrls.isEmpty()) {
            throw new IllegalStateException("没有可合成的视频素材：数字人未返回可访问 URL，产品 B-roll 也为空");
        }
        String bgmUrl = params.get("bgmUrl") instanceof String s ? s : null;
        String scriptText = resolveProjectScriptText(projectId, userId);
        VideoEditService.AutoComposeRequest request = new VideoEditService.AutoComposeRequest(
                videoUrls,
                null,
                scriptText,
                StringUtils.hasText(bgmUrl) ? bgmUrl : null,
                "digital_human_commerce",
                voiceClipUrls.isEmpty() ? null : voiceClipUrls,
                null,
                null
        );
        VideoEditService.VideoResult result = videoEditService.autoCompose(request, userId);
        String finalVideoUrl = result != null ? result.videoUrl() : null;
        finalVideoUrl = uploadFinalVideoIfLocal(finalVideoUrl, projectId, userId);
        if (!StringUtils.hasText(finalVideoUrl)) {
            throw new IllegalStateException("合成服务未返回成片地址");
        }
        int durationSec = result != null && result.duration() != null ? (int) (result.duration() / 1000) : 0;
        SvProjectSaveVO save = baseProjectSave(project);
        save.setFinalVideoUrl(finalVideoUrl);
        save.setDuration(durationSec > 0 ? durationSec : project.getDuration());
        save.setStatus("completed");
        projectService.save(save, userId);
        putTaskValue(taskId, "composeVideoCount", videoUrls.size());
        putTaskValue(taskId, "finalVideoUrl", finalVideoUrl);
        return finalVideoUrl;
    }

    /** 音效生成步骤：调用已配置的 SfxGenerationService */
    private void runSfxGenStep(String taskId, Long projectId, Map<String, Object> params, Long userId) {
        try {
            if (sfxGenerationService == null) {
                throw new IllegalStateException("SFX 服务未注册");
            }
            String sceneDesc = params.get("sceneDesc") instanceof String s ? s : resolveProjectScriptText(projectId, userId);
            double duration = params.get("durationSec") instanceof Number n ? n.doubleValue() : 5.0;
            List<SfxGenerationService.SfxResult> results = sfxGenerationService.generateSfxFromScene(sceneDesc, duration, userId);
            if (results.isEmpty()) {
                throw new IllegalStateException("未从场景描述中生成可用音效");
            }
            List<String> urls = results.stream().map(SfxGenerationService.SfxResult::audioUrl)
                    .filter(StringUtils::hasText).toList();
            saveTaskStatus(taskId, "completed", "sfxGen", 72, projectId, userId);
            Map<String, Object> status = loadTaskStatus(taskId);
            if (status != null) {
                status.put("sfxUrls", urls);
                memoryFallback.put(taskId, status);
            }
            log.info("工作流 sfxGen 步骤完成: projectId={}, count={}", projectId, urls.size());
        } catch (Exception e) {
            log.warn("工作流 sfxGen 步骤失败: {}", e.getMessage());
            saveTaskStatus(taskId, "failed", "sfxGen", 0, projectId, userId);
        }
    }

    /** 数字人口播步骤：调用 DigitalHumanSynthesisService；未配置则明确失败 */
    private void runDigitalHumanStep(String taskId, Long projectId, Map<String, Object> params, Long userId) {
        try {
            if (digitalHumanSynthesisService == null || !digitalHumanSynthesisService.isConfigured()) {
                throw new IllegalStateException("数字人服务未配置");
            }
            Map<String, Object> request = new HashMap<>(params);
            request.putIfAbsent("scriptText", resolveProjectScriptText(projectId, userId));
            String videoUrl = digitalHumanSynthesisService.synthesize(request, userId, projectId);
            if (!StringUtils.hasText(videoUrl)) {
                throw new IllegalStateException("数字人服务未返回任务或视频地址");
            }
            saveTaskStatus(taskId, "completed", "digitalHuman", 72, projectId, userId);
            Map<String, Object> status = loadTaskStatus(taskId);
            if (status != null) {
                status.put("digitalHumanVideoUrl", videoUrl);
                memoryFallback.put(taskId, status);
            }
            log.info("工作流 digitalHuman 步骤完成: projectId={}", projectId);
        } catch (Exception e) {
            log.warn("工作流 digitalHuman 步骤失败: {}", e.getMessage());
            saveTaskStatus(taskId, "failed", "digitalHuman", 0, projectId, userId);
        }
    }

    private String resolveProjectScriptText(Long projectId, Long userId) {
        SvProjectVO project = projectService.get(projectId, userId, java.util.List.of(userId));
        if (project != null && project.getScriptId() != null && project.getScriptId() > 0) {
            SvScriptVO scriptVo = scriptService.get(project.getScriptId(), userId);
            if (scriptVo != null && StringUtils.hasText(scriptVo.getContent())) {
                return scriptVo.getContent();
            }
        }
        return "";
    }

    /**
     * 智能合成步骤：合并视频、配音、字幕、BGM，上传 BOS，更新项目成片 URL
     */
    private void runComposeStep(String taskId, Long projectId, Map<String, Object> params, Long userId) {
        try {
            SvProjectVO project = projectService.get(projectId, userId, java.util.List.of(userId));
            if (project == null) {
                saveTaskStatus(taskId, "failed", "compose", 0, projectId, userId);
                return;
            }
            Long shotListId = project.getShotListId();
            if (shotListId == null || shotListId <= 0) {
                log.warn("项目无 shotListId，请先执行分镜设计");
                saveTaskStatus(taskId, "failed", "compose", 0, projectId, userId);
                return;
            }
            var shotListVo = shotListService.get(shotListId, userId);
            if (shotListVo == null || shotListVo.getShots() == null || shotListVo.getShots().isEmpty()) {
                log.warn("分镜列表为空, shotListId={}", shotListId);
                saveTaskStatus(taskId, "failed", "compose", 0, projectId, userId);
                return;
            }
            List<String> videoUrls = new ArrayList<>();
            List<String> voiceClipUrls = new ArrayList<>();
            for (SvShotVO shot : shotListVo.getShots()) {
                if (StringUtils.hasText(shot.getVideoUrl())) {
                    videoUrls.add(shot.getVideoUrl());
                    voiceClipUrls.add(StringUtils.hasText(shot.getAudioUrl()) ? shot.getAudioUrl() : null);
                }
            }
            boolean allHaveAudio = voiceClipUrls.stream().allMatch(StringUtils::hasText);
            if (!allHaveAudio) {
                voiceClipUrls = null;
            }
            if (videoUrls.isEmpty()) {
                log.warn("无视频片段可合成，请先执行视频生成");
                saveTaskStatus(taskId, "failed", "compose", 0, projectId, userId);
                return;
            }
            String scriptText = "";
            if (project.getScriptId() != null && project.getScriptId() > 0) {
                SvScriptVO scriptVo = scriptService.get(project.getScriptId(), userId);
                if (scriptVo != null && StringUtils.hasText(scriptVo.getContent())) {
                    scriptText = scriptVo.getContent();
                }
            }
            String bgmUrl = params.get("bgmUrl") instanceof String s ? s : null;
            @SuppressWarnings("unchecked")
            List<String> sfxUrls = params.get("sfxUrls") instanceof List ? (List<String>) params.get("sfxUrls") : null;
            VideoEditService.AutoComposeRequest req = new VideoEditService.AutoComposeRequest(
                    videoUrls,
                    null,
                    scriptText,
                    StringUtils.hasText(bgmUrl) ? bgmUrl : null,
                    "default",
                    voiceClipUrls != null && !voiceClipUrls.isEmpty() ? voiceClipUrls : null,
                    sfxUrls,
                    null
            );
            VideoEditService.VideoResult result = videoEditService.autoCompose(req, userId);
            String finalVideoUrl = result != null ? result.videoUrl() : null;
            if (StringUtils.hasText(finalVideoUrl) && bosStorageService.isConfigured() && projectId != null) {
                try {
                    Path p = Path.of(finalVideoUrl);
                    if (Files.exists(p)) {
                        byte[] bytes = Files.readAllBytes(p);
                        String date = LocalDate.now().format(DATE_FMT);
                        String key = ShortVideoPathHelper.finalVideoKey(userId, date, projectId);
                        var mf = new ByteArrayMultipartFile("file", "final.mp4", "video/mp4", bytes);
                        finalVideoUrl = bosStorageService.upload(key, mf);
                    }
                } catch (Exception e) {
                    log.warn("成片上传 BOS 失败: {}", e.getMessage());
                }
            }
            int durationSec = result != null && result.duration() != null ? (int) (result.duration() / 1000) : 0;
            if (projectId != null && StringUtils.hasText(finalVideoUrl)) {
                try {
                    SvProjectSaveVO save = new SvProjectSaveVO();
                    save.setId(projectId);
                    save.setTitle(project.getTitle());
                    save.setProjectType(StringUtils.hasText(project.getProjectType()) ? project.getProjectType() : "daily");
                    save.setFinalVideoUrl(finalVideoUrl);
                    save.setDuration(durationSec > 0 ? durationSec : null);
                    projectService.save(save, userId);
                } catch (Exception e) {
                    log.warn("更新项目成片 URL 失败: {}", e.getMessage());
                }
            }
            log.info("工作流 compose 步骤完成: projectId={}, finalVideoUrl={}", projectId, finalVideoUrl);
            saveTaskStatus(taskId, "completed", "compose", 90, projectId, userId);
            putTaskValue(taskId, "finalVideoUrl", finalVideoUrl);
        } catch (Exception e) {
            log.error("工作流 compose 步骤失败", e);
            saveTaskStatus(taskId, "failed", "compose", 0, projectId, userId);
        }
    }

    /**
     * 发布评估步骤：标题生成 + AI 审核
     */
    private void runPublishStep(String taskId, Long projectId, Map<String, Object> params, Long userId) {
        try {
            SvProjectVO project = projectService.get(projectId, userId, java.util.List.of(userId));
            if (project == null) {
                saveTaskStatus(taskId, "failed", "publish", 0, projectId, userId);
                return;
            }
            String scriptText = "";
            if (project.getScriptId() != null && project.getScriptId() > 0) {
                SvScriptVO scriptVo = scriptService.get(project.getScriptId(), userId);
                if (scriptVo != null && StringUtils.hasText(scriptVo.getContent())) {
                    scriptText = scriptVo.getContent();
                }
            }
            if (shortVideoAiService != null && StringUtils.hasText(scriptText)) {
                var vo = new cn.gaifan.douyinOperations.module.shortvideo.vo.AiTitleGenerateVO();
                vo.setCopyText(scriptText);
                vo.setCount(3);
                shortVideoAiService.generateTitles(vo, userId);
            }
            if (contentAuditService != null && StringUtils.hasText(project.getFinalVideoUrl())) {
                contentAuditService.auditVideo(project.getFinalVideoUrl());
            }
            log.info("工作流 publish 步骤完成: projectId={}", projectId);
            saveTaskStatus(taskId, "completed", "publish", 100, projectId, userId);
        } catch (Exception e) {
            log.error("工作流 publish 步骤失败", e);
            saveTaskStatus(taskId, "failed", "publish", 0, projectId, userId);
        }
    }

    /**
     * 查询工作流任务状态
     */
    public Map<String, Object> getStatus(String taskId) {
        Map<String, Object> data = loadTaskStatus(taskId);
        if (data != null) {
            Map<String, Object> result = new LinkedHashMap<>(data);
            String status = String.valueOf(result.getOrDefault("status", "unknown"));
            String currentStep = String.valueOf(result.getOrDefault("currentStep", "script"));
            Object p = data.get("progress");
            int progress = p instanceof Number n ? n.intValue() : ("completed".equals(status) ? 100 : "failed".equals(status) ? 0 : 50);
            result.put("status", status);
            result.put("currentStep", currentStep);
            result.put("progress", progress);
            return result;
        }
        return Map.of("status", "unknown", "currentStep", "script", "progress", 0);
    }

    /**
     * AI 微调某个步骤的参数：接入 LLM 解析用户意图，返回更新后的参数建议
     */
    public Map<String, Object> aiAssistNode(Long projectId, String nodeId, String userInput, Long userId) {
        Map<String, Object> updatedParams = new HashMap<>();
        String reply = "已理解您的需求。";
        if (llmClient != null && StringUtils.hasText(userInput)) {
            try {
                List<AiModel> models = resolveShortVideoModels();
                if (!models.isEmpty()) {
                    String system = """
                            你是短视频工作流 AI 助手。用户会描述对某个步骤的修改需求。
                            请根据用户输入，返回 JSON 格式的参数字段建议，例如：
                            {"style":"搞笑","duration":60} 或 {"shotCount":8,"style":"悬疑"} 或 {"defaultCamera":"orbit","quality":"cinema-4k"}
                            只返回 JSON，不要其他说明。若无法解析则返回 {}。
                            """;
                    String prompt = "用户对步骤 " + nodeId + " 的修改需求：" + userInput + "\n请返回 JSON 格式的参数字段：";
                    var response = llmClient.chatWithFallback(models, system, prompt);
                    if (response.success() && StringUtils.hasText(response.content())) {
                        String content = response.content().trim();
                        if (content.startsWith("```")) {
                            int start = content.indexOf("{");
                            int end = content.lastIndexOf("}") + 1;
                            if (start >= 0 && end > start) content = content.substring(start, end);
                        }
                        if (content.startsWith("{")) {
                            Map<?, ?> parsed = JSON.parseObject(content, Map.class);
                            if (parsed != null && !parsed.isEmpty()) {
                                parsed.forEach((k, v) -> updatedParams.put(String.valueOf(k), v));
                                reply = "已根据您的需求更新参数：" + content;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("aiAssistNode LLM 调用失败: {}", e.getMessage());
            }
        }
        return Map.of("reply", reply, "updatedParams", updatedParams);
    }

    private List<AiModel> resolveShortVideoModels() {
        var config = taskModelConfigRepository.findByTaskCodeAndStatusAndDeleted("short_video_workflow", 1, 0)
                .or(() -> taskModelConfigRepository.findByTaskCodeAndStatusAndDeleted("short_video_script", 1, 0));
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

    private SvProjectVO requireProject(Long projectId, Long userId) {
        SvProjectVO project = projectService.get(projectId, userId, java.util.List.of(userId));
        if (project == null) {
            throw new IllegalStateException("项目不存在或无权限访问");
        }
        return project;
    }

    private SvShotListVO requireShotList(SvProjectVO project, Long userId) {
        Long shotListId = project.getShotListId();
        if (shotListId == null || shotListId <= 0) {
            throw new IllegalStateException("项目未关联分镜，无法生产素材");
        }
        SvShotListVO shotList = shotListService.get(shotListId, userId);
        if (shotList == null || shotList.getShots() == null || shotList.getShots().isEmpty()) {
            throw new IllegalStateException("分镜列表为空");
        }
        return shotList;
    }

    private SvProjectSaveVO baseProjectSave(SvProjectVO project) {
        SvProjectSaveVO save = new SvProjectSaveVO();
        save.setId(project.getId());
        save.setTitle(project.getTitle());
        save.setProjectType(StringUtils.hasText(project.getProjectType()) ? project.getProjectType() : "daily");
        save.setAccountId(project.getAccountId());
        save.setPersonaId(project.getPersonaId());
        save.setScheduleDate(project.getScheduleDate() == null ? null : project.getScheduleDate().toString());
        save.setShootStatus(project.getShootStatus());
        save.setStatus(project.getStatus());
        save.setScriptId(project.getScriptId());
        save.setShotListId(project.getShotListId());
        save.setFinalVideoUrl(project.getFinalVideoUrl());
        save.setThumbnailUrl(project.getThumbnailUrl());
        save.setCharacterReferenceUrl(project.getCharacterReferenceUrl());
        save.setSceneReferenceUrl(project.getSceneReferenceUrl());
        save.setDuration(project.getDuration());
        save.setRelatedProductIds(project.getRelatedProductIds());
        save.setPublishTitle(project.getPublishTitle());
        save.setPublishPlatforms(project.getPublishPlatforms());
        save.setPublishTime(project.getPublishTime() == null ? null : project.getPublishTime().toString());
        save.setReviewStatus(project.getReviewStatus());
        return save;
    }

    private String buildCommerceProductInfo(SvProjectVO project) {
        StringBuilder sb = new StringBuilder();
        sb.append("数字人口播带货，要求口播 + 产品细节展示 + 使用演示 + 合规 CTA。");
        if (project.getRelatedProductIds() != null && !project.getRelatedProductIds().isEmpty()) {
            sb.append("关联商品ID：").append(project.getRelatedProductIds()).append("。");
        }
        return sb.toString();
    }

    private String resolveDigitalHumanScriptText(Long projectId, Long userId) {
        String scriptText = resolveProjectScriptText(projectId, userId);
        if (!StringUtils.hasText(scriptText)) {
            return "";
        }
        String[] lines = scriptText.split("\\R");
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            String trimmed = line.trim();
            if (!StringUtils.hasText(trimmed)) {
                continue;
            }
            if (trimmed.contains("数字人") || trimmed.contains("口播") || trimmed.contains("dialogue")
                    || trimmed.contains("台词") || trimmed.contains("钩子") || trimmed.contains("总结")) {
                sb.append(trimmed.replaceAll("^[\\-\\d\\.、\\s]+", "")).append('\n');
            }
        }
        String result = sb.toString().trim();
        return StringUtils.hasText(result) ? result : scriptText;
    }

    private boolean isAvatarOnlyShot(SvShotVO shot) {
        String text = ((shot.getSceneDescription() == null ? "" : shot.getSceneDescription()) + " "
                + (shot.getAction() == null ? "" : shot.getAction()) + " "
                + (shot.getCameraType() == null ? "" : shot.getCameraType()) + " "
                + (shot.getDialogue() == null ? "" : shot.getDialogue())).toLowerCase();
        return text.contains("avatar_talking_head")
                || (text.contains("数字人") && !text.contains("产品") && !text.contains("商品") && !text.contains("细节"));
    }

    private String enrichProductBrollPrompt(SvShotVO shot) {
        String scene = StringUtils.hasText(shot.getSceneDescription()) ? shot.getSceneDescription() : "产品细节展示";
        return scene + "。竖屏 9:16，电商带货产品 B-roll，清晰展示产品包装、材质、质地、使用动作或对比证据，避免纯数字人头像画面。";
    }

    private boolean isPendingProviderUrl(String url) {
        return url != null && (url.startsWith("heygen:pending:") || url.startsWith("did:pending:"));
    }

    private int readInt(Object value, int fallback) {
        if (value instanceof Number n) {
            return n.intValue();
        }
        if (value instanceof String s && StringUtils.hasText(s)) {
            try {
                return Integer.parseInt(s.trim());
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private String uploadFinalVideoIfLocal(String finalVideoUrl, Long projectId, Long userId) {
        if (!StringUtils.hasText(finalVideoUrl) || !bosStorageService.isConfigured() || projectId == null) {
            return finalVideoUrl;
        }
        try {
            Path p = Path.of(finalVideoUrl);
            if (Files.exists(p)) {
                byte[] bytes = Files.readAllBytes(p);
                String date = LocalDate.now().format(DATE_FMT);
                String key = ShortVideoPathHelper.finalVideoKey(userId, date, projectId);
                var mf = new ByteArrayMultipartFile("file", "final.mp4", "video/mp4", bytes);
                return bosStorageService.upload(key, mf);
            }
        } catch (Exception e) {
            log.warn("成片上传 BOS 失败: {}", e.getMessage());
        }
        return finalVideoUrl;
    }

    private List<Map<String, Object>> commercePipelineSteps() {
        return List.of(
                Map.of("step", "script", "label", "带货脚本", "progress", 10),
                Map.of("step", "shotList", "label", "口播+B-roll 分镜", "progress", 24),
                Map.of("step", "digitalHuman", "label", "数字人口播片段", "progress", 36),
                Map.of("step", "productBroll:keyframe", "label", "产品细节关键帧", "progress", 50),
                Map.of("step", "productBroll:video", "label", "产品 B-roll 视频", "progress", 66),
                Map.of("step", "voice", "label", "分镜配音", "progress", 76),
                Map.of("step", "compose", "label", "自动合成成片", "progress", 88),
                Map.of("step", "completed", "label", "完成", "progress", 100)
        );
    }

    private void putTaskValue(String taskId, String key, Object value) {
        Map<String, Object> status = loadTaskStatus(taskId);
        if (status == null) {
            status = new HashMap<>();
        } else {
            status = new HashMap<>(status);
        }
        status.put(key, value);
        memoryFallback.put(taskId, status);
        persistTaskStatus(taskId, status);
    }

    private void persistTaskStatus(String taskId, Map<String, Object> status) {
        if (stringRedisTemplate != null) {
            try {
                stringRedisTemplate.opsForValue().set(
                        REDIS_KEY_PREFIX + taskId,
                        JSON.toJSONString(status),
                        REDIS_TTL_HOURS,
                        TimeUnit.HOURS);
            } catch (Exception e) {
                log.warn("Redis 保存工作流扩展状态失败: {}", e.getMessage());
            }
        }
    }
}
