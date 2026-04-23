package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.module.shortvideo.entity.SvViralVideo;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvViralVideoRepository;
import cn.gaifan.douyinOperations.module.shortvideo.service.ViralRemakeService;
import cn.gaifan.douyinOperations.module.shortvideo.service.ViralVideoDeepAnalysisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ViralAutoOrchestrationServiceTest {

    @Mock
    private SvViralVideoRepository viralVideoRepository;

    @Mock
    private ViralVideoDeepAnalysisService deepAnalysisService;

    @Mock
    private ViralRemakeService viralRemakeService;

    @InjectMocks
    private ViralAutoOrchestrationService service;

    @BeforeEach
    void injectConfigDefaults() {
        ReflectionTestUtils.setField(service, "batchLimit", 20);
        ReflectionTestUtils.setField(service, "minViralScore", 70);
    }

    @Test
    void runPipeline_skipsRecommendWhenDeepProcessing() {
        SvViralVideo v = candidate();
        v.setDeepAnalyzeStatus("processing");
        when(viralVideoRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(v)));
        when(viralVideoRepository.findById(1L)).thenReturn(Optional.of(v));

        Map<String, Object> r = service.runPipeline();

        assertEquals(0, r.get("recommended"));
        verify(viralRemakeService, never()).recommendRemake(anyLong(), anyLong());
    }

    @Test
    void runPipeline_recommendsWhenDeepCompleted() {
        SvViralVideo v = candidate();
        v.setDeepAnalyzeStatus("completed");
        when(viralVideoRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(v)));
        when(viralVideoRepository.findById(1L)).thenReturn(Optional.of(v));

        service.runPipeline();

        verify(viralRemakeService).recommendRemake(1L, 0L);
    }

    private static SvViralVideo candidate() {
        SvViralVideo v = new SvViralVideo();
        v.setId(1L);
        v.setOwnerId(0L);
        v.setDeleted(0);
        v.setAutoCollected(true);
        v.setViralScore(80);
        v.setRemakeStatus(0);
        return v;
    }
}
