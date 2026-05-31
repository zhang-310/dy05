package cn.gaifan.douyinOperations.module.ai.service.official;

import java.util.Map;

public interface DouyinSchoolCollectorService {

    Map<String, Object> collect();

    Map<String, Object> collectTopic(String topicCode);

    Map<String, Object> status();

    Map<String, Object> topics();
}
