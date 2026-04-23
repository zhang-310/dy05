package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.entity.AiKbDocument;
import cn.gaifan.douyinOperations.module.ai.repository.AiKbDocumentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ContentEffectivenessServiceImpl 效果统计测试")
class ContentEffectivenessServiceImplTest {

    @InjectMocks
    private ContentEffectivenessServiceImpl service;

    @Mock
    private AiKbDocumentRepository documentRepository;

    @Mock
    private AiKbDocumentCounterTransactionalService counterTransactionalService;

    @Test
    void recordRetrieval_withIds_callsIncrement() {
        when(counterTransactionalService.incrementRetrievalCount(anyList())).thenReturn(3);

        service.recordRetrieval(List.of(1L, 2L, 3L));

        verify(counterTransactionalService).incrementRetrievalCount(List.of(1L, 2L, 3L));
    }

    @Test
    void recordRetrieval_nullOrEmpty_doesNothing() {
        service.recordRetrieval(null);
        service.recordRetrieval(Collections.emptyList());

        verify(counterTransactionalService, org.mockito.Mockito.never()).incrementRetrievalCount(anyList());
    }

    @Test
    void recordCitation_withIds_callsIncrement_sorted() {
        when(counterTransactionalService.incrementCitationCount(anyList())).thenReturn(2);

        service.recordCitation(List.of(20L, 10L));

        verify(counterTransactionalService).incrementCitationCount(List.of(10L, 20L));
    }

    @Test
    void sortedDistinctIds_dedupesAndSorts() {
        List<Long> in = new ArrayList<>(Arrays.asList(3L, 1L, 3L, null, 2L));
        assertThat(ContentEffectivenessServiceImpl.sortedDistinctIds(in)).containsExactly(1L, 2L, 3L);
    }

    @Test
    void getEffectivenessBySourceType_returnsAggregatedStats() {
        AiKbDocument d1 = new AiKbDocument();
        d1.setRetrievalCount(5L);
        d1.setCitationCount(2L);
        AiKbDocument d2 = new AiKbDocument();
        d2.setRetrievalCount(3L);
        d2.setCitationCount(1L);
        when(documentRepository.findBySourceTypeAndDeleted("evolved", 0))
                .thenReturn(List.of(d1, d2));

        Map<String, Object> result = service.getEffectivenessBySourceType("evolved");

        assertThat(result).containsEntry("sourceType", "evolved");
        assertThat(result).containsEntry("totalDocs", 2L);
        assertThat(result).containsEntry("totalRetrievalCount", 8L);
        assertThat(result).containsEntry("totalCitationCount", 3L);
    }

    @Test
    void getTopCitedDocs_returnsTopN() {
        AiKbDocument d = new AiKbDocument();
        d.setId(1L);
        d.setTitle("Test");
        d.setCitationCount(10L);
        d.setContent("summary");
        when(documentRepository.findTopCitedBySourceType(eq("viral_analysis"), any(PageRequest.class)))
                .thenReturn(List.of(d));

        List<Map<String, Object>> result = service.getTopCitedDocs("viral_analysis", 5);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).containsEntry("docId", 1L);
        assertThat(result.get(0)).containsEntry("title", "Test");
        assertThat(result.get(0)).containsEntry("citationCount", 10L);
    }
}
