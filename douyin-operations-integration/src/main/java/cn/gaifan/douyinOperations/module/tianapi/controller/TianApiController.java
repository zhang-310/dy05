package cn.gaifan.douyinOperations.module.tianapi.controller;

import cn.gaifan.douyinOperations.common.annotation.CurrentUserId;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.tianapi.service.TianApiMaterialImportService;
import cn.gaifan.douyinOperations.module.tianapi.service.TianApiService;
import cn.gaifan.douyinOperations.module.tianapi.vo.*;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * TianAPI（天聚数行）统一对接接口
 * 路径：/api/v1/tianapi
 */
@RestController
@RequestMapping("/api/v1/tianapi")
@Tag(name = "TianAPI 天聚数行", description = "热搜、文案素材、广告法违禁词、文本审核等")
public class TianApiController {

    @Resource
    private TianApiService tianApiService;
    @Resource
    private TianApiMaterialImportService materialImportService;

    // ─── 热搜榜 ──────────────────────────────────────

    @PostMapping("/hot/douyin")
    @Operation(summary = "抖音热搜榜")
    public RESTResult<List<HotItemVO>> douyinHot(@CurrentUserId Long userId) {
        RESTResult<List<HotItemVO>> r = RESTResult.getSuccess(tianApiService.douyinHot());
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/hot/toutiao")
    @Operation(summary = "头条热搜榜")
    public RESTResult<List<HotItemVO>> toutiaoHot(@CurrentUserId Long userId) {
        RESTResult<List<HotItemVO>> r = RESTResult.getSuccess(tianApiService.toutiaoHot());
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/hot/weibo")
    @Operation(summary = "微博热搜榜")
    public RESTResult<List<HotItemVO>> weiboHot(@CurrentUserId Long userId) {
        RESTResult<List<HotItemVO>> r = RESTResult.getSuccess(tianApiService.weiboHot());
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/hot/network")
    @Operation(summary = "全网热搜榜")
    public RESTResult<List<HotItemVO>> networkHot(@CurrentUserId Long userId) {
        RESTResult<List<HotItemVO>> r = RESTResult.getSuccess(tianApiService.networkHot());
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/hot/baidu")
    @Operation(summary = "百度热搜榜")
    public RESTResult<List<HotItemVO>> baiduHot(@CurrentUserId Long userId) {
        RESTResult<List<HotItemVO>> r = RESTResult.getSuccess(tianApiService.baiduHot());
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/hot/tencent")
    @Operation(summary = "腾讯/微信热搜榜")
    public RESTResult<List<HotItemVO>> tencentHot(@CurrentUserId Long userId) {
        RESTResult<List<HotItemVO>> r = RESTResult.getSuccess(tianApiService.tencentHot());
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    // ─── 文案/话术素材 ──────────────────────────────────────

    @PostMapping("/material/pyq-wenan")
    @Operation(summary = "朋友圈文案（随机）")
    public RESTResult<Map<String, String>> pyqWenan(@CurrentUserId Long userId) {
        String content = tianApiService.pyqWenan();
        RESTResult<Map<String, String>> r = RESTResult.getSuccess(Map.of("content", content != null ? content : ""));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/material/dagongren")
    @Operation(summary = "打工人语录")
    public RESTResult<Map<String, String>> dagongren(@CurrentUserId Long userId) {
        String content = tianApiService.dagongrenYulu();
        RESTResult<Map<String, String>> r = RESTResult.getSuccess(Map.of("content", content != null ? content : ""));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/material/tuwei-qinghua")
    @Operation(summary = "土味情话")
    public RESTResult<Map<String, String>> tuweiQinghua(@CurrentUserId Long userId) {
        String content = tianApiService.tuweiQinghua();
        RESTResult<Map<String, String>> r = RESTResult.getSuccess(Map.of("content", content != null ? content : ""));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/material/dujitang")
    @Operation(summary = "毒鸡汤")
    public RESTResult<Map<String, String>> duJitang(@CurrentUserId Long userId) {
        String content = tianApiService.duJitang();
        RESTResult<Map<String, String>> r = RESTResult.getSuccess(Map.of("content", content != null ? content : ""));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/material/caihongpi")
    @Operation(summary = "彩虹屁")
    public RESTResult<Map<String, String>> caihongPi(@CurrentUserId Long userId) {
        String content = tianApiService.caihongPi();
        RESTResult<Map<String, String>> r = RESTResult.getSuccess(Map.of("content", content != null ? content : ""));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/material/zhanan")
    @Operation(summary = "渣男语录")
    public RESTResult<Map<String, String>> zhananYulu(@CurrentUserId Long userId) {
        String content = tianApiService.zhananYulu();
        RESTResult<Map<String, String>> r = RESTResult.getSuccess(Map.of("content", content != null ? content : ""));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/material/zaoan")
    @Operation(summary = "早安心语")
    public RESTResult<Map<String, String>> zaoAnXinyu(@CurrentUserId Long userId) {
        String content = tianApiService.zaoAnXinyu();
        RESTResult<Map<String, String>> r = RESTResult.getSuccess(Map.of("content", content != null ? content : ""));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/material/wanan")
    @Operation(summary = "晚安心语")
    public RESTResult<Map<String, String>> wanAnXinyu(@CurrentUserId Long userId) {
        String content = tianApiService.wanAnXinyu();
        RESTResult<Map<String, String>> r = RESTResult.getSuccess(Map.of("content", content != null ? content : ""));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/material/dialogue")
    @Operation(summary = "经典台词（中英对照）")
    public RESTResult<Map<String, String>> classicDialogue(@CurrentUserId Long userId) {
        RESTResult<Map<String, String>> r = RESTResult.getSuccess(tianApiService.classicDialogue());
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/material/godreply")
    @Operation(summary = "神回复")
    public RESTResult<List<Map<String, String>>> godReply(@CurrentUserId Long userId, @RequestBody(required = false) Map<String, Object> body) {
        int num = body != null && body.get("num") != null ? ((Number) body.get("num")).intValue() : 5;
        RESTResult<List<Map<String, String>>> r = RESTResult.getSuccess(tianApiService.godReply(num));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/material/cangtoushi")
    @Operation(summary = "藏头诗生成")
    public RESTResult<Map<String, String>> cangtoushi(@CurrentUserId Long userId, @RequestBody(required = false) Map<String, Object> body) {
        String word = body != null && body.get("word") != null ? body.get("word").toString() : "";
        int len = body != null && body.get("len") != null ? ((Number) body.get("len")).intValue() : 0;
        String content = tianApiService.cangtoushi(word, len);
        RESTResult<Map<String, String>> r = RESTResult.getSuccess(Map.of("content", content != null ? content : ""));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/material/hotword")
    @Operation(summary = "网络流行语")
    public RESTResult<List<Map<String, String>>> hotWord(@CurrentUserId Long userId, @RequestBody(required = false) Map<String, Object> body) {
        String word = body != null && body.get("word") != null ? body.get("word").toString() : "一哥";
        int num = body != null && body.get("num") != null ? ((Number) body.get("num")).intValue() : 5;
        RESTResult<List<Map<String, String>>> r = RESTResult.getSuccess(tianApiService.hotWord(word, num));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/material/dictum")
    @Operation(summary = "名言警句")
    public RESTResult<List<Map<String, String>>> dictum(@CurrentUserId Long userId, @RequestBody(required = false) Map<String, Object> body) {
        int num = body != null && body.get("num") != null ? ((Number) body.get("num")).intValue() : 5;
        RESTResult<List<Map<String, String>>> r = RESTResult.getSuccess(tianApiService.dictum(num));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/material/mingyan")
    @Operation(summary = "名人名言")
    public RESTResult<List<Map<String, String>>> mingyan(@CurrentUserId Long userId, @RequestBody(required = false) Map<String, Object> body) {
        int num = body != null && body.get("num") != null ? ((Number) body.get("num")).intValue() : 5;
        Integer typeid = body != null && body.get("typeid") != null ? ((Number) body.get("typeid")).intValue() : null;
        RESTResult<List<Map<String, String>>> r = RESTResult.getSuccess(tianApiService.mingyan(num, typeid));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/material/tiangou")
    @Operation(summary = "舔狗日记")
    public RESTResult<Map<String, String>> tiangouRiji(@CurrentUserId Long userId) {
        RESTResult<Map<String, String>> r = RESTResult.getSuccess(Map.of("content", nullToEmpty(tianApiService.tiangouRiji())));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/material/joke")
    @Operation(summary = "雷人笑话")
    public RESTResult<List<Map<String, String>>> joke(@CurrentUserId Long userId, @RequestBody(required = false) Map<String, Object> body) {
        int num = body != null && body.get("num") != null ? ((Number) body.get("num")).intValue() : 5;
        RESTResult<List<Map<String, String>>> r = RESTResult.getSuccess(tianApiService.joke(num));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/material/xiehouyu")
    @Operation(summary = "歇后语")
    public RESTResult<List<Map<String, String>>> xiehouyu(@CurrentUserId Long userId, @RequestBody(required = false) Map<String, Object> body) {
        int num = body != null && body.get("num") != null ? ((Number) body.get("num")).intValue() : 5;
        RESTResult<List<Map<String, String>>> r = RESTResult.getSuccess(tianApiService.xiehouyu(num));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/material/moodpoetry")
    @Operation(summary = "情绪诗句")
    public RESTResult<Map<String, String>> moodPoetry(@CurrentUserId Long userId) {
        RESTResult<Map<String, String>> r = RESTResult.getSuccess(Map.of("content", nullToEmpty(tianApiService.moodPoetry())));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/material/msdl")
    @Operation(summary = "民俗对联")
    public RESTResult<List<Map<String, String>>> msDuilian(@CurrentUserId Long userId, @RequestBody(required = false) Map<String, Object> body) {
        int num = body != null && body.get("num") != null ? ((Number) body.get("num")).intValue() : 5;
        String fenlei = body != null && body.get("fenlei") != null ? body.get("fenlei").toString() : "";
        RESTResult<List<Map<String, String>>> r = RESTResult.getSuccess(tianApiService.msDuilian(num, fenlei));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/material/flmj")
    @Operation(summary = "分类名句")
    public RESTResult<List<Map<String, String>>> flMingju(@CurrentUserId Long userId, @RequestBody(required = false) Map<String, Object> body) {
        String type = body != null && body.get("type") != null ? body.get("type").toString() : "春天";
        int num = body != null && body.get("num") != null ? ((Number) body.get("num")).intValue() : 5;
        RESTResult<List<Map<String, String>>> r = RESTResult.getSuccess(tianApiService.flMingju(type, num));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/material/zmsc")
    @Operation(summary = "最美宋词")
    public RESTResult<Map<String, String>> zuiMeiSongci(@CurrentUserId Long userId) {
        RESTResult<Map<String, String>> r = RESTResult.getSuccess(Map.of("content", nullToEmpty(tianApiService.zuiMeiSongci())));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/material/gjmj")
    @Operation(summary = "古籍名句")
    public RESTResult<Map<String, String>> guJiMingju(@CurrentUserId Long userId) {
        RESTResult<Map<String, String>> r = RESTResult.getSuccess(Map.of("content", nullToEmpty(tianApiService.guJiMingju())));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/material/lzmy")
    @Operation(summary = "励志古言")
    public RESTResult<Map<String, String>> liZhiGuyan(@CurrentUserId Long userId) {
        RESTResult<Map<String, String>> r = RESTResult.getSuccess(Map.of("content", nullToEmpty(tianApiService.liZhiGuyan())));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/material/hotreview")
    @Operation(summary = "云音乐热评")
    public RESTResult<Map<String, String>> hotReview(@CurrentUserId Long userId) {
        RESTResult<Map<String, String>> r = RESTResult.getSuccess(Map.of("content", nullToEmpty(tianApiService.hotReview())));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/material/mnpara")
    @Operation(summary = "小段子")
    public RESTResult<Map<String, String>> xiaoDuanzi(@CurrentUserId Long userId) {
        RESTResult<Map<String, String>> r = RESTResult.getSuccess(Map.of("content", nullToEmpty(tianApiService.xiaoDuanzi())));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/material/skl")
    @Operation(summary = "顺口溜")
    public RESTResult<Map<String, String>> shunKouliu(@CurrentUserId Long userId) {
        RESTResult<Map<String, String>> r = RESTResult.getSuccess(Map.of("content", nullToEmpty(tianApiService.shunKouliu())));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/material/sentence")
    @Operation(summary = "精美句子")
    public RESTResult<Map<String, String>> jingMeiJuzi(@CurrentUserId Long userId) {
        RESTResult<Map<String, String>> r = RESTResult.getSuccess(Map.of("content", nullToEmpty(tianApiService.jingMeiJuzi())));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/material/qingshi")
    @Operation(summary = "古代情诗")
    public RESTResult<Map<String, String>> guDaiQingshi(@CurrentUserId Long userId) {
        RESTResult<Map<String, String>> r = RESTResult.getSuccess(Map.of("content", nullToEmpty(tianApiService.guDaiQingshi())));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/material/hsjz")
    @Operation(summary = "失恋分手句子")
    public RESTResult<Map<String, String>> shiLianFenshou(@CurrentUserId Long userId) {
        RESTResult<Map<String, String>> r = RESTResult.getSuccess(Map.of("content", nullToEmpty(tianApiService.shiLianFenshou())));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/material/raokouling")
    @Operation(summary = "绕口令")
    public RESTResult<Map<String, String>> raoKouling(@CurrentUserId Long userId) {
        RESTResult<Map<String, String>> r = RESTResult.getSuccess(Map.of("content", nullToEmpty(tianApiService.raoKouling())));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    private static String nullToEmpty(String s) {
        return s != null ? s : "";
    }

    // ─── 合规/审核 ──────────────────────────────────────

    @PostMapping("/ad-review")
    @Operation(summary = "广告法违禁词检测")
    public RESTResult<AdReviewResultVO> adReview(@CurrentUserId Long userId, @RequestBody(required = false) Map<String, String> body) {
        String content = body != null ? body.get("content") : null;
        RESTResult<AdReviewResultVO> r = RESTResult.getSuccess(tianApiService.adReview(content));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/text-audit")
    @Operation(summary = "文本审核")
    public RESTResult<TextAuditResultVO> textAudit(@CurrentUserId Long userId, @RequestBody(required = false) Map<String, String> body) {
        String content = body != null ? body.get("content") : null;
        RESTResult<TextAuditResultVO> r = RESTResult.getSuccess(tianApiService.textAudit(content));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    // ─── 智能文案 ──────────────────────────────────────

    @PostMapping("/ai-text")
    @Operation(summary = "智能文案生成")
    public RESTResult<Map<String, String>> aiText(@CurrentUserId Long userId, @RequestBody(required = false) Map<String, String> body) {
        String text = body != null ? body.get("text") : null;
        String content = tianApiService.aiTextGenerate(text);
        RESTResult<Map<String, String>> r = RESTResult.getSuccess(Map.of("content", content != null ? content : ""));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    // ─── 节假日/简报 ──────────────────────────────────────

    @PostMapping("/jiejiari")
    @Operation(summary = "节假日查询")
    public RESTResult<JsonNode> jiejiari(@CurrentUserId Long userId, @RequestBody(required = false) Map<String, Object> body) {
        String date = body != null && body.get("date") != null ? body.get("date").toString() : java.time.LocalDate.now().toString();
        int type = body != null && body.get("type") != null ? ((Number) body.get("type")).intValue() : 0;
        JsonNode result = tianApiService.jiejiari(date, type);
        RESTResult<JsonNode> r = RESTResult.getSuccess(result);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/bulletin")
    @Operation(summary = "每日简报")
    public RESTResult<List<BulletinItemVO>> bulletin(@CurrentUserId Long userId) {
        RESTResult<List<BulletinItemVO>> r = RESTResult.getSuccess(tianApiService.bulletin());
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/status")
    @Operation(summary = "TianAPI 是否已配置")
    public RESTResult<Map<String, Boolean>> status() {
        RESTResult<Map<String, Boolean>> r = RESTResult.getSuccess(Map.of("enabled", tianApiService.isEnabled()));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/material/import")
    @Operation(summary = "手动触发素材入库到文案库")
    public RESTResult<Map<String, Object>> materialImport(@CurrentUserId Long userId) {
        Map<String, Object> result = materialImportService.runImport();
        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(result);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
