package cn.gaifan.douyinOperations.module.log.controller;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.log.vo.LogAuditSearchVO;
import cn.gaifan.douyinOperations.module.log.vo.LogAuditVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

/**
 * 审计日志 Controller
 */
@RestController
@RequestMapping("/api/v1/log/audit")
public class LogAuditController {

    @PostMapping("/search")
    public RESTResult<PageResultVO<LogAuditVO>> search(@Valid @RequestBody LogAuditSearchVO vo) {
        vo.validateParams();
        return RESTResult.success(new PageResultVO<>(0L, Collections.emptyList(), vo.getPage(), vo.getRows()));
    }
}
