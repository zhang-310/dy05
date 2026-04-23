package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.entity.AiKbDocument;
import cn.gaifan.douyinOperations.module.ai.repository.AiKbDocumentRepository;
import cn.gaifan.douyinOperations.module.ai.service.KbDocumentQualityService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KbDocumentQualityServiceImplTest {

    @InjectMocks
    private KbDocumentQualityServiceImpl service;

    @Mock
    private AiKbDocumentRepository documentRepository;

    @Test
    void markTier_persists() {
        AiKbDocument d = new AiKbDocument();
        d.setId(9L);
        when(documentRepository.findById(9L)).thenReturn(Optional.of(d));

        service.markTier(9L, KbDocumentQualityService.TIER_LOW);

        assertThat(d.getQualityTier()).isEqualTo(2);
        assertThat(d.getLastQualityEvalAt()).isNotNull();
        verify(documentRepository).save(d);
    }

    @Test
    void scanHeuristicPage_empty_returnsMinusOne() {
        when(documentRepository.findByDeletedOrderByIdAsc(eq(0), any()))
                .thenReturn(new PageImpl<>(List.of()));

        assertThat(service.scanHeuristicPage(0, 50)).isEqualTo(-1);
        verify(documentRepository, never()).save(any());
    }

    @Test
    void scanHeuristicPage_setsLowTierForWeakSignals() {
        ReflectionTestUtils.setField(service, "lowThreshold", 40);
        ReflectionTestUtils.setField(service, "healthyThreshold", 65);

        AiKbDocument d = new AiKbDocument();
        d.setId(1L);
        d.setQualityTier(0);
        d.setRetrievalCount(0L);
        d.setCitationCount(0L);
        d.setBoostFactor(new BigDecimal("0.80"));
        when(documentRepository.findByDeletedOrderByIdAsc(eq(0), any()))
                .thenReturn(new PageImpl<>(List.of(d), PageRequest.of(0, 50), 1));

        int n = service.scanHeuristicPage(0, 50);
        assertThat(n).isEqualTo(1);
        assertThat(d.getQualityTier()).isEqualTo(KbDocumentQualityService.TIER_LOW);
        assertThat(d.getQualityHeuristicScore()).isNotNull();
        verify(documentRepository).save(d);
    }
}
