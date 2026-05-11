package cn.gaifan.douyinOperations.module.log.controller;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.log.vo.LogOperationSearchVO;
import cn.gaifan.douyinOperations.module.log.vo.LogOperationVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

/**
 * 操作日志 Controller
 */
@RestController
@RequestMapping("/api/v1/log/operation")
public class LogOperationController {

    @PostMapping("/search")
    public RESTResult<PageResultVO<LogOperationVO>> search(@Valid @RequestBody LogOperationSearchVO vo) {
        vo.validateParams();
        return RESTResult.success(new PageResultVO<>(0L, Collections.emptyList(), vo.getPage(), vo.getRows()));
    }
}
