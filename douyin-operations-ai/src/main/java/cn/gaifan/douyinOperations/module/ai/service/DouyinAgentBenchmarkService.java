package cn.gaifan.douyinOperations.module.ai.service;

import java.util.List;
import java.util.Map;

public interface DouyinAgentBenchmarkService {

    Map<String, Object> runSmokeBenchmark();

    List<Map<String, Object>> listBuiltinCases();
}
