package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.entity.AiTaskModelConfig;
import cn.gaifan.douyinOperations.module.ai.repository.AiModelRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiTaskModelConfigRepository;
import cn.gaifan.douyinOperations.module.ai.service.LlmClient;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvDrama;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvDramaCharacter;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvDramaEpisode;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvDramaCharacterRepository;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvDramaEpisodeRepository;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvDramaRepository;
import cn.gaifan.douyinOperations.module.shortvideo.service.DramaService;
import cn.gaifan.douyinOperations.module.shortvideo.service.SvProjectService;
import cn.gaifan.douyinOperations.module.shortvideo.service.SvScriptService;
import cn.gaifan.douyinOperations.module.shortvideo.service.SvShotListService;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvProjectSaveVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvScriptSaveVO;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 短剧服务实现 (Phase 3)
 */
@Service
public class DramaServiceImpl implements DramaService {

    private static final Logger log = LoggerFactory.getLogger(DramaServiceImpl.class);

    @Resource
    private SvDramaRepository dramaRepository;
    @Resource
    private SvDramaEpisodeRepository episodeRepository;
    @Resource
    private SvDramaCharacterRepository characterRepository;
    @Autowired(required = false)
    private LlmClient llmClient;
    @Autowired(required = false)
    private AiTaskModelConfigRepository taskModelConfigRepository;
    @Autowired(required = false)
    private AiModelRepository modelRepository;
    @Resource
    private SvScriptService scriptService;
    @Resource
    private SvShotListService shotListService;
    @Resource
    private SvProjectService projectService;

