package cn.gaifan.douyinOperations.module.benchmark.service;

import cn.gaifan.douyinOperations.module.benchmark.entity.BenchmarkAnalysis;
import cn.gaifan.douyinOperations.module.benchmark.entity.BenchmarkVideo;
import cn.gaifan.douyinOperations.module.benchmark.metrics.BenchmarkMetrics;
import cn.gaifan.douyinOperations.module.benchmark.repository.BenchmarkAnalysisRepository;
import cn.gaifan.douyinOperations.module.benchmark.repository.BenchmarkVideoRepository;
import cn.gaifan.douyinOperations.module.benchmark.service.impl.BenchmarkAnalysisServiceImpl;
import cn.gaifan.douyinOperations.module.benchmark.vo.BenchmarkAnalysisVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@DisplayName("BenchmarkAnalysisService 单元测试")
class BenchmarkAnalysisServiceTest {

    @Mock
    private BenchmarkVideoRepository videoRepository;

    @Mock
    private BenchmarkAnalysisRepository analysisRepository;

    @Mock
    private BenchmarkVideoService videoService;

    @Mock
    private BenchmarkTaskService taskService;

    @Mock
    private OcrService ocrService;

    @Mock
    private BenchmarkMetrics metrics;

    @InjectMocks
    private BenchmarkAnalysisServiceImpl service;

    private static final Long TEST_USER_ID = 1L;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new BenchmarkAnalysisServiceImpl(
                videoRepository,
                analysisRepository,
                videoService,
                taskService,
                ocrService,
                new ObjectMapper(),
                metrics
        );
    }

    @Test
    @DisplayName("查询分析结果 - 应优先按 ownerId 读取")
    void getByVideoId_shouldUseOwnerScopedResult() {
        BenchmarkAnalysis analysis = new BenchmarkAnalysis();
        analysis.setId(1L);
        analysis.setOwnerId(TEST_USER_ID);
        analysis.setBenchmarkVideoId(11L);
        analysis.setAiSummary("适合复刻");

        when(analysisRepository.findByBenchmarkVideoIdAndOwnerId(11L, TEST_USER_ID))
                .thenReturn(Optional.of(analysis));

        BenchmarkAnalysisVO result = service.getByVideoId(11L, TEST_USER_ID);

        assertThat(result.getAiSummary()).isEqualTo("适合复刻");
        verify(analysisRepository, never()).findByBenchmarkVideoId(11L);
    }

    @Test
    @DisplayName("查询老数据分析结果 - 应按视频归属回填 ownerId")
    void getByVideoId_legacyOwnerZero_shouldBackfillOwner() {
        BenchmarkAnalysis analysis = new BenchmarkAnalysis();
        analysis.setId(1L);
        analysis.setOwnerId(0L);
        analysis.setBenchmarkVideoId(11L);

        BenchmarkVideo video = new BenchmarkVideo();
        video.setId(11L);
        video.setOwnerId(TEST_USER_ID);

        when(analysisRepository.findByBenchmarkVideoIdAndOwnerId(11L, TEST_USER_ID))
                .thenReturn(Optional.empty());
        when(analysisRepository.findByBenchmarkVideoId(11L)).thenReturn(Optional.of(analysis));
        when(videoRepository.findById(11L)).thenReturn(Optional.of(video));
        when(analysisRepository.save(analysis)).thenReturn(analysis);

        BenchmarkAnalysisVO result = service.getByVideoId(11L, TEST_USER_ID);

        assertThat(result).isNotNull();
        assertThat(analysis.getOwnerId()).isEqualTo(TEST_USER_ID);
        verify(analysisRepository).save(analysis);
    }

    @Test
    @DisplayName("查询其他 owner 分析结果 - 应拒绝")
    void getByVideoId_otherOwner_shouldThrow() {
        BenchmarkAnalysis analysis = new BenchmarkAnalysis();
        analysis.setId(1L);
        analysis.setOwnerId(99L);
        analysis.setBenchmarkVideoId(11L);

        BenchmarkVideo video = new BenchmarkVideo();
        video.setId(11L);
        video.setOwnerId(99L);

        when(analysisRepository.findByBenchmarkVideoIdAndOwnerId(11L, TEST_USER_ID))
                .thenReturn(Optional.empty());
        when(analysisRepository.findByBenchmarkVideoId(11L)).thenReturn(Optional.of(analysis));
        when(videoRepository.findById(11L)).thenReturn(Optional.of(video));

        assertThatThrownBy(() -> service.getByVideoId(11L, TEST_USER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("无权访问该视频");
    }
}
