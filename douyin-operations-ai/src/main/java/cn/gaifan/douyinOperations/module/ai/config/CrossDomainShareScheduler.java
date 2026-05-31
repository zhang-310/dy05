package cn.gaifan.douyinOperations.module.ai.config;

import cn.gaifan.douyinOperations.module.ai.repository.AiKnowledgeBaseRepository;
import cn.gaifan.douyinOperations.module.ai.service.CrossDomainShareService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 跨域共享 Agent 定时任务（阶段四）
 * 每周执行：将高引用 evolved 文档跨 KB 共享（同用户）
 */
@Component
public class CrossDomainShareScheduler {

    private static final Logger log = LoggerFactory.getLogger(CrossDomainShareScheduler.class);

    @Value("${app.ai.cross-domain-share.enabled:true}")
    private boolean crossDomainShareEnabled;

    @Value("${app.ai.cross-domain-share.max-per-run:50}")
    private int maxPerRun;

    @Resource
    private CrossDomainShareService crossDomainShareService;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private cn.gaifan.douyinOperations.module.ai.service.impl.CrossDomainShareServiceImpl crossDomainShareServiceImpl;
    @Resource
    private AiKnowledgeBaseRepository knowledgeBaseRepository;

    @Scheduled(cron = "${app.ai.cross-domain-share.cron:0 0 5 * * SAT}")
    public void runCrossDomainShare() {
        if (!crossDomainShareEnabled) {
            log.debug("跨域共享已禁用，跳过");
            return;
        }
        List<Long> userIds = knowledgeBaseRepository.findDistinctUserIds();
        int totalShared = 0;
        for (Long userId : userIds) {
            try {
                Map<String, Object> result = crossDomainShareService.runCrossDomainShare(userId, maxPerRun / Math.max(1, userIds.size()));
                Object shared = result.get("sharedCount");
                if (shared instanceof Number n) totalShared += n.intValue();
            } catch (Exception e) {
                log.warn("跨域共享失败 userId={}: {}", userId, e.getMessage());
            }
        }
        if (totalShared > 0) log.info("跨域共享完成，处理 {} 条候选", totalShared);
    }

    @Value("${app.ai.public-sink.enabled:true}")
    private boolean publicSinkEnabled;

    @Value("${app.ai.public-sink.max-per-run:30}")
    private int publicSinkMaxPerRun;

    @Scheduled(cron = "${app.ai.public-sink.cron:0 0 6 * * SUN}")
    public void runWeeklyPublicSink() {
        if (!publicSinkEnabled) {
            log.debug("公共知识沉淀已禁用，跳过");
            return;
        }
        try {
            if (crossDomainShareServiceImpl != null) {
                var result = crossDomainShareServiceImpl.runWeeklyPublicSink(publicSinkMaxPerRun);
                Object sinked = result.get("sinkedCount");
                if (sinked instanceof Number n && n.intValue() > 0) {
                    log.info("公共知识库沉淀完成: {} 条", n.intValue());
                }
            }
        } catch (Exception e) {
            log.warn("公共知识沉淀失败: {}", e.getMessage());
        }
    }
}
