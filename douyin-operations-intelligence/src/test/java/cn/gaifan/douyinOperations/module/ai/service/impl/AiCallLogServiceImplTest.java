package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.ai.entity.AiCallLog;
import cn.gaifan.douyinOperations.module.ai.repository.AiCallLogRepository;
import cn.gaifan.douyinOperations.module.ai.service.AiCallLogService;
import cn.gaifan.douyinOperations.module.ai.vo.CallLogSearchVO;
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

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AiCallLogServiceImpl 单元测试
 *
 * 测试覆盖：
 * - 日志记录（log）
 * - 分页查询（search）
 * - 效果归因关联（linkToPublish）
 * - 数据隔离（userId 校验）
 * - Token 汇总计算
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AI 调用日志服务测试")
class AiCallLogServiceImplTest {

    @Mock
    private AiCallLogRepository aiCallLogRepository;

    @InjectMocks
    private AiCallLogServiceImpl aiCallLogService;

    private AiCallLog testLog;
    private static final Long TEST_USER_ID = 1001L;
    private static final Long TEST_LOG_ID = 3001L;
    private static final Long TEST_VIDEO_ID = 4001L;
    private static final Long TEST_SESSION_ID = 5001L;

    @BeforeEach
    void setUp() {
        // 准备测试数据
        testLog = new AiCallLog();
        testLog.setId(TEST_LOG_ID);
        testLog.setUserId(TEST_USER_ID);
        testLog.setCallType("script_generation");
        testLog.setTemplateCode("live_script_v1");
        testLog.setModelCode("gpt-4");
        testLog.setInputSummary("生成直播话术");
        testLog.setOutputLength(500);
        testLog.setPromptTokens(100);
        testLog.setCompletionTokens(400);
        testLog.setTotalTokens(500);
        testLog.setDurationMs(2000L);
        testLog.setStatus(1);
        testLog.setIsFallback(0);
        testLog.setCreateTime(new Timestamp(System.currentTimeMillis()));
    }

    // ==================== 日志记录测试 ====================

    @Test
    @DisplayName("记录日志 - 成功（完整参数）")
    void testLog_Success_FullParams() {
        // Arrange
        AiCallLogService.LogEntry entry = new AiCallLogService.LogEntry(
                TEST_USER_ID,
                "script_generation",
                "live_script_v1",
                "gpt-4",
                "生成直播话术：护肤品推广",
                500,
                100,
                400,
                2000L,
                1,
                null,
                false,
                "chunk_1,chunk_2",
                "{\"retrieval\":100,\"generation\":1900}"
        );

        when(aiCallLogRepository.save(any(AiCallLog.class)))
                .thenAnswer(invocation -> {
                    AiCallLog log = invocation.getArgument(0);
                    log.setId(TEST_LOG_ID);
                    return log;
                });

        // Act
        Long logId = aiCallLogService.log(entry);

        // Assert
        assertThat(logId).isEqualTo(TEST_LOG_ID);

        verify(aiCallLogRepository).save(argThat(log ->
                log.getUserId().equals(TEST_USER_ID) &&
                log.getCallType().equals("script_generation") &&
                log.getTemplateCode().equals("live_script_v1") &&
                log.getModelCode().equals("gpt-4") &&
                log.getOutputLength().equals(500) &&
                log.getPromptTokens().equals(100) &&
                log.getCompletionTokens().equals(400) &&
                log.getTotalTokens().equals(500) &&
                log.getDurationMs().equals(2000L) &&
                log.getStatus() == 1 &&
                log.getIsFallback() == 0
        ));
    }

    @Test
    @DisplayName("记录日志 - 成功（最小参数）")
    void testLog_Success_MinimalParams() {
        // Arrange
        AiCallLogService.LogEntry entry = AiCallLogService.LogEntry.of(
                TEST_USER_ID,
                "kb_search",
                "embedding-v1",
                500L,
                1
        );

        when(aiCallLogRepository.save(any(AiCallLog.class)))
                .thenAnswer(invocation -> {
                    AiCallLog log = invocation.getArgument(0);
                    log.setId(TEST_LOG_ID);
                    return log;
                });

        // Act
        Long logId = aiCallLogService.log(entry);

        // Assert
        assertThat(logId).isEqualTo(TEST_LOG_ID);

        verify(aiCallLogRepository).save(argThat(log ->
                log.getUserId().equals(TEST_USER_ID) &&
                log.getCallType().equals("kb_search") &&
                log.getModelCode().equals("embedding-v1") &&
                log.getDurationMs().equals(500L) &&
                log.getStatus() == 1
        ));
    }

