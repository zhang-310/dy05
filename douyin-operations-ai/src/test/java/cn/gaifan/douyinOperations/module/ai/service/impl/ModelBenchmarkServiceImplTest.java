package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.entity.AiCallLog;
import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.entity.AiModelBenchmark;
import cn.gaifan.douyinOperations.module.ai.entity.AiTaskModelConfig;
import cn.gaifan.douyinOperations.module.ai.repository.AiCallLogRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiModelBenchmarkRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiModelRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiTaskModelConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ModelBenchmarkServiceImplTest {

    @Mock
    private AiModelBenchmarkRepository benchmarkRepository;

    @Mock
    private AiModelRepository modelRepository;

    @Mock
    private AiCallLogRepository callLogRepository;

    @Mock
    private AiTaskModelConfigRepository taskModelConfigRepository;

    @InjectMocks
    private ModelBenchmarkServiceImpl service;

    @BeforeEach
    void setUp() {
        when(modelRepository.findById(1L)).thenAnswer(inv -> {
            AiModel m = new AiModel();
            m.setId(1L);
            m.setModelName("Fast");
            m.setModelVersion("fast-v1");
            return Optional.of(m);
        });
        when(modelRepository.findById(2L)).thenAnswer(inv -> {
            AiModel m = new AiModel();
            m.setId(2L);
            m.setModelName("Slow");
            m.setModelVersion("slow-v1");
            return Optional.of(m);
        });
        when(modelRepository.findByStatusAndDeleted(1, 0)).thenReturn(List.of(model(1L, "Fast", "fast-v1"), model(2L, "Slow", "slow-v1")));
    }

    @Test
    void selectBestModel_latency_prefersLowerMs() {
        Object[] r1 = {1L, "t1", 100.0, 1.0, 50.0, 10L};
        Object[] r2 = {2L, "t1", 500.0, 1.0, 40.0, 10L};
        when(benchmarkRepository.aggregateByTaskCode("t1")).thenReturn(rows(r1, r2));

        Long best = service.selectBestModel("t1", "latency");
        assertThat(best).isEqualTo(1L);
    }

    @Test
    void selectBestModel_success_prefersHigherRate() {
        Object[] r1 = {1L, "t1", 100.0, 0.8, 50.0, 10L};
        Object[] r2 = {2L, "t1", 100.0, 0.95, 50.0, 10L};
        when(benchmarkRepository.aggregateByTaskCode("t1")).thenReturn(rows(r1, r2));

        Long best = service.selectBestModel("t1", "success");
        assertThat(best).isEqualTo(2L);
    }

    @Test
    void selectBestModel_cost_prefersLowerTokens() {
        Object[] r1 = {1L, "t1", 100.0, 1.0, 80.0, 10L};
        Object[] r2 = {2L, "t1", 100.0, 1.0, 30.0, 10L};
        when(benchmarkRepository.aggregateByTaskCode("t1")).thenReturn(rows(r1, r2));

        Long best = service.selectBestModel("t1", "cost");
        assertThat(best).isEqualTo(2L);
    }

    @Test
    void getModelComparison_nullTask_aggregatesAll() {
        when(benchmarkRepository.aggregateAllByModelAndTask()).thenReturn(List.of());
        when(callLogRepository.aggregateModelBenchmarkFromCallLog(null)).thenReturn(List.of());
        assertThat(service.getModelComparison(null)).isEmpty();
    }

    @Test
    void getModelComparison_mapsRowShape() {
        Object[] r1 = {1L, "evolve", 200.0, 0.9, 100.0, 5L};
        when(benchmarkRepository.aggregateByTaskCode("evolve")).thenReturn(rows(r1));

        List<Map<String, Object>> mapped = service.getModelComparison("evolve");
        assertThat(mapped).hasSize(1);
        assertThat(mapped.get(0).get("modelId")).isEqualTo(1L);
        assertThat(mapped.get(0).get("modelName")).isEqualTo("Fast");
        assertThat(mapped.get(0).get("taskCode")).isEqualTo("evolve");
        assertThat(mapped.get(0).get("avgLatencyMs")).isEqualTo(200.0);
        assertThat(mapped.get(0).get("successRate")).isEqualTo(0.9);
        assertThat(mapped.get(0).get("avgTokens")).isEqualTo(100.0);
        assertThat(mapped.get(0).get("totalCalls")).isEqualTo(5L);
    }

    @Test
    void getModelComparison_whenBenchmarkTableEmpty_fallsBackToRealCallLogs() {
        AiTaskModelConfig config = new AiTaskModelConfig();
        config.setTaskCode("short_video_script");
        config.setPrimaryModelId(1L);
        when(taskModelConfigRepository.findByTaskCodeAndStatusAndDeleted("short_video_script", 1, 0))
                .thenReturn(Optional.of(config));
        when(benchmarkRepository.aggregateByTaskCode("short_video_script")).thenReturn(List.of());
        Object[] row = {"short_video_copy", "short_video_script", "fast-v1", 1200.0, 1.0, 360.0, 3L};
        when(callLogRepository.aggregateModelBenchmarkFromCallLog("short_video_script")).thenReturn(rows(row));

        List<Map<String, Object>> mapped = service.getModelComparison("short_video_script");

        assertThat(mapped).hasSize(1);
        assertThat(mapped.get(0).get("modelId")).isEqualTo(1L);
        assertThat(mapped.get(0).get("modelName")).isEqualTo("Fast");
        assertThat(mapped.get(0).get("taskCode")).isEqualTo("short_video_script");
        assertThat(mapped.get(0).get("source")).isEqualTo("ai_call_log");
    }

    @Test
    void recordFromCallLog_resolvesExplicitModelAndTaskConfig() {
        AiTaskModelConfig config = new AiTaskModelConfig();
        config.setTaskCode("copy_processing");
        config.setPrimaryModelId(1L);
        when(taskModelConfigRepository.findByTaskCodeAndStatusAndDeleted("copy_processing", 1, 0))
                .thenReturn(Optional.of(config));
        AiCallLog log = new AiCallLog();
        log.setCallType("live_script_full");
        log.setTemplateCode("copy_processing");
        log.setModelCode("model:2");
        log.setDurationMs(2000L);
        log.setTotalTokens(321);
        log.setStatus(1);

        service.recordFromCallLog(log);

        verify(benchmarkRepository).save(any(AiModelBenchmark.class));
    }

    private static List<Object[]> rows(Object[]... elements) {
        List<Object[]> list = new ArrayList<>(elements.length);
        for (Object[] row : elements) {
            list.add(row);
        }
        return list;
    }

    private static AiModel model(Long id, String name, String version) {
        AiModel model = new AiModel();
        model.setId(id);
        model.setModelName(name);
        model.setModelVersion(version);
        model.setStatus(1);
        model.setDeleted(0);
        return model;
    }
}
