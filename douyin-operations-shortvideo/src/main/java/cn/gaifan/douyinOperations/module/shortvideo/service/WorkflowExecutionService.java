package cn.gaifan.douyinOperations.module.shortvideo.service;

import cn.gaifan.douyinOperations.common.util.ByteArrayMultipartFile;
import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.entity.AiTaskModelConfig;
import cn.gaifan.douyinOperations.module.ai.repository.AiModelRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiTaskModelConfigRepository;
import cn.gaifan.douyinOperations.module.ai.service.LlmClient;
import cn.gaifan.douyinOperations.module.ai.service.VideoEditService;
import cn.gaifan.douyinOperations.module.shortvideo.util.ShortVideoPathHelper;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvWorkflowTask;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvWorkflowTaskRepository;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvProjectSaveVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvProjectVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvScriptSaveVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvScriptVO;
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
 * 3. projectId > 0 时实际调用 script/shotList 服务
 * 4. 其他步骤或 projectId=0 时占位完成
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

    private void saveTaskStatus(String taskId, String status, String currentStep, int progress,
                                Long projectId, Long ownerId) {
        Map<String, Object> data = new HashMap<>();
        data.put("status", status);
        data.put("currentStep", currentStep != null ? currentStep : "script");
        data.put("progress", progress);
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

        workflowTaskExecutor.execute(() -> runStep(taskId, projectId, startStep, params, userId));

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
            }
            // 占位：projectId=0 时
            Thread.sleep(2000);
            saveTaskStatus(taskId, "completed", "completed", 100, projectId, userId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            saveTaskStatus(taskId, "failed", startStep, 0, projectId, userId);
        } catch (Exception e) {
            log.warn("工作流步骤执行失败: taskId={}, step={}, error={}", taskId, startStep, e.getMessage());
            saveTaskStatus(taskId, "failed", startStep, 0, projectId, userId);
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

    /** BGM 生成步骤：占位，compose 时可用 AiMusicService 生成 */
    private void runMusicGenStep(String taskId, Long projectId, Map<String, Object> params, Long userId) {
        log.info("工作流 musicGen 步骤: 占位通过 projectId={}", projectId);
        saveTaskStatus(taskId, "completed", "musicGen", 72, projectId, userId);
    }

    /** 音效生成步骤：占位，compose 时可用 SfxGenerationService */
    private void runSfxGenStep(String taskId, Long projectId, Map<String, Object> params, Long userId) {
        log.info("工作流 sfxGen 步骤: 占位通过 projectId={}", projectId);
        saveTaskStatus(taskId, "completed", "sfxGen", 72, projectId, userId);
    }

    /** 数字人口播步骤：占位，口播类型项目可用 HeyGenProvider */
    private void runDigitalHumanStep(String taskId, Long projectId, Map<String, Object> params, Long userId) {
        log.info("工作流 digitalHuman 步骤: 占位通过 projectId={}", projectId);
        saveTaskStatus(taskId, "completed", "digitalHuman", 72, projectId, userId);
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
                    voiceClipUrls.isEmpty() ? null : voiceClipUrls,
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
            String status = String.valueOf(data.getOrDefault("status", "unknown"));
            String currentStep = String.valueOf(data.getOrDefault("currentStep", "script"));
            Object p = data.get("progress");
            int progress = p instanceof Number n ? n.intValue() : ("completed".equals(status) ? 100 : "failed".equals(status) ? 0 : 50);
            return Map.of(
                    "status", status,
                    "currentStep", currentStep,
                    "progress", progress
            );
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
}
