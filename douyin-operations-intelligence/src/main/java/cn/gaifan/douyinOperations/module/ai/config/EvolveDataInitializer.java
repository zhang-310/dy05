package cn.gaifan.douyinOperations.module.ai.config;

import cn.gaifan.douyinOperations.module.ai.entity.AiEvolveTopic;
import cn.gaifan.douyinOperations.module.ai.repository.AiEvolveTopicRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


/**
 * 进化主题池初始数据：表为空时插入默认主题
 */
@Component
public class EvolveDataInitializer {

    private static final Logger log = LoggerFactory.getLogger(EvolveDataInitializer.class);

    private static final String[][] DEFAULT_TOPICS = {
            {"抖音运营 直播 话术 粉丝互动", "live"},
            {"短视频 脚本 黄金3秒 内容创作", "basic"},
            {"数据分析 关键指标 优化决策", "data"},
            {"算法推荐 流量 爆款 运营", "algorithm"},
            {"商业化 广告 电商 知识付费 变现", "commercial"},
            {"AI AIGC 豆包 剪映AI 数字人", "ai"},
            {"AI 辅助创作 大模型 短视频 直播", "ai"},
            {"垂类策略 美妆 知识科普 剧情 户外", "vertical"},
            {"剪映 工具链 发布质检 平台规则", "compliance"},
            {"账号矩阵 团队管理 中高级战术", "team"},
    };

    /**
     * 返回规范主题列表（供 EvolveTopicSync 使用）
     */
    public static String[][] getCanonicalTopics() {
        return DEFAULT_TOPICS.clone();
    }

    @Resource
    private AiEvolveTopicRepository topicRepository;

    @PostConstruct
    public void init() {
        try {
            long count = topicRepository.countByStatusAndDeleted(1, 0);
            if (count > 0) return;
            for (String[] t : DEFAULT_TOPICS) {
                AiEvolveTopic topic = new AiEvolveTopic();
                topic.setTopic(t[0]);
                topic.setCategory(t[1]);
                topic.setPriority(100);
                topic.setSource("initial");
                topicRepository.save(topic);
            }
            log.info("进化主题池初始化完成，共 {} 个默认主题", DEFAULT_TOPICS.length);
        } catch (Exception e) {
            log.warn("进化主题池初始化跳过（可能表未创建）: {}", e.getMessage());
        }
    }
}
