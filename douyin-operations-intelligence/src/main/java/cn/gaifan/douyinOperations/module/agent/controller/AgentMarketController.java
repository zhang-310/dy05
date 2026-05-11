package cn.gaifan.douyinOperations.module.agent.controller;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.agent.vo.AgentMarketSearchVO;
import cn.gaifan.douyinOperations.module.agent.vo.AgentMarketVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

/**
 * 智能体市场 Controller
 */
@RestController
@RequestMapping("/api/v1/agent/market")
public class AgentMarketController {

    @PostMapping("/list")
    public RESTResult<PageResultVO<AgentMarketVO>> list(@Valid @RequestBody AgentMarketSearchVO vo) {
        vo.validateParams();
        return RESTResult.success(new PageResultVO<>(0L, Collections.emptyList(), vo.getPage(), vo.getRows()));
    }
}
