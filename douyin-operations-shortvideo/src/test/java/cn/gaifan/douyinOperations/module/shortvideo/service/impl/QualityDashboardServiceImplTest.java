package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.module.shortvideo.entity.SvGenerationLog;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvGenerationLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QualityDashboardServiceImplTest {

    @Mock
    private SvGenerationLogRepository generationLogRepository;

    @InjectMocks
    private QualityDashboardServiceImpl service;

    @Test
    void getAiReflectionsBuildsSuggestionsFromGenerationLogs() {
        SvGenerationLog high = new SvGenerationLog();
        high.setSuccess(true);
        high.setQualityScore(new BigDecimal("88.00"));
        high.setGenerationTimeMs(90_000L);
        high.setHasAudio(false);

        SvGenerationLog low = new SvGenerationLog();
        low.setSuccess(false);
        low.setQualityScore(new BigDecimal("62.00"));
        low.setGenerationTimeMs(160_000L);
        low.setHasAudio(false);

        when(generationLogRepository.findByOwnerIdAndCreateTimeBetweenOrderByCreateTimeAsc(eq(7L), any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(List.of(high, low));
        List<Object[]> providerRows = java.util.Collections.singletonList(new Object[]{"kling", new BigDecimal("88.50")});
        List<Object[]> cameraRows = java.util.Collections.singletonList(new Object[]{"push_in", new BigDecimal("86.20")});
        when(generationLogRepository.avgQualityByProvider(eq(7L), any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(providerRows);
        when(generationLogRepository.avgQualityByCameraType(eq(7L), any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(cameraRows);

        List<String> result = service.getAiReflections(7L);

        assertThat(result).anySatisfy(text -> assertThat(text).contains("生成 2 次").contains("成功率 50.0%"));
        assertThat(result).anySatisfy(text -> assertThat(text).contains("成功率低于 90%"));
        assertThat(result).anySatisfy(text -> assertThat(text).contains("模型 kling").contains("88.5"));
        assertThat(result).anySatisfy(text -> assertThat(text).contains("运镜 push_in").contains("86.2"));
        assertThat(result).anySatisfy(text -> assertThat(text).contains("超过 120 秒"));
        assertThat(result).anySatisfy(text -> assertThat(text).contains("未带音频"));
    }

    @Test
    void getAiReflectionsExplainsEmptyLogs() {
        when(generationLogRepository.findByOwnerIdAndCreateTimeBetweenOrderByCreateTimeAsc(eq(7L), any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(List.of());

        List<String> result = service.getAiReflections(7L);

        assertThat(result).containsExactly("近 7 天暂无生成日志，建议先完成成片生成并让质量评分落库，再查看 AI 反思。");
    }
}
