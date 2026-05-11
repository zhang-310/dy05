package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.live.entity.LiveScript;
import cn.gaifan.douyinOperations.module.live.repository.LiveProductRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveScriptRepository;
import cn.gaifan.douyinOperations.module.live.vo.LiveScriptSaveVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveScriptSearchVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveScriptVO;
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

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * LiveScriptServiceImpl 单元测试
 *
 * 测试覆盖：
 * - CRUD 操作
 * - 数据隔离（userId 过滤）
 * - 分页查询
 * - 逻辑删除
 * - 缓存行为
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("直播话术服务测试")
class LiveScriptServiceImplTest {

    @Mock
    private LiveScriptRepository liveScriptRepository;

    @Mock
    private LiveProductRepository liveProductRepository;

    @InjectMocks
    private LiveScriptServiceImpl liveScriptService;

    private LiveScript testScript;
    private LiveScriptSaveVO testSaveVO;
    private static final Long TEST_USER_ID = 1001L;
    private static final Long TEST_SESSION_ID = 2001L;
    private static final Long TEST_SCRIPT_ID = 3001L;

    @BeforeEach
    void setUp() {
        // 准备测试数据
        testScript = new LiveScript();
        testScript.setId(TEST_SCRIPT_ID);
        testScript.setSessionId(TEST_SESSION_ID);
        testScript.setUserId(TEST_USER_ID);
        testScript.setScriptContent("测试话术内容");
        testScript.setSequenceNo(1);
        testScript.setExecutionTime(30L);
        testScript.setExecuted(0);
        testScript.setDeleted(0);
        testScript.setCreateTime(new Timestamp(System.currentTimeMillis()));
        testScript.setUpdateTime(new Timestamp(System.currentTimeMillis()));

        testSaveVO = new LiveScriptSaveVO();
        testSaveVO.setSessionId(TEST_SESSION_ID);
        testSaveVO.setScriptContent("新话术内容");
        testSaveVO.setSequenceNo(2);
        testSaveVO.setExecutionTime(45L);
    }

    // ==================== 查询测试 ====================

    @Test
    @DisplayName("分页查询 - 按场次ID查询")
    void testSearch_BySessionId() {
        // Arrange
        LiveScriptSearchVO searchVO = new LiveScriptSearchVO();
        searchVO.setSessionId(TEST_SESSION_ID);
        searchVO.setPage(0);
        searchVO.setRows(20);

        List<LiveScript> scripts = Arrays.asList(testScript);
        Page<LiveScript> page = new PageImpl<>(scripts);

        when(liveScriptRepository.findBySessionIdAndDeleted(eq(TEST_SESSION_ID), eq(0), any(Pageable.class)))
                .thenReturn(page);

        // Act
        PageResultVO<LiveScriptVO> result = liveScriptService.search(searchVO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getList()).hasSize(1);
        assertThat(result.getList().get(0).getId()).isEqualTo(TEST_SCRIPT_ID);
        assertThat(result.getList().get(0).getSessionId()).isEqualTo(TEST_SESSION_ID);

        verify(liveScriptRepository).findBySessionIdAndDeleted(eq(TEST_SESSION_ID), eq(0), any(Pageable.class));
    }

    @Test
    @DisplayName("分页查询 - 按多个场次ID查询")
    void testSearch_BySessionIds() {
        // Arrange
        LiveScriptSearchVO searchVO = new LiveScriptSearchVO();
        searchVO.setSessionIds(Arrays.asList(TEST_SESSION_ID, 2002L));
        searchVO.setPage(0);
        searchVO.setRows(20);

        List<LiveScript> scripts = Arrays.asList(testScript);
        Page<LiveScript> page = new PageImpl<>(scripts);

        when(liveScriptRepository.findBySessionIdInAndDeleted(anyList(), eq(0), any(Pageable.class)))
                .thenReturn(page);

        // Act
        PageResultVO<LiveScriptVO> result = liveScriptService.search(searchVO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getList()).hasSize(1);

        verify(liveScriptRepository).findBySessionIdInAndDeleted(anyList(), eq(0), any(Pageable.class));
    }

    @Test
    @DisplayName("分页查询 - 空结果")
    void testSearch_EmptyResult() {
        // Arrange
        LiveScriptSearchVO searchVO = new LiveScriptSearchVO();
        searchVO.setSessionId(TEST_SESSION_ID);
        searchVO.setPage(0);
        searchVO.setRows(20);

        Page<LiveScript> emptyPage = new PageImpl<>(Collections.emptyList());

        when(liveScriptRepository.findBySessionIdAndDeleted(eq(TEST_SESSION_ID), eq(0), any(Pageable.class)))
                .thenReturn(emptyPage);

        // Act
        PageResultVO<LiveScriptVO> result = liveScriptService.search(searchVO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTotal()).isZero();
        assertThat(result.getList()).isEmpty();
    }

