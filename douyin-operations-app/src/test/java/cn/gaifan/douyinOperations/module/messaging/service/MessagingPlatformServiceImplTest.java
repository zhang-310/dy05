package cn.gaifan.douyinOperations.module.messaging.service;

import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.messaging.entity.MsgPlatformConfig;
import cn.gaifan.douyinOperations.module.messaging.repository.MsgPlatformConfigRepository;
import cn.gaifan.douyinOperations.module.messaging.service.impl.MessagingPlatformServiceImpl;
import cn.gaifan.douyinOperations.module.messaging.vo.MsgPlatformConfigSearchVO;
import cn.gaifan.douyinOperations.module.messaging.vo.MsgPlatformConfigSaveVO;
import cn.gaifan.douyinOperations.module.messaging.vo.MsgPlatformConfigVO;
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
@DisplayName("MessagingPlatformService 单元测试")
class MessagingPlatformServiceImplTest {

    @Mock
    private MsgPlatformConfigRepository repository;

    @InjectMocks
    private MessagingPlatformServiceImpl messagingPlatformService;

    private MsgPlatformConfig mockConfig;
    private Long ownerId = 1L;

    @BeforeEach
    void setUp() {
        mockConfig = new MsgPlatformConfig();
        mockConfig.setId(1L);
        mockConfig.setOwnerId(ownerId);
        mockConfig.setPlatform("wecom");
        mockConfig.setAppId("test-app-id");
        mockConfig.setCorpId("test-corp-id");
        mockConfig.setSecret("test-secret");
        mockConfig.setCallbackToken("test-token");
        mockConfig.setCallbackEncodingAesKey("test-aes-key");
        mockConfig.setAgentId(1001L);
        mockConfig.setStatus(1);
        mockConfig.setDeleted(0);
    }

