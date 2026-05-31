package cn.gaifan.douyinOperations.module.agent.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.agent.entity.*;
import cn.gaifan.douyinOperations.module.agent.repository.*;
import cn.gaifan.douyinOperations.module.agent.vo.AgentSearchVO;
import cn.gaifan.douyinOperations.module.agent.vo.AgentSaveVO;
import cn.gaifan.douyinOperations.module.agent.vo.AgentVO;
import cn.gaifan.douyinOperations.module.agent.service.AgentFunctionCallingService;
import cn.gaifan.douyinOperations.module.agent.service.SkillExecutor;
import cn.gaifan.douyinOperations.module.agent.service.SkillExecutor.ToolCallResult;
import cn.gaifan.douyinOperations.module.agent.util.PromptInjectionDetector;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import jakarta.persistence.criteria.Predicate;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

@Service
public class AgentServiceImpl implements cn.gaifan.douyinOperations.module.agent.service.AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentServiceImpl.class);

    @Resource private AgentRepository agentRepository;
    @Resource private AgentConversationRepository conversationRepository;
    @Resource private AgentMessageRepository messageRepository;
    @Resource private cn.gaifan.douyinOperations.module.agent.service.SkillExecutor skillExecutor;
    @Resource private cn.gaifan.douyinOperations.module.agent.service.AgentFunctionCallingService agentFunctionCallingService;

    // P0-6: 注入 Caffeine 缓存
    @Resource(name = "agentListCache")
    private Cache<String, Object> agentListCache;

    @Resource(name = "agentDetailCache")
    private Cache<Long, Object> agentDetailCache;

    // ==================== Agent CRUD ====================

    // P0-6: 智能体列表查询缓存（响应时间 150ms → 10ms）
    @Transactional(readOnly = true)
    public PageResultVO<AgentVO> searchAgents(Long userId, AgentSearchVO searchVO) {
        // 验证分页参数
        searchVO.validateParams();

        // 构建缓存键：userId + 查询条件哈希
        String cacheKey = buildCacheKey(userId, searchVO);

        // 尝试从 L1 缓存获取
        @SuppressWarnings("unchecked")
        PageResultVO<AgentVO> cached = (PageResultVO<AgentVO>) agentListCache.getIfPresent(cacheKey);
        if (cached != null) {
            log.debug("[AgentCache] L1 cache hit: {}", cacheKey);
            return cached;
        }

        // 缓存未命中，查询数据库
        Sort sort = buildSort(searchVO.getSortBy());
        Pageable pageable = PageRequest.of(searchVO.getPage(), searchVO.getRows(), sort);
        Specification<Agent> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("deleted"), 0));
            predicates.add(cb.equal(root.get("userId"), userId));
            if (searchVO.getAgentType() != null) predicates.add(cb.equal(root.get("agentType"), searchVO.getAgentType()));
            if (searchVO.getStatusEnabled() != null) predicates.add(cb.equal(root.get("status"), searchVO.getStatusEnabled()));
            if (searchVO.getAgentName() != null && !searchVO.getAgentName().isBlank()) {
                predicates.add(cb.like(root.get("agentName"), "%" + searchVO.getAgentName().trim() + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<Agent> p = agentRepository.findAll(spec, pageable);
        PageResultVO<AgentVO> result = PageResultVO.of(p.getTotalElements(),
                p.getContent().stream().map(this::agentToVO).collect(Collectors.toList()),
                searchVO.getPage(), searchVO.getRows());

        // 写入 L1 缓存
        agentListCache.put(cacheKey, result);
        log.debug("[AgentCache] L1 cache miss, stored: {}", cacheKey);

        return result;
    }

    public AgentVO getAgentById(Long id) {
        // 尝试从 L1 缓存获取
        @SuppressWarnings("unchecked")
        AgentVO cached = (AgentVO) agentDetailCache.getIfPresent(id);
        if (cached != null) {
            log.debug("[AgentCache] L1 detail cache hit: {}", id);
            return cached;
        }

        // 缓存未命中，查询数据库
        Agent agent = agentRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "智能体不存在"));
        AgentVO vo = agentToVO(agent);

        // 写入 L1 缓存
        agentDetailCache.put(id, vo);
        log.debug("[AgentCache] L1 detail cache miss, stored: {}", id);

        return vo;
    }

    // P0-6: 保存智能体时清除缓存
    @Transactional(rollbackFor = Exception.class)
    public long saveAgent(Long userId, AgentSaveVO saveVO) {
        Agent entity;
        if (saveVO.getId() != null && saveVO.getId() > 0) {
            entity = agentRepository.findByIdAndDeleted(saveVO.getId(), 0)
                    .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "智能体不存在"));
            // 清除详情缓存
            agentDetailCache.invalidate(saveVO.getId());
        } else {
            entity = new Agent();
            entity.setUserId(userId);
        }
        entity.setAgentName(saveVO.getAgentName());
        entity.setAgentType(saveVO.getAgentType());
        if (saveVO.getDescription() != null) entity.setDescription(saveVO.getDescription());
        if (saveVO.getSystemPrompt() != null) entity.setSystemPrompt(saveVO.getSystemPrompt());
        if (saveVO.getModelConfig() != null) entity.setModelConfig(saveVO.getModelConfig());
        if (saveVO.getResponseMode() != null) entity.setResponseMode(saveVO.getResponseMode());
        if (saveVO.getAvailableTools() != null) entity.setAvailableTools(saveVO.getAvailableTools());
        long savedId = agentRepository.save(entity).getId();

        // 清除列表缓存（所有用户的查询结果都可能受影响）
        agentListCache.invalidateAll();
        log.debug("[AgentCache] Invalidated all list cache after save");

        return savedId;
    }

    // P0-6: 删除智能体时清除缓存
    @Transactional(rollbackFor = Exception.class)
    public void deleteAgent(Long id, Long userId) {
        Agent entity = agentRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "智能体不存在"));
        // P0-2: 数据隔离 - 校验所有权
        if (!entity.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权限删除该智能体");
        }
        entity.setDeleted(1);
        agentRepository.save(entity);

        // 清除缓存
        agentDetailCache.invalidate(id);
        agentListCache.invalidateAll();
        log.debug("[AgentCache] Invalidated cache after delete: {}", id);
    }

    // P0-6: 更新智能体状态时清除缓存
    @Transactional(rollbackFor = Exception.class)
    public void updateAgentStatus(Long id, Integer status) {
        agentRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "智能体不存在"));
        agentRepository.updateStatus(id, status);

        // 清除缓存
        agentDetailCache.invalidate(id);
        agentListCache.invalidateAll();
        log.debug("[AgentCache] Invalidated cache after status update: {}", id);
    }

    // ==================== Conversation ====================

    @Transactional(rollbackFor = Exception.class)
    public long createConversation(Long agentId, Long userId, String topic) {
        agentRepository.findByIdAndDeleted(agentId, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "智能体不存在"));
        agentRepository.incrementConversationCount(agentId);
        AgentConversation conv = new AgentConversation();
        conv.setAgentId(agentId);
        conv.setUserId(userId);
        conv.setConversationTopic(topic != null ? topic : "新对话");
        return conversationRepository.save(conv).getId();
    }

    public List<Map<String, Object>> listConversations(Long userId, Long agentId) {
        // P1-3: N+1 查询优化 - 使用 JOIN FETCH 一次性加载智能体信息
        List<AgentConversation> conversations;
        if (agentId != null) {
            conversations = conversationRepository.findByUserIdAndAgentIdWithAgent(userId, agentId);
        } else {
            conversations = conversationRepository.findByUserIdWithAgent(userId);
        }
        return conversations.stream().map(this::convToMap).collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteConversation(Long id, Long userId) {
        AgentConversation entity = conversationRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "对话不存在"));
        // P0-2: 数据隔离 - 校验所有权
        if (!entity.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权限删除该对话");
        }
        entity.setDeleted(1);
        conversationRepository.save(entity);
    }

    // ==================== Message ====================

    @Transactional(rollbackFor = Exception.class)
    public long sendMessage(Long conversationId, Integer senderType, String content, Integer tokens) {
        conversationRepository.findByIdAndDeleted(conversationId, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "对话不存在"));
        AgentMessage msg = new AgentMessage();
        msg.setConversationId(conversationId);
        msg.setSenderType(senderType);
        msg.setContent(content);
        msg.setTokens(tokens != null ? tokens : 0);
        long msgId = messageRepository.save(msg).getId();
        conversationRepository.incrementMessageCount(conversationId, new Timestamp(System.currentTimeMillis()));
        return msgId;
    }

    // P0-4: 分页查询对话历史（防止 N+1 查询和内存溢出）
    public PageResultVO<Map<String, Object>> listMessages(Long conversationId, int page, int rows) {
        Pageable pageable = PageRequest.of(page, rows, Sort.by(Sort.Direction.ASC, "createTime"));
        Page<AgentMessage> p = messageRepository.findByConversationIdAndDeleted(conversationId, 0, pageable);
        List<Map<String, Object>> list = p.getContent().stream().map(this::msgToMap).collect(Collectors.toList());
        return PageResultVO.of(p.getTotalElements(), list, page, rows);
    }

    // 保留旧方法用于内部调用（如 chatWithAgent 需要完整历史）
    public List<Map<String, Object>> listMessagesAll(Long conversationId) {
        return messageRepository.findByConversationIdOrderByCreateTimeAsc(conversationId)
                .stream().map(this::msgToMap).collect(Collectors.toList());
    }

    // ==================== chatWithAgent ====================

    /**
     * 与智能体对话：检测技能调用，执行并生成回复
     */
    @Transactional(rollbackFor = Exception.class)
    public String chatWithAgent(Long agentId, Long userId, String userMessage, Long conversationId, Object context,
                              java.util.function.BiConsumer<String, String> statusEmitter) {
        // P0-3: Prompt 注入防护（使用统一工具类）
        if (PromptInjectionDetector.detectInjection(userMessage)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "输入包含不安全内容，请重新输入");
        }
        String sanitizedMessage = PromptInjectionDetector.sanitize(userMessage);

        // 1. 获取智能体
        Agent agent = agentRepository.findByIdAndDeleted(agentId, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "智能体不存在"));

        // 2. 处理 conversationId（为 null 时自动创建）
        if (conversationId == null) {
            AgentConversation conv = new AgentConversation();
            conv.setAgentId(agentId);
            conv.setUserId(userId);
            conv.setConversationTopic(sanitizedMessage.length() > 20 ? sanitizedMessage.substring(0, 20) + "..." : sanitizedMessage);
            conversationId = conversationRepository.save(conv).getId();
        }

        // 3. 保存用户消息（使用清理后的内容）
        AgentMessage userMsg = new AgentMessage();
        userMsg.setConversationId(conversationId);
        userMsg.setSenderType(1); // 1=用户
        userMsg.setContent(sanitizedMessage);
        userMsg.setTokens(estimateTokens(sanitizedMessage));
        messageRepository.save(userMsg);
        conversationRepository.incrementMessageCount(conversationId, new Timestamp(System.currentTimeMillis()));

        // 4. 发送 SSE 状态事件：正在分析需求
        sendStatus(statusEmitter, "正在分析需求...");

        // 5. LLM 驱动的 Function Calling（带 SSE 事件回调）
        AgentFunctionCallingService.FunctionCallingResult fcResult = agentFunctionCallingService.execute(
                agentId, userId, conversationId, agent.getSystemPrompt(), sanitizedMessage, statusEmitter);
        String reply = fcResult.content;
        List<SkillExecutor.ToolCallResult> toolResults = fcResult.toolCalls != null
                ? fcResult.toolCalls
                : new java.util.ArrayList<>();

        // 6. 发送 SSE 状态事件：工具调用完成
        if (toolResults.isEmpty()) {
            sendStatus(statusEmitter, "无需调用工具");
        } else {
            String[] toolNames = toolResults.stream().map(r -> r.toolName).toArray(String[]::new);
            sendStatus(statusEmitter, "工具执行完成: " + String.join(", ", toolNames));
        }

        // 7. 生成回复（如果 FC 未返回内容，使用降级回复）
        if (reply == null || reply.isBlank()) {
            reply = generateFallbackReply(agent.getAgentName(), agent.getSystemPrompt(), userMessage);
        }

        // 8. 保存助手消息
        AgentMessage assistantMsg = new AgentMessage();
        assistantMsg.setConversationId(conversationId);
        assistantMsg.setSenderType(2); // 2=助手
        assistantMsg.setContent(reply);
        assistantMsg.setTokens(estimateTokens(reply));
        if (!toolResults.isEmpty()) {
            assistantMsg.setToolCalls(skillExecutor.serializeToolCalls(toolResults));
        }
        messageRepository.save(assistantMsg);
        conversationRepository.incrementMessageCount(conversationId, new Timestamp(System.currentTimeMillis()));

        return reply;
    }

    /**
     * 发送 SSE 状态事件（线程安全）
     */
    private void sendStatus(java.util.function.BiConsumer<String, String> emitter, String message) {
        if (emitter != null) {
            try {
                emitter.accept("status", message);
            } catch (Exception e) {
                log.warn("Failed to send status event: {}", e.getMessage());
            }
        }
    }

    /**
     * 生成回复：根据技能执行结果和智能体配置生成文本回复
     */
    private String generateReply(Agent agent, String userMessage, List<SkillExecutor.ToolCallResult> toolResults) {
        String agentName = agent.getAgentName();
        String systemPrompt = agent.getSystemPrompt();

        if (toolResults.isEmpty()) {
            return generateFallbackReply(agentName, systemPrompt, userMessage);
        }

        StringBuilder reply = new StringBuilder();
        for (SkillExecutor.ToolCallResult result : toolResults) {
            if (result.success) {
                reply.append(result.result).append("\n\n");
            } else {
                reply.append("调用工具[").append(result.toolName).append("]时出错：")
                        .append(result.error).append("\n\n");
            }
        }
        reply.append("以上是工具执行结果。有什么需要我进一步说明的吗？");
        return reply.toString().trim();
    }

    /**
     * 无技能匹配时的降级回复
     */
    private String generateFallbackReply(String agentName, String systemPrompt, String userMessage) {
        if (systemPrompt != null && systemPrompt.contains("话术")) {
            return "您好！我是" + agentName + "，专注于直播话术策划。\n\n" +
                    "请告诉我您的需求，例如：\n" +
                    "- 生成开场白\n" +
                    "- 撰写产品介绍话术\n" +
                    "- 设计促销话术\n\n" +
                    "我会结合知识库和话术模板为您生成专业话术。";
        } else if (systemPrompt != null && systemPrompt.contains("检测")) {
            return "您好！我是" + agentName + "，专注于内容合规检测。\n\n" +
                    "请发送需要检测的文本，我会帮您识别违规风险并提供修改建议。";
        } else if (systemPrompt != null && systemPrompt.contains("商品")) {
            return "您好！我是" + agentName + "，专注于商品分析。\n\n" +
                    "请告诉我需要分析的商品或品类，我会帮您提炼卖点、分析市场趋势。";
        } else if (systemPrompt != null && systemPrompt.contains("直播场次")) {
            return "您好！我是" + agentName + "，专注于直播数据分析。\n\n" +
                    "请告诉我您想查询的场次信息，我会帮您分析数据并给出优化建议。";
        } else if (systemPrompt != null && systemPrompt.contains("选品")) {
            return "您好！我是" + agentName + "，专注于选品策略。\n\n" +
                    "请告诉我您的选品需求（如品类、目标人群、预算等），我会为您推荐合适的选品策略。";
        } else if (systemPrompt != null && systemPrompt.contains("咨询")) {
            return "您好！我是" + agentName + "，很高兴为您服务。\n\n" +
                    "请告诉我您的产品或服务相关信息，我会根据知识库为您提供专业的咨询服务。";
        } else {
            return "您好！我是" + agentName + "。\n\n" +
                    "请告诉我您的具体需求，我会尽力帮助您。";
        }
    }

    /**
     * 估算 token 数量（中文约 2 字符/token，英文约 4 字符/token）
     */
    private int estimateTokens(String text) {
        if (text == null || text.isEmpty()) return 0;
        int chinese = 0, english = 0;
        for (char c : text.toCharArray()) {
            if (c > 0x4E00 && c < 0x9FFF) chinese++;
            else if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) english++;
        }
        return chinese / 2 + english / 4 + text.length() / 4;
    }

    // ==================== rateMessage & exportConversation ====================

    @Transactional(rollbackFor = Exception.class)
    public void rateMessage(Long messageId, String rating) {
        AgentMessage msg = messageRepository.findById(messageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "消息不存在"));
        msg.setRating(rating);
        messageRepository.save(msg);
    }

    public String exportConversation(Long conversationId) {
        // 获取对话和消息
        AgentConversation conv = conversationRepository.findByIdAndDeleted(conversationId, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "对话不存在"));

        List<AgentMessage> messages = messageRepository.findByConversationIdOrderByCreateTimeAsc(conversationId);

        // 获取智能体名称
        String agentName = "智能体";
        try {
            agentName = agentRepository.findByIdAndDeleted(conv.getAgentId(), 0)
                    .map(Agent::getAgentName).orElse("智能体");
        } catch (Exception e) {
            log.warn("获取智能体名称失败: {}", e.getMessage());
        }

        // 生成 Markdown
        StringBuilder md = new StringBuilder();
        md.append("# ").append(conv.getConversationTopic()).append("\n\n");
        md.append("**智能体**: ").append(agentName).append("\n\n");
        md.append("**创建时间**: ").append(conv.getCreateTime()).append("\n\n");
        md.append("---\n\n");

        for (AgentMessage msg : messages) {
            String sender = msg.getSenderType() == 1 ? "**用户**" : "**助手**";
            md.append(sender).append("\n\n");
            // P0-1: XSS 防护 - HTML 转义
            md.append(StringEscapeUtils.escapeHtml4(msg.getContent())).append("\n\n");
            if (msg.getToolCalls() != null && !msg.getToolCalls().isEmpty()) {
                md.append("*工具调用: ").append(StringEscapeUtils.escapeHtml4(msg.getToolCalls())).append("*\n\n");
            }
            md.append("---\n\n");
        }

        // TODO: 保存到文件存储，返回下载 URL
        // 暂时返回内嵌内容
        return "data:text/markdown;charset=utf-8," + java.net.URLEncoder.encode(md.toString(), java.nio.charset.StandardCharsets.UTF_8);
    }

    // ==================== toVO/toMap ====================

    private AgentVO agentToVO(Agent e) {
        AgentVO vo = new AgentVO();
        vo.setId(e.getId());
        vo.setAgentName(e.getAgentName());
        vo.setAgentType(e.getAgentType());
        vo.setDescription(e.getDescription());
        vo.setSystemPrompt(e.getSystemPrompt());
        vo.setModelConfig(e.getModelConfig());
        vo.setResponseMode(e.getResponseMode());
        vo.setStatus(e.getStatus());
        vo.setVersion(e.getVersion());
        vo.setAvailableTools(e.getAvailableTools());
        vo.setCreatedAt(e.getCreateTime());
        vo.setUpdatedAt(e.getUpdateTime());
        // 评分统计
        int count = e.getRatingCount() != null ? e.getRatingCount() : 0;
        int sum = e.getRatingSum() != null ? e.getRatingSum() : 0;
        vo.setRatingCount(count);
        vo.setAverageRating(count > 0 ? Math.round((double) sum / count * 10) / 10.0 : 0.0);
        vo.setConversationCount(e.getConversationCount() != null ? e.getConversationCount() : 0);
        return vo;
    }

    private Map<String, Object> agentToMap(Agent e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.getId()); m.put("userId", e.getUserId()); m.put("agentName", e.getAgentName());
        m.put("description", e.getDescription()); m.put("agentType", e.getAgentType());
        m.put("systemPrompt", e.getSystemPrompt()); m.put("modelConfig", e.getModelConfig());
        m.put("availableTools", e.getAvailableTools());
        m.put("responseMode", e.getResponseMode()); m.put("status", e.getStatus());
        m.put("version", e.getVersion()); m.put("createTime", e.getCreateTime()); m.put("updateTime", e.getUpdateTime());
        return m;
    }

    private Map<String, Object> convToMap(AgentConversation e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.getId()); m.put("agentId", e.getAgentId()); m.put("userId", e.getUserId());
        m.put("conversationTopic", e.getConversationTopic()); m.put("status", e.getStatus());
        m.put("messageCount", e.getMessageCount()); m.put("lastMessageTime", e.getLastMessageTime());
        m.put("createTime", e.getCreateTime());
        return m;
    }

    private Map<String, Object> msgToMap(AgentMessage e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.getId()); m.put("conversationId", e.getConversationId());
        m.put("senderType", e.getSenderType()); m.put("content", e.getContent());
        m.put("tokens", e.getTokens()); m.put("toolCalls", e.getToolCalls());
        m.put("rating", e.getRating()); m.put("createTime", e.getCreateTime());
        return m;
    }

    private Sort buildSort(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "createTime");
        }
        return switch (sortBy) {
            case "rating" -> Sort.by(Sort.Direction.DESC, "createTime"); // rating 暂用 createTime，待 DB 层支持计算字段
            case "popular" -> Sort.by(Sort.Direction.DESC, "conversationCount");
            case "name" -> Sort.by(Sort.Direction.ASC, "agentName");
            default -> Sort.by(Sort.Direction.DESC, "createTime");
        };
    }

    // ==================== 工具方法 ====================

    /**
     * P0-6: 构建缓存键
     * 格式: userId:page:rows:sortBy:agentType:status:name
     */
    private String buildCacheKey(Long userId, AgentSearchVO searchVO) {
        return String.format("%d:%d:%d:%s:%s:%s:%s",
                userId,
                searchVO.getPage(),
                searchVO.getRows(),
                searchVO.getSortBy() != null ? searchVO.getSortBy() : "default",
                searchVO.getAgentType() != null ? searchVO.getAgentType() : "all",
                searchVO.getStatusEnabled() != null ? searchVO.getStatusEnabled() : "all",
                searchVO.getAgentName() != null ? searchVO.getAgentName().trim() : "all"
        );
    }
}
