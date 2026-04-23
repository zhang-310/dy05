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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * LiveScriptServiceImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LiveScriptServiceImpl 单元测试")
class LiveScriptServiceImplTest {

    @Mock
    private LiveScriptRepository liveScriptRepository;

    @Mock
    private LiveProductRepository liveProductRepository;

    @InjectMocks
    private LiveScriptServiceImpl liveScriptService;

    private LiveScript sampleScript;

    @BeforeEach
    void setUp() {
        sampleScript = new LiveScript();
        sampleScript.setId(1L);
        sampleScript.setSessionId(100L);
        sampleScript.setScriptContent("Hello everyone, welcome to the live stream!");
        sampleScript.setSequenceNo(1);
        sampleScript.setExecutionTime(60L);
        sampleScript.setExecuted(0);
        sampleScript.setDeleted(0);
        sampleScript.setCreateTime(new Timestamp(System.currentTimeMillis()));
        sampleScript.setUpdateTime(new Timestamp(System.currentTimeMillis()));
    }

    // ==================== save 测试 ====================

    @Nested
    @DisplayName("save 方法测试")
    class SaveTests {

        @Test
        @DisplayName("save_validInput_returnsVO - 正常保存新话术返回 ID")
        void save_validInput_returnsVO() {
            LiveScriptSaveVO saveVO = new LiveScriptSaveVO();
            saveVO.setSessionId(100L);
            saveVO.setScriptContent("Test script content");
            saveVO.setSequenceNo(1);
            saveVO.setExecutionTime(30L);

            when(liveScriptRepository.save(any(LiveScript.class))).thenAnswer(invocation -> {
                LiveScript saved = invocation.getArgument(0);
                saved.setId(1L);
                return saved;
            });

            long id = liveScriptService.save(saveVO);

            assertEquals(1L, id);
            verify(liveScriptRepository).save(any(LiveScript.class));
        }

        @Test
        @DisplayName("save_existingId_updatesAndReturnsVO - 更新已有话术")
        void save_existingId_updatesAndReturnsVO() {
            LiveScriptSaveVO saveVO = new LiveScriptSaveVO();
            saveVO.setId(1L);
            saveVO.setSessionId(100L);
            saveVO.setScriptContent("Updated content");
            saveVO.setSequenceNo(2);

            when(liveScriptRepository.findById(1L)).thenReturn(Optional.of(sampleScript));
            when(liveScriptRepository.save(any(LiveScript.class))).thenReturn(sampleScript);

            long id = liveScriptService.save(saveVO);

            assertEquals(1L, id);
            verify(liveScriptRepository).findById(1L);
            verify(liveScriptRepository).save(any(LiveScript.class));
        }

        @Test
        @DisplayName("save_nullSessionId_throwsException - sessionId 为空抛出异常")
        void save_nullSessionId_throwsException() {
            LiveScriptSaveVO saveVO = new LiveScriptSaveVO();
            saveVO.setSessionId(null);
            saveVO.setScriptContent("Test content");

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> liveScriptService.save(saveVO));

            assertEquals(ErrorCode.VALIDATION_FAIL, ex.getCode());
            assertTrue(ex.getMessage().contains("直播场次 ID 无效"));
        }

        @Test
        @DisplayName("save_zeroSessionId_throwsException - sessionId 为 0 抛出异常")
        void save_zeroSessionId_throwsException() {
            LiveScriptSaveVO saveVO = new LiveScriptSaveVO();
            saveVO.setSessionId(0L);
            saveVO.setScriptContent("Test content");

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> liveScriptService.save(saveVO));

