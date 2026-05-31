package cn.gaifan.douyinOperations.module.script.service.impl;

import cn.gaifan.douyinOperations.module.script.repository.ScriptVectorEmbeddingRepository;
import cn.gaifan.douyinOperations.module.script.service.VectorEmbeddingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class VectorEmbeddingTaskTest {

    @Mock
    private VectorEmbeddingService vectorEmbeddingService;

    @Mock
    private ScriptVectorEmbeddingRepository scriptVectorEmbeddingRepository;

    @InjectMocks
    private VectorEmbeddingTask task;

    @Test
    void indexUnindexedEmbeddings_shouldSkipWhenDisabled() {
        ReflectionTestUtils.setField(task, "schedulerEnabled", false);

        task.indexUnindexedEmbeddings();

        verify(vectorEmbeddingService, never()).getUnindexedEmbeddingIds(100);
    }

    @Test
    void checkVectorHealthStatus_shouldSkipWhenDisabled() {
        ReflectionTestUtils.setField(task, "schedulerEnabled", false);

        task.checkVectorHealthStatus();

        verify(scriptVectorEmbeddingRepository, never()).countByOwnerId(null);
    }
}
