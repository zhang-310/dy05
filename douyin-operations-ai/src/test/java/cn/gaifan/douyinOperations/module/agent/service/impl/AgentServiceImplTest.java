package cn.gaifan.douyinOperations.module.agent.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.agent.entity.Agent;
import cn.gaifan.douyinOperations.module.agent.entity.AgentConversation;
import cn.gaifan.douyinOperations.module.agent.entity.AgentMessage;
import cn.gaifan.douyinOperations.module.agent.repository.AgentConversationRepository;
import cn.gaifan.douyinOperations.module.agent.repository.AgentMessageRepository;
import cn.gaifan.douyinOperations.module.agent.repository.AgentRepository;
import cn.gaifan.douyinOperations.module.agent.service.AgentFunctionCallingService;
import cn.gaifan.douyinOperations.module.agent.service.SkillExecutor;
import cn.gaifan.douyinOperations.module.agent.vo.AgentSearchVO;
import cn.gaifan.douyinOperations.module.agent.vo.AgentSaveVO;
import cn.gaifan.douyinOperations.module.agent.vo.AgentVO;
import com.github.benmanes.caffeine.cache.Cache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * P0-8: AgentServiceImpl 单元测试（阶段 1）
 * 测试核心业务逻辑：CRUD、数据隔离、缓存
 */
@ExtendWith(MockitoExtension.class)
class AgentServiceImplTest {

    @Mock
    private AgentRepository agentRepository;

    @Mock
    private AgentConversationRepository conversationRepository;

    @Mock
    private AgentMessageRepository messageRepository;

    @Mock
    private SkillExecutor skillExecutor;

    @Mock
    private AgentFunctionCallingService agentFunctionCallingService;

    @Mock
    private Cache<String, Object> agentListCache;

    @Mock
    private Cache<Long, Object> agentDetailCache;

    @InjectMocks
    private AgentServiceImpl agentService;

    private Agent testAgent;
    private AgentConversation testConversation;
    private AgentMessage testMessage;
    private Long userId = 1L;
    private Long agentId = 100L;

    @BeforeEach
    void setUp() {
        // 准备测试数据
        testAgent = new Agent();
        testAgent.setId(agentId);
        testAgent.setUserId(userId);
        testAgent.setAgentName("测试智能体");
        testAgent.setAgentType(0); // Integer type
        testAgent.setDescription("测试描述");
        testAgent.setSystemPrompt("你是测试助手");
        testAgent.setStatus(1);
        testAgent.setDeleted(0);
        testAgent.setCreateTime(new Timestamp(System.currentTimeMillis()));

        testConversation = new AgentConversation();
        testConversation.setId(200L);
        testConversation.setAgentId(agentId);
        testConversation.setUserId(userId);
        testConversation.setConversationTopic("测试对话");
        testConversation.setDeleted(0);

        testMessage = new AgentMessage();
        testMessage.setId(300L);
        testMessage.setConversationId(200L);
        testMessage.setSenderType(1);
        testMessage.setContent("测试消息");
        // AgentMessage 没有 deleted 字段
    }

    // ==================== searchAgents 测试 ====================

