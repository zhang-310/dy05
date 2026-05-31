package cn.gaifan.douyinOperations.module.agent.service.impl;

import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.module.agent.entity.AgentShare;
import cn.gaifan.douyinOperations.module.agent.entity.AgentConversation;
import cn.gaifan.douyinOperations.module.agent.entity.AgentMessage;
import cn.gaifan.douyinOperations.module.agent.repository.AgentShareRepository;
import cn.gaifan.douyinOperations.module.agent.repository.AgentConversationRepository;
import cn.gaifan.douyinOperations.module.agent.repository.AgentMessageRepository;
import cn.gaifan.douyinOperations.module.agent.service.AgentShareService;
import cn.gaifan.douyinOperations.module.agent.vo.AgentShareRequestVO;
import cn.gaifan.douyinOperations.module.agent.vo.AgentShareVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AgentShareServiceImpl implements AgentShareService {

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    @Autowired
    private AgentShareRepository shareRepository;

    @Autowired
    private AgentConversationRepository conversationRepository;

    @Autowired
    private AgentMessageRepository messageRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentShareVO createShare(Long userId, AgentShareRequestVO request) {
        // 验证对话是否存在且属于当前用户
        AgentConversation conversation = conversationRepository.findByIdAndDeleted(request.getConversationId(), 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "对话不存在"));

        if (!conversation.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权分享此对话");
        }

        // 生成唯一分享码
        String shareCode = generateShareCode();
        while (shareRepository.findByShareCodeAndDeleted(shareCode, 0).isPresent()) {
            shareCode = generateShareCode();
        }

        // 统计消息数量
        List<AgentMessage> messages = messageRepository.findByConversationIdOrderByIdAsc(
                request.getConversationId());
        int messageCount = messages.size();

        // 计算摘要（取前3条用户消息）
        String summary = request.getSummary();
        if (summary == null || summary.isBlank()) {
            StringBuilder sb = new StringBuilder();
            int count = 0;
            for (AgentMessage msg : messages) {
                if (msg.getSenderType() == 1 && count < 3) {
                    String content = msg.getContent();
                    if (content != null && !content.isBlank()) {
                        sb.append(content.length() > 100 ? content.substring(0, 100) + "..." : content);
                        sb.append(" / ");
                        count++;
                    }
                }
            }
            summary = sb.length() > 0 ? sb.toString().replaceAll("/\\s*$", "") : null;
        }

        // 创建分享记录
        AgentShare share = new AgentShare();
        share.setShareCode(shareCode);
        share.setConversationId(request.getConversationId());
        share.setAgentId(conversation.getAgentId());
        share.setOwnerId(userId);
        share.setTitle(request.getTitle() != null ? request.getTitle() : conversation.getConversationTopic());
        share.setSummary(summary);
        share.setMessageCount(messageCount);
        share.setViewCount(0);
        share.setIsPublic(request.getIsPublic() != null ? request.getIsPublic() : 1);
        share.setDeleted(0);

        if (request.getExpiresDays() != null && request.getExpiresDays() > 0) {
            long expiryTime = System.currentTimeMillis() + (long) request.getExpiresDays() * 24 * 60 * 60 * 1000;
            share.setExpiresAt(new Timestamp(expiryTime));
        }

        share = shareRepository.save(share);
        return toShareVO(share);
    }

    @Override
    public AgentShareVO getByShareCode(String shareCode) {
        AgentShare share = shareRepository.findByShareCodeAndDeleted(shareCode, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "分享不存在或已失效"));

        // 检查过期
        if (share.getExpiresAt() != null && share.getExpiresAt().getTime() < System.currentTimeMillis()) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "分享已过期");
        }

        // 累加浏览次数
        shareRepository.incrementViewCount(shareCode);

        return toShareVO(share);
    }

    @Override
    public List<AgentShareVO> listByUser(Long userId) {
        return shareRepository.findAll((root, query, cb) -> {
            query.where(cb.equal(root.get("ownerId"), userId), cb.equal(root.get("deleted"), 0));
            query.orderBy(cb.desc(root.get("createTime")));
            return cb.conjunction();
        }).stream().map(this::toShareVO).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteShare(Long userId, Long shareId) {
        AgentShare share = shareRepository.findById(shareId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "分享不存在"));

        if (!share.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权删除此分享");
        }

        share.setDeleted(1);
        shareRepository.save(share);
    }

    @Override
    public Object getShareConversationData(String shareCode) {
        AgentShare share = shareRepository.findByShareCodeAndDeleted(shareCode, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "分享不存在或已失效"));

        // 检查过期
        if (share.getExpiresAt() != null && share.getExpiresAt().getTime() < System.currentTimeMillis()) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "分享已过期");
        }

        List<AgentMessage> messages = messageRepository.findByConversationIdOrderByIdAsc(
                share.getConversationId());

        Map<String, Object> result = new HashMap<>();
        result.put("share", toShareVO(share));
        result.put("messages", messages.stream().map(msg -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", msg.getId());
            m.put("role", msg.getSenderType() == 1 ? "user" : "assistant");
            m.put("content", msg.getContent());
            m.put("toolCalls", msg.getToolCalls());
            m.put("createdAt", msg.getCreateTime() != null ? msg.getCreateTime().toString() : null);
            return m;
        }).collect(Collectors.toList()));
        return result;
    }

    private String generateShareCode() {
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    private AgentShareVO toShareVO(AgentShare share) {
        AgentShareVO vo = new AgentShareVO();
        vo.setId(share.getId());
        vo.setShareCode(share.getShareCode());
        vo.setConversationId(share.getConversationId());
        vo.setAgentId(share.getAgentId());
        vo.setTitle(share.getTitle());
        vo.setSummary(share.getSummary());
        vo.setMessageCount(share.getMessageCount());
        vo.setViewCount(share.getViewCount());
        vo.setIsPublic(share.getIsPublic());
        vo.setExpiresAt(share.getExpiresAt() != null ? share.getExpiresAt().toString() : null);
        vo.setCreateTime(share.getCreateTime() != null ? share.getCreateTime().toString() : null);
        return vo;
    }
}