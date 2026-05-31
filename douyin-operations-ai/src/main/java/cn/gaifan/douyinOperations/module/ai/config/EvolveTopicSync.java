package cn.gaifan.douyinOperations.module.ai.config;

import cn.gaifan.douyinOperations.module.ai.entity.AiEvolveTopic;
import cn.gaifan.douyinOperations.module.ai.repository.AiEvolveTopicRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 主题池增量同步：确保 DEFAULT_TOPICS 中缺失的主题被补充到已有环境。
 * 与 EvolveDataInitializer 解耦，表非空时也能补充新增主题。
 */
@Component
@Order(200)
public class EvolveTopicSync {

    private static final Logger log = LoggerFactory.getLogger(EvolveTopicSync.class);

    /** 与 EvolveDataInitializer 保持一致的规范主题列表（topic, category） */
    private static final String[][] CANONICAL_TOPICS = EvolveDataInitializer.getCanonicalTopics();

    @Resource
    private AiEvolveTopicRepository topicRepository;

    @PostConstruct
    public void syncMissingTopics() {
        try {
            Set<String> existingTopics = topicRepository.findByStatusAndDeletedOrderByPriorityAscCreateTimeDesc(1, 0)
                    .stream()
                    .map(AiEvolveTopic::getTopic)
                    .collect(Collectors.toSet());

            int added = 0;
            for (String[] t : CANONICAL_TOPICS) {
                if (!existingTopics.contains(t[0])) {
                    AiEvolveTopic topic = new AiEvolveTopic();
                    topic.setTopic(t[0]);
                    topic.setCategory(t[1]);
                    topic.setPriority(100);
                    topic.setSource("sync");
                    topicRepository.save(topic);
                    existingTopics.add(t[0]);
                    added++;
                }
            }
            if (added > 0) {
                log.info("进化主题池增量同步完成，新增 {} 个主题", added);
            }
        } catch (Exception e) {
            log.warn("进化主题池增量同步跳过: {}", e.getMessage());
        }
    }

}
