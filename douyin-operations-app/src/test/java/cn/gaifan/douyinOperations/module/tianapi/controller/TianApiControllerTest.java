package cn.gaifan.douyinOperations.module.tianapi.controller;

import cn.gaifan.douyinOperations.module.tianapi.service.TianApiMaterialImportService;
import cn.gaifan.douyinOperations.module.tianapi.service.TianApiService;
import cn.gaifan.douyinOperations.module.tianapi.vo.AdReviewResultVO;
import cn.gaifan.douyinOperations.module.tianapi.vo.BulletinItemVO;
import cn.gaifan.douyinOperations.module.tianapi.vo.HotItemVO;
import cn.gaifan.douyinOperations.module.tianapi.vo.TextAuditResultVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("TianApiController 集成测试")
class TianApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TianApiService tianApiService;

    @MockBean
    private TianApiMaterialImportService materialImportService;

    // ==================== 热搜榜 ====================

    @Test
    @DisplayName("抖音热搜榜 - 应返回 200")
    void douyinHot_shouldReturn200() throws Exception {
        HotItemVO hotItem = new HotItemVO();
        hotItem.setWord("热搜标题");
        hotItem.setHotZh("100万");

        when(tianApiService.douyinHot())
                .thenReturn(List.of(hotItem));

        mockMvc.perform(post("/api/v1/tianapi/hot/douyin")
                        .requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("头条热搜榜 - 应返回 200")
    void toutiaoHot_shouldReturn200() throws Exception {
        HotItemVO hotItem = new HotItemVO();
        hotItem.setWord("头条热搜");

        when(tianApiService.toutiaoHot())
                .thenReturn(List.of(hotItem));

        mockMvc.perform(post("/api/v1/tianapi/hot/toutiao")
                        .requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("微博热搜榜 - 应返回 200")
    void weiboHot_shouldReturn200() throws Exception {
        HotItemVO hotItem = new HotItemVO();
        hotItem.setWord("微博热搜");

        when(tianApiService.weiboHot())
                .thenReturn(List.of(hotItem));

        mockMvc.perform(post("/api/v1/tianapi/hot/weibo")
                        .requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("全网热搜榜 - 应返回 200")
    void networkHot_shouldReturn200() throws Exception {
        HotItemVO hotItem = new HotItemVO();
        hotItem.setWord("全网热搜");

        when(tianApiService.networkHot())
                .thenReturn(List.of(hotItem));

        mockMvc.perform(post("/api/v1/tianapi/hot/network")
                        .requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("百度热搜榜 - 应返回 200")
    void baiduHot_shouldReturn200() throws Exception {
        HotItemVO hotItem = new HotItemVO();
        hotItem.setWord("百度热搜");

        when(tianApiService.baiduHot())
                .thenReturn(List.of(hotItem));

        mockMvc.perform(post("/api/v1/tianapi/hot/baidu")
                        .requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("腾讯/微信热搜榜 - 应返回 200")
    void tencentHot_shouldReturn200() throws Exception {
        HotItemVO hotItem = new HotItemVO();
        hotItem.setWord("腾讯热搜");

        when(tianApiService.tencentHot())
                .thenReturn(List.of(hotItem));

        mockMvc.perform(post("/api/v1/tianapi/hot/tencent")
                        .requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    // ==================== 文案/话术素材 ====================

    @Test
    @DisplayName("朋友圈文案 - 应返回 200")
    void pyqWenan_shouldReturn200() throws Exception {
        when(tianApiService.pyqWenan())
                .thenReturn("今天天气真好");

        mockMvc.perform(post("/api/v1/tianapi/material/pyq-wenan")
                        .requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.content").value("今天天气真好"));
    }

    @Test
    @DisplayName("打工人语录 - 应返回 200")
    void dagongren_shouldReturn200() throws Exception {
        when(tianApiService.dagongrenYulu())
                .thenReturn("打工人，打工魂");

        mockMvc.perform(post("/api/v1/tianapi/material/dagongren")
                        .requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.content").value("打工人，打工魂"));
    }

    @Test
    @DisplayName("土味情话 - 应返回 200")
    void tuweiQinghua_shouldReturn200() throws Exception {
        when(tianApiService.tuweiQinghua())
                .thenReturn("你知道我的缺点是什么吗？缺点你");

        mockMvc.perform(post("/api/v1/tianapi/material/tuwei-qinghua")
                        .requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("毒鸡汤 - 应返回 200")
    void duJitang_shouldReturn200() throws Exception {
        when(tianApiService.duJitang())
                .thenReturn("努力不一定成功，但不努力一定很轻松");

        mockMvc.perform(post("/api/v1/tianapi/material/dujitang")
                        .requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("彩虹屁 - 应返回 200")
    void caihongPi_shouldReturn200() throws Exception {
        when(tianApiService.caihongPi())
                .thenReturn("你真棒");

        mockMvc.perform(post("/api/v1/tianapi/material/caihongpi")
                        .requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("渣男语录 - 应返回 200")
    void zhananYulu_shouldReturn200() throws Exception {
        when(tianApiService.zhananYulu())
                .thenReturn("我只是把你当妹妹");

        mockMvc.perform(post("/api/v1/tianapi/material/zhanan")
                        .requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("早安心语 - 应返回 200")
    void zaoAnXinyu_shouldReturn200() throws Exception {
        when(tianApiService.zaoAnXinyu())
                .thenReturn("早安，新的一天");

        mockMvc.perform(post("/api/v1/tianapi/material/zaoan")
                        .requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("晚安心语 - 应返回 200")
    void wanAnXinyu_shouldReturn200() throws Exception {
        when(tianApiService.wanAnXinyu())
                .thenReturn("晚安，好梦");

        mockMvc.perform(post("/api/v1/tianapi/material/wanan")
                        .requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("经典台词 - 应返回 200")
    void classicDialogue_shouldReturn200() throws Exception {
        Map<String, String> dialogue = new HashMap<>();
        dialogue.put("english", "To be or not to be");
        dialogue.put("chinese", "生存还是毁灭");

        when(tianApiService.classicDialogue())
                .thenReturn(dialogue);

        mockMvc.perform(post("/api/v1/tianapi/material/dialogue")
                        .requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("神回复 - 应返回 200")
    void godReply_shouldReturn200() throws Exception {
        Map<String, String> reply = new HashMap<>();
        reply.put("question", "问题");
        reply.put("answer", "回复");

        Map<String, Object> body = new HashMap<>();
        body.put("num", 5);

        when(tianApiService.godReply(eq(5)))
                .thenReturn(List.of(reply));

        mockMvc.perform(post("/api/v1/tianapi/material/godreply")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("藏头诗生成 - 应返回 200")
    void cangtoushi_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("word", "测试");
        body.put("len", 5);

        when(tianApiService.cangtoushi(eq("测试"), eq(5)))
                .thenReturn("测试藏头诗内容");

        mockMvc.perform(post("/api/v1/tianapi/material/cangtoushi")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("网络流行语 - 应返回 200")
    void hotWord_shouldReturn200() throws Exception {
        Map<String, String> word = new HashMap<>();
        word.put("word", "yyds");
        word.put("meaning", "永远的神");

        Map<String, Object> body = new HashMap<>();
        body.put("word", "一哥");
        body.put("num", 5);

        when(tianApiService.hotWord(eq("一哥"), eq(5)))
                .thenReturn(List.of(word));

        mockMvc.perform(post("/api/v1/tianapi/material/hotword")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("名言警句 - 应返回 200")
    void dictum_shouldReturn200() throws Exception {
        Map<String, String> dictum = new HashMap<>();
        dictum.put("content", "知识就是力量");

        Map<String, Object> body = new HashMap<>();
        body.put("num", 5);

        when(tianApiService.dictum(eq(5)))
                .thenReturn(List.of(dictum));

        mockMvc.perform(post("/api/v1/tianapi/material/dictum")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("名人名言 - 应返回 200")
    void mingyan_shouldReturn200() throws Exception {
        Map<String, String> mingyan = new HashMap<>();
        mingyan.put("content", "天才是百分之一的灵感加百分之九十九的汗水");

        Map<String, Object> body = new HashMap<>();
        body.put("num", 5);
        body.put("typeid", 1);

        when(tianApiService.mingyan(eq(5), eq(1)))
                .thenReturn(List.of(mingyan));

        mockMvc.perform(post("/api/v1/tianapi/material/mingyan")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    // ==================== 合规/审核 ====================

    @Test
    @DisplayName("广告法违禁词检测 - 应返回 200")
    void adReview_shouldReturn200() throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("content", "最好的产品");

        AdReviewResultVO resultVO = new AdReviewResultVO();
        resultVO.setCompliant(false);
        resultVO.setWords(List.of("最好"));

        when(tianApiService.adReview(eq("最好的产品")))
                .thenReturn(resultVO);

        mockMvc.perform(post("/api/v1/tianapi/ad-review")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("文本审核 - 应返回 200")
    void textAudit_shouldReturn200() throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("content", "测试文本");

        TextAuditResultVO resultVO = new TextAuditResultVO();
        resultVO.setCompliant(true);

        when(tianApiService.textAudit(eq("测试文本")))
                .thenReturn(resultVO);

        mockMvc.perform(post("/api/v1/tianapi/text-audit")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    // ==================== 智能文案 ====================

    @Test
    @DisplayName("智能文案生成 - 应返回 200")
    void aiText_shouldReturn200() throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("text", "护肤品");

        when(tianApiService.aiTextGenerate(eq("护肤品")))
                .thenReturn("AI 生成的文案内容");

        mockMvc.perform(post("/api/v1/tianapi/ai-text")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.content").value("AI 生成的文案内容"));
    }

    // ==================== 节假日/简报 ====================

    @Test
    @DisplayName("节假日查询 - 应返回 200")
    void jiejiari_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("date", "2026-01-01");
        body.put("type", 0);

        JsonNode result = JsonNodeFactory.instance.objectNode();

        when(tianApiService.jiejiari(eq("2026-01-01"), eq(0)))
                .thenReturn(result);

        mockMvc.perform(post("/api/v1/tianapi/jiejiari")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("每日简报 - 应返回 200")
    void bulletin_shouldReturn200() throws Exception {
        BulletinItemVO item = new BulletinItemVO();
        item.setTitle("今日新闻");

        when(tianApiService.bulletin())
                .thenReturn(List.of(item));

        mockMvc.perform(post("/api/v1/tianapi/bulletin")
                        .requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("TianAPI 是否已配置 - 应返回 200")
    void status_shouldReturn200() throws Exception {
        when(tianApiService.isEnabled())
                .thenReturn(true);

        mockMvc.perform(post("/api/v1/tianapi/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.enabled").value(true));
    }

    @Test
    @DisplayName("手动触发素材入库 - 应返回 200")
    void materialImport_shouldReturn200() throws Exception {
        Map<String, Object> result = new HashMap<>();
        result.put("imported", 100);

        when(materialImportService.runImport())
                .thenReturn(result);

        mockMvc.perform(post("/api/v1/tianapi/material/import")
                        .requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }
}
