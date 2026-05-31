package cn.gaifan.douyinOperations.module.ai.controller;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.ai.entity.AiCallLog;
import cn.gaifan.douyinOperations.module.ai.service.AiCallLogService;
import cn.gaifan.douyinOperations.module.ai.vo.CallLogSearchVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@Tag(name = "AI 调用日志（管理员）")
@RestController
@RequestMapping("/api/v1/ai/admin/call-log")
public class AiAdminCallLogController {

    @Resource
    private AiCallLogService aiCallLogService;

    @Operation(summary = "分页查询调用日志")
    @PostMapping("/search")
    public RESTResult<PageResultVO<AiCallLog>> search(
            @RequestBody(required = false) CallLogSearchVO vo,
            HttpServletRequest request
    ) {
        requireAdmin(request);
        if (vo == null) vo = new CallLogSearchVO();
        return RESTResult.getSuccess(aiCallLogService.search(vo));
    }

    private void requireAdmin(HttpServletRequest request) {
        String roleCode = (String) request.getAttribute("roleCode");
        if (roleCode == null || !"admin".equals(roleCode)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅管理员可访问");
        }
    }
}