            assertEquals(ErrorCode.VALIDATION_FAIL, ex.getCode());
        }

        @Test
        @DisplayName("save_nonExistingId_throwsException - 更新不存在的话术抛出异常")
        void save_nonExistingId_throwsException() {
            LiveScriptSaveVO saveVO = new LiveScriptSaveVO();
            saveVO.setId(999L);
            saveVO.setSessionId(100L);
            saveVO.setScriptContent("Test content");

            when(liveScriptRepository.findById(999L)).thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> liveScriptService.save(saveVO));

            assertEquals(ErrorCode.DATA_NOT_FOUND, ex.getCode());
        }
    }

    // ==================== search 测试 ====================

    @Nested
    @DisplayName("search 方法测试")
    class SearchTests {

        @Test
        @DisplayName("search_withSessionIdFilter_returnsPageResult - 带 sessionId 条件搜索")
        void search_withSessionIdFilter_returnsPageResult() {
            LiveScriptSearchVO searchVO = new LiveScriptSearchVO();
            searchVO.setSessionId(100L);
            searchVO.setPage(0);
            searchVO.setRows(10);

            Page<LiveScript> page = new PageImpl<>(List.of(sampleScript));
            when(liveScriptRepository.findBySessionIdAndDeleted(eq(100L), eq(0), any(Pageable.class)))
                    .thenReturn(page);

            PageResultVO<LiveScriptVO> result = liveScriptService.search(searchVO);

            assertNotNull(result);
            assertEquals(1L, result.getTotal());
            assertEquals(1, result.getList().size());
            assertEquals(sampleScript.getId(), result.getList().get(0).getId());
        }

        @Test
        @DisplayName("search_withSessionIds_returnsPageResult - 多个 sessionId 搜索")
        void search_withSessionIds_returnsPageResult() {
            LiveScriptSearchVO searchVO = new LiveScriptSearchVO();
            searchVO.setSessionIds(List.of(100L, 200L));
            searchVO.setPage(0);
            searchVO.setRows(10);

            Page<LiveScript> page = new PageImpl<>(List.of(sampleScript));
            when(liveScriptRepository.findBySessionIdInAndDeleted(eq(List.of(100L, 200L)), eq(0), any(Pageable.class)))
                    .thenReturn(page);

            PageResultVO<LiveScriptVO> result = liveScriptService.search(searchVO);

            assertNotNull(result);
            assertEquals(1L, result.getTotal());
        }

        @Test
        @DisplayName("search_withExecutedFilter_returnsList - 带 executed 条件过滤")
        void search_withExecutedFilter_returnsList() {
            LiveScriptSearchVO searchVO = new LiveScriptSearchVO();
            searchVO.setSessionId(100L);
            searchVO.setExecuted(1);
            searchVO.setPage(0);
            searchVO.setRows(10);

            when(liveScriptRepository.findBySessionIdAndExecutedAndDeleted(100L, 1, 0))
                    .thenReturn(List.of(sampleScript));
            // When executed filter is set, it falls into the list-based branch
            when(liveScriptRepository.findBySessionIdAndDeleted(100L, 0))
                    .thenReturn(List.of(sampleScript));

            PageResultVO<LiveScriptVO> result = liveScriptService.search(searchVO);

            assertNotNull(result);
        }

        @Test
        @DisplayName("search_noFilter_returnsAll - 无过滤条件返回全部")
        void search_noFilter_returnsAll() {
            LiveScriptSearchVO searchVO = new LiveScriptSearchVO();
            searchVO.setPage(0);
            searchVO.setRows(10);

            Page<LiveScript> page = new PageImpl<>(List.of(sampleScript));
            when(liveScriptRepository.findAll(any(Pageable.class))).thenReturn(page);

            PageResultVO<LiveScriptVO> result = liveScriptService.search(searchVO);

            assertNotNull(result);
            assertEquals(1L, result.getTotal());
        }
    }

    // ==================== getById 测试 ====================

    @Nested
    @DisplayName("getById 方法测试")
    class GetByIdTests {

        @Test
        @DisplayName("getById_existingId_returnsVO - 正常查询返回 VO")
        void getById_existingId_returnsVO() {
            when(liveScriptRepository.findById(1L)).thenReturn(Optional.of(sampleScript));

            LiveScriptVO result = liveScriptService.getById(1L);

            assertNotNull(result);
            assertEquals(1L, result.getId());
            assertEquals(100L, result.getSessionId());
            assertEquals("Hello everyone, welcome to the live stream!", result.getScriptContent());
        }

        @Test
        @DisplayName("getById_nonExistingId_throwsException - 不存在 ID 抛出异常")
        void getById_nonExistingId_throwsException() {
            when(liveScriptRepository.findById(999L)).thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> liveScriptService.getById(999L));

            assertEquals(ErrorCode.DATA_NOT_FOUND, ex.getCode());
            assertTrue(ex.getMessage().contains("直播话术不存在"));
        }

        @Test
        @DisplayName("getById_nullId_throwsException - null ID 抛出异常")
        void getById_nullId_throwsException() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> liveScriptService.getById(null));

            assertEquals(ErrorCode.VALIDATION_FAIL, ex.getCode());
        }

        @Test
        @DisplayName("getById_zeroId_throwsException - 0 ID 抛出异常")
        void getById_zeroId_throwsException() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> liveScriptService.getById(0L));

            assertEquals(ErrorCode.VALIDATION_FAIL, ex.getCode());
        }

        @Test
        @DisplayName("getById_negativeId_throwsException - 负数 ID 抛出异常")
        void getById_negativeId_throwsException() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> liveScriptService.getById(-1L));

            assertEquals(ErrorCode.VALIDATION_FAIL, ex.getCode());
        }
    }

    // ==================== delete 测试 ====================

    @Nested
    @DisplayName("delete 方法测试")
    class DeleteTests {

        @Test
        @DisplayName("delete_existingId_success - 正常逻辑删除")
        void delete_existingId_success() {
            when(liveScriptRepository.findById(1L)).thenReturn(Optional.of(sampleScript));
            when(liveScriptRepository.save(any(LiveScript.class))).thenReturn(sampleScript);

            assertDoesNotThrow(() -> liveScriptService.delete(1L));

            verify(liveScriptRepository).save(argThat(script -> script.getDeleted() == 1));
        }

        @Test
        @DisplayName("delete_nonExistingId_throwsException - 删除不存在的话术抛出异常")
        void delete_nonExistingId_throwsException() {
            when(liveScriptRepository.findById(999L)).thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> liveScriptService.delete(999L));

            assertEquals(ErrorCode.DATA_NOT_FOUND, ex.getCode());
        }

        @Test
        @DisplayName("delete_nullId_throwsException - null ID 抛出异常")
        void delete_nullId_throwsException() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> liveScriptService.delete(null));

            assertEquals(ErrorCode.VALIDATION_FAIL, ex.getCode());
        }

        @Test
        @DisplayName("delete_zeroId_throwsException - 0 ID 抛出异常")
        void delete_zeroId_throwsException() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> liveScriptService.delete(0L));

            assertEquals(ErrorCode.VALIDATION_FAIL, ex.getCode());
        }
    }

    // ==================== getBySessionId 测试 ====================

    @Nested
    @DisplayName("getBySessionId 方法测试")
    class GetBySessionIdTests {

        @Test
        @DisplayName("getBySessionId_validId_returnsList - 正常查询返回列表")
        void getBySessionId_validId_returnsList() {
            when(liveScriptRepository.findBySessionIdAndDeleted(100L, 0))
                    .thenReturn(List.of(sampleScript));

            List<LiveScriptVO> result = liveScriptService.getBySessionId(100L);

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(100L, result.get(0).getSessionId());
        }

        @Test
        @DisplayName("getBySessionId_nullSessionId_throwsException - null sessionId 抛出异常")
        void getBySessionId_nullSessionId_throwsException() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> liveScriptService.getBySessionId(null));

            assertEquals(ErrorCode.VALIDATION_FAIL, ex.getCode());
        }

        @Test
        @DisplayName("getBySessionId_emptyResult_returnsEmptyList - 无数据返回空列表")
        void getBySessionId_emptyResult_returnsEmptyList() {
            when(liveScriptRepository.findBySessionIdAndDeleted(100L, 0))
                    .thenReturn(Collections.emptyList());

            List<LiveScriptVO> result = liveScriptService.getBySessionId(100L);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    // ==================== updateExecuted 测试 ====================

    @Nested
    @DisplayName("updateExecuted 方法测试")
    class UpdateExecutedTests {

        @Test
        @DisplayName("updateExecuted_markAsExecuted_setsActualExecutionTime - 标记已执行设置实际执行时间")
        void updateExecuted_markAsExecuted_setsActualExecutionTime() {
            when(liveScriptRepository.findById(1L)).thenReturn(Optional.of(sampleScript));
            when(liveScriptRepository.save(any(LiveScript.class))).thenReturn(sampleScript);

            liveScriptService.updateExecuted(1L, 1);

            verify(liveScriptRepository).save(argThat(script ->
                    script.getExecuted() == 1 && script.getActualExecutionTime() != null));
        }

        @Test
        @DisplayName("updateExecuted_nullId_throwsException - null ID 抛出异常")
        void updateExecuted_nullId_throwsException() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> liveScriptService.updateExecuted(null, 1));

            assertEquals(ErrorCode.VALIDATION_FAIL, ex.getCode());
        }
    }

    // ==================== exportScripts 测试 ====================

    @Nested
    @DisplayName("exportScripts 方法测试")
    class ExportScriptsTests {

        @Test
        @DisplayName("exportScripts_validSessionId_returnsText - 正常导出")
        void exportScripts_validSessionId_returnsText() {
            when(liveScriptRepository.findBySessionIdAndDeleted(100L, 0))
                    .thenReturn(List.of(sampleScript));

            String result = liveScriptService.exportScripts(100L);

            assertNotNull(result);
            assertTrue(result.contains("Hello everyone"));
        }
    }
}
