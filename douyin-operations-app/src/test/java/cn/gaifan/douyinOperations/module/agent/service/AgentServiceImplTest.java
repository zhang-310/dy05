package cn.gaifan.douyinOperations.module.agent.service;

import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.agent.entity.Agent;
import cn.gaifan.douyinOperations.module.agent.repository.AgentRepository;
import cn.gaifan.douyinOperations.module.agent.repository.AgentConversationRepository;
import cn.gaifan.douyinOperations.module.agent.repository.AgentMessageRepository;
import cn.gaifan.douyinOperations.module.agent.service.impl.AgentServiceImpl;
import cn.gaifan.douyinOperations.module.agent.vo.AgentSearchVO;
import cn.gaifan.douyinOperations.module.agent.vo.AgentSaveVO;
import cn.gaifan.douyinOperations.module.agent.vo.AgentVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AgentService 单元测试")
class AgentServiceImplTest {

    @Mock
    private AgentRepository agentRepository;

    @Mock
    private AgentConversationRepository conversationRepository;

    @Mock
    private AgentMessageRepository messageRepository;

    @InjectMocks
    private AgentServiceImpl agentService;

    private Agent mockAgent;
    private Long userId = 1L;

    @BeforeEach
    void setUp() {
        mockAgent = new Agent();
        mockAgent.setId(1L);
        mockAgent.setUserId(userId);
        mockAgent.setAgentName("测试智能体");
        mockAgent.setAgentType(1); // 1=内容生成
        mockAgent.setDescription("测试描述");
        mockAgent.setSystemPrompt("你是一个测试助手");
        mockAgent.setStatus(1);
        mockAgent.setDeleted(0);
    }