    @Test
    @DisplayName("搜索配置 - 应返回分页结果")
    void search_shouldReturnPageResult() {
        // Given
        MsgPlatformConfigSearchVO searchVO = new MsgPlatformConfigSearchVO();
        searchVO.setPage(0);
        searchVO.setRows(10);
        searchVO.setOwnerId(ownerId);

        Page<MsgPlatformConfig> page = new PageImpl<>(List.of(mockConfig));
        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        // When
        PageResultVO<MsgPlatformConfigVO> result = messagingPlatformService.search(searchVO);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotal()).isEqualTo(1L);
        assertThat(result.getList()).hasSize(1);
        verify(repository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("搜索配置 - 按平台过滤")
    void search_withPlatformFilter_shouldReturnFilteredResult() {
        // Given
        MsgPlatformConfigSearchVO searchVO = new MsgPlatformConfigSearchVO();
        searchVO.setPage(0);
        searchVO.setRows(10);
        searchVO.setPlatform("wecom");

        Page<MsgPlatformConfig> page = new PageImpl<>(List.of(mockConfig));
        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        // When
        PageResultVO<MsgPlatformConfigVO> result = messagingPlatformService.search(searchVO);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotal()).isEqualTo(1L);
        verify(repository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("获取配置详情 - 应返回配置信息")
    void getById_shouldReturnConfig() {
        // Given
        when(repository.findByIdAndDeleted(1L, 0))
                .thenReturn(Optional.of(mockConfig));

        // When
        MsgPlatformConfigVO result = messagingPlatformService.getById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getPlatform()).isEqualTo("wecom");
        verify(repository).findByIdAndDeleted(1L, 0);
    }

    @Test
    @DisplayName("获取配置详情 - 配置不存在应抛出异常")
    void getById_notFound_shouldThrowException() {
        // Given
        when(repository.findByIdAndDeleted(999L, 0))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> messagingPlatformService.getById(999L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("配置不存在");
        verify(repository).findByIdAndDeleted(999L, 0);
    }

    @Test
    @DisplayName("保存配置 - 新建应返回ID")
    void save_create_shouldReturnId() {
        // Given
        MsgPlatformConfigSaveVO saveVO = new MsgPlatformConfigSaveVO();
        saveVO.setOwnerId(ownerId);
        saveVO.setPlatform("wecom");
        saveVO.setAppId("new-app-id");
        saveVO.setCorpId("new-corp-id");
        saveVO.setSecret("new-secret");
        saveVO.setCallbackToken("new-token");
        saveVO.setCallbackEncodingAesKey("new-aes-key");
        saveVO.setAgentId(1001L);

        when(repository.save(any(MsgPlatformConfig.class)))
                .thenReturn(mockConfig);

        // When
        long result = messagingPlatformService.save(saveVO);

        // Then
        assertThat(result).isEqualTo(1L);
        verify(repository).save(any(MsgPlatformConfig.class));
    }

    @Test
    @DisplayName("保存配置 - 更新应返回ID")
    void save_update_shouldReturnId() {
        // Given
        MsgPlatformConfigSaveVO saveVO = new MsgPlatformConfigSaveVO();
        saveVO.setId(1L);
        saveVO.setOwnerId(ownerId);
        saveVO.setPlatform("wecom");
        saveVO.setAppId("updated-app-id");
        saveVO.setCorpId("updated-corp-id");
        saveVO.setSecret("updated-secret");
        saveVO.setCallbackToken("updated-token");
        saveVO.setCallbackEncodingAesKey("updated-aes-key");
        saveVO.setAgentId(1002L);

        when(repository.findByIdAndDeleted(1L, 0))
                .thenReturn(Optional.of(mockConfig));
        when(repository.save(any(MsgPlatformConfig.class)))
                .thenReturn(mockConfig);

        // When
        long result = messagingPlatformService.save(saveVO);

        // Then
        assertThat(result).isEqualTo(1L);
        verify(repository).findByIdAndDeleted(1L, 0);
        verify(repository).save(any(MsgPlatformConfig.class));
    }

    @Test
    @DisplayName("保存配置 - 更新不存在的配置应抛出异常")
    void save_updateNotFound_shouldThrowException() {
        // Given
        MsgPlatformConfigSaveVO saveVO = new MsgPlatformConfigSaveVO();
        saveVO.setId(999L);
        saveVO.setOwnerId(ownerId);
        saveVO.setPlatform("wecom");
        saveVO.setAppId("app-id");
        saveVO.setCorpId("corp-id");
        saveVO.setSecret("secret");
        saveVO.setCallbackToken("token");
        saveVO.setCallbackEncodingAesKey("aes-key");
        saveVO.setAgentId(1003L);

        when(repository.findByIdAndDeleted(999L, 0))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> messagingPlatformService.save(saveVO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("配置不存在");
        verify(repository).findByIdAndDeleted(999L, 0);
        verify(repository, never()).save(any(MsgPlatformConfig.class));
    }

    @Test
    @DisplayName("删除配置 - 应标记为已删除")
    void delete_shouldMarkAsDeleted() {
        // Given
        when(repository.findByIdAndDeleted(1L, 0))
                .thenReturn(Optional.of(mockConfig));
        when(repository.save(any(MsgPlatformConfig.class)))
                .thenReturn(mockConfig);

        // When
        messagingPlatformService.delete(1L);

        // Then
        verify(repository).findByIdAndDeleted(1L, 0);
        verify(repository).save(argThat(config -> config.getDeleted() == 1));
    }

    @Test
    @DisplayName("删除配置 - 配置不存在应抛出异常")
    void delete_notFound_shouldThrowException() {
        // Given
        when(repository.findByIdAndDeleted(999L, 0))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> messagingPlatformService.delete(999L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("配置不存在");
        verify(repository).findByIdAndDeleted(999L, 0);
        verify(repository, never()).save(any(MsgPlatformConfig.class));
    }

    @Test
    @DisplayName("根据平台和 Token 获取配置 - 应返回配置")
    void getByPlatformAndToken_shouldReturnConfig() {
        // Given
        when(repository.findByPlatformAndCallbackTokenAndDeleted("wecom", "test-token", 0))
                .thenReturn(Optional.of(mockConfig));

        // When
        MsgPlatformConfigVO result = messagingPlatformService.getByPlatformAndToken("wecom", "test-token");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPlatform()).isEqualTo("wecom");
        verify(repository).findByPlatformAndCallbackTokenAndDeleted("wecom", "test-token", 0);
    }

    @Test
    @DisplayName("根据平台和 Token 获取配置 - 配置不存在应返回 null")
    void getByPlatformAndToken_notFound_shouldReturnNull() {
        // Given
        when(repository.findByPlatformAndCallbackTokenAndDeleted("wecom", "invalid-token", 0))
                .thenReturn(Optional.empty());

        // When
        MsgPlatformConfigVO result = messagingPlatformService.getByPlatformAndToken("wecom", "invalid-token");

        // Then
        assertThat(result).isNull();
        verify(repository).findByPlatformAndCallbackTokenAndDeleted("wecom", "invalid-token", 0);
    }

    @Test
    @DisplayName("根据平台和 Token 获取配置实体 - platform 为 null 应返回 null")
    void getConfigEntityByPlatformAndToken_nullPlatform_shouldReturnNull() {
        // When
        MsgPlatformConfig result = messagingPlatformService.getConfigEntityByPlatformAndToken(null, "test-token");

        // Then
        assertThat(result).isNull();
        verify(repository, never()).findByPlatformAndCallbackTokenAndDeleted(anyString(), anyString(), anyInt());
    }

    @Test
    @DisplayName("根据平台和 Token 获取配置实体 - token 为 null 应返回 null")
    void getConfigEntityByPlatformAndToken_nullToken_shouldReturnNull() {
        // When
        MsgPlatformConfig result = messagingPlatformService.getConfigEntityByPlatformAndToken("wecom", null);

        // Then
        assertThat(result).isNull();
        verify(repository, never()).findByPlatformAndCallbackTokenAndDeleted(anyString(), anyString(), anyInt());
    }

    @Test
    @DisplayName("根据平台和 Token 获取配置实体 - token 为空字符串应返回 null")
    void getConfigEntityByPlatformAndToken_blankToken_shouldReturnNull() {
        // When
        MsgPlatformConfig result = messagingPlatformService.getConfigEntityByPlatformAndToken("wecom", "  ");

        // Then
        assertThat(result).isNull();
        verify(repository, never()).findByPlatformAndCallbackTokenAndDeleted(anyString(), anyString(), anyInt());
    }
}
