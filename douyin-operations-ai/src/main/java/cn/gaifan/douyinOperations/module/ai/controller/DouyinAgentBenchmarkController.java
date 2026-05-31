package cn.gaifan.douyinOperations.module.ai.controller;

import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.ai.service.DouyinAgentBenchmarkService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai/douyin-agent-benchmark")
public class DouyinAgentBenchmarkController {

    @Resource
    private DouyinAgentBenchmarkService benchmarkService;

    @PostMapping("/cases")
    public RESTResult<List<Map<String, Object>>> cases() {
        return RESTResult.success("OK", benchmarkService.listBuiltinCases());
    }

    @PostMapping("/smoke")
    public RESTResult<Map<String, Object>> smoke() {
        return RESTResult.success("OK", benchmarkService.runSmokeBenchmark());
    }
}
