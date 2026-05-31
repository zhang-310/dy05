package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.repository.AiModelRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiTaskModelConfigRepository;
import cn.gaifan.douyinOperations.module.ai.service.LlmClient;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvScript;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvShot;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvShotList;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvScriptRepository;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvShotListRepository;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvShotRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SvShotListServiceImplCreativePromptTest {

    @Mock
    private SvShotListRepository shotListRepository;
    @Mock
    private SvShotRepository shotRepository;
    @Mock
    private SvScriptRepository scriptRepository;
    @Mock
    private LlmClient llmClient;
    @Mock
    private AiTaskModelConfigRepository taskModelConfigRepository;
    @Mock
    private AiModelRepository modelRepository;
    @InjectMocks
    private SvShotListServiceImpl service;

    @Test
    void generatePersistsCameraTypeAndDurationForProductionPipeline() {
        ReflectionTestUtils.setField(service, "objectMapper", new ObjectMapper());

        AiModel model = new AiModel();
        model.setId(1L);
        model.setStatus(1);
        model.setDeleted(0);
        model.setTemperature(new BigDecimal("0.70"));
        model.setMaxTokens(2048);
        when(taskModelConfigRepository.findByTaskCodeAndStatusAndDeleted(eq("short_video_script"), eq(1), eq(0)))
                .thenReturn(Optional.empty());
        when(modelRepository.findByStatusAndDeleted(1, 0)).thenReturn(List.of(model));
        when(llmClient.chatWithFallback(any(), any(), any()))
                .thenReturn(new LlmClient.LlmResponse("""
                        [
                          {
                            "timeRange": "0-3s",
                            "sceneDescription": "桌面近景展示痛点字幕",
                            "cameraAngle": "特写",
                            "cameraType": "push-in",
                            "action": "手拿产品入画",
                            "dialogue": "先看这三个判断点",
                            "mood": "提醒",
                            "duration": 3
                          }
                        ]
                        """, 120, true, null));

        SvScript script = new SvScript();
        script.setId(202L);
        script.setOwnerId(7L);
        when(scriptRepository.findById(202L)).thenReturn(Optional.of(script));
        when(shotListRepository.save(any(SvShotList.class))).thenAnswer(invocation -> {
            SvShotList list = invocation.getArgument(0);
            list.setId(303L);
            return list;
        });

        var shots = service.generate(202L, "【短视频创作简报】\n素材计划\n【脚本正文】\n口播", 6, "专业干货", 7L);

        assertThat(shots).hasSize(1);
        assertThat(shots.get(0).getCameraType()).isEqualTo("push-in");
        assertThat(shots.get(0).getDuration()).isEqualTo(3);

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(llmClient).chatWithFallback(any(), any(), promptCaptor.capture());
        assertThat(promptCaptor.getValue())
                .contains("可执行分镜")
                .contains("cameraType")
                .contains("duration")
                .contains("创作简报和脚本");

        ArgumentCaptor<SvShot> shotCaptor = ArgumentCaptor.forClass(SvShot.class);
        verify(shotRepository).save(shotCaptor.capture());
        assertThat(shotCaptor.getValue().getCameraType()).isEqualTo("push-in");
        assertThat(shotCaptor.getValue().getDuration()).isEqualTo(3);
    }
}
