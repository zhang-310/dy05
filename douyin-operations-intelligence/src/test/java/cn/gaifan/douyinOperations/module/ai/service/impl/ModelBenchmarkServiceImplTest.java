package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.repository.AiModelBenchmarkRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiModelRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ModelBenchmarkServiceImplTest {

    @Mock
    private AiModelBenchmarkRepository benchmarkRepository;

    @Mock
    private AiModelRepository modelRepository;

    @InjectMocks
    private ModelBenchmarkServiceImpl service;

    @BeforeEach
    void setUp() {
        when(modelRepository.findById(1L)).thenAnswer(inv -> {
            AiModel m = new AiModel();
            m.setId(1L);
            m.setModelName("Fast");
            return Optional.of(m);
        });
        when(modelRepository.findById(2L)).thenAnswer(inv -> {
            AiModel m = new AiModel();
            m.setId(2L);
            m.setModelName("Slow");
            return Optional.of(m);
        });
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

    private static List<Object[]> rows(Object[]... elements) {
        List<Object[]> list = new ArrayList<>(elements.length);
        for (Object[] row : elements) {
            list.add(row);
        }
        return list;
    }
}
