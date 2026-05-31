package cn.gaifan.douyinOperations.module.shortvideo.config;

import cn.gaifan.douyinOperations.module.shortvideo.repository.SvHotTopicRepository;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvViralVideoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ViralVideoCollectorSchedulerTest {

    @Mock
    private SvHotTopicRepository svHotTopicRepository;

    @Mock
    private SvViralVideoRepository svViralVideoRepository;

    @Mock
    private ShortVideoVerticalCollectorProperties verticalCollectorProperties;

    @InjectMocks
    private ViralVideoCollectorScheduler scheduler;

    @Test
    void collectViralFromHotTopics_shouldSkipWhenDisabled() {
        ReflectionTestUtils.setField(scheduler, "collectorEnabled", false);

        scheduler.collectViralFromHotTopics();

        verify(svHotTopicRepository, never()).findAll(org.mockito.ArgumentMatchers.any(org.springframework.data.jpa.domain.Specification.class));
    }

    @Test
    void collectVerticalVirals_shouldSkipWhenDisabled() {
        ReflectionTestUtils.setField(scheduler, "collectorEnabled", false);

        scheduler.collectVerticalVirals();

        verify(svHotTopicRepository, never()).findAll();
    }
}
