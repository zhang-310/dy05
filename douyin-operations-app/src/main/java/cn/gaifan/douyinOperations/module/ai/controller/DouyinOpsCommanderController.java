package cn.gaifan.douyinOperations.module.ai.controller;

import cn.gaifan.douyinOperations.common.annotation.CurrentUserId;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.ai.service.DouyinOpsCommanderService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai/douyin-ops-commander")
public class DouyinOpsCommanderController {

    @Resource
    private DouyinOpsCommanderService commanderService;

    @PostMapping("/brief")
    public RESTResult<Map<String, Object>> brief(@CurrentUserId Long userId) {
        return RESTResult.success("OK", commanderService.buildBrief(userId));
    }
}