    @Override
    public SvDrama createDrama(Long ownerId, String title, String description, String genre, int totalEpisodes) {
        if (ownerId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        if (!StringUtils.hasText(title)) throw new BusinessException(ErrorCode.VALIDATION_FAIL, "标题不能为空");
        SvDrama drama = new SvDrama();
        drama.setOwnerId(ownerId);
        drama.setTitle(title.trim());
        drama.setDescription(StringUtils.hasText(description) ? description.trim() : null);
        drama.setGenre(StringUtils.hasText(genre) ? genre.trim() : null);
        drama.setTotalEpisodes(totalEpisodes > 0 ? totalEpisodes : 1);
        drama.setStatus("draft");
        drama = dramaRepository.save(drama);
        int n = drama.getTotalEpisodes() != null && drama.getTotalEpisodes() > 0 ? drama.getTotalEpisodes() : 1;
        for (int i = 1; i <= n; i++) {
            addEpisode(drama.getId(), ownerId, i, "第" + i + "集", null, null);
        }
        return drama;
    }

    @Override
    public SvDrama getDrama(Long dramaId, Long ownerId) {
        if (ownerId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        if (dramaId == null) throw new BusinessException(ErrorCode.VALIDATION_FAIL, "短剧ID不能为空");
        SvDrama drama = dramaRepository.findById(dramaId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "短剧不存在"));
        if (!drama.getOwnerId().equals(ownerId)) throw new BusinessException(ErrorCode.FORBIDDEN, "无权限访问");
        return drama;
    }

    @Override
    public List<SvDrama> listDramas(Long ownerId) {
        if (ownerId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        List<SvDrama> list = dramaRepository.findByOwnerIdOrderByUpdateTimeDesc(ownerId);
        for (SvDrama d : list) {
            List<SvDramaEpisode> eps = episodeRepository.findByDramaIdOrderByEpisodeNumberAsc(d.getId());
            int withSynopsis = (int) eps.stream().filter(e -> StringUtils.hasText(e.getSynopsis())).count();
            int withProject = (int) eps.stream().filter(e -> e.getProjectId() != null).count();
            d.setEpisodesWithSynopsis(withSynopsis);
            d.setEpisodesWithProject(withProject);
        }
        return list;
    }

    @Override
    public void deleteDrama(Long dramaId, Long ownerId) {
        if (ownerId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        if (dramaId == null) throw new BusinessException(ErrorCode.VALIDATION_FAIL, "短剧ID不能为空");
        SvDrama drama = getDrama(dramaId, ownerId);
        List<SvDramaEpisode> episodes = episodeRepository.findByDramaIdOrderByEpisodeNumberAsc(dramaId);
        List<SvDramaCharacter> characters = characterRepository.findByDramaIdOrderByIdAsc(dramaId);
        for (SvDramaEpisode ep : episodes) {
            ep.setDeleted(1);
            episodeRepository.save(ep);
        }
        for (SvDramaCharacter c : characters) {
            c.setDeleted(1);
            characterRepository.save(c);
        }
        drama.setDeleted(1);
        dramaRepository.save(drama);
    }

    @Override
    public SvDrama updateDrama(Long dramaId, Long ownerId, String title, String description, String genre, Integer totalEpisodes) {
        if (ownerId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        if (dramaId == null) throw new BusinessException(ErrorCode.VALIDATION_FAIL, "短剧ID不能为空");
        SvDrama drama = getDrama(dramaId, ownerId);
        if (StringUtils.hasText(title)) drama.setTitle(title.trim());
        if (description != null) drama.setDescription(description.trim().isEmpty() ? null : description.trim());
        if (genre != null) drama.setGenre(genre.trim().isEmpty() ? null : genre.trim());
        if (totalEpisodes != null && totalEpisodes > 0) drama.setTotalEpisodes(totalEpisodes);
        return dramaRepository.save(drama);
    }

    @Override
    public SvDramaEpisode addEpisode(Long dramaId, Long ownerId, int episodeNumber, String title, String synopsis, String cliffhanger) {
        if (ownerId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        if (dramaId == null) throw new BusinessException(ErrorCode.VALIDATION_FAIL, "短剧ID不能为空");
        getDrama(dramaId, ownerId);
        SvDramaEpisode episode = new SvDramaEpisode();
        episode.setDramaId(dramaId);
        episode.setEpisodeNumber(episodeNumber);
        episode.setTitle(StringUtils.hasText(title) ? title.trim() : null);
        episode.setSynopsis(StringUtils.hasText(synopsis) ? synopsis.trim() : null);
        episode.setCliffhanger(StringUtils.hasText(cliffhanger) ? cliffhanger.trim() : null);
        episode.setStatus("draft");
        return episodeRepository.save(episode);
    }

    @Override
    public List<SvDramaEpisode> listEpisodes(Long dramaId, Long ownerId) {
        if (ownerId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        if (dramaId == null) throw new BusinessException(ErrorCode.VALIDATION_FAIL, "短剧ID不能为空");
        getDrama(dramaId, ownerId);
        return episodeRepository.findByDramaIdOrderByEpisodeNumberAsc(dramaId);
    }

    @Override
    public void linkEpisodeToProject(Long episodeId, Long projectId, Long ownerId) {
        if (ownerId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        if (episodeId == null) throw new BusinessException(ErrorCode.VALIDATION_FAIL, "剧集ID不能为空");
        SvDramaEpisode episode = episodeRepository.findById(episodeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "剧集不存在"));
        getDrama(episode.getDramaId(), ownerId);
        episode.setProjectId(projectId);
        episodeRepository.save(episode);
    }

    @Override
    public void updateEpisode(Long episodeId, Long ownerId, String title, String synopsis, String cliffhanger) {
        if (ownerId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        if (episodeId == null) throw new BusinessException(ErrorCode.VALIDATION_FAIL, "剧集ID不能为空");
        SvDramaEpisode episode = episodeRepository.findById(episodeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "剧集不存在"));
        getDrama(episode.getDramaId(), ownerId);
        if (title != null) episode.setTitle(title.trim().isEmpty() ? null : title.trim());
        if (synopsis != null) episode.setSynopsis(synopsis.trim().isEmpty() ? null : synopsis.trim());
        if (cliffhanger != null) episode.setCliffhanger(cliffhanger.trim().isEmpty() ? null : cliffhanger.trim());
        episodeRepository.save(episode);
    }

    @Override
    public void deleteEpisode(Long episodeId, Long ownerId) {
        if (ownerId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        if (episodeId == null) throw new BusinessException(ErrorCode.VALIDATION_FAIL, "剧集ID不能为空");
        SvDramaEpisode episode = episodeRepository.findById(episodeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "剧集不存在"));
        getDrama(episode.getDramaId(), ownerId);
        if (episode.getProjectId() != null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "已关联项目的剧集不能删除");
        }
        episode.setDeleted(1);
        episodeRepository.save(episode);
    }

    @Override
    public int applyScriptToEpisodes(Long dramaId, Long ownerId, String script) {
        if (ownerId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        if (dramaId == null) throw new BusinessException(ErrorCode.VALIDATION_FAIL, "短剧ID不能为空");
        if (!StringUtils.hasText(script)) throw new BusinessException(ErrorCode.VALIDATION_FAIL, "剧本内容不能为空");
        getDrama(dramaId, ownerId);
        List<SvDramaEpisode> existing = episodeRepository.findByDramaIdOrderByEpisodeNumberAsc(dramaId);
        Map<Integer, SvDramaEpisode> byNumber = existing.stream()
                .collect(Collectors.toMap(SvDramaEpisode::getEpisodeNumber, e -> e, (a, b) -> a));

        Pattern p = Pattern.compile("第\\s*(\\d+)\\s*集");
        Matcher m = p.matcher(script);
        List<int[]> matches = new ArrayList<>();
        while (m.find()) {
            matches.add(new int[]{Integer.parseInt(m.group(1)), m.start(), m.end()});
        }
        int applied = 0;
        if (matches.isEmpty()) {
            String content = script.trim();
            if (!content.isEmpty()) {
                int epNum = 1;
                SvDramaEpisode ep = byNumber.get(epNum);
                if (ep != null) {
                    ep.setSynopsis(content);
                    episodeRepository.save(ep);
                } else {
                    addEpisode(dramaId, ownerId, epNum, "第1集", content, null);
                }
                applied = 1;
            }
            return applied;
        }
        for (int i = 0; i < matches.size(); i++) {
            int epNum = matches.get(i)[0];
            int contentStart = matches.get(i)[2];
            int contentEnd = i + 1 < matches.size() ? matches.get(i + 1)[1] : script.length();
            String content = script.substring(contentStart, contentEnd).trim();
            if (content.isEmpty()) continue;

            SvDramaEpisode ep = byNumber.get(epNum);
            if (ep != null) {
                ep.setSynopsis(content);
                episodeRepository.save(ep);
            } else {
                SvDramaEpisode newEp = addEpisode(dramaId, ownerId, epNum, "第" + epNum + "集", content, null);
                byNumber.put(epNum, newEp);
            }
            applied++;
        }
        return applied;
    }

    @Override
    public SvDramaCharacter addCharacter(Long dramaId, Long ownerId, String name, String description,
                                          String referenceImageUrl, String voiceId) {
        if (ownerId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        if (dramaId == null) throw new BusinessException(ErrorCode.VALIDATION_FAIL, "短剧ID不能为空");
        if (!StringUtils.hasText(name)) throw new BusinessException(ErrorCode.VALIDATION_FAIL, "角色名不能为空");
        getDrama(dramaId, ownerId);
        SvDramaCharacter character = new SvDramaCharacter();
        character.setDramaId(dramaId);
        character.setCharacterName(name.trim());
        character.setDescription(StringUtils.hasText(description) ? description.trim() : null);
        character.setReferenceImageUrl(StringUtils.hasText(referenceImageUrl) ? referenceImageUrl.trim() : null);
        character.setVoiceId(StringUtils.hasText(voiceId) ? voiceId.trim() : null);
        return characterRepository.save(character);
    }

    @Override
    public List<SvDramaCharacter> listCharacters(Long dramaId, Long ownerId) {
        if (ownerId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        if (dramaId == null) throw new BusinessException(ErrorCode.VALIDATION_FAIL, "短剧ID不能为空");
        getDrama(dramaId, ownerId);
        return characterRepository.findByDramaIdOrderByIdAsc(dramaId);
    }

    @Override
    public void deleteCharacter(Long characterId, Long ownerId) {
        if (ownerId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        if (characterId == null) throw new BusinessException(ErrorCode.VALIDATION_FAIL, "角色ID不能为空");
        SvDramaCharacter character = characterRepository.findById(characterId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "角色不存在"));
        getDrama(character.getDramaId(), ownerId);
        character.setDeleted(1);
        characterRepository.save(character);
    }

    @Override
    public SvDramaCharacter updateCharacter(Long characterId, Long ownerId, String name, String description) {
        if (ownerId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        if (characterId == null) throw new BusinessException(ErrorCode.VALIDATION_FAIL, "角色ID不能为空");
        SvDramaCharacter character = characterRepository.findById(characterId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "角色不存在"));
        getDrama(character.getDramaId(), ownerId);
        if (StringUtils.hasText(name)) character.setCharacterName(name.trim());
        if (description != null) character.setDescription(description.trim().isEmpty() ? null : description.trim());
        return characterRepository.save(character);
    }

    @Override
    public String generateDramaScript(Long dramaId, Long ownerId, String theme, String style) {
        if (ownerId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        if (dramaId == null) throw new BusinessException(ErrorCode.VALIDATION_FAIL, "短剧ID不能为空");
        SvDrama drama = getDrama(dramaId, ownerId);
        List<SvDramaCharacter> characters = listCharacters(dramaId, ownerId);
        List<SvDramaEpisode> episodes = listEpisodes(dramaId, ownerId);

        if (llmClient != null) {
            try {
                List<AiModel> models = resolveDramaModels();
                if (!models.isEmpty()) {
                    String charDesc = characters.stream()
                            .map(c -> c.getCharacterName() + (StringUtils.hasText(c.getDescription()) ? "：" + c.getDescription() : ""))
                            .reduce("", (a, b) -> a + "\n- " + b);
                    if (!charDesc.isEmpty()) charDesc = "\n角色设定：\n" + charDesc;

                    String episodeContext = episodes.stream()
                            .map(e -> "第" + e.getEpisodeNumber() + "集：" + (e.getTitle() != null ? e.getTitle() : "") + (StringUtils.hasText(e.getSynopsis()) ? " " + e.getSynopsis() : ""))
                            .reduce("", (a, b) -> a + "\n" + b);
                    if (!episodeContext.isEmpty()) episodeContext = "\n已有剧集概要：\n" + episodeContext;

                    String system = """
                            你是专业的短剧编剧。根据用户提供的短剧信息，生成符合抖音短剧风格的剧本。
                            输出格式：每集包含【场景】【人物】【对白】【动作提示】【悬念钩子】。
                            每集时长约60-90秒，对白简洁有力，节奏紧凑，适合竖屏短视频。
                            只输出剧本正文，不要额外说明。
                            """;
                    String prompt = String.format("短剧《%s》%d集%s%s%s%s。请生成完整剧本（可指定集数或全部）：",
                            drama.getTitle(),
                            drama.getTotalEpisodes() != null ? drama.getTotalEpisodes() : 1,
                            StringUtils.hasText(drama.getGenre()) ? "，类型：" + drama.getGenre() : "",
                            StringUtils.hasText(drama.getDescription()) ? "，简介：" + drama.getDescription() : "",
                            StringUtils.hasText(theme) ? "，题材：" + theme : "",
                            StringUtils.hasText(style) ? "，风格：" + style : "")
                            + charDesc + episodeContext;

                    var response = llmClient.chatWithFallback(models, system, prompt);
                    if (response.success() && StringUtils.hasText(response.content())) {
                        return response.content().trim();
                    }
                }
            } catch (Exception e) {
                log.warn("Drama generate-script LLM 调用失败: {}", e.getMessage());
            }
        }

        return "【AI 剧本生成】短剧《" + drama.getTitle() + "》" + (drama.getTotalEpisodes() != null ? drama.getTotalEpisodes() : 1) + "集"
                + (StringUtils.hasText(theme) ? "，题材：" + theme : "")
                + (StringUtils.hasText(style) ? "，风格：" + style : "")
                + "。请配置 LLM 以启用完整剧本生成。";
    }

    @Override
    public Long createProjectFromEpisode(Long episodeId, Long ownerId) {
        if (ownerId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        if (episodeId == null) throw new BusinessException(ErrorCode.VALIDATION_FAIL, "剧集ID不能为空");
        SvDramaEpisode episode = episodeRepository.findById(episodeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "剧集不存在"));
        SvDrama drama = getDrama(episode.getDramaId(), ownerId);
        if (!StringUtils.hasText(episode.getSynopsis())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "剧集暂无剧情，请先生成剧本并应用到剧集");
        }
        if (episode.getProjectId() != null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "该剧集已关联项目");
        }

        String title = drama.getTitle() + " 第" + episode.getEpisodeNumber() + "集";
        SvScriptSaveVO scriptVo = new SvScriptSaveVO();
        scriptVo.setTitle(title);
        scriptVo.setContent(episode.getSynopsis());
        scriptVo.setScriptType("daily");
        Long scriptId = scriptService.save(scriptVo, ownerId);

        var genResult = shotListService.generateWithResult(scriptId, episode.getSynopsis(), 6, "短剧", ownerId);
        Long shotListId = genResult.shotListId();

        SvProjectSaveVO projectVo = new SvProjectSaveVO();
        projectVo.setTitle(title);
        projectVo.setProjectType("shortvideo");
        projectVo.setStatus("draft");
        projectVo.setScriptId(scriptId);
        projectVo.setShotListId(shotListId);
        Long projectId = projectService.save(projectVo, ownerId);

        episode.setProjectId(projectId);
        episodeRepository.save(episode);

        return projectId;
    }

    @Override
    public Map<String, Object> getDramaByProjectId(Long projectId, Long ownerId) {
        if (ownerId == null || projectId == null) return null;
        SvDramaEpisode episode = episodeRepository.findByProjectId(projectId).orElse(null);
        if (episode == null) return null;
        SvDrama drama = dramaRepository.findById(episode.getDramaId()).orElse(null);
        if (drama == null || drama.getDeleted() != null && drama.getDeleted() != 0) return null;
        if (!drama.getOwnerId().equals(ownerId)) return null;
        Map<String, Object> result = new HashMap<>();
        result.put("dramaId", drama.getId());
        result.put("episodeId", episode.getId());
        result.put("dramaTitle", drama.getTitle());
        result.put("episodeNumber", episode.getEpisodeNumber());
        return result;
    }

    private List<AiModel> resolveDramaModels() {
        var config = taskModelConfigRepository != null
                ? taskModelConfigRepository.findByTaskCodeAndStatusAndDeleted("short_video_script", 1, 0)
                : java.util.Optional.<AiTaskModelConfig>empty();
        if (config.isPresent() && modelRepository != null) {
            AiTaskModelConfig tc = config.get();
            List<AiModel> result = new ArrayList<>();
            for (Long modelId : Arrays.asList(tc.getPrimaryModelId(), tc.getFallbackModelId(), tc.getFallback2ModelId())) {
                if (modelId == null) continue;
                modelRepository.findById(modelId).filter(m -> m.getStatus() == 1 && m.getDeleted() == 0)
                        .ifPresent(result::add);
            }
            if (!result.isEmpty()) return result;
        }
        if (modelRepository != null) {
            return modelRepository.findByStatusAndDeleted(1, 0).stream().limit(3).toList();
        }
        return List.of();
    }
}
