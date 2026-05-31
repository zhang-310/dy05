package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.common.vo.BasicQueryDto;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvVideoGenerationTask;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvWebhookDlq;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvVideoGenerationTaskRepository;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvWebhookDlqRepository;
import cn.gaifan.douyinOperations.module.shortvideo.service.ShortVideoMaterialService;
import cn.gaifan.douyinOperations.module.shortvideo.service.VideoGenerationTaskWebhookNotifier;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvWebhookDlqVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VideoGenerationTaskServiceImplWebhookDlqTest {

    @Mock
    private SvWebhookDlqRepository webhookDlqRepository;
    @Mock
    private SvVideoGenerationTaskRepository taskRepository;
    @Mock
    private ShortVideoMaterialService materialService;
    @Mock
    private VideoGenerationTaskWebhookNotifier taskWebhookNotifier;

    @InjectMocks
    private VideoGenerationTaskServiceImpl service;

    @Test
    void listWebhookDlq_fingerprintsLongSha256() {
        SvWebhookDlq row = new SvWebhookDlq();
        row.setId(1L);
        row.setCreateTime(new Timestamp(1_700_000_000_000L));
        row.setTaskId(99L);
        row.setWebhookUrlSha256("abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789");
        row.setLastHttpStatus(500);
        row.setAttemptCount(4);
        row.setErrorPreview("http_500");
        row.setEventCode("sv.video_generation.task.ended");

        when(webhookDlqRepository.findByOwnerIdOrderByIdDesc(eq(7L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(row), PageRequest.of(0, 30), 1L));

        BasicQueryDto q = new BasicQueryDto();
        PageResultVO<SvWebhookDlqVO> res = service.listWebhookDlq(7L, q);

        assertThat(res.getTotal()).isEqualTo(1);
        assertThat(res.getList()).hasSize(1);
        SvWebhookDlqVO vo = res.getList().get(0);
        assertThat(vo.getWebhookUrlFingerprint()).isEqualTo("abcdef012345…");
        assertThat(vo.getTaskId()).isEqualTo(99L);
        assertThat(vo.getLastHttpStatus()).isEqualTo(500);
        assertThat(vo.getAttemptCount()).isEqualTo(4);
    }

    @Test
    void listTasks_exposesCompletedVideoResultUrl() {
        SvVideoGenerationTask task = new SvVideoGenerationTask();
        task.setId(11L);
        task.setOwnerId(7L);
        task.setProjectId(3L);
        task.setShotListId(5L);
        task.setStatus("completed");
        task.setProgressCurrent(1);
        task.setProgressTotal(1);
        task.setResultJson("[{\"shotId\":9,\"shotNumber\":1,\"videoUrl\":\"https://cdn.example.com/shot-1.mp4\",\"bosKey\":\"sv/shot-1.mp4\",\"duration\":6}]");
        task.setCreateTime(new Timestamp(1_700_000_000_000L));

        when(taskRepository.findByOwnerIdAndProjectIdOrderByCreateTimeDesc(eq(7L), eq(3L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(task), PageRequest.of(0, 20), 1L));

        PageResultVO<Map<String, Object>> res = service.listTasks(7L, 0, 20, 3L);

        assertThat(res.getTotal()).isEqualTo(1);
        Map<String, Object> row = res.getList().get(0);
        assertThat(row.get("progress")).isEqualTo(100);
        assertThat(row.get("outputUrl")).isEqualTo("https://cdn.example.com/shot-1.mp4");
        assertThat(row.get("videoCount")).isEqualTo(1);
        assertThat(row.get("videos")).asList()
                .first()
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                .containsEntry("videoUrl", "https://cdn.example.com/shot-1.mp4");
    }
}