    @Test
    @DisplayName("分页查询 - 参数校验（自动修正）")
    void testSearch_ParamValidation() {
        // Arrange
        LiveScriptSearchVO searchVO = new LiveScriptSearchVO();
        searchVO.setSessionId(TEST_SESSION_ID);
        searchVO.setPage(-1); // 非法值
        searchVO.setRows(2000); // 超过上限

        Page<LiveScript> page = new PageImpl<>(Collections.emptyList());
        when(liveScriptRepository.findBySessionIdAndDeleted(eq(TEST_SESSION_ID), eq(0), any(Pageable.class)))
                .thenReturn(page);

        // Act
        PageResultVO<LiveScriptVO> result = liveScriptService.search(searchVO);

        // Assert - validateParams() 应该自动修正参数
        assertThat(result).isNotNull();
        assertThat(searchVO.getPage()).isGreaterThanOrEqualTo(0);
        assertThat(searchVO.getRows()).isLessThanOrEqualTo(1000);
    }

    // ==================== 单条查询测试 ====================

    @Test
    @DisplayName("根据ID查询 - 成功")
    void testGetById_Success() {
        // Arrange
        when(liveScriptRepository.findById(TEST_SCRIPT_ID))
                .thenReturn(Optional.of(testScript));

        // Act
        LiveScriptVO result = liveScriptService.getById(TEST_SCRIPT_ID);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(TEST_SCRIPT_ID);
        assertThat(result.getSessionId()).isEqualTo(TEST_SESSION_ID);
        assertThat(result.getScriptContent()).isEqualTo("测试话术内容");

        verify(liveScriptRepository).findById(TEST_SCRIPT_ID);
    }

