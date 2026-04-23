package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.module.ai.service.KnowledgeBaseService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

/**
 * 直播链路知识库访问解析器。
 * 当前用户优先；仅在显式开启共享回退时，才切换到共享 owner 的知识库上下文。
 */
@Component
public class LiveKnowledgeBaseAccessResolver {

    @Resource
    private KnowledgeBaseService knowledgeBaseService;

    @Value("${app.ai.kb.shared-owner-id:0}")
    private Long sharedKbOwnerId;

    @Value("${app.ai.kb.allow-shared-fallback:false}")
    private boolean allowSharedKbFallback;

    public ResolvedKnowledgeBase resolveHuashu(Long userId) {
        return resolveByName(userId, "huashu");
    }

    public ResolvedKnowledgeBase resolveByName(Long userId, String kbName) {
        if (userId == null || kbName == null || kbName.isBlank()) {
            return null;
        }

        Long kbId = knowledgeBaseService.resolveKbIdByName(userId, kbName);
        if (kbId != null) {
            return new ResolvedKnowledgeBase(kbId, userId, false);
        }

        if (!allowSharedKbFallback || sharedKbOwnerId == null || sharedKbOwnerId < 0) {
            return null;
        }

        Long sharedKbId = knowledgeBaseService.resolveKbIdByName(sharedKbOwnerId, kbName);
        if (sharedKbId == null) {
            return null;
        }
        return new ResolvedKnowledgeBase(sharedKbId, sharedKbOwnerId, true);
    }

    public record ResolvedKnowledgeBase(Long kbId, Long accessUserId, boolean sharedFallback) {
    }
}
