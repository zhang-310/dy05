package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.entity.AiSearchLog;
import cn.gaifan.douyinOperations.module.ai.repository.AiSearchLogRepository;
import cn.gaifan.douyinOperations.module.ai.service.KbDocumentQualityService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * SearchLogServiceImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SearchLogServiceImpl 单元测试")
class SearchLogServiceImplTest {

    @Mock
    private AiSearchLogRepository searchLogRepository;

    @Mock
    private KbDocumentQualityService kbDocumentQualityService;

    @InjectMocks
    private SearchLogServiceImpl searchLogService;

    @Nested
    @DisplayName("logSearchAsync")
    class LogSearchAsyncTests {

        @Test
        @DisplayName("logSearchAsync_nullOwner_shouldReturnEarly")
        void logSearchAsync_nullOwner_shouldReturnEarly() {
            searchLogService.logSearchAsync(null, "query", 1L, null, null, "hybrid", 100);
            verify(searchLogRepository, never()).save(any());
        }

        @Test
        @DisplayName("logSearchAsync_nullQuery_shouldReturnEarly")
        void logSearchAsync_nullQuery_shouldReturnEarly() {
            searchLogService.logSearchAsync(1L, null, 1L, null, null, "hybrid", 100);
            verify(searchLogRepository, never()).save(any());
        }

        @Test
        @DisplayName("logSearchAsync_valid_shouldSave")
        void logSearchAsync_valid_shouldSave() {
            when(searchLogRepository.save(any(AiSearchLog.class))).thenAnswer(i -> i.getArgument(0));

            searchLogService.logSearchAsync(1L, "测试查询", 10L, List.of(1L, 2L), 0.95, "hybrid", 50);

            verify(searchLogRepository).save(argThat(log ->
                    log.getOwnerId().equals(1L) && "测试查询".equals(log.getQueryText())
                            && log.getHitCount() == 2 && log.getLatencyMs() == 50));
        }
    }

    @Nested
    @DisplayName("updateFeedback")
    class UpdateFeedbackTests {

        @Test
        @DisplayName("updateFeedback_nullLogId_shouldReturnEarly")
        void updateFeedback_nullLogId_shouldReturnEarly() {
            searchLogService.updateFeedback(null, "helpful", null);
            verify(searchLogRepository, never()).findById(any());
        }

        @Test
        @DisplayName("updateFeedback_invalidFeedback_shouldReturnEarly")
        void updateFeedback_invalidFeedback_shouldReturnEarly() {
            searchLogService.updateFeedback(1L, "invalid", null);
            verify(searchLogRepository, never()).findById(any());
        }

        @Test
        @DisplayName("updateFeedback_valid_shouldUpdate")
        void updateFeedback_valid_shouldUpdate() {
            AiSearchLog log = new AiSearchLog();
            log.setId(1L);
            log.setOwnerId(1L);
            log.setHitDocIds("100,200");
            when(searchLogRepository.findById(1L)).thenReturn(Optional.of(log));
            when(searchLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            searchLogService.updateFeedback(1L, "helpful", 1L);

            verify(searchLogRepository).save(argThat(l -> "helpful".equals(l.getUserFeedback())));
            verify(kbDocumentQualityService).markTier(100L, KbDocumentQualityService.TIER_HEALTHY);
            verify(kbDocumentQualityService, never()).markTier(eq(200L), anyInt());
        }

        @Test
        @DisplayName("updateFeedback_notHelpful_marksAllHitDocsLow")
        void updateFeedback_notHelpful_marksAllHitDocsLow() {
            AiSearchLog log = new AiSearchLog();
            log.setId(2L);
            log.setOwnerId(1L);
            log.setHitDocIds("100,200");
            when(searchLogRepository.findById(2L)).thenReturn(Optional.of(log));
            when(searchLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            searchLogService.updateFeedback(2L, "not_helpful", 1L);

            verify(kbDocumentQualityService).markTier(100L, KbDocumentQualityService.TIER_LOW);
            verify(kbDocumentQualityService).markTier(200L, KbDocumentQualityService.TIER_LOW);
        }
    }

    @Nested
    @DisplayName("getUsageCountForDoc")
    class GetUsageCountForDocTests {

        @Test
        @DisplayName("getUsageCountForDoc_nullOwner_shouldReturn0")
        void getUsageCountForDoc_nullOwner_shouldReturn0() {
            long count = searchLogService.getUsageCountForDoc(null, 1L, 7);
            assertThat(count).isEqualTo(0);
        }

        @Test
        @DisplayName("getUsageCountForDoc_valid_shouldReturnCount")
        void getUsageCountForDoc_valid_shouldReturnCount() {
            when(searchLogRepository.countHitsForDocSince(any(), any(), any())).thenReturn(5L);

            long count = searchLogService.getUsageCountForDoc(1L, 10L, 7);

            assertThat(count).isEqualTo(5L);
        }
    }
}
