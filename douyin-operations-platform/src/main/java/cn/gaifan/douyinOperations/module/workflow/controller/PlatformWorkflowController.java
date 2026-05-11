package cn.gaifan.douyinOperations.module.workflow.controller;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.workflow.vo.WorkflowSearchVO;
import cn.gaifan.douyinOperations.module.workflow.vo.WorkflowVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

/**
 * 工作流 Controller
 */
@RestController
@RequestMapping("/api/v1/workflow")
public class PlatformWorkflowController {

    @PostMapping("/search")
    public RESTResult<PageResultVO<WorkflowVO>> search(@Valid @RequestBody WorkflowSearchVO vo) {
        vo.validateParams();
        return RESTResult.success(new PageResultVO<>(0L, Collections.emptyList(), vo.getPage(), vo.getRows()));
    }
}
