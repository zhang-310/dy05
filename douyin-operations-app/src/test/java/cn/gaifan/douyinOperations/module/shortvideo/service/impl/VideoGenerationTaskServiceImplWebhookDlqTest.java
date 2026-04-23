package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.common.vo.BasicQueryDto;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
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
}
