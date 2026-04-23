package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.module.ai.service.KnowledgeBaseService;
import cn.gaifan.douyinOperations.module.ai.vo.RagRetrieveItemVO;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvDailyBatch;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvHotTopic;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvViralVideo;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvDailyBatchRepository;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvHotTopicRepository;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvViralVideoRepository;
import cn.gaifan.douyinOperations.module.shortvideo.service.ContentCalendarService;
import cn.gaifan.douyinOperations.module.shortvideo.service.ShortVideoAiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DailyContentServiceImpl 测试")
class DailyContentServiceImplTest {

    @Mock
    private SvDailyBatchRepository dailyBatchRepository;
    @Mock
    private SvHotTopicRepository hotTopicRepository;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private KnowledgeBaseService knowledgeBaseService;
    @Mock
    private ShortVideoAiService shortVideoAiService;
    @Mock
    private ContentCalendarService contentCalendarService;
    @Mock
    private SvViralVideoRepository svViralVideoRepository;

    @InjectMocks
    private DailyContentServiceImpl service;

    @Test
    @DisplayName("viral_video 来源应优先使用用户爆款视频标题")
    void generateBatch_shouldUseViralVideoTitles() throws Exception {
        SvViralVideo viral = new SvViralVideo();
        viral.setTitle("高转化护肤脚本");

        when(dailyBatchRepository.save(any(SvDailyBatch.class))).thenAnswer(invocation -> {
            SvDailyBatch batch = invocation.getArgument(0);
            if (batch.getId() == null) {
                batch.setId(1L);
            }
            return batch;
        });
        when(svViralVideoRepository.findByOwnerIdAndDeletedOrderByViewCountDesc(eq(9L), eq(0), any(PageRequest.class)))
                .thenReturn(List.of(viral));
        when(shortVideoAiService.generateCopy(any(), eq(9L))).thenReturn("AI文案");
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"ok\":true}");

        SvDailyBatch result = service.generateBatch(9L, 1L, "viral_video", 1);

        assertThat(result.getStatus()).isEqualTo("completed");
        assertThat(result.getResultSummary()).isEqualTo("{\"ok\":true}");
    }

    @Test
    @DisplayName("无热点与爆款时应回退到知识库选题而非默认话题")
    void generateBatch_shouldFallbackToKnowledgeTopics() throws Exception {
        RagRetrieveItemVO ragItem = new RagRetrieveItemVO();
        ragItem.setTitle("春季护肤选题");

        when(dailyBatchRepository.save(any(SvDailyBatch.class))).thenAnswer(invocation -> {
            SvDailyBatch batch = invocation.getArgument(0);
            if (batch.getId() == null) {
                batch.setId(2L);
            }
            return batch;
        });
        when(hotTopicRepository.findAll(any(PageRequest.class))).thenReturn(new PageImpl<>(List.of()));
        when(knowledgeBaseService.hybridSearchAllKbs(eq(9L), eq("短视频 选题 热点 内容 方向"), eq(3), eq("shortvideo"), eq(null)))
                .thenReturn(List.of(ragItem));
        when(shortVideoAiService.generateCopy(any(), eq(9L))).thenReturn("AI文案");
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"ok\":true}");

        SvDailyBatch result = service.generateBatch(9L, 1L, "manual", 1);

        assertThat(result.getStatus()).isEqualTo("completed");
        assertThat(result.getResultSummary()).isEqualTo("{\"ok\":true}");
    }
}
