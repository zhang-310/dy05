package cn.gaifan.douyinOperations.module.ai.controller;

import cn.gaifan.douyinOperations.common.annotation.CurrentUserId;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.ai.entity.AiPromptTemplate;
import cn.gaifan.douyinOperations.module.ai.service.PromptTemplateService;
import cn.gaifan.douyinOperations.module.ai.vo.AiPromptTemplateSaveVO;
import cn.gaifan.douyinOperations.module.ai.vo.AiPromptTemplateSearchVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

@Tag(name = "AI 提示词模板")
@RestController
@RequestMapping("/api/v1/ai/prompt-template")
public class PromptTemplateController {

    @Resource
    private PromptTemplateService promptTemplateService;

    @Operation(summary = "分页查询提示词模板")
    @PostMapping("/list")
    public RESTResult<PageResultVO<AiPromptTemplate>> list(
            @RequestBody(required = false) AiPromptTemplateSearchVO searchVO,
            @CurrentUserId Long userId) {
        if (searchVO == null) searchVO = new AiPromptTemplateSearchVO();
        // 非管理员只能看自己的 + 系统模板，此处暂不限制（模板可共享查看）
        PageResultVO<AiPromptTemplate> result = promptTemplateService.search(searchVO);
        return RESTResult.getSuccess(result);
    }

    @Operation(summary = "获取模板详情")
    @PostMapping("/get")
    public RESTResult<AiPromptTemplate> get(
            @RequestBody Map<String, Object> body,
            @CurrentUserId Long userId) {
        Long id = ((Number) body.get("id")).longValue();
        AiPromptTemplate template = promptTemplateService.getById(id);
        return RESTResult.getSuccess(template);
    }

    @Operation(summary = "保存模板（新增或更新）")
    @PostMapping("/save")
    public RESTResult<AiPromptTemplate> save(
            @Valid @RequestBody AiPromptTemplateSaveVO saveVO,
            @CurrentUserId Long userId) {
        if (saveVO.getOwnerId() == null) saveVO.setOwnerId(userId);
        if (saveVO.getUserId() == null) saveVO.setUserId(userId);
        AiPromptTemplate saved = promptTemplateService.save(saveVO);
        return saved.getId() != null && saveVO.getId() != null
                ? RESTResult.updateSuccess(saved)
                : RESTResult.addSuccess(saved);
    }

    @Operation(summary = "删除模板")
    @PostMapping("/delete")
    public RESTResult<?> delete(
            @RequestBody Map<String, Object> body,
            @CurrentUserId Long userId) {
        Long id = ((Number) body.get("id")).longValue();
        // 归属校验：只能删除自己创建的模板
        AiPromptTemplate existing = promptTemplateService.getById(id);
        if (existing.getOwnerId() != null && existing.getOwnerId() != 0L
                && !existing.getOwnerId().equals(userId)) {
            return RESTResult.error(2002, "无权删除此模板");
        }
        promptTemplateService.delete(id);
        return RESTResult.deleteSuccess(1);
    }

    @Operation(summary = "获取当前激活模板（按回退链查找）")
    @PostMapping("/get-active")
    public RESTResult<AiPromptTemplate> getActive(
            @RequestBody Map<String, Object> body,
            @CurrentUserId Long userId) {
        String templateCode = (String) body.get("templateCode");
        String variantName = (String) body.getOrDefault("variantName", "default");
        AiPromptTemplate template = promptTemplateService.getActiveTemplate(templateCode, variantName, userId);
        return RESTResult.getSuccess(template);
    }

    @Operation(summary = "测试渲染模板（将变量填入模板返回预览文本）")
    @PostMapping("/test-render")
    public RESTResult<Map<String, Object>> testRender(
            @RequestBody Map<String, Object> body,
            @CurrentUserId Long userId) {
        String templateContent = (String) body.get("templateContent");
        @SuppressWarnings("unchecked")
        Map<String, String> variables = (Map<String, String>) body.getOrDefault("variables", Map.of());

        if (templateContent == null || templateContent.isEmpty()) {
            return RESTResult.error(1005, "模板内容不能为空");
        }

        // 提取模板中的变量
        Set<String> extractedVars = extractVariables(templateContent);

        // 简单变量替换
        String rendered = renderTemplate(templateContent, variables);

        return RESTResult.getSuccess(Map.of(
                "rendered", rendered != null ? rendered : "",
                "variables", extractedVars,
                "missingVariables", extractedVars.stream()
                        .filter(v -> !variables.containsKey(v))
                        .toList()
        ));
    }

    @Operation(summary = "提取模板变量")
    @PostMapping("/extract-variables")
    public RESTResult<Set<String>> extractVariables(@RequestBody Map<String, Object> body) {
        String templateContent = (String) body.get("templateContent");
        if (templateContent == null || templateContent.isEmpty()) {
            return RESTResult.getSuccess(Set.of());
        }
        Set<String> variables = extractVariables(templateContent);
        return RESTResult.getSuccess(variables);
    }

    @Operation(summary = "记录模板使用")
    @PostMapping("/record-usage")
    public RESTResult<Void> recordUsage(
            @RequestBody Map<String, Object> body,
            @CurrentUserId Long userId) {
        Long id = ((Number) body.get("id")).longValue();
        promptTemplateService.incrementUsageCount(id);
        return RESTResult.getSuccess(null);
    }

    /**
     * 提取模板中的变量（{{variable}} 格式）
     */
    private Set<String> extractVariables(String template) {
        if (template == null || template.isEmpty()) return Set.of();
        Pattern pattern = Pattern.compile("\\{\\{([^}]+)\\}\\}");
        Matcher matcher = pattern.matcher(template);
        Set<String> variables = new HashSet<>();
        while (matcher.find()) {
            variables.add(matcher.group(1).trim());
        }
        return variables;
    }

    /**
     * 简单模板渲染：将 {{key}} 替换为变量值
     */
    private String renderTemplate(String template, Map<String, String> variables) {
        if (template == null || template.isEmpty()) return template;
        if (variables == null || variables.isEmpty()) return template;
        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue() != null ? entry.getValue() : "");
        }
        return result;
    }
}
