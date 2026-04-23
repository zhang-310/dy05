package cn.gaifan.douyinOperations.module.shortvideo.service;

import cn.gaifan.douyinOperations.module.shortvideo.entity.SvDrama;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvDramaCharacter;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvDramaEpisode;

import java.util.List;

/**
 * 短剧服务 (Phase 3)
 */
public interface DramaService {

    /** 创建短剧 */
    SvDrama createDrama(Long ownerId, String title, String description, String genre, int totalEpisodes);

    /** 获取短剧详情 (含剧集和角色列表)，需校验 ownerId */
    SvDrama getDrama(Long dramaId, Long ownerId);

    /** 获取用户的短剧列表 */
    List<SvDrama> listDramas(Long ownerId);

    /** 删除短剧（软删除，同时软删除其剧集和角色） */
    void deleteDrama(Long dramaId, Long ownerId);

    /** 更新短剧基本信息（标题、简介、类型、集数） */
    SvDrama updateDrama(Long dramaId, Long ownerId, String title, String description, String genre, Integer totalEpisodes);

    /** 添加剧集 */
    SvDramaEpisode addEpisode(Long dramaId, Long ownerId, int episodeNumber, String title, String synopsis, String cliffhanger);

    /** 获取剧集列表 */
    List<SvDramaEpisode> listEpisodes(Long dramaId, Long ownerId);

    /** 关联剧集到项目 */
    void linkEpisodeToProject(Long episodeId, Long projectId, Long ownerId);

    /** 更新剧集（标题、剧情概要/剧本内容、悬念钩子） */
    void updateEpisode(Long episodeId, Long ownerId, String title, String synopsis, String cliffhanger);

    /** 删除剧集（软删除，仅允许未关联项目的剧集） */
    void deleteEpisode(Long episodeId, Long ownerId);

    /**
     * 将 AI 生成的剧本解析并按集应用到剧集：按「第X集」切分，创建或更新对应剧集
     * @return 成功应用的集数
     */
    int applyScriptToEpisodes(Long dramaId, Long ownerId, String script);

    /** 添加角色 */
    SvDramaCharacter addCharacter(Long dramaId, Long ownerId, String name, String description,
                                  String referenceImageUrl, String voiceId);

    /** 获取角色列表 */
    List<SvDramaCharacter> listCharacters(Long dramaId, Long ownerId);

    /** 删除角色（软删除） */
    void deleteCharacter(Long characterId, Long ownerId);

    /** 更新角色（名称、描述） */
    SvDramaCharacter updateCharacter(Long characterId, Long ownerId, String name, String description);

    /**
     * AI 生成剧本 (多集) - 占位实现
     * 输入: 短剧类型、题材、集数
     * 输出: 每集的剧本、分镜、悬念钩子
     */
    String generateDramaScript(Long dramaId, Long ownerId, String theme, String style);

    /**
     * 从剧集创建项目：保存脚本 → AI 生成分镜 → 创建项目并关联剧集
     * @return projectId，可直接跳转分镜设计页
     */
    Long createProjectFromEpisode(Long episodeId, Long ownerId);

    /**
     * 根据项目 ID 查询关联的短剧信息（项目由短剧剧集创建时）
     * @return { dramaId, episodeId, dramaTitle, episodeNumber } 或 null
     */
    java.util.Map<String, Object> getDramaByProjectId(Long projectId, Long ownerId);
}
