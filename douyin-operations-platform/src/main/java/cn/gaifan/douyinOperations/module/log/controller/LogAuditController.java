package cn.gaifan.douyinOperations.module.log.controller;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.log.service.AuditLogService;
import cn.gaifan.douyinOperations.module.log.vo.LogAuditSearchVO;
import cn.gaifan.douyinOperations.module.log.vo.LogAuditVO;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 审计日志 Controller
 */
@RestController
@RequestMapping("/api/v1/log/audit")
public class LogAuditController {

    @Resource
    private AuditLogService auditLogService;

    @PostMapping("/search")
    public RESTResult<PageResultVO<LogAuditVO>> search(@Valid @RequestBody LogAuditSearchVO vo) {
        return RESTResult.success(auditLogService.search(vo));
    }
}
