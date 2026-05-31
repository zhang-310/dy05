package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.entity.AiTaskModelConfig;
import cn.gaifan.douyinOperations.module.ai.repository.AiModelRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiTaskModelConfigRepository;
import cn.gaifan.douyinOperations.module.ai.service.LlmClient;
import cn.gaifan.douyinOperations.module.ai.service.OperationalStrategyKnowledgeService;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvScript;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvShot;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvShotList;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvScriptRepository;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvShotListRepository;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvShotRepository;
import cn.gaifan.douyinOperations.module.shortvideo.service.SvShotListService;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvShotListVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvShotSaveVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvShotVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SvShotListServiceImpl implements SvShotListService {

    @Resource
    private SvShotListRepository shotListRepository;
    @Resource
    private SvShotRepository shotRepository;
    @Resource
    private SvScriptRepository scriptRepository;
    @Resource
    private LlmClient llmClient;
    @Resource
    private AiTaskModelConfigRepository taskModelConfigRepository;
    @Resource
    private AiModelRepository modelRepository;
    @Resource
    private ObjectMapper objectMapper;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private OperationalStrategyKnowledgeService operationalStrategyKnowledgeService;

    private static final String TASK_CODE = "short_video_script";

    @Override
    public SvShotListVO get(Long id, Long ownerId) {
        if (ownerId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        SvShotList list = shotListRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "分镜列表不存在"));
        if (!list.getOwnerId().equals(ownerId)) throw new BusinessException(ErrorCode.FORBIDDEN, "无权限访问");
        if (list.getDeleted() != 0) throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "分镜列表不存在");
        SvShotListVO vo = new SvShotListVO();
        vo.setId(list.getId());
        vo.setOwnerId(list.getOwnerId());
        vo.setScriptId(list.getScriptId());
        vo.setShotCount(list.getShotCount());
        vo.setCreateTime(list.getCreateTime());
        vo.setUpdateTime(list.getUpdateTime());
        List<SvShot> shots = shotRepository.findByShotListIdAndDeletedOrderByShotNumberAsc(list.getId(), 0);
        vo.setShots(shots.stream().map(this::shotToVO).toList());
        return vo;
    }

    @Override
    public PageResultVO<SvShotListVO> list(Long ownerId, Integer page, Integer rows) {
        if (ownerId == null) return PageResultVO.of(0L, List.of(), 0, 20);
        int p = page != null && page >= 0 ? page : 0;
        int r = rows != null && rows > 0 ? Math.min(rows, 100) : 20;
        var pageable = PageRequest.of(p, r, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createTime"));
        var pg = shotListRepository.findByOwnerIdAndDeletedOrderByCreateTimeDesc(ownerId, 0, pageable);
        List<SvShotListVO> list = pg.getContent().stream().map(sl -> {
            SvShotListVO vo = new SvShotListVO();
            vo.setId(sl.getId());
            vo.setOwnerId(sl.getOwnerId());
            vo.setScriptId(sl.getScriptId());
            vo.setShotCount(sl.getShotCount());
            vo.setCreateTime(sl.getCreateTime());
            vo.setUpdateTime(sl.getUpdateTime());
            return vo;
        }).toList();
        return PageResultVO.of(pg.getTotalElements(), list, p, r);
    }

    @Override
    public SvShotListVO getLatestByScriptId(Long scriptId, Long ownerId) {
        if (scriptId == null || ownerId == null) return null;
        List<SvShotList> lists = shotListRepository.findByScriptIdAndDeletedOrderByCreateTimeDesc(scriptId, 0);
        if (lists.isEmpty()) return null;
        SvShotList list = lists.get(0);
        if (!list.getOwnerId().equals(ownerId)) return null;
        return get(list.getId(), ownerId);
    }

    @Override
    public Long save(SvShotSaveVO vo, Long ownerId) {
        if (ownerId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        if (vo.getShotListId() == null) throw new BusinessException(ErrorCode.VALIDATION_FAIL, "shotListId 不能为空");
        SvShotList list = shotListRepository.findById(vo.getShotListId()).orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "分镜列表不存在"));
        if (!list.getOwnerId().equals(ownerId)) throw new BusinessException(ErrorCode.FORBIDDEN, "无权限修改");
        if (vo.getShots() != null) {
            for (SvShotSaveVO.SvShotItemVO item : vo.getShots()) {
                SvShot shot;
                if (item.getId() != null && item.getId() > 0) {
                    shot = shotRepository.findById(item.getId()).orElse(new SvShot());
                    if (shot.getId() != null && !shotRepository.findById(shot.getId()).map(s -> s.getShotListId().equals(list.getId())).orElse(false))
                        continue;
                } else {
                    shot = new SvShot();
                }
                shot.setShotListId(list.getId());
                shot.setShotNumber(item.getShotNumber() != null ? item.getShotNumber() : 0);
                shot.setTimeRange(item.getTimeRange());
                shot.setSceneDescription(item.getSceneDescription());
                shot.setCameraAngle(item.getCameraAngle());
                shot.setCameraType(item.getCameraType());
                shot.setAction(item.getAction());
                shot.setDialogue(item.getDialogue());
                shot.setMood(item.getMood());
                shot.setKeyframeUrl(item.getKeyframeUrl());
                shot.setKeyframeBosKey(item.getKeyframeBosKey());
                shot.setEndFrameUrl(item.getEndFrameUrl());
                shot.setEndFrameBosKey(item.getEndFrameBosKey());
                shot.setVideoUrl(item.getVideoUrl());
                shot.setVideoBosKey(item.getVideoBosKey());
                shot.setAudioUrl(item.getAudioUrl());
                shot.setAudioBosKey(item.getAudioBosKey());
                shot.setDuration(item.getDuration());
                shotRepository.save(shot);
            }
        }
        return list.getId();
    }

    @Override
    public void deleteShot(Long shotId, Long ownerId) {
        if (ownerId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        if (shotId == null) throw new BusinessException(ErrorCode.VALIDATION_FAIL, "shotId 不能为空");
        SvShot shot = shotRepository.findById(shotId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "分镜不存在"));
        SvShotList list = shotListRepository.findById(shot.getShotListId())
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "分镜列表不存在"));
        if (!list.getOwnerId().equals(ownerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权限删除");
        }
        shot.setDeleted(1);
        shotRepository.save(shot);
        int remaining = Math.max(0, list.getShotCount() != null ? list.getShotCount() - 1 : 0);
        list.setShotCount(remaining);
        shotListRepository.save(list);
    }

    @Override
    public List<SvShotVO> generate(Long scriptId, String scriptContent, Integer shotCount, String style, Long ownerId) {
        if (ownerId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        if (!StringUtils.hasText(scriptContent)) throw new BusinessException(ErrorCode.VALIDATION_FAIL, "脚本内容不能为空");
        int count = shotCount != null && shotCount > 0 ? Math.min(shotCount, 12) : 6;
        String commerceInstruction = isDigitalHumanCommerce(scriptContent, style) ? """
                数字人口播带货专项分镜约束：
                - 每个分镜还必须包含 shotRole 和 visualAssetType。
                - shotRole 只能从 avatar_talking_head、product_closeup、usage_demo、proof_overlay、cta 中选择。
                - visualAssetType 只能从 digital_human、product_broll、screen_overlay、comparison 中选择。
                - 数字人正脸口播约占 35%-45%，产品特写、使用演示、证据覆盖和对比镜头约占 55%-65%。
                - product_closeup 至少 2 条，usage_demo 至少 1 条，proof_overlay 至少 1 条；画面必须证明口播，不要只有空泛情绪镜头。
                - 口播、字幕和画面都必须避开绝对化、医疗化、无法证明的价格/功效承诺。
                """ : "";
        String prompt = String.format("""
                请将以下短视频创作简报和脚本拆分为 %d 个可执行分镜。
                每个分镜必须包含：timeRange（如0-3s）、sceneDescription（场景描述）、cameraAngle（机位：俯拍/平拍/仰拍/特写）、cameraType（运镜：push-in/pan/static/handheld）、action（动作）、dialogue（口播/台词）、mood（情绪）、duration（秒）。
                %s
                %s
                以 JSON 数组格式输出，不要其他说明。
                示例：[{"shotNumber":1,"timeRange":"0-3s","shotRole":"avatar_talking_head","visualAssetType":"digital_human","sceneDescription":"数字人正脸近景，右侧叠加痛点字幕","cameraAngle":"近景","cameraType":"push-in","action":"数字人看镜头抛出痛点","dialogue":"先别急着买，先看这三个判断点","mood":"提醒","duration":3}]
                创作简报和脚本：
                %s
                """, count, buildShortVideoShotOpsContext(ownerId, scriptContent + " " + style), commerceInstruction, scriptContent);
        List<AiModel> models = resolveShortVideoModels();
        if (models.isEmpty()) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "请先在「AI 模型配置」中配置至少一个可用模型（如 DeepSeek/Ollama）");
        }
        LlmClient.LlmResponse respObj = llmClient.chatWithFallback(models, "你是专业的分镜师，输出严格为 JSON 数组，不要其他说明。", prompt);
        if (respObj == null || !respObj.success()) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "AI 生成分镜失败: " + (respObj != null ? respObj.errorMsg() : "无可用模型"));
        }
        String resp = respObj.content() != null ? respObj.content() : "";
        List<SvShotVO> shots = parseShotsFromLlm(resp, count);
        if (scriptId != null) {
            SvScript script = scriptRepository.findById(scriptId).orElse(null);
            if (script != null && script.getOwnerId().equals(ownerId)) {
                SvShotList list = new SvShotList();
                list.setOwnerId(ownerId);
                list.setScriptId(scriptId);
                list.setShotCount(shots.size());
                list = shotListRepository.save(list);
                for (int i = 0; i < shots.size(); i++) {
                    SvShot shot = new SvShot();
                    shot.setShotListId(list.getId());
                    shot.setShotNumber(i + 1);
                    shot.setTimeRange(shots.get(i).getTimeRange());
                    shot.setSceneDescription(shots.get(i).getSceneDescription());
                    shot.setCameraAngle(shots.get(i).getCameraAngle());
                    shot.setCameraType(shots.get(i).getCameraType());
                    shot.setAction(shots.get(i).getAction());
                    shot.setDialogue(shots.get(i).getDialogue());
                    shot.setMood(shots.get(i).getMood());
                    shot.setDuration(shots.get(i).getDuration());
                    shotRepository.save(shot);
                }
            }
        }
        return shots;
    }

    @Override
    public SvShotListService.GenerateResult generateWithResult(Long scriptId, String scriptContent, Integer shotCount, String style, Long ownerId) {
        List<SvShotVO> shots = generate(scriptId, scriptContent, shotCount, style, ownerId);
        Long shotListId = null;
        if (scriptId != null) {
            List<SvShotList> lists = shotListRepository.findByScriptIdAndDeletedOrderByCreateTimeDesc(scriptId, 0);
            if (!lists.isEmpty()) shotListId = lists.get(0).getId();
        }
        return new SvShotListService.GenerateResult(shots, shotListId);
    }

    private List<AiModel> resolveShortVideoModels() {
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

    private String buildShortVideoShotOpsContext(Long ownerId, String query) {
        if (operationalStrategyKnowledgeService == null || ownerId == null || ownerId <= 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        try {
            var official = operationalStrategyKnowledgeService.buildShortVideoGenerationContext(
                    ownerId,
                    String.join(" ", query != null ? query : "", "短视频分镜 数字人成片 官方规则 违规规则 千川素材审核"),
                    2200);
            if (official != null && official.hasText()) {
                sb.append(official.promptBlock()).append("\n");
            }
        } catch (Exception ignored) {
        }
        try {
            var viral = operationalStrategyKnowledgeService.buildViralPatternContext(
                    ownerId,
                    String.join(" ", query != null ? query : "", "爆款分镜 镜头节奏 产品展示 数字人口播"),
                    1600);
            if (viral != null && viral.hasText()) {
                sb.append(viral.promptBlock()).append("\n");
            }
        } catch (Exception ignored) {
        }
        if (sb.isEmpty()) {
            return "";
        }
        sb.append("分镜强制约束：必须遵守 douyin 与 douyin_weigui 引用；爆款模式只用于镜头节奏和结构借鉴，禁止复刻违规表达。\n");
        return sb.toString();
    }

    private List<SvShotVO> parseShotsFromLlm(String resp, int defaultCount) {
        List<SvShotVO> result = new ArrayList<>();
        if (!StringUtils.hasText(resp)) return result;
        try {
            String json = resp.trim();
            int start = json.indexOf('[');
            int end = json.lastIndexOf(']');
            if (start >= 0 && end > start) json = json.substring(start, end + 1);
            List<Map<String, Object>> arr = objectMapper.readValue(json, new TypeReference<>() {});
            for (int i = 0; i < arr.size(); i++) {
                Map<String, Object> m = arr.get(i);
                SvShotVO vo = new SvShotVO();
                vo.setShotNumber(i + 1);
                vo.setTimeRange(getStr(m, "timeRange", (i + 1) + "-" + (i + 2) + "s"));
                vo.setSceneDescription(getStr(m, "sceneDescription", ""));
                vo.setCameraAngle(getStr(m, "cameraAngle", "平拍"));
                vo.setCameraType(getStr(m, "cameraType", ""));
                vo.setAction(getStr(m, "action", ""));
                vo.setDialogue(getStr(m, "dialogue", ""));
                vo.setMood(getStr(m, "mood", ""));
                vo.setDuration(parseInteger(m.get("duration")));
                result.add(vo);
            }
        } catch (Exception e) {
            Pattern p = Pattern.compile("(\\d+)-(\\d+)s|场景[：:]?([^，,]+)|台词[：:]?([^，,]+)");
            Matcher matcher = p.matcher(resp);
            int i = 1;
            while (matcher.find() && result.size() < defaultCount) {
                SvShotVO vo = new SvShotVO();
                vo.setShotNumber(i++);
                vo.setTimeRange(matcher.group(0));
                vo.setSceneDescription(resp.substring(Math.max(0, matcher.start() - 20), Math.min(resp.length(), matcher.end() + 50)));
                result.add(vo);
            }
        }
        if (result.isEmpty()) {
            for (int i = 1; i <= defaultCount; i++) {
                SvShotVO vo = new SvShotVO();
                vo.setShotNumber(i);
                vo.setTimeRange((i - 1) * 5 + "-" + i * 5 + "s");
                vo.setSceneDescription("分镜" + i);
                result.add(vo);
            }
        }
        return result;
    }

    private String getStr(Map<String, Object> m, String key, String def) {
        Object v = m.get(key);
        return v != null ? v.toString().trim() : def;
    }

    private Integer parseInteger(Object value) {
        if (value instanceof Number n) {
            return n.intValue();
        }
        if (value instanceof String s && StringUtils.hasText(s)) {
            try {
                return Integer.parseInt(s.trim().replaceAll("[^0-9-]", ""));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static boolean isDigitalHumanCommerce(String scriptContent, String style) {
        String text = ((scriptContent == null ? "" : scriptContent) + " " + (style == null ? "" : style)).toLowerCase();
        return containsAny(text,
                "digital_human", "数字人", "avatar_talking_head", "product_closeup", "usage_demo",
                "口播带货", "带货口播", "带货", "商品", "产品", "开箱", "测评", "种草", "软广", "product");
    }

    private static boolean containsAny(String text, String... words) {
        if (text == null) {
            return false;
        }
        for (String word : words) {
            if (word != null && text.contains(word.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private SvShotVO shotToVO(SvShot s) {
        SvShotVO vo = new SvShotVO();
        vo.setId(s.getId());
        vo.setShotListId(s.getShotListId());
        vo.setShotNumber(s.getShotNumber());
        vo.setReviewStatus(s.getReviewStatus());
        vo.setReviewerNote(s.getReviewerNote());
        vo.setTimeRange(s.getTimeRange());
        vo.setSceneDescription(s.getSceneDescription());
        vo.setCameraAngle(s.getCameraAngle());
        vo.setCameraType(s.getCameraType());
        vo.setAction(s.getAction());
        vo.setDialogue(s.getDialogue());
        vo.setMood(s.getMood());
        vo.setKeyframeUrl(s.getKeyframeUrl());
        vo.setKeyframeBosKey(s.getKeyframeBosKey());
        vo.setEndFrameUrl(s.getEndFrameUrl());
        vo.setEndFrameBosKey(s.getEndFrameBosKey());
        vo.setVideoUrl(s.getVideoUrl());
        vo.setVideoBosKey(s.getVideoBosKey());
        vo.setAudioUrl(s.getAudioUrl());
        vo.setAudioBosKey(s.getAudioBosKey());
        vo.setDuration(s.getDuration());
        vo.setCreateTime(s.getCreateTime());
        vo.setUpdateTime(s.getUpdateTime());
        return vo;
    }
}
