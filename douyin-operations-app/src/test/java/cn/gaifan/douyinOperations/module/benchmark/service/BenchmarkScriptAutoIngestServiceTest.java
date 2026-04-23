package cn.gaifan.douyinOperations.module.benchmark.service;

import cn.gaifan.douyinOperations.module.benchmark.service.impl.BenchmarkScriptAutoIngestServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class BenchmarkScriptAutoIngestServiceTest {

    @InjectMocks
    private BenchmarkScriptAutoIngestServiceImpl autoIngestService;

    @Test
    void testGetAndSetIngestThreshold() {
        // When
        BigDecimal defaultThreshold = autoIngestService.getIngestThreshold();
        autoIngestService.setIngestThreshold(BigDecimal.valueOf(80.0));
        BigDecimal newThreshold = autoIngestService.getIngestThreshold();

        // Then
        assertThat(defaultThreshold).isEqualTo(BigDecimal.valueOf(70.0));
        assertThat(newThreshold).isEqualTo(BigDecimal.valueOf(80.0));
    }
}
