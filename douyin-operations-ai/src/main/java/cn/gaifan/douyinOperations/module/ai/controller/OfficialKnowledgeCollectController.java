package cn.gaifan.douyinOperations.module.ai.controller;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.ai.entity.AiOfficialKnowledgeCollectItem;
import cn.gaifan.douyinOperations.module.ai.service.OfficialKnowledgeCollectItemService;
import cn.gaifan.douyinOperations.module.ai.vo.OfficialKnowledgeCollectSearchVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "AI 官方知识采集闭环")
@RestController
@RequestMapping("/api/v1/ai/admin/official-knowledge-collect")
public class OfficialKnowledgeCollectController {

    @Resource
    private OfficialKnowledgeCollectItemService service;

    @GetMapping("/summary")
    @Operation(summary = "官方知识采集闭环总览")
    public RESTResult<Map<String, Object>> summary(HttpServletRequest request) {
        requireAdmin(request);
        return RESTResult.getSuccess(service.summary());
    }

    @PostMapping("/search")
    @Operation(summary = "官方知识采集闭环明细查询")
    public RESTResult<PageResultVO<AiOfficialKnowledgeCollectItem>> search(
            @RequestBody OfficialKnowledgeCollectSearchVO vo,
            HttpServletRequest request
    ) {
        requireAdmin(request);
        return RESTResult.getSuccess(service.search(vo));
    }

    private void requireAdmin(HttpServletRequest request) {
        String roleCode = (String) request.getAttribute("roleCode");
        if (roleCode == null || !"admin".equals(roleCode)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅管理员可访问");
        }
    }
}
