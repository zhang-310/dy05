package cn.gaifan.douyinOperations.module.ai.controller;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.ai.entity.AiKnowledgeSource;
import cn.gaifan.douyinOperations.module.ai.service.KnowledgeSourceService;
import cn.gaifan.douyinOperations.module.ai.vo.KnowledgeSourceSearchVO;
import cn.gaifan.douyinOperations.module.ai.vo.KnowledgeSourceSaveVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 知识源管理 API（管理员）
 */
@Tag(name = "AI 知识源管理")
@RestController
@RequestMapping("/api/v1/ai/admin/knowledge-source")
public class KnowledgeSourceController {

    @Resource
    private KnowledgeSourceService knowledgeSourceService;

    @Operation(summary = "知识源分页查询")
    @PostMapping("/search")
    public RESTResult<PageResultVO<AiKnowledgeSource>> search(
            @RequestBody KnowledgeSourceSearchVO vo,
            HttpServletRequest request
    ) {
        requireAdmin(request);
        return RESTResult.getSuccess(knowledgeSourceService.search(vo));
    }

    @Operation(summary = "知识源详情")
    @PostMapping("/get")
    public RESTResult<AiKnowledgeSource> get(@RequestParam Long id, HttpServletRequest request) {
        requireAdmin(request);
        return RESTResult.getSuccess(knowledgeSourceService.getById(id));
    }

    @Operation(summary = "新增/更新知识源")
    @PostMapping("/save")
    public RESTResult<Long> save(@Valid @RequestBody KnowledgeSourceSaveVO vo, HttpServletRequest request) {
        requireAdmin(request);
        return RESTResult.addSuccess(knowledgeSourceService.save(vo));
    }

    @Operation(summary = "删除知识源")
    @PostMapping("/delete")
    public RESTResult<Void> delete(@RequestParam Long id, HttpServletRequest request) {
        requireAdmin(request);
        knowledgeSourceService.delete(id);
        return RESTResult.deleteSuccess(null);
    }

    private void requireAdmin(HttpServletRequest request) {
        String roleCode = (String) request.getAttribute("roleCode");
        if (roleCode == null || !"admin".equals(roleCode)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅管理员可访问");
        }
    }
}