    @Test
    void searchAgents_shouldReturnCachedResult_whenCacheHit() {
        // Given
        AgentSearchVO searchVO = new AgentSearchVO();
        searchVO.setPage(0);
        searchVO.setRows(10);

        PageResultVO<AgentVO> cachedResult = PageResultVO.of(1L, Collections.emptyList(), 0, 10);
        when(agentListCache.getIfPresent(anyString())).thenReturn(cachedResult);

        // When
        PageResultVO<AgentVO> result = agentService.searchAgents(userId, searchVO);

        // Then
        assertThat(result).isEqualTo(cachedResult);
        verify(agentRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void searchAgents_shouldQueryDatabase_whenCacheMiss() {
        // Given
        AgentSearchVO searchVO = new AgentSearchVO();
        searchVO.setPage(0);
        searchVO.setRows(10);

        when(agentListCache.getIfPresent(anyString())).thenReturn(null);
        Page<Agent> page = new PageImpl<>(List.of(testAgent));
        when(agentRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        // When
        PageResultVO<AgentVO> result = agentService.searchAgents(userId, searchVO);

        // Then
        assertThat(result.getTotal()).isEqualTo(1L);
        assertThat(result.getList()).hasSize(1);
        assertThat(result.getList().get(0).getAgentName()).isEqualTo("测试智能体");
        verify(agentListCache).put(anyString(), any());
    }

    @Test
    void searchAgents_shouldFilterByAgentType() {
        // Given
        AgentSearchVO searchVO = new AgentSearchVO();
        searchVO.setPage(0);
        searchVO.setRows(10);
        searchVO.setAgentType(0); // Integer type

        when(agentListCache.getIfPresent(anyString())).thenReturn(null);
        Page<Agent> page = new PageImpl<>(List.of(testAgent));
        when(agentRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        // When
        PageResultVO<AgentVO> result = agentService.searchAgents(userId, searchVO);

        // Then
        assertThat(result.getList()).hasSize(1);
        verify(agentRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    // ==================== getAgentById 测试 ====================

    @Test
    void getAgentById_shouldReturnCachedAgent_whenCacheHit() {
        // Given
        AgentVO cachedVO = new AgentVO();
        cachedVO.setId(agentId);
        cachedVO.setAgentName("缓存智能体");
        when(agentDetailCache.getIfPresent(agentId)).thenReturn(cachedVO);

        // When
        AgentVO result = agentService.getAgentById(agentId);

        // Then
        assertThat(result).isEqualTo(cachedVO);
        verify(agentRepository, never()).findByIdAndDeleted(anyLong(), anyInt());
    }

    @Test
    void getAgentById_shouldQueryDatabase_whenCacheMiss() {
        // Given
        when(agentDetailCache.getIfPresent(agentId)).thenReturn(null);
        when(agentRepository.findByIdAndDeleted(agentId, 0)).thenReturn(Optional.of(testAgent));

        // When
        AgentVO result = agentService.getAgentById(agentId);

        // Then
        assertThat(result.getId()).isEqualTo(agentId);
        assertThat(result.getAgentName()).isEqualTo("测试智能体");
        verify(agentDetailCache).put(eq(agentId), any());
    }

    @Test
    void getAgentById_shouldThrowException_whenAgentNotFound() {
        // Given
        when(agentDetailCache.getIfPresent(agentId)).thenReturn(null);
        when(agentRepository.findByIdAndDeleted(agentId, 0)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> agentService.getAgentById(agentId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DATA_NOT_FOUND);
    }

    // ==================== saveAgent 测试 ====================

    @Test
    void saveAgent_shouldCreateNewAgent_whenIdIsNull() {
        // Given
        AgentSaveVO saveVO = new AgentSaveVO();
        saveVO.setAgentName("新智能体");
        saveVO.setAgentType(0); // Integer type
        saveVO.setDescription("新描述");

        Agent savedAgent = new Agent();
        savedAgent.setId(999L);
        when(agentRepository.save(any(Agent.class))).thenReturn(savedAgent);

        // When
        long result = agentService.saveAgent(userId, saveVO);

        // Then
        assertThat(result).isEqualTo(999L);
        verify(agentRepository).save(any(Agent.class));
        verify(agentListCache).invalidateAll();
    }

    @Test
    void saveAgent_shouldUpdateExistingAgent_whenIdProvided() {
        // Given
        AgentSaveVO saveVO = new AgentSaveVO();
        saveVO.setId(agentId);
        saveVO.setAgentName("更新智能体");
        saveVO.setAgentType(0); // Integer type

        when(agentRepository.findByIdAndDeleted(agentId, 0)).thenReturn(Optional.of(testAgent));
        when(agentRepository.save(any(Agent.class))).thenReturn(testAgent);

        // When
        long result = agentService.saveAgent(userId, saveVO);

        // Then
        assertThat(result).isEqualTo(agentId);
        verify(agentDetailCache).invalidate(agentId);
        verify(agentListCache).invalidateAll();
    }

    @Test
    void saveAgent_shouldThrowException_whenAgentNotFound() {
        // Given
        AgentSaveVO saveVO = new AgentSaveVO();
        saveVO.setId(agentId);
        saveVO.setAgentName("更新智能体");

        when(agentRepository.findByIdAndDeleted(agentId, 0)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> agentService.saveAgent(userId, saveVO))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DATA_NOT_FOUND);
    }

    // ==================== deleteAgent 测试（P0-2: 数据隔离）====================

    @Test
    void deleteAgent_shouldDeleteAgent_whenOwnerMatches() {
        // Given
        when(agentRepository.findByIdAndDeleted(agentId, 0)).thenReturn(Optional.of(testAgent));
        when(agentRepository.save(any(Agent.class))).thenReturn(testAgent);

        // When
        agentService.deleteAgent(agentId, userId);

        // Then
        verify(agentRepository).save(argThat(agent -> agent.getDeleted() == 1));
        verify(agentDetailCache).invalidate(agentId);
        verify(agentListCache).invalidateAll();
    }

    @Test
    void deleteAgent_shouldThrowException_whenOwnerMismatch() {
        // Given
        Long otherUserId = 999L;
        when(agentRepository.findByIdAndDeleted(agentId, 0)).thenReturn(Optional.of(testAgent));

        // When & Then
        assertThatThrownBy(() -> agentService.deleteAgent(agentId, otherUserId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN)
                .hasMessageContaining("无权限删除该智能体");
    }

    @Test
    void deleteAgent_shouldThrowException_whenAgentNotFound() {
        // Given
        when(agentRepository.findByIdAndDeleted(agentId, 0)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> agentService.deleteAgent(agentId, userId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DATA_NOT_FOUND);
    }

    // ==================== updateAgentStatus 测试 ====================

    @Test
    void updateAgentStatus_shouldUpdateStatus_whenAgentExists() {
        // Given
        Integer newStatus = 0;
        when(agentRepository.findByIdAndDeleted(agentId, 0)).thenReturn(Optional.of(testAgent));

        // When
        agentService.updateAgentStatus(agentId, newStatus);

        // Then
        verify(agentRepository).updateStatus(agentId, newStatus);
        verify(agentDetailCache).invalidate(agentId);
        verify(agentListCache).invalidateAll();
    }

    @Test
    void updateAgentStatus_shouldThrowException_whenAgentNotFound() {
        // Given
        when(agentRepository.findByIdAndDeleted(agentId, 0)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> agentService.updateAgentStatus(agentId, 0))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DATA_NOT_FOUND);
    }

    // ==================== createConversation 测试 ====================

    @Test
    void createConversation_shouldCreateConversation_whenAgentExists() {
        // Given
        String topic = "新对话";
        when(agentRepository.findByIdAndDeleted(agentId, 0)).thenReturn(Optional.of(testAgent));
        when(conversationRepository.save(any(AgentConversation.class))).thenReturn(testConversation);

        // When
        long result = agentService.createConversation(agentId, userId, topic);

        // Then
        assertThat(result).isEqualTo(200L);
        verify(agentRepository).incrementConversationCount(agentId);
        verify(conversationRepository).save(any(AgentConversation.class));
    }

    @Test
    void createConversation_shouldThrowException_whenAgentNotFound() {
        // Given
        when(agentRepository.findByIdAndDeleted(agentId, 0)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> agentService.createConversation(agentId, userId, "新对话"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DATA_NOT_FOUND);
    }

    // ==================== deleteConversation 测试（P0-2: 数据隔离）====================

    @Test
    void deleteConversation_shouldDeleteConversation_whenOwnerMatches() {
        // Given
        Long conversationId = 200L;
        when(conversationRepository.findByIdAndDeleted(conversationId, 0))
                .thenReturn(Optional.of(testConversation));
        when(conversationRepository.save(any(AgentConversation.class))).thenReturn(testConversation);

        // When
        agentService.deleteConversation(conversationId, userId);

        // Then
        verify(conversationRepository).save(argThat(conv -> conv.getDeleted() == 1));
    }

    @Test
    void deleteConversation_shouldThrowException_whenOwnerMismatch() {
        // Given
        Long conversationId = 200L;
        Long otherUserId = 999L;
        when(conversationRepository.findByIdAndDeleted(conversationId, 0))
                .thenReturn(Optional.of(testConversation));

        // When & Then
        assertThatThrownBy(() -> agentService.deleteConversation(conversationId, otherUserId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN)
                .hasMessageContaining("无权限删除该对话");
    }

    // ==================== sendMessage 测试 ====================

    @Test
    void sendMessage_shouldSaveMessage_whenConversationExists() {
        // Given
        Long conversationId = 200L;
        String content = "测试消息";
        when(conversationRepository.findByIdAndDeleted(conversationId, 0))
                .thenReturn(Optional.of(testConversation));
        when(messageRepository.save(any(AgentMessage.class))).thenReturn(testMessage);

        // When
        long result = agentService.sendMessage(conversationId, 1, content, 100);

        // Then
        assertThat(result).isEqualTo(300L);
        verify(messageRepository).save(any(AgentMessage.class));
        verify(conversationRepository).incrementMessageCount(eq(conversationId), any(Timestamp.class));
    }

    @Test
    void sendMessage_shouldThrowException_whenConversationNotFound() {
        // Given
        Long conversationId = 200L;
        when(conversationRepository.findByIdAndDeleted(conversationId, 0))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> agentService.sendMessage(conversationId, 1, "测试", 100))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DATA_NOT_FOUND);
    }
}