    @Test
    @DisplayName("记录日志 - 失败状态")
    void testLog_FailureStatus() {
        // Arrange
        AiCallLogService.LogEntry entry = new AiCallLogService.LogEntry(
                TEST_USER_ID,
                "script_generation",
                null,
                "gpt-4",
                "生成直播话术",
                null,
                null,
                null,
                1500L,
                0, // 失败状态
                "API rate limit exceeded",
                false,
                null,
                null
        );

        when(aiCallLogRepository.save(any(AiCallLog.class)))
                .thenAnswer(invocation -> {
                    AiCallLog log = invocation.getArgument(0);
                    log.setId(TEST_LOG_ID);
                    return log;
                });

        // Act
        Long logId = aiCallLogService.log(entry);

        // Assert
        assertThat(logId).isEqualTo(TEST_LOG_ID);

        verify(aiCallLogRepository).save(argThat(log ->
                log.getStatus() == 0 &&
                log.getErrorMessage() != null &&
                log.getErrorMessage().contains("rate limit")
        ));
    }

    @Test
    @DisplayName("记录日志 - Fallback 标记")
    void testLog_WithFallback() {
        // Arrange
        AiCallLogService.LogEntry entry = new AiCallLogService.LogEntry(
                TEST_USER_ID,
                "script_generation",
                "live_script_v1",
                "gpt-3.5-turbo", // fallback model
                "生成直播话术",
                300,
                80,
                220,
                1000L,
                1,
                null,
                true, // isFallback = true
                null,
                null
        );

        when(aiCallLogRepository.save(any(AiCallLog.class)))
                .thenAnswer(invocation -> {
                    AiCallLog log = invocation.getArgument(0);
                    log.setId(TEST_LOG_ID);
                    return log;
                });

        // Act
        Long logId = aiCallLogService.log(entry);

        // Assert
        assertThat(logId).isEqualTo(TEST_LOG_ID);

        verify(aiCallLogRepository).save(argThat(log ->
                log.getIsFallback() == 1 &&
                log.getModelCode().equals("gpt-3.5-turbo")
        ));
    }

    // ==================== 分页查询测试 ====================

