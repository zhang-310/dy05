package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.entity.EvolutionFitnessRecord;
import cn.gaifan.douyinOperations.module.ai.repository.EvolutionFitnessRecordRepository;
import cn.gaifan.douyinOperations.module.ai.service.KnowledgeBaseService;
import cn.gaifan.douyinOperations.module.ai.vo.EvolutionFitnessListVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EvolutionFitnessServiceImplTest {

    @Mock
    private EvolutionFitnessRecordRepository repository;

    @Mock
    private KnowledgeBaseService knowledgeBaseService;

    @InjectMocks
    private EvolutionFitnessServiceImpl service;

    @BeforeEach
    void setRecordEnabled() {
        ReflectionTestUtils.setField(service, "recordEnabled", true);
    }

    @Test
    void record_persistsWithExperimentId() {
        service.record(10L, "task-1", "parent-1", "evolve_completed", 88.0, "{}", "exp-A");
        ArgumentCaptor<EvolutionFitnessRecord> cap = ArgumentCaptor.forClass(EvolutionFitnessRecord.class);
        verify(repository).save(cap.capture());
        EvolutionFitnessRecord row = cap.getValue();
        assertThat(row.getTaskId()).isEqualTo("task-1");
        assertThat(row.getParentTaskId()).isEqualTo("parent-1");
        assertThat(row.getMetricName()).isEqualTo("evolve_completed");
        assertThat(row.getMetricValue()).isEqualTo(88.0);
        assertThat(row.getExperimentId()).isEqualTo("exp-A");
    }

    @Test
    void record_skipsWhenDisabled() {
        ReflectionTestUtils.setField(service, "recordEnabled", false);
        service.record(10L, "t", null, "m", 1.0, "{}");
        verifyNoInteractions(repository);
    }

    @Test
    void listForKb_assertsOwnershipAndReturnsPage() {
        EvolutionFitnessListVO vo = new EvolutionFitnessListVO();
        vo.setPage(0);
        vo.setRows(10);
        when(repository.findAll(ArgumentMatchers.<Specification<EvolutionFitnessRecord>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        var result = service.listForKb(99L, 5L, vo);

        verify(knowledgeBaseService).assertKbOwnership(eq(5L), eq(99L));
        assertThat(result.getList()).isEmpty();
    }
}