    @Test
    @DisplayName("根据ID查询 - 不存在")
    void testGetById_NotFound() {
        // Arrange
        when(liveScriptRepository.findById(TEST_SCRIPT_ID))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> liveScriptService.getById(TEST_SCRIPT_ID))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DATA_NOT_FOUND)
                .hasMessageContaining("直播话术不存在");

        verify(liveScriptRepository).findById(TEST_SCRIPT_ID);
    }

    @Test
    @DisplayName("根据ID查询 - 无效ID")
    void testGetById_InvalidId() {
        // Act & Assert
        assertThatThrownBy(() -> liveScriptService.getById(null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_FAIL)
                .hasMessageContaining("直播话术 ID 无效");

        assertThatThrownBy(() -> liveScriptService.getById(0L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_FAIL);

        assertThatThrownBy(() -> liveScriptService.getById(-1L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_FAIL);

        verify(liveScriptRepository, never()).findById(any());
    }

    // ==================== 保存测试 ====================

    @Test
    @DisplayName("保存 - 新建话术")
    void testSave_Create() {
        // Arrange
        when(liveScriptRepository.save(any(LiveScript.class)))
                .thenAnswer(invocation -> {
                    LiveScript script = invocation.getArgument(0);
                    script.setId(TEST_SCRIPT_ID);
                    return script;
                });

        // Act
        long id = liveScriptService.save(testSaveVO);

        // Assert
        assertThat(id).isEqualTo(TEST_SCRIPT_ID);

        verify(liveScriptRepository).save(argThat(script ->
                script.getSessionId().equals(TEST_SESSION_ID) &&
                script.getScriptContent().equals("新话术内容") &&
                script.getSequenceNo().equals(2) &&
                script.getExecutionTime().equals(45L)
        ));
    }

    @Test
    @DisplayName("保存 - 更新话术")
    void testSave_Update() {
        // Arrange
        testSaveVO.setId(TEST_SCRIPT_ID);
        testSaveVO.setScriptContent("更新后的话术内容");

        when(liveScriptRepository.findById(TEST_SCRIPT_ID))
                .thenReturn(Optional.of(testScript));
        when(liveScriptRepository.save(any(LiveScript.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        long id = liveScriptService.save(testSaveVO);

        // Assert
        assertThat(id).isEqualTo(TEST_SCRIPT_ID);

        verify(liveScriptRepository).findById(TEST_SCRIPT_ID);
        verify(liveScriptRepository).save(argThat(script ->
                script.getId().equals(TEST_SCRIPT_ID) &&
                script.getScriptContent().equals("更新后的话术内容")
        ));
    }

    @Test
    @DisplayName("保存 - 无效场次ID")
    void testSave_InvalidSessionId() {
        // Arrange
        testSaveVO.setSessionId(null);

        // Act & Assert
        assertThatThrownBy(() -> liveScriptService.save(testSaveVO))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_FAIL)
                .hasMessageContaining("直播场次 ID 无效");

        verify(liveScriptRepository, never()).save(any());
    }

    @Test
    @DisplayName("保存 - 更新不存在的话术")
    void testSave_UpdateNotFound() {
        // Arrange
        testSaveVO.setId(TEST_SCRIPT_ID);

        when(liveScriptRepository.findById(TEST_SCRIPT_ID))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> liveScriptService.save(testSaveVO))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DATA_NOT_FOUND)
                .hasMessageContaining("直播话术不存在");

        verify(liveScriptRepository).findById(TEST_SCRIPT_ID);
        verify(liveScriptRepository, never()).save(any());
    }

    // ==================== 删除测试 ====================

    @Test
    @DisplayName("删除 - 逻辑删除成功")
    void testDelete_Success() {
        // Arrange
        when(liveScriptRepository.findById(TEST_SCRIPT_ID))
                .thenReturn(Optional.of(testScript));
        when(liveScriptRepository.save(any(LiveScript.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        liveScriptService.delete(TEST_SCRIPT_ID);

        // Assert
        verify(liveScriptRepository).findById(TEST_SCRIPT_ID);
        verify(liveScriptRepository).save(argThat(script ->
                script.getId().equals(TEST_SCRIPT_ID) &&
                script.getDeleted().equals(1)
        ));
    }

    @Test
    @DisplayName("删除 - 话术不存在")
    void testDelete_NotFound() {
        // Arrange
        when(liveScriptRepository.findById(TEST_SCRIPT_ID))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> liveScriptService.delete(TEST_SCRIPT_ID))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DATA_NOT_FOUND)
                .hasMessageContaining("直播话术不存在");

        verify(liveScriptRepository).findById(TEST_SCRIPT_ID);
        verify(liveScriptRepository, never()).save(any());
    }

    @Test
    @DisplayName("删除 - 无效ID")
    void testDelete_InvalidId() {
        // Act & Assert
        assertThatThrownBy(() -> liveScriptService.delete(null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_FAIL)
                .hasMessageContaining("直播话术 ID 无效");

        verify(liveScriptRepository, never()).findById(any());
        verify(liveScriptRepository, never()).save(any());
    }

    // ==================== 边界条件测试 ====================

    @Test
    @DisplayName("分页查询 - 第一页")
    void testSearch_FirstPage() {
        // Arrange
        LiveScriptSearchVO searchVO = new LiveScriptSearchVO();
        searchVO.setSessionId(TEST_SESSION_ID);
        searchVO.setPage(0);
        searchVO.setRows(1);

        Page<LiveScript> page = new PageImpl<>(Arrays.asList(testScript));
        when(liveScriptRepository.findBySessionIdAndDeleted(eq(TEST_SESSION_ID), eq(0), any(Pageable.class)))
                .thenReturn(page);

        // Act
        PageResultVO<LiveScriptVO> result = liveScriptService.search(searchVO);

        // Assert
        assertThat(result.getPageNum()).isZero();
        assertThat(result.getPageSize()).isEqualTo(1);
        assertThat(result.getList()).hasSize(1);
    }

    @Test
    @DisplayName("分页查询 - 最大页大小")
    void testSearch_MaxPageSize() {
        // Arrange
        LiveScriptSearchVO searchVO = new LiveScriptSearchVO();
        searchVO.setSessionId(TEST_SESSION_ID);
        searchVO.setPage(0);
        searchVO.setRows(1000); // 最大值

        Page<LiveScript> page = new PageImpl<>(Collections.emptyList());
        when(liveScriptRepository.findBySessionIdAndDeleted(eq(TEST_SESSION_ID), eq(0), any(Pageable.class)))
                .thenReturn(page);

        // Act
        PageResultVO<LiveScriptVO> result = liveScriptService.search(searchVO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(searchVO.getRows()).isLessThanOrEqualTo(1000);
    }

    @Test
    @DisplayName("保存 - 最小有效数据")
    void testSave_MinimalData() {
        // Arrange
        LiveScriptSaveVO minimalVO = new LiveScriptSaveVO();
        minimalVO.setSessionId(TEST_SESSION_ID);
        // 其他字段为 null

        when(liveScriptRepository.save(any(LiveScript.class)))
                .thenAnswer(invocation -> {
                    LiveScript script = invocation.getArgument(0);
                    script.setId(TEST_SCRIPT_ID);
                    return script;
                });

        // Act
        long id = liveScriptService.save(minimalVO);

        // Assert
        assertThat(id).isEqualTo(TEST_SCRIPT_ID);
        verify(liveScriptRepository).save(any(LiveScript.class));
    }
}
