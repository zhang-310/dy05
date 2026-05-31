package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.live.entity.LiveSession;
import cn.gaifan.douyinOperations.module.live.repository.LiveSessionRepository;
import cn.gaifan.douyinOperations.module.live.vo.LiveSessionSaveVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveSessionSearchVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveSessionVO;
import com.github.benmanes.caffeine.cache.Cache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.jpa.domain.Specification;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * LiveSessionServiceImpl 单元测试
 *
 * 测试覆盖：
 * - 场次创建与更新
 * - 状态流转
 * - 数据隔离（userId 过滤）
 * - 分页查询
 * - 缓存行为
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("直播场次服务测试")
class LiveSessionServiceImplTest {

    @Mock
    private LiveSessionRepository liveSessionRepository;

    @Mock
    private Cache<String, Object> liveSessionListCache;

    @Mock
    private Cache<Long, Object> liveSessionDetailCache;

    @InjectMocks
    private LiveSessionServiceImpl liveSessionService;

    private LiveSession testSession;
    private LiveSessionSaveVO testSaveVO;
    private static final Long TEST_USER_ID = 1001L;
    private static final Long TEST_SESSION_ID = 2001L;

    @BeforeEach
    void setUp() {
        // 准备测试数据
        testSession = new LiveSession();
        testSession.setId(TEST_SESSION_ID);
        testSession.setUserId(TEST_USER_ID);
        testSession.setLiveTitle("测试直播场次");
        testSession.setLiveDescription("测试描述");
        testSession.setPersonaId(3001L);
        testSession.setScriptStyle("专业");
        testSession.setSessionType("品牌专场");
        testSession.setLiveFormat("单人");
        testSession.setScheduledEndTime(new Timestamp(System.currentTimeMillis() + 3600_000L));
        testSession.setStatus(0); // 0=draft
        testSession.setDeleted(0);
        testSession.setCreateTime(new Timestamp(System.currentTimeMillis()));
        testSession.setUpdateTime(new Timestamp(System.currentTimeMillis()));

        testSaveVO = new LiveSessionSaveVO();
        testSaveVO.setUserId(TEST_USER_ID);
        testSaveVO.setLiveTitle("新直播场次");
        testSaveVO.setLiveDescription("新描述");
    }

    // ==================== 查询测试 ====================

