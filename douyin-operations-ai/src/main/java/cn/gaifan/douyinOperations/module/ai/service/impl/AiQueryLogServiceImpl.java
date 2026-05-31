package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.entity.AiQueryLog;
import cn.gaifan.douyinOperations.module.ai.repository.AiQueryLogRepository;
import cn.gaifan.douyinOperations.module.ai.service.AiQueryLogService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiQueryLogServiceImpl implements AiQueryLogService {

    private static final Logger log = LoggerFactory.getLogger(AiQueryLogServiceImpl.class);

    @Resource
    private AiQueryLogRepository repository;

    @Value("${app.ai.query-log.enabled:false}")
    private boolean enabled;

    @Override
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void log(Long userId, Long kbId, String queryText, int topK, int hitCount, Integer latencyMs, int cacheHit) {
        if (!enabled || userId == null || kbId == null) return;
        try {
            AiQueryLog log = new AiQueryLog();
            log.setUserId(userId);
            log.setKbId(kbId);
            log.setQueryText(truncate(queryText, 512));
            log.setTopK(topK);
            log.setHitCount(hitCount);
            log.setLatencyMs(latencyMs);
            log.setCacheHit(cacheHit);
            log.setSource("user");
            repository.save(log);
        } catch (Exception e) {
            log.debug("查询日志写入失败: {}", e.getMessage());
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