    @Test
    @DisplayName("分页查询 - 按调用类型查询")
    void testSearch_ByCallType() {
        // Arrange
        CallLogSearchVO searchVO = new CallLogSearchVO();
        searchVO.setCallType("script_generation");
        searchVO.setPage(0);
        searchVO.setRows(20);

        List<AiCallLog> logs = Arrays.asList(testLog);
        Page<AiCallLog> page = new PageImpl<>(logs);

        when(aiCallLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        // Act
        PageResultVO<AiCallLog> result = aiCallLogService.search(searchVO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getList()).hasSize(1);
        assertThat(result.getList().get(0).getCallType()).isEqualTo("script_generation");

        verify(aiCallLogRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("分页查询 - 按状态查询")
    void testSearch_ByStatus() {
        // Arrange
        CallLogSearchVO searchVO = new CallLogSearchVO();
        searchVO.setStatus(1); // 成功
        searchVO.setPage(0);
        searchVO.setRows(20);

        List<AiCallLog> logs = Arrays.asList(testLog);
        Page<AiCallLog> page = new PageImpl<>(logs);

        when(aiCallLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        // Act
        PageResultVO<AiCallLog> result = aiCallLogService.search(searchVO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getList().get(0).getStatus()).isEqualTo(1);
    }

    @Test
    @DisplayName("分页查询 - 关键词搜索")
    void testSearch_ByKeyword() {
        // Arrange
        CallLogSearchVO searchVO = new CallLogSearchVO();
        searchVO.setKeyword("直播话术");
        searchVO.setPage(0);
        searchVO.setRows(20);

        List<AiCallLog> logs = Arrays.asList(testLog);
        Page<AiCallLog> page = new PageImpl<>(logs);

        when(aiCallLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        // Act
        PageResultVO<AiCallLog> result = aiCallLogService.search(searchVO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTotal()).isEqualTo(1);
    }

    @Test
    @DisplayName("分页查询 - 时间范围查询")
    void testSearch_ByTimeRange() {
        // Arrange
        CallLogSearchVO searchVO = new CallLogSearchVO();
        searchVO.setStartTime("2026-01-01 00:00:00");
        searchVO.setEndTime("2026-12-31 23:59:59");
        searchVO.setPage(0);
        searchVO.setRows(20);

        List<AiCallLog> logs = Arrays.asList(testLog);
        Page<AiCallLog> page = new PageImpl<>(logs);

        when(aiCallLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        // Act
        PageResultVO<AiCallLog> result = aiCallLogService.search(searchVO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTotal()).isEqualTo(1);
    }

    @Test
    @DisplayName("分页查询 - 空结果")
    void testSearch_EmptyResult() {
        // Arrange
        CallLogSearchVO searchVO = new CallLogSearchVO();
        searchVO.setCallType("non_existent_type");
        searchVO.setPage(0);
        searchVO.setRows(20);

        Page<AiCallLog> emptyPage = new PageImpl<>(Collections.emptyList());

        when(aiCallLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(emptyPage);

        // Act
        PageResultVO<AiCallLog> result = aiCallLogService.search(searchVO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTotal()).isZero();
        assertThat(result.getList()).isEmpty();
    }

    @Test
    @DisplayName("分页查询 - 参数校验（rows 上限）")
    void testSearch_ParamValidation() {
        // Arrange
        CallLogSearchVO searchVO = new CallLogSearchVO();
        searchVO.setPage(0);
        searchVO.setRows(200); // 超过上限，应该被限制为 100

        Page<AiCallLog> page = new PageImpl<>(Collections.emptyList());
        when(aiCallLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        // Act
        PageResultVO<AiCallLog> result = aiCallLogService.search(searchVO);

        // Assert - rows 应该被限制为 100
        assertThat(result).isNotNull();
        assertThat(result.getPageSize()).isLessThanOrEqualTo(100);
    }

    // ==================== 效果归因关联测试 ====================

    @Test
    @DisplayName("关联发布内容 - 关联短视频成功")
    void testLinkToPublish_Video_Success() {
        // Arrange
        when(aiCallLogRepository.findById(TEST_LOG_ID))
                .thenReturn(Optional.of(testLog));
        when(aiCallLogRepository.save(any(AiCallLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        aiCallLogService.linkToPublish(TEST_LOG_ID, TEST_VIDEO_ID, null, TEST_USER_ID);

        // Assert
        verify(aiCallLogRepository).findById(TEST_LOG_ID);
        verify(aiCallLogRepository).save(argThat(log ->
                log.getId().equals(TEST_LOG_ID) &&
                log.getLinkedVideoId().equals(TEST_VIDEO_ID)
        ));
    }

    @Test
    @DisplayName("关联发布内容 - 关联直播场次成功")
    void testLinkToPublish_Session_Success() {
        // Arrange
        when(aiCallLogRepository.findById(TEST_LOG_ID))
                .thenReturn(Optional.of(testLog));
        when(aiCallLogRepository.save(any(AiCallLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        aiCallLogService.linkToPublish(TEST_LOG_ID, null, TEST_SESSION_ID, TEST_USER_ID);

        // Assert
        verify(aiCallLogRepository).findById(TEST_LOG_ID);
        verify(aiCallLogRepository).save(argThat(log ->
                log.getId().equals(TEST_LOG_ID) &&
                log.getLinkedSessionId().equals(TEST_SESSION_ID)
        ));
    }

    @Test
    @DisplayName("关联发布内容 - 同时关联视频和场次")
    void testLinkToPublish_Both_Success() {
        // Arrange
        when(aiCallLogRepository.findById(TEST_LOG_ID))
                .thenReturn(Optional.of(testLog));
        when(aiCallLogRepository.save(any(AiCallLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        aiCallLogService.linkToPublish(TEST_LOG_ID, TEST_VIDEO_ID, TEST_SESSION_ID, TEST_USER_ID);

        // Assert
        verify(aiCallLogRepository).findById(TEST_LOG_ID);
        verify(aiCallLogRepository).save(argThat(log ->
                log.getId().equals(TEST_LOG_ID) &&
                log.getLinkedVideoId().equals(TEST_VIDEO_ID) &&
                log.getLinkedSessionId().equals(TEST_SESSION_ID)
        ));
    }

    @Test
    @DisplayName("关联发布内容 - 数据隔离校验失败")
    void testLinkToPublish_DataIsolation_Forbidden() {
        // Arrange
        testLog.setUserId(9999L); // 不同用户

        when(aiCallLogRepository.findById(TEST_LOG_ID))
                .thenReturn(Optional.of(testLog));

        // Act & Assert
        assertThatThrownBy(() -> aiCallLogService.linkToPublish(TEST_LOG_ID, TEST_VIDEO_ID, null, TEST_USER_ID))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN)
                .hasMessageContaining("无权操作此日志");

        verify(aiCallLogRepository).findById(TEST_LOG_ID);
        verify(aiCallLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("关联发布内容 - 日志不存在")
    void testLinkToPublish_LogNotFound() {
        // Arrange
        when(aiCallLogRepository.findById(TEST_LOG_ID))
                .thenReturn(Optional.empty());

        // Act
        aiCallLogService.linkToPublish(TEST_LOG_ID, TEST_VIDEO_ID, null, TEST_USER_ID);

        // Assert - 不存在时静默返回
        verify(aiCallLogRepository).findById(TEST_LOG_ID);
        verify(aiCallLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("关联发布内容 - 参数为空")
    void testLinkToPublish_NullParams() {
        // Act
        aiCallLogService.linkToPublish(null, TEST_VIDEO_ID, null, TEST_USER_ID);

        // Assert - 参数为空时静默返回
        verify(aiCallLogRepository, never()).findById(any());
        verify(aiCallLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("关联发布内容 - videoId 和 sessionId 都为空")
    void testLinkToPublish_BothIdsNull() {
        // Act
        aiCallLogService.linkToPublish(TEST_LOG_ID, null, null, TEST_USER_ID);

        // Assert - 两个 ID 都为空时静默返回
        verify(aiCallLogRepository, never()).findById(any());
        verify(aiCallLogRepository, never()).save(any());
    }
}
