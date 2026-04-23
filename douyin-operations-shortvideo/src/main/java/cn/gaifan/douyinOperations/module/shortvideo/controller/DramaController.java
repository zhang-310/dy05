package cn.gaifan.douyinOperations.module.shortvideo.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvDrama;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvDramaCharacter;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvDramaEpisode;
import cn.gaifan.douyinOperations.module.shortvideo.service.DramaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 短剧 API (Phase 3)
 * 路径：/api/v1/short-video/drama
 */
@RestController
@RequestMapping("/api/v1/short-video/drama")
@Tag(name = "短剧", description = "短剧/剧集/角色 CRUD")
public class DramaController {

    @Resource
    private DramaService dramaService;

    @PostMapping("/list")
    @Operation(summary = "短剧列表")
    public RESTResult<List<SvDrama>> list(HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        List<SvDrama> list = dramaService.listDramas(userId);
        RESTResult<List<SvDrama>> r = RESTResult.getSuccess(list);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/get")
    @Operation(summary = "短剧详情")
    public RESTResult<SvDrama> get(@RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long id = body != null && body.get("id") instanceof Number n ? n.longValue() : null;
        if (id == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 id");
        SvDrama drama = dramaService.getDrama(id, userId);
        RESTResult<SvDrama> r = RESTResult.getSuccess(drama);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/delete")
    @Operation(summary = "删除短剧（软删除）")
    public RESTResult<Void> delete(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long id = body != null && body.get("id") instanceof Number n ? n.longValue() : null;
        if (id == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 id");
        dramaService.deleteDrama(id, userId);
        RESTResult<Void> r = RESTResult.deleteSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/update")
    @Operation(summary = "更新短剧基本信息")
    public RESTResult<SvDrama> update(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long id = body != null && body.get("id") instanceof Number n ? n.longValue() : null;
        if (id == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 id");
        String title = body.get("title") instanceof String s ? s : null;
        String description = body.get("description") instanceof String s ? s : null;
        String genre = body.get("genre") instanceof String s ? s : null;
        Integer totalEpisodes = body.get("totalEpisodes") instanceof Number n ? n.intValue() : null;
        SvDrama drama = dramaService.updateDrama(id, userId, title, description, genre, totalEpisodes);
        RESTResult<SvDrama> r = RESTResult.updateSuccess(drama);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/create")
    @Operation(summary = "创建短剧")
    public RESTResult<SvDrama> create(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        String title = body.get("title") instanceof String s ? s : null;
        String description = body.get("description") instanceof String s ? s : null;
        String genre = body.get("genre") instanceof String s ? s : null;
        int totalEpisodes = body.get("totalEpisodes") instanceof Number n ? n.intValue() : 1;
        SvDrama drama = dramaService.createDrama(userId, title, description, genre, totalEpisodes);
        RESTResult<SvDrama> r = RESTResult.addSuccess(drama);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/episodes")
    @Operation(summary = "剧集列表")
    public RESTResult<List<SvDramaEpisode>> episodes(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long dramaId = body.get("dramaId") instanceof Number n ? n.longValue() : null;
        if (dramaId == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 dramaId");
        List<SvDramaEpisode> list = dramaService.listEpisodes(dramaId, userId);
        RESTResult<List<SvDramaEpisode>> r = RESTResult.getSuccess(list);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/add-episode")
    @Operation(summary = "添加剧集")
    public RESTResult<SvDramaEpisode> addEpisode(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long dramaId = body.get("dramaId") instanceof Number n ? n.longValue() : null;
        if (dramaId == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 dramaId");
        int episodeNumber = body.get("episodeNumber") instanceof Number n ? n.intValue() : 1;
        String title = body.get("title") instanceof String s ? s : null;
        String synopsis = body.get("synopsis") instanceof String s ? s : null;
        String cliffhanger = body.get("cliffhanger") instanceof String s ? s : null;
        SvDramaEpisode episode = dramaService.addEpisode(dramaId, userId, episodeNumber, title, synopsis, cliffhanger);
        RESTResult<SvDramaEpisode> r = RESTResult.addSuccess(episode);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/link-episode")
    @Operation(summary = "关联剧集到项目")
    public RESTResult<Void> linkEpisode(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long episodeId = body.get("episodeId") instanceof Number n ? n.longValue() : null;
        Long projectId = body.get("projectId") instanceof Number n ? n.longValue() : null;
        if (episodeId == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 episodeId");
        dramaService.linkEpisodeToProject(episodeId, projectId, userId);
        RESTResult<Void> r = RESTResult.updateSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/delete-episode")
    @Operation(summary = "删除剧集（软删除，仅未关联项目）")
    public RESTResult<Void> deleteEpisode(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long episodeId = body != null && body.get("episodeId") instanceof Number n ? n.longValue() : null;
        if (episodeId == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 episodeId");
        dramaService.deleteEpisode(episodeId, userId);
        RESTResult<Void> r = RESTResult.deleteSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/update-episode")
    @Operation(summary = "更新剧集")
    public RESTResult<Void> updateEpisode(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long episodeId = body.get("episodeId") instanceof Number n ? n.longValue() : null;
        if (episodeId == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 episodeId");
        String title = body.get("title") instanceof String s ? s : null;
        String synopsis = body.get("synopsis") instanceof String s ? s : null;
        String cliffhanger = body.get("cliffhanger") instanceof String s ? s : null;
        dramaService.updateEpisode(episodeId, userId, title, synopsis, cliffhanger);
        RESTResult<Void> r = RESTResult.updateSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/apply-script-to-episodes")
    @Operation(summary = "将剧本解析并按集应用到剧集")
    public RESTResult<Integer> applyScriptToEpisodes(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long dramaId = body.get("dramaId") instanceof Number n ? n.longValue() : null;
        if (dramaId == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 dramaId");
        String script = body.get("script") instanceof String s ? s : null;
        int applied = dramaService.applyScriptToEpisodes(dramaId, userId, script);
        RESTResult<Integer> r = RESTResult.getSuccess(applied);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/characters")
    @Operation(summary = "角色列表")
    public RESTResult<List<SvDramaCharacter>> characters(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long dramaId = body.get("dramaId") instanceof Number n ? n.longValue() : null;
        if (dramaId == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 dramaId");
        List<SvDramaCharacter> list = dramaService.listCharacters(dramaId, userId);
        RESTResult<List<SvDramaCharacter>> r = RESTResult.getSuccess(list);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/update-character")
    @Operation(summary = "更新角色")
    public RESTResult<SvDramaCharacter> updateCharacter(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long id = body != null && body.get("id") instanceof Number n ? n.longValue() : null;
        if (id == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 id");
        String name = body.get("name") instanceof String s ? s : null;
        String description = body.get("description") instanceof String s ? s : null;
        SvDramaCharacter character = dramaService.updateCharacter(id, userId, name, description);
        RESTResult<SvDramaCharacter> r = RESTResult.updateSuccess(character);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/delete-character")
    @Operation(summary = "删除角色（软删除）")
    public RESTResult<Void> deleteCharacter(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long id = body != null && body.get("id") instanceof Number n ? n.longValue() : null;
        if (id == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 id");
        dramaService.deleteCharacter(id, userId);
        RESTResult<Void> r = RESTResult.deleteSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/add-character")
    @Operation(summary = "添加角色")
    public RESTResult<SvDramaCharacter> addCharacter(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long dramaId = body.get("dramaId") instanceof Number n ? n.longValue() : null;
        if (dramaId == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 dramaId");
        String name = body.get("name") instanceof String s ? s : null;
        String description = body.get("description") instanceof String s ? s : null;
        String referenceImageUrl = body.get("referenceImageUrl") instanceof String s ? s : null;
        String voiceId = body.get("voiceId") instanceof String s ? s : null;
        SvDramaCharacter character = dramaService.addCharacter(dramaId, userId, name, description, referenceImageUrl, voiceId);
        RESTResult<SvDramaCharacter> r = RESTResult.addSuccess(character);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/create-project-from-episode")
    @Operation(summary = "从剧集创建项目（脚本+分镜+关联）")
    public RESTResult<Long> createProjectFromEpisode(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long episodeId = body.get("episodeId") instanceof Number n ? n.longValue() : null;
        if (episodeId == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 episodeId");
        Long projectId = dramaService.createProjectFromEpisode(episodeId, userId);
        RESTResult<Long> r = RESTResult.getSuccess(projectId);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/by-project")
    @Operation(summary = "根据项目ID查询关联短剧（项目由短剧剧集创建时）")
    public RESTResult<Map<String, Object>> getByProject(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long projectId = body != null && body.get("projectId") instanceof Number n ? n.longValue() : null;
        if (projectId == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 projectId");
        Map<String, Object> info = dramaService.getDramaByProjectId(projectId, userId);
        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(info);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/generate-script")
    @Operation(summary = "AI 生成剧本 (占位)")
    public RESTResult<String> generateScript(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long dramaId = body.get("dramaId") instanceof Number n ? n.longValue() : null;
        if (dramaId == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 dramaId");
        String theme = body.get("theme") instanceof String s ? s : null;
        String style = body.get("style") instanceof String s ? s : null;
        String script = dramaService.generateDramaScript(dramaId, userId, theme, style);
        RESTResult<String> r = RESTResult.getSuccess(script);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