    @Test
    @DisplayName("搜索智能体 - 应返回分页结果")
    void searchAgents_shouldReturnPageResult() {
        // Given
        AgentSearchVO searchVO = new AgentSearchVO();
        searchVO.setPage(0);
        searchVO.setRows(10);

        Page<Agent> page = new PageImpl<>(List.of(mockAgent));
        when(agentRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        // When
        PageResultVO<AgentVO> result = agentService.searchAgents(userId, searchVO);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotal()).isEqualTo(1L);
        assertThat(result.getList()).hasSize(1);
        assertThat(result.getList().get(0).getAgentName()).isEqualTo("测试智能体");
        verify(agentRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("搜索智能体 - 按名称过滤")
    void searchAgents_withNameFilter_shouldReturnFilteredResult() {
        // Given
        AgentSearchVO searchVO = new AgentSearchVO();
        searchVO.setPage(0);
        searchVO.setRows(10);
        searchVO.setAgentName("测试");

        Page<Agent> page = new PageImpl<>(List.of(mockAgent));
        when(agentRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        // When
        PageResultVO<AgentVO> result = agentService.searchAgents(userId, searchVO);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotal()).isEqualTo(1L);
        verify(agentRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("获取智能体详情 - 应返回智能体信息")
    void getAgentById_shouldReturnAgent() {
        // Given
        when(agentRepository.findByIdAndDeleted(1L, 0))
                .thenReturn(Optional.of(mockAgent));

        // When
        AgentVO result = agentService.getAgentById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getAgentName()).isEqualTo("测试智能体");
        verify(agentRepository).findByIdAndDeleted(1L, 0);
    }

    @Test
    @DisplayName("获取智能体详情 - 智能体不存在应抛出异常")
    void getAgentById_notFound_shouldThrowException() {
        // Given
        when(agentRepository.findByIdAndDeleted(999L, 0))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> agentService.getAgentById(999L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("智能体不存在");
        verify(agentRepository).findByIdAndDeleted(999L, 0);
    }

    @Test
    @DisplayName("保存智能体 - 新建应返回ID")
    void saveAgent_create_shouldReturnId() {
        // Given
        AgentSaveVO saveVO = new AgentSaveVO();
        saveVO.setAgentName("新智能体");
        saveVO.setAgentType(1); // 1=内容生成
        saveVO.setDescription("新描述");
        saveVO.setSystemPrompt("新提示词");

        when(agentRepository.save(any(Agent.class)))
                .thenReturn(mockAgent);

        // When
        long result = agentService.saveAgent(userId, saveVO);

        // Then
        assertThat(result).isEqualTo(1L);
        verify(agentRepository).save(any(Agent.class));
    }

    @Test
    @DisplayName("保存智能体 - 更新应返回ID")
    void saveAgent_update_shouldReturnId() {
        // Given
        AgentSaveVO saveVO = new AgentSaveVO();
        saveVO.setId(1L);
        saveVO.setAgentName("更新智能体");
        saveVO.setAgentType(1); // 1=内容生成

        when(agentRepository.findByIdAndDeleted(1L, 0))
                .thenReturn(Optional.of(mockAgent));
        when(agentRepository.save(any(Agent.class)))
                .thenReturn(mockAgent);

        // When
        long result = agentService.saveAgent(userId, saveVO);

        // Then
        assertThat(result).isEqualTo(1L);
        verify(agentRepository).findByIdAndDeleted(1L, 0);
        verify(agentRepository).save(any(Agent.class));
    }

    @Test
    @DisplayName("保存智能体 - 更新不存在的智能体应抛出异常")
    void saveAgent_updateNotFound_shouldThrowException() {
        // Given
        AgentSaveVO saveVO = new AgentSaveVO();
        saveVO.setId(999L);
        saveVO.setAgentName("更新智能体");
        saveVO.setAgentType(1); // 1=内容生成

        when(agentRepository.findByIdAndDeleted(999L, 0))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> agentService.saveAgent(userId, saveVO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("智能体不存在");
        verify(agentRepository).findByIdAndDeleted(999L, 0);
        verify(agentRepository, never()).save(any(Agent.class));
    }

    @Test
    @DisplayName("删除智能体 - 应标记为已删除")
    void deleteAgent_shouldMarkAsDeleted() {
        // Given
        when(agentRepository.findByIdAndDeleted(1L, 0))
                .thenReturn(Optional.of(mockAgent));
        when(agentRepository.save(any(Agent.class)))
                .thenReturn(mockAgent);

        // When
        agentService.deleteAgent(1L);

        // Then
        verify(agentRepository).findByIdAndDeleted(1L, 0);
        verify(agentRepository).save(argThat(agent -> agent.getDeleted() == 1));
    }

    @Test
    @DisplayName("删除智能体 - 智能体不存在应抛出异常")
    void deleteAgent_notFound_shouldThrowException() {
        // Given
        when(agentRepository.findByIdAndDeleted(999L, 0))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> agentService.deleteAgent(999L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("智能体不存在");
        verify(agentRepository).findByIdAndDeleted(999L, 0);
        verify(agentRepository, never()).save(any(Agent.class));
    }

    @Test
    @DisplayName("更新智能体状态 - 应更新状态")
    void updateAgentStatus_shouldUpdateStatus() {
        // Given
        when(agentRepository.findByIdAndDeleted(1L, 0))
                .thenReturn(Optional.of(mockAgent));

        // When
        agentService.updateAgentStatus(1L, 0);

        // Then
        verify(agentRepository).findByIdAndDeleted(1L, 0);
        verify(agentRepository).updateStatus(1L, 0);
    }

    @Test
    @DisplayName("更新智能体状态 - 智能体不存在应抛出异常")
    void updateAgentStatus_notFound_shouldThrowException() {
        // Given
        when(agentRepository.findByIdAndDeleted(999L, 0))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> agentService.updateAgentStatus(999L, 0))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("智能体不存在");
        verify(agentRepository).findByIdAndDeleted(999L, 0);
        verify(agentRepository, never()).updateStatus(anyLong(), anyInt());
    }
}
