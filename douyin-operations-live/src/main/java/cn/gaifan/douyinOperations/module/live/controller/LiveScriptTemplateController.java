package cn.gaifan.douyinOperations.module.live.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.live.entity.LiveScriptTemplate;
import cn.gaifan.douyinOperations.module.live.repository.LiveScriptTemplateRepository;
import cn.gaifan.douyinOperations.module.live.service.LiveScriptTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/live/template")
@Tag(name = "话术模板库", description = "模板浏览/搜索/应用/自动沉淀")
public class LiveScriptTemplateController {

    @Resource
    private LiveScriptTemplateRepository templateRepository;
    @Resource
    private LiveScriptTemplateService templateService;

    @PostMapping("/search")
    @Operation(summary = "分页搜索模板")
    public RESTResult<PageResultVO<LiveScriptTemplate>> search(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        Long userId = requireUserId(request);

        String scriptType = (String) body.get("scriptType");
        String category = (String) body.get("category");
        String keyword = (String) body.get("keyword");
        String industryCode = (String) body.get("industryCode");
        boolean presetOnly = Boolean.TRUE.equals(body.get("presetOnly"));
        int page = body.get("page") != null ? ((Number) body.get("page")).intValue() : 0;
        int rows = body.get("rows") != null ? Math.min(((Number) body.get("rows")).intValue(), 50) : 20;

        Specification<LiveScriptTemplate> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("deleted"), 0));
            predicates.add(cb.equal(root.get("status"), 1));
            // 可见范围：系统模板(ownerId IS NULL) + 自己的模板
            predicates.add(cb.or(
                    cb.isNull(root.get("ownerId")),
                    cb.equal(root.get("ownerId"), userId)
            ));
            if (scriptType != null && !scriptType.isBlank()) {
                predicates.add(cb.equal(root.get("scriptType"), scriptType));
            }
            if (category != null && !category.isBlank()) {
                predicates.add(cb.like(root.get("category"), "%" + category + "%"));
            }
            if (keyword != null && !keyword.isBlank()) {
                predicates.add(cb.or(
                        cb.like(root.get("templateName"), "%" + keyword + "%"),
                        cb.like(root.get("content"), "%" + keyword + "%")
                ));
            }
            // P3-04: 按行业编码筛选
            if (industryCode != null && !industryCode.isBlank()) {
                predicates.add(cb.or(
                        cb.equal(root.get("industryCode"), industryCode),
                        cb.isNull(root.get("industryCode"))
                ));
            }
            // 仅预置模板
            if (presetOnly) {
                predicates.add(cb.equal(root.get("isPreset"), 1));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<LiveScriptTemplate> result = templateRepository.findAll(spec,
                PageRequest.of(page, rows, Sort.by(Sort.Direction.DESC, "effectivenessScore")));

        PageResultVO<LiveScriptTemplate> vo = new PageResultVO<>();
        vo.setTotal(result.getTotalElements());
        vo.setList(result.getContent());
        vo.setPageNum(page);
        vo.setPageSize(rows);
        return RESTResult.success(vo);
    }

    @PostMapping("/save-from-script")
    @Operation(summary = "手动保存话术为模板")
    public RESTResult<LiveScriptTemplate> saveFromScript(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        requireUserId(request);
        Long scriptId = ((Number) body.get("scriptId")).longValue();
        String templateName = (String) body.get("templateName");
        String category = (String) body.get("category");
        return RESTResult.success(templateService.saveFromScript(scriptId, templateName, category));
    }

    @PostMapping("/apply")
    @Operation(summary = "应用模板到话术（返回模板内容）")
    public RESTResult<Map<String, Object>> apply(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        requireUserId(request);
        Long templateId = ((Number) body.get("templateId")).longValue();
        LiveScriptTemplate tpl = templateRepository.findById(templateId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEMPLATE_NOT_FOUND, "模板不存在"));
        // 增加使用次数
        tpl.setUsageCount((tpl.getUsageCount() != null ? tpl.getUsageCount() : 0) + 1);
        templateRepository.save(tpl);
        return RESTResult.success(Map.of(
                "content", tpl.getContent(),
                "scriptType", tpl.getScriptType(),
                "templateName", tpl.getTemplateName()
        ));
    }

    @PostMapping("/auto-collect")
    @Operation(summary = "手动触发自动沉淀高效话术（管理员）")
    public RESTResult<Integer> autoCollect(HttpServletRequest request) {
        Long userId = requireUserId(request);
        String roleCode = AuthTokenFilter.getRoleCode(request);
        if (!"admin".equalsIgnoreCase(roleCode)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅管理员可触发自动沉淀");
        }
        int count = templateService.importFromHighEffectivenessScripts();
        return RESTResult.success(count);
    }

    private Long requireUserId(HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        return userId;
    }
}