    @Test
    @DisplayName("分页查询 - 按用户ID查询")
    void testSearch_ByUserId() {
        // Arrange
        LiveSessionSearchVO searchVO = new LiveSessionSearchVO();
        searchVO.setUserId(TEST_USER_ID);
        searchVO.setPage(0);
        searchVO.setRows(20);

        when(liveSessionListCache.getIfPresent(anyString())).thenReturn(null);

        List<LiveSession> sessions = Arrays.asList(testSession);
        Page<LiveSession> page = new PageImpl<>(sessions);

        when(liveSessionRepository.findAll(any(Specification.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(page);

        // Act
        PageResultVO<LiveSessionVO> result = liveSessionService.search(searchVO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getList()).hasSize(1);
        assertThat(result.getList().get(0).getId()).isEqualTo(TEST_SESSION_ID);
        assertThat(result.getList().get(0).getUserId()).isEqualTo(TEST_USER_ID);

        verify(liveSessionRepository).findAll(any(Specification.class), any(org.springframework.data.domain.Pageable.class));
        verify(liveSessionListCache).put(anyString(), any());
    }

    @Test
    @DisplayName("分页查询 - 缓存命中")
    void testSearch_CacheHit() {
        // Arrange
        LiveSessionSearchVO searchVO = new LiveSessionSearchVO();
        searchVO.setUserId(TEST_USER_ID);
        searchVO.setPage(0);
        searchVO.setRows(20);

        PageResultVO<LiveSessionVO> cachedResult = new PageResultVO<>();
        cachedResult.setTotal(1L);
        cachedResult.setList(Arrays.asList(new LiveSessionVO()));

        when(liveSessionListCache.getIfPresent(anyString())).thenReturn(cachedResult);

        // Act
        PageResultVO<LiveSessionVO> result = liveSessionService.search(searchVO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isSameAs(cachedResult);

        verify(liveSessionListCache).getIfPresent(anyString());
        verify(liveSessionRepository, never()).findAll(any(Specification.class), any(org.springframework.data.domain.Pageable.class));
    }

    @Test
    @DisplayName("分页查询 - 按状态查询")
    void testSearch_ByStatus() {
        // Arrange
        LiveSessionSearchVO searchVO = new LiveSessionSearchVO();
        searchVO.setUserId(TEST_USER_ID);
        searchVO.setStatus(0); // 0=draft
        searchVO.setPage(0);
        searchVO.setRows(20);

        when(liveSessionListCache.getIfPresent(anyString())).thenReturn(null);

        List<LiveSession> sessions = Arrays.asList(testSession);
        Page<LiveSession> page = new PageImpl<>(sessions);

        when(liveSessionRepository.findAll(any(Specification.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(page);

        // Act
        PageResultVO<LiveSessionVO> result = liveSessionService.search(searchVO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getList().get(0).getStatus()).isEqualTo(0);
    }

    @Test
    @DisplayName("分页查询 - 关键词搜索")
    void testSearch_ByKeyword() {
        // Arrange
        LiveSessionSearchVO searchVO = new LiveSessionSearchVO();
        searchVO.setUserId(TEST_USER_ID);
        searchVO.setKeyword("测试");
        searchVO.setPage(0);
        searchVO.setRows(20);

        when(liveSessionListCache.getIfPresent(anyString())).thenReturn(null);

        List<LiveSession> sessions = Arrays.asList(testSession);
        Page<LiveSession> page = new PageImpl<>(sessions);

        when(liveSessionRepository.findAll(any(Specification.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(page);

        // Act
        PageResultVO<LiveSessionVO> result = liveSessionService.search(searchVO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTotal()).isEqualTo(1);
    }

    @Test
    @DisplayName("分页查询 - 空结果")
    void testSearch_EmptyResult() {
        // Arrange
        LiveSessionSearchVO searchVO = new LiveSessionSearchVO();
        searchVO.setUserId(TEST_USER_ID);
        searchVO.setPage(0);
        searchVO.setRows(20);

        when(liveSessionListCache.getIfPresent(anyString())).thenReturn(null);

        Page<LiveSession> emptyPage = new PageImpl<>(Collections.emptyList());

        when(liveSessionRepository.findAll(any(Specification.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(emptyPage);

        // Act
        PageResultVO<LiveSessionVO> result = liveSessionService.search(searchVO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTotal()).isZero();
        assertThat(result.getList()).isEmpty();
    }

    // ==================== 单条查询测试 ====================

    @Test
    @DisplayName("根据ID查询 - 成功")
    void testGetById_Success() {
        // Arrange
        when(liveSessionDetailCache.getIfPresent(TEST_SESSION_ID)).thenReturn(null);
        when(liveSessionRepository.findByIdAndDeleted(TEST_SESSION_ID, 0))
                .thenReturn(Optional.of(testSession));

        // Act
        LiveSessionVO result = liveSessionService.getById(TEST_SESSION_ID);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(TEST_SESSION_ID);
        assertThat(result.getLiveTitle()).isEqualTo("测试直播场次");
        assertThat(result.getPersonaId()).isEqualTo(3001L);
        assertThat(result.getScriptStyle()).isEqualTo("专业");
        assertThat(result.getSessionType()).isEqualTo("品牌专场");
        assertThat(result.getLiveFormat()).isEqualTo("单人");
        assertThat(result.getScheduledEndTime()).isNotNull();

        verify(liveSessionRepository).findByIdAndDeleted(TEST_SESSION_ID, 0);
        verify(liveSessionDetailCache).put(eq(TEST_SESSION_ID), any());
    }

    @Test
    @DisplayName("根据ID查询 - 缓存命中")
    void testGetById_CacheHit() {
        // Arrange
        LiveSessionVO cachedVO = new LiveSessionVO();
        cachedVO.setId(TEST_SESSION_ID);
        cachedVO.setLiveTitle("缓存的场次");

        when(liveSessionDetailCache.getIfPresent(TEST_SESSION_ID)).thenReturn(cachedVO);

        // Act
        LiveSessionVO result = liveSessionService.getById(TEST_SESSION_ID);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isSameAs(cachedVO);

        verify(liveSessionDetailCache).getIfPresent(TEST_SESSION_ID);
        verify(liveSessionRepository, never()).findById(any());
    }

    @Test
    @DisplayName("根据ID查询 - 不存在")
    void testGetById_NotFound() {
        // Arrange
        when(liveSessionDetailCache.getIfPresent(TEST_SESSION_ID)).thenReturn(null);
        when(liveSessionRepository.findByIdAndDeleted(TEST_SESSION_ID, 0))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> liveSessionService.getById(TEST_SESSION_ID))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DATA_NOT_FOUND)
                .hasMessageContaining("直播场次不存在");

        verify(liveSessionRepository).findByIdAndDeleted(TEST_SESSION_ID, 0);
    }

    @Test
    @DisplayName("根据ID查询 - 无效ID")
    void testGetById_InvalidId() {
        // Act & Assert
        assertThatThrownBy(() -> liveSessionService.getById(null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_FAIL)
                .hasMessageContaining("直播场次 ID 无效");

        assertThatThrownBy(() -> liveSessionService.getById(0L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_FAIL);

        verify(liveSessionRepository, never()).findById(any());
    }

    // ==================== 保存测试 ====================

    @Test
    @DisplayName("保存 - 新建场次")
    void testSave_Create() {
        // Arrange
        when(liveSessionRepository.save(any(LiveSession.class)))
                .thenAnswer(invocation -> {
                    LiveSession session = invocation.getArgument(0);
                    session.setId(TEST_SESSION_ID);
                    return session;
                });

        // Act
        long id = liveSessionService.save(testSaveVO);

        // Assert
        assertThat(id).isEqualTo(TEST_SESSION_ID);

        verify(liveSessionRepository).save(argThat(session ->
                session.getUserId().equals(TEST_USER_ID) &&
                session.getLiveTitle().equals("新直播场次") &&
                session.getLiveDescription().equals("新描述") &&
                session.getDeleted().equals(0) &&
                session.getViewers().equals(0) &&
                session.getLikes().equals(0L) &&
                session.getAutoSyncEnabled().equals(0) &&
                session.getCreateTime() != null &&
                session.getUpdateTime() != null
        ));
        verify(liveSessionListCache).invalidateAll();
    }

    @Test
    @DisplayName("保存 - 更新场次")
    void testSave_Update() {
        // Arrange
        testSaveVO.setId(TEST_SESSION_ID);
        testSaveVO.setLiveTitle("更新后的标题");

        when(liveSessionRepository.findByIdAndDeleted(TEST_SESSION_ID, 0))
                .thenReturn(Optional.of(testSession));
        when(liveSessionRepository.save(any(LiveSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        long id = liveSessionService.save(testSaveVO);

        // Assert
        assertThat(id).isEqualTo(TEST_SESSION_ID);

        verify(liveSessionRepository).findByIdAndDeleted(TEST_SESSION_ID, 0);
        verify(liveSessionRepository).save(argThat(session ->
                session.getId().equals(TEST_SESSION_ID) &&
                session.getLiveTitle().equals("更新后的标题")
        ));
        verify(liveSessionDetailCache).invalidate(TEST_SESSION_ID);
        verify(liveSessionListCache).invalidateAll();
    }

    @Test
    @DisplayName("保存 - 无效用户ID")
    void testSave_InvalidUserId() {
        // Arrange
        testSaveVO.setUserId(null);

        // Act & Assert
        assertThatThrownBy(() -> liveSessionService.save(testSaveVO))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_FAIL)
                .hasMessageContaining("用户 ID 无效");

        verify(liveSessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("保存 - 更新不存在的场次")
    void testSave_UpdateNotFound() {
        // Arrange
        testSaveVO.setId(TEST_SESSION_ID);

        when(liveSessionRepository.findByIdAndDeleted(TEST_SESSION_ID, 0))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> liveSessionService.save(testSaveVO))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DATA_NOT_FOUND)
                .hasMessageContaining("直播场次不存在");

        verify(liveSessionRepository).findByIdAndDeleted(TEST_SESSION_ID, 0);
        verify(liveSessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("克隆场次 - 成功后清理列表缓存")
    void testCloneSession_InvalidatesListCache() {
        // Arrange
        when(liveSessionRepository.findByIdAndDeleted(TEST_SESSION_ID, 0))
                .thenReturn(Optional.of(testSession));
        when(liveSessionRepository.save(any(LiveSession.class)))
                .thenAnswer(invocation -> {
                    LiveSession session = invocation.getArgument(0);
                    session.setId(3002L);
                    return session;
                });

        // Act
        long cloneId = liveSessionService.cloneSession(TEST_SESSION_ID, TEST_USER_ID, null);

        // Assert
        assertThat(cloneId).isEqualTo(3002L);
        verify(liveSessionRepository).save(argThat(session ->
                session.getUserId().equals(TEST_USER_ID) &&
                session.getLiveTitle().equals("测试直播场次 (副本)") &&
                session.getPersonaId().equals(3001L) &&
                session.getScriptStyle().equals("专业") &&
                session.getSessionType().equals("品牌专场") &&
                session.getLiveFormat().equals("单人") &&
                session.getDeleted().equals(0) &&
                session.getStatus().equals(0)
        ));
        verify(liveSessionListCache).invalidateAll();
    }

    // ==================== 删除测试 ====================

    @Test
    @DisplayName("删除 - 逻辑删除成功")
    void testDelete_Success() {
        // Arrange
        when(liveSessionRepository.findByIdAndDeleted(TEST_SESSION_ID, 0))
                .thenReturn(Optional.of(testSession));
        when(liveSessionRepository.save(any(LiveSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        liveSessionService.delete(TEST_SESSION_ID);

        // Assert
        verify(liveSessionRepository).findByIdAndDeleted(TEST_SESSION_ID, 0);
        verify(liveSessionRepository).save(argThat(session ->
                session.getId().equals(TEST_SESSION_ID) &&
                session.getDeleted().equals(1)
        ));
        verify(liveSessionDetailCache).invalidate(TEST_SESSION_ID);
        verify(liveSessionListCache).invalidateAll();
    }

    @Test
    @DisplayName("删除 - 场次不存在")
    void testDelete_NotFound() {
        // Arrange
        when(liveSessionRepository.findByIdAndDeleted(TEST_SESSION_ID, 0))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> liveSessionService.delete(TEST_SESSION_ID))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DATA_NOT_FOUND)
                .hasMessageContaining("直播场次不存在");

        verify(liveSessionRepository).findByIdAndDeleted(TEST_SESSION_ID, 0);
        verify(liveSessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("删除 - 无效ID")
    void testDelete_InvalidId() {
        // Act & Assert
        assertThatThrownBy(() -> liveSessionService.delete(null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_FAIL)
                .hasMessageContaining("直播场次 ID 无效");

        verify(liveSessionRepository, never()).findById(any());
        verify(liveSessionRepository, never()).save(any());
    }

    // ==================== 状态流转测试 ====================

    @Test
    @DisplayName("状态流转 - draft → live")
    void testStatusTransition_DraftToLive() {
        // Arrange
        testSaveVO.setId(TEST_SESSION_ID);
        testSaveVO.setStatus(1); // 1=live

        when(liveSessionRepository.findByIdAndDeleted(TEST_SESSION_ID, 0))
                .thenReturn(Optional.of(testSession));
        when(liveSessionRepository.save(any(LiveSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        long id = liveSessionService.save(testSaveVO);

        // Assert
        assertThat(id).isEqualTo(TEST_SESSION_ID);

        verify(liveSessionRepository).save(argThat(session ->
                session.getStatus().equals(1)
        ));
    }

    @Test
    @DisplayName("状态流转 - live → ended")
    void testStatusTransition_LiveToEnded() {
        // Arrange
        testSession.setStatus(1); // 1=live
        testSaveVO.setId(TEST_SESSION_ID);
        testSaveVO.setStatus(2); // 2=ended

        when(liveSessionRepository.findByIdAndDeleted(TEST_SESSION_ID, 0))
                .thenReturn(Optional.of(testSession));
        when(liveSessionRepository.save(any(LiveSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        long id = liveSessionService.save(testSaveVO);

        // Assert
        assertThat(id).isEqualTo(TEST_SESSION_ID);

        verify(liveSessionRepository).save(argThat(session ->
                session.getStatus().equals(2)
        ));
    }

    // ==================== 数据隔离测试 ====================

    @Test
    @DisplayName("数据隔离 - 只查询当前用户的场次")
    void testDataIsolation_OnlyCurrentUser() {
        // Arrange
        LiveSessionSearchVO searchVO = new LiveSessionSearchVO();
        searchVO.setUserId(TEST_USER_ID);
        searchVO.setPage(0);
        searchVO.setRows(20);

        when(liveSessionListCache.getIfPresent(anyString())).thenReturn(null);

        List<LiveSession> sessions = Arrays.asList(testSession);
        Page<LiveSession> page = new PageImpl<>(sessions);

        when(liveSessionRepository.findAll(any(Specification.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(page);

        // Act
        PageResultVO<LiveSessionVO> result = liveSessionService.search(searchVO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getList()).allMatch(vo -> vo.getUserId().equals(TEST_USER_ID));
    }
}
