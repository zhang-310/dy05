package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvAccountCollectTask;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvViralVideo;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvAccountCollectTaskRepository;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvViralVideoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountVideoCollectServiceImpl 测试")
class AccountVideoCollectServiceImplTest {

    @Mock
    private SvAccountCollectTaskRepository taskRepository;
    @Mock
    private SvViralVideoRepository viralVideoRepository;
    @Mock
    private AccountCollectAsyncRunner asyncRunner;
    @Mock
    private DouyinUrlResolver douyinUrlResolver;

    @InjectMocks
    private AccountVideoCollectServiceImpl service;

    @Test
    @DisplayName("listTaskVideos 应给推演 transcript 暴露 evidence 标签")
    void listTaskVideos_shouldExposeEvidenceLabels() {
        SvAccountCollectTask task = new SvAccountCollectTask();
        task.setId(5L);
        task.setOwnerId(9L);

        SvViralVideo viral = new SvViralVideo();
        viral.setId(11L);
        viral.setOwnerId(9L);
        viral.setCollectTaskId(5L);
        viral.setTitle("熬夜修护爆款");
        viral.setTranscript("【推演口播】先指出熬夜暗沉，再给修护方案");
        viral.setSceneDescriptions("【推演场景】\n[0-3s] 镜前特写");
        viral.setDeepAnalysisResult("""
                {
                  "evidenceLevel": "inferred",
                  "evidenceDetails": {
                    "overallLevel": "inferred",
                    "transcriptLevel": "inferred",
                    "sceneLevel": "inferred",
                    "commentLevel": "missing",
                    "hasCommentSamples": false
                  }
                }
                """);

        when(taskRepository.findByIdAndDeleted(5L, 0)).thenReturn(Optional.of(task));
        when(viralVideoRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(viral), PageRequest.of(0, 20), 1));

        PageResultVO<Map<String, Object>> result = service.listTaskVideos(5L, 9L, 0, 20);

        assertThat(result.getList()).hasSize(1);
        Map<String, Object> row = result.getList().get(0);
        assertThat(row.get("evidenceLevel")).isEqualTo("inferred");
        assertThat(row.get("transcriptEvidenceLevel")).isEqualTo("inferred");
        assertThat(row.get("sceneEvidenceLevel")).isEqualTo("inferred");
        assertThat(row.get("transcriptLabel")).isEqualTo("推演口播稿（非 ASR 实录）");
        assertThat(row.get("sceneLabel")).isEqualTo("推演场景（非真实抽帧）");
        assertThat(row.get("inferenceRisk")).isEqualTo(true);
    }
}
