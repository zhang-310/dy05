package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ShortVideoCreativePlannerTest {

    private final ShortVideoCreativePlanner planner = new ShortVideoCreativePlanner();

    @Test
    void buildCreativeBriefCreatesEndToEndProductionPlan() {
        Map<String, Object> brief = planner.buildCreativeBrief(
                "护肤品开箱测评",
                "美白, 保湿, 平价好物",
                "专业干货",
                60);

        assertThat(brief)
                .containsEntry("theme", "护肤品开箱测评")
                .containsEntry("style", "专业干货")
                .containsEntry("durationSeconds", 60)
                .containsEntry("aspectRatio", "9:16");
        assertThat((List<String>) brief.get("keywords")).containsExactly("美白", "保湿", "平价好物");
        assertThat((String) brief.get("targetAudience")).contains("美妆护肤用户");
        assertThat((String) brief.get("corePromise")).contains("值不值得买");
        assertThat((List<String>) brief.get("hookOptions")).anySatisfy(item -> assertThat(item).contains("先看这 3 个判断点"));
        assertThat((List<Map<String, Object>>) brief.get("storyBeats")).hasSize(4);
        assertThat((List<Map<String, Object>>) brief.get("materialPlan"))
                .anySatisfy(item -> assertThat(item.get("type")).isEqualTo("proof"));
        assertThat((List<String>) brief.get("riskChecklist"))
                .anySatisfy(item -> assertThat(item).contains("医疗化表达"));
        assertThat((List<String>) brief.get("acceptanceCriteria"))
                .contains("素材清单覆盖主体、证据、过渡和封面");

        String promptBlock = planner.toPromptBlock(brief);
        assertThat(promptBlock)
                .contains("【短视频创作简报】")
                .contains("开头钩子候选")
                .contains("素材计划")
                .contains("风险清单");
    }

    @Test
    void buildCreativeBriefAddsDigitalHumanProductDetailPlanForCommerceVideo() {
        Map<String, Object> brief = planner.buildCreativeBrief(
                "平价护肤品数字人口播带货",
                "保湿, 产品细节, 使用演示",
                "数字人口播带货 产品细节展示",
                45);

        assertThat(brief)
                .containsEntry("commerceFormat", "digital_human_product_detail");
        assertThat((List<Map<String, Object>>) brief.get("storyBeats"))
                .hasSize(6)
                .anySatisfy(item -> assertThat(item.get("label")).isEqualTo("产品入镜"))
                .anySatisfy(item -> assertThat(item.get("label")).isEqualTo("使用演示/对比"));
        assertThat((List<Map<String, Object>>) brief.get("digitalHumanPlan"))
                .anySatisfy(item -> assertThat(item.get("type")).isEqualTo("opening_avatar"));
        assertThat((List<Map<String, Object>>) brief.get("productDetailPlan"))
                .anySatisfy(item -> assertThat(item.get("type")).isEqualTo("detail_closeup"));
        assertThat((List<Map<String, Object>>) brief.get("bRollSequence"))
                .anySatisfy(item -> assertThat(item.get("label")).isEqualTo("product_closeup"))
                .anySatisfy(item -> assertThat(item.get("label")).isEqualTo("usage_demo"));
        assertThat((List<Map<String, Object>>) brief.get("conversionPlan"))
                .anySatisfy(item -> assertThat(item.get("type")).isEqualTo("decision_filter"));

        String promptBlock = planner.toPromptBlock(brief);
        assertThat(promptBlock)
                .contains("商业内容形态：digital_human_product_detail")
                .contains("数字人口播计划")
                .contains("产品细节展示计划")
                .contains("B-roll 镜头序列")
                .contains("转化计划");
    }
}
