package cn.gaifan.douyinOperations.module.ai.config;

import cn.gaifan.douyinOperations.module.ai.entity.AiKnowledgeBase;
import cn.gaifan.douyinOperations.module.ai.repository.AiKnowledgeBaseRepository;
import cn.gaifan.douyinOperations.module.ai.service.KnowledgeBaseService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 默认知识库初始化：douyin（抖音知识库）、zhishi（技术知识库）
 */
@Component
public class KnowledgeBaseInitializer {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseInitializer.class);

    @Value("${app.ai.kb.init-enabled:true}")
    private boolean initEnabled;

    /**
     * 默认知识库初始化使用共享 owner。
     * 0 表示系统公共 owner，供所有用户共享使用。
     */
    @Value("${app.ai.kb.shared-owner-id:0}")
    private Long sharedOwnerId;

    @Resource
    private AiKnowledgeBaseRepository knowledgeBaseRepository;

    @Resource
    private KnowledgeBaseService knowledgeBaseService;

    @PostConstruct
    public void init() {
        if (!initEnabled) return;
        if (sharedOwnerId == null || sharedOwnerId < 0) {
            log.info("知识库初始化跳过：shared owner 配置无效");
            return;
        }
        try {
            createIfAbsent("douyin", "抖音知识库：抖音运营、直播、短视频、话术、商业化等");
            createIfAbsent("zhishi", "技术知识库：技术文档、开发规范、框架与工具等");
            createIfAbsent("huashu", "话术知识库：直播/产品话术参考案例，供 RAG 注入");
        } catch (Exception e) {
            log.warn("知识库初始化跳过（可能 Milvus/ES 未就绪）: {}", e.getMessage());
        }
    }

    private void createIfAbsent(String name, String description) {
        if (knowledgeBaseRepository.findByUserIdAndKbNameAndDeleted(sharedOwnerId, name, 0).isPresent()) {
            log.debug("知识库 [{}] 已存在，跳过", name);
            return;
        }
        try {
            AiKnowledgeBase kb = knowledgeBaseService.createKnowledgeBase(name, description, sharedOwnerId);
            log.info("知识库 [{}] 创建成功，sharedOwnerId={}, id={}", name, sharedOwnerId, kb.getId());
        } catch (Exception e) {
            log.warn("知识库 [{}] 创建失败: {}", name, e.getMessage());
        }
    }
}
