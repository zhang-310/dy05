package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.module.shortvideo.service.SvProjectService;
import cn.gaifan.douyinOperations.module.shortvideo.service.SvScriptService;
import cn.gaifan.douyinOperations.module.shortvideo.service.SvShotListService;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvProjectSaveVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvScriptSaveVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvShotVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShortVideoQuickServiceImplTest {

    @Mock
    private SvProjectService projectService;
    @Mock
    private SvScriptService scriptService;
    @Mock
    private SvShotListService shotListService;
    @Spy
    private ShortVideoCreativePlanner creativePlanner;
    @InjectMocks
    private ShortVideoQuickServiceImpl service;

    @Test
    void quickGenerateWiresCreativeBriefIntoScriptShotsAndResult() {
        when(projectService.save(any(SvProjectSaveVO.class), eq(7L))).thenReturn(101L, 101L);
        when(scriptService.generate(eq("daily"), eq("护肤教程"), eq(null), any(String.class), eq("专业干货"), eq(60), eq(7L)))
                .thenReturn("## 创意简报\n## 镜头1\n口播内容");
        when(scriptService.save(any(SvScriptSaveVO.class), eq(7L))).thenReturn(202L);
        when(shotListService.generateWithResult(eq(202L), any(String.class), eq(9), eq("专业干货"), eq(7L)))
                .thenReturn(new SvShotListService.GenerateResult(List.of(new SvShotVO()), 303L));

        Map<String, Object> result = service.quickGenerate(" 护肤教程 ", "美白,保湿", "专业干货", 7L);

        assertThat(result)
                .containsEntry("projectId", 101L)
                .containsEntry("scriptId", 202L)
                .containsEntry("shotListId", 303L)
                .containsKey("creativeBrief")
                .containsKey("productionPlan");
        Map<String, Object> creativeBrief = (Map<String, Object>) result.get("creativeBrief");
        assertThat(creativeBrief)
                .containsEntry("theme", "护肤教程")
                .containsEntry("durationSeconds", 60);

        ArgumentCaptor<SvProjectSaveVO> projectCaptor = ArgumentCaptor.forClass(SvProjectSaveVO.class);
        verify(projectService, org.mockito.Mockito.times(2)).save(projectCaptor.capture(), eq(7L));
        assertThat(projectCaptor.getAllValues().get(0).getStatus()).isEqualTo("draft");
        assertThat(projectCaptor.getAllValues().get(0).getDuration()).isEqualTo(60);
        assertThat(projectCaptor.getAllValues().get(1).getStatus()).isEqualTo("processing");
        assertThat(projectCaptor.getAllValues().get(1).getScriptId()).isEqualTo(202L);
        assertThat(projectCaptor.getAllValues().get(1).getShotListId()).isEqualTo(303L);

        ArgumentCaptor<String> scriptSeedCaptor = ArgumentCaptor.forClass(String.class);
        verify(scriptService).generate(eq("daily"), eq("护肤教程"), eq(null), scriptSeedCaptor.capture(), eq("专业干货"), eq(60), eq(7L));
        assertThat(scriptSeedCaptor.getValue())
                .contains("【短视频创作简报】")
                .contains("素材计划")
                .contains("风险清单");

        ArgumentCaptor<SvScriptSaveVO> scriptCaptor = ArgumentCaptor.forClass(SvScriptSaveVO.class);
        verify(scriptService).save(scriptCaptor.capture(), eq(7L));
        assertThat(scriptCaptor.getValue().getGenerationType()).isEqualTo("ai");
        assertThat(scriptCaptor.getValue().getTags()).contains("creativeBrief");
        assertThat(scriptCaptor.getValue().getAiPrompt()).contains("开头钩子候选");

        ArgumentCaptor<String> shotPromptCaptor = ArgumentCaptor.forClass(String.class);
        verify(shotListService).generateWithResult(eq(202L), shotPromptCaptor.capture(), eq(9), eq("专业干货"), eq(7L));
        assertThat(shotPromptCaptor.getValue())
                .contains("【短视频创作简报】")
                .contains("【脚本正文】")
                .contains("口播内容");

        Map<String, Object> productionPlan = (Map<String, Object>) result.get("productionPlan");
        assertThat((List<String>) productionPlan.get("nextActions"))
                .contains("生成关键帧/视频片段后进入自动合成");
    }
}
