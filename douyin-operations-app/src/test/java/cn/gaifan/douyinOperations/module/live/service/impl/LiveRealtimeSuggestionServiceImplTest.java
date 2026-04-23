package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.common.config.BusinessParamConfig;
import cn.gaifan.douyinOperations.module.live.config.LiveDanmakuSentimentProperties;
import cn.gaifan.douyinOperations.module.live.entity.LiveDanmakuRecord;
import cn.gaifan.douyinOperations.module.live.repository.LiveDanmakuRecordRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveScriptRepository;
import cn.gaifan.douyinOperations.module.live.service.DanmakuAnalysisService;
import cn.gaifan.douyinOperations.module.live.service.DanmakuSentimentService;
import cn.gaifan.douyinOperations.module.live.vo.DanmakuSentimentSnapshotVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveSessionRealtimeDataVO;
import cn.gaifan.douyinOperations.module.live.vo.RealtimeSuggestionVO;
import cn.gaifan.douyinOperations.module.product.repository.DyProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LiveRealtimeSuggestionServiceImpl 实时建议测试")
class LiveRealtimeSuggestionServiceImplTest {

    private LiveRealtimeSuggestionServiceImpl service;

    @Mock
    private DanmakuSentimentService danmakuSentimentService;
    @Mock
    private DanmakuAnalysisService danmakuAnalysisService;
    @Mock
    private LiveScriptRepository liveScriptRepository;
    @Mock
    private LiveDanmakuRecordRepository liveDanmakuRecordRepository;
    @Mock
    private DyProductRepository dyProductRepository;

    @BeforeEach
    void setUp() {
        service = new LiveRealtimeSuggestionServiceImpl();
        BusinessParamConfig config = new BusinessParamConfig();
        LiveDanmakuSentimentProperties danmakuProps = new LiveDanmakuSentimentProperties();
        danmakuProps.setWindowSeconds(30);

        ReflectionTestUtils.setField(service, "businessParamConfig", config);
        ReflectionTestUtils.setField(service, "danmakuSentimentService", danmakuSentimentService);
        ReflectionTestUtils.setField(service, "danmakuAnalysisService", danmakuAnalysisService);
        ReflectionTestUtils.setField(service, "danmakuSentimentProperties", danmakuProps);
        ReflectionTestUtils.setField(service, "liveScriptRepository", liveScriptRepository);
        ReflectionTestUtils.setField(service, "liveDanmakuRecordRepository", liveDanmakuRecordRepository);
        ReflectionTestUtils.setField(service, "dyProductRepository", dyProductRepository);
    }

    @Test
    void evaluateSuggestions_shouldUseRecentDanmakuTextsForIntentAnalysis() {
        LiveSessionRealtimeDataVO data = new LiveSessionRealtimeDataVO();
        data.setViewerCount(100);
        data.setWatchedCount(80);
        data.setLikeCount(10);
        data.setCommentCount(8);
        data.setProductPurchaseCount(3);

        when(danmakuSentimentService.getSnapshot(10L))
                .thenReturn(new DanmakuSentimentSnapshotVO(0, 0, 1, 1, 30, "negative", System.currentTimeMillis()));
        when(danmakuSentimentService.getKeywordFrequency(10L)).thenReturn(Map.of());

        LiveDanmakuRecord record = new LiveDanmakuRecord();
        record.setContent("怎么买");
        record.setDanmakuTime(Timestamp.from(Instant.now()));
        when(liveDanmakuRecordRepository.findBySessionIdAndDanmakuTimeAfterAndDeleted(eq(10L), any(Timestamp.class), eq(0)))
                .thenReturn(List.of(record));
        when(danmakuAnalysisService.analyzeBatchIntents(10L, List.of("怎么买")))
                .thenReturn(Map.of(
                        "purchase_intent", 1,
                        "question", 0,
                        "interaction", 0,
                        "negative", 0,
                        "total", 1
                ));

        List<RealtimeSuggestionVO> suggestions = service.evaluateSuggestions(data, 10L);

        verify(danmakuAnalysisService).analyzeBatchIntents(10L, List.of("怎么买"));
        assertThat(suggestions).extracting(RealtimeSuggestionVO::getType)
                .contains("purchase_intent_high");
    }

    @Test
    void evaluateSuggestions_shouldSkipIntentAnalysisWhenDanmakuWindowIsEmpty() {
        LiveSessionRealtimeDataVO data = new LiveSessionRealtimeDataVO();
        data.setViewerCount(100);
        data.setWatchedCount(80);
        data.setLikeCount(10);
        data.setCommentCount(20);
        data.setProductPurchaseCount(3);

        when(danmakuSentimentService.getSnapshot(10L))
                .thenReturn(new DanmakuSentimentSnapshotVO(0, 0, 0, 0, 30, "neutral", System.currentTimeMillis()));
        when(danmakuSentimentService.getKeywordFrequency(10L)).thenReturn(Map.of());
        when(liveDanmakuRecordRepository.findBySessionIdAndDanmakuTimeAfterAndDeleted(eq(10L), any(Timestamp.class), eq(0)))
                .thenReturn(List.of());

        List<RealtimeSuggestionVO> suggestions = service.evaluateSuggestions(data, 10L);

        verify(danmakuAnalysisService, never()).analyzeBatchIntents(eq(10L), any());
        assertThat(suggestions).extracting(RealtimeSuggestionVO::getType)
                .doesNotContain("purchase_intent_high");
    }
}
