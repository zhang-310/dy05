package cn.gaifan.douyinOperations.module.tianapi.service.impl;

import cn.gaifan.douyinOperations.module.guiguiya.client.GuiguiyaHotClient;
import cn.gaifan.douyinOperations.module.tianapi.client.TianApiClient;
import cn.gaifan.douyinOperations.module.tianapi.config.TianApiProperties;
import cn.gaifan.douyinOperations.module.tianapi.service.TianApiService;
import cn.gaifan.douyinOperations.module.tianapi.vo.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.annotation.Resource;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * TianAPI 服务实现
 */
@Service
public class TianApiServiceImpl implements TianApiService {

    private static final Logger log = LoggerFactory.getLogger(TianApiServiceImpl.class);

    private static final String CACHE_KEY_DOUYIN = "hot:douyin";  // 鬼鬼鸭优先，TianAPI 兜底
    private static final String CACHE_KEY_TOUTIAO = "tianapi:hot:toutiao";
    private static final String CACHE_KEY_WEIBO = "tianapi:hot:weibo";
    private static final String CACHE_KEY_NETWORK = "tianapi:hot:network";
    private static final String CACHE_KEY_BAIDU = "tianapi:hot:baidu";
    private static final String CACHE_KEY_TENCENT = "tianapi:hot:tencent";

    @Resource
    private TianApiClient client;
    @Resource
    private TianApiProperties properties;
    @Autowired(required = false)
    private GuiguiyaHotClient guiguiyaHotClient;
    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean isEnabled() {
        return properties.isConfigured();
    }

    // ─── 热搜榜 ──────────────────────────────────────

    @Override
    public List<HotItemVO> douyinHot() {
        // 优先鬼鬼鸭（免费免 key），失败时兜底 TianAPI
        return getDouyinHotWithGuiguiyaFirst();
    }

    private List<HotItemVO> getDouyinHotWithGuiguiyaFirst() {
        if (redisTemplate != null) {
            String cached = redisTemplate.opsForValue().get(CACHE_KEY_DOUYIN);
            if (cached != null && !cached.isEmpty()) {
                try {
                    return parseCachedHot(cached);
                } catch (Exception e) {
                    log.warn("解析热搜缓存失败: {}", e.getMessage());
                }
            }
        }
        List<HotItemVO> list = Collections.emptyList();
        // 1. 尝试鬼鬼鸭（http://api.guiguiya.com/api/hotlist/dy）
        if (guiguiyaHotClient != null) {
            try {
                list = guiguiyaHotClient.fetchDouyinHot();
            } catch (Exception e) {
                log.warn("鬼鬼鸭获取失败: {}", e.getMessage());
            }
        }
        // 2. 鬼鬼鸭无数据时兜底 TianAPI
        if (list.isEmpty() && client != null) {
            JsonNode result = client.get("douyinhot/index");
            if (result != null) list = parseDouyinHot(result);
        }
        if (redisTemplate != null && !list.isEmpty()) {
            try {
                String json = objectMapper.writeValueAsString(list);
                int minutes = properties != null ? properties.getHotCacheMinutes() : 5;
                redisTemplate.opsForValue().set(CACHE_KEY_DOUYIN, json, Duration.ofMinutes(minutes));
            } catch (Exception e) {
                log.warn("写入抖音热搜缓存失败: {}", e.getMessage());
            }
        }
        return list;
    }

    @Override
    public List<HotItemVO> toutiaoHot() {
        return getHotWithCache(CACHE_KEY_TOUTIAO, "toutiaohot/index", "toutiao", this::parseToutiaoHot);
    }

    @Override
    public List<HotItemVO> weiboHot() {
        return getHotWithCache(CACHE_KEY_WEIBO, "weibohot/index", "weibo", this::parseWeiboHot);
    }

    @Override
    public List<HotItemVO> networkHot() {
        return getHotWithCache(CACHE_KEY_NETWORK, "networkhot/index", "network", this::parseNetworkHot);
    }

    @Override
    public List<HotItemVO> baiduHot() {
        return getHotWithCache(CACHE_KEY_BAIDU, "nethot/index", "baidu", this::parseBaiduHot);
    }

    @Override
    public List<HotItemVO> tencentHot() {
        return getHotWithCache(CACHE_KEY_TENCENT, "wxhottopic/index", "tencent", this::parseTencentHot);
    }

    @SuppressWarnings("unchecked")
    private List<HotItemVO> getHotWithCache(String cacheKey, String path, String source,
                                            java.util.function.Function<JsonNode, List<HotItemVO>> parser) {
        if (redisTemplate != null) {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null && !cached.isEmpty()) {
                try {
                    return parseCachedHot(cached);
                } catch (Exception e) {
                    log.warn("解析热搜缓存失败: {}", e.getMessage());
                }
            }
        }
        JsonNode result = client.get(path);
        List<HotItemVO> list = result == null ? Collections.emptyList() : parser.apply(result);
        if (redisTemplate != null && !list.isEmpty()) {
            try {
                String json = objectMapper.writeValueAsString(list);
                redisTemplate.opsForValue().set(cacheKey, json, Duration.ofMinutes(properties.getHotCacheMinutes()));
            } catch (Exception e) {
                log.warn("写入{}缓存失败: {}", source, e.getMessage());
            }
        }
        return list;
    }

    private List<HotItemVO> parseCachedHot(String json) {
        try {
            List<HotItemVO> list = objectMapper.readValue(json, new TypeReference<>() {});
            return list != null ? list : Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private List<HotItemVO> parseDouyinHot(JsonNode result) {
        List<JsonNode> items = TianApiClient.toList(result);
        return items.stream()
                .map(n -> new HotItemVO(
                        TianApiClient.text(n, "word"),
                        String.valueOf(TianApiClient.num(n, "label", 0)),
                        (long) TianApiClient.num(n, "hotindex", 0),
                        "douyin"
                ))
                .collect(Collectors.toList());
    }

    private List<HotItemVO> parseToutiaoHot(JsonNode result) {
        List<JsonNode> items = TianApiClient.toList(result);
        return items.stream()
                .map(n -> new HotItemVO(
                        TianApiClient.text(n, "word"),
                        "",
                        (long) TianApiClient.num(n, "hotindex", 0),
                        "toutiao"
                ))
                .collect(Collectors.toList());
    }

    private List<HotItemVO> parseWeiboHot(JsonNode result) {
        List<JsonNode> items = TianApiClient.toList(result);
        return items.stream()
                .map(n -> {
                    String numStr = TianApiClient.text(n, "hotwordnum").replaceAll("[^0-9]", "");
                    long hot = numStr.isEmpty() ? 0 : Long.parseLong(numStr);
                    return new HotItemVO(TianApiClient.text(n, "hotword"), TianApiClient.text(n, "hottag"), hot, "weibo");
                })
                .collect(Collectors.toList());
    }

    private List<HotItemVO> parseNetworkHot(JsonNode result) {
        List<JsonNode> items = TianApiClient.toList(result);
        return items.stream()
                .map(n -> new HotItemVO(
                        TianApiClient.text(n, "title"),
                        "",
                        (long) TianApiClient.num(n, "hotnum", 0),
                        "network"
                ))
                .collect(Collectors.toList());
    }

    private List<HotItemVO> parseBaiduHot(JsonNode result) {
        List<JsonNode> items = TianApiClient.toList(result);
        return items.stream()
                .map(n -> {
                    String idx = TianApiClient.text(n, "index").replaceAll("[^0-9]", "");
                    long hot = idx.isEmpty() ? 0 : Long.parseLong(idx);
                    return new HotItemVO(TianApiClient.text(n, "keyword"), TianApiClient.text(n, "trend"), hot, "baidu");
                })
                .collect(Collectors.toList());
    }

    private List<HotItemVO> parseTencentHot(JsonNode result) {
        List<JsonNode> items = TianApiClient.toList(result);
        return items.stream()
                .map(n -> new HotItemVO(
                        TianApiClient.text(n, "word"),
                        "",
                        (long) TianApiClient.num(n, "index", 0),
                        "tencent"
                ))
                .collect(Collectors.toList());
    }

    // ─── 文案/话术素材 ──────────────────────────────────────

    @Override
    public String pyqWenan() {
        return getContent("pyqwenan/index", "content", "source");
    }

    @Override
    public String dagongrenYulu() {
        return getContent("dgryl/index", "content");
    }

    @Override
    public String tuweiQinghua() {
        return getContent("saylove/index", "content");
    }

    @Override
    public String duJitang() {
        return getContent("dujitang/index", "content");
    }

    @Override
    public String caihongPi() {
        return getContent("caihongpi/index", "content");
    }

    @Override
    public String zhananYulu() {
        return getContent("zhanan/index", "content");
    }

    @Override
    public String zaoAnXinyu() {
        return getContent("zaoan/index", "content");
    }

    @Override
    public String wanAnXinyu() {
        return getContent("wanan/index", "content");
    }

    @Override
    public Map<String, String> classicDialogue() {
        JsonNode result = client.get("dialogue/index");
        if (result == null) return Collections.emptyMap();
        JsonNode item = TianApiClient.toList(result).stream().findFirst().orElse(null);
        if (item == null) return Collections.emptyMap();
        Map<String, String> m = new HashMap<>();
        m.put("dialogue", TianApiClient.text(item, "dialogue"));
        m.put("english", TianApiClient.text(item, "english"));
        m.put("source", TianApiClient.text(item, "source"));
        return m;
    }

    @Override
    public List<Map<String, String>> godReply(int num) {
        int n = Math.max(1, Math.min(10, num));
        Map<String, String> params = Map.of("num", String.valueOf(n));
        JsonNode result = client.post("godreply/index", params);
        List<JsonNode> items = TianApiClient.toList(result);
        return items.stream()
                .map(node -> {
                    Map<String, String> m = new HashMap<>();
                    m.put("title", TianApiClient.text(node, "title"));
                    m.put("content", TianApiClient.text(node, "content"));
                    return m;
                })
                .collect(Collectors.toList());
    }

    @Override
    public String cangtoushi(String word, int len) {
        if (!StringUtils.hasText(word)) return "";
        String w = word.length() > 8 ? word.substring(0, 8) : word;
        if (w.length() < 2) return "";
        Map<String, String> params = new HashMap<>();
        params.put("word", w);
        params.put("len", String.valueOf(len == 1 ? 1 : 0)); // 0五言 1七言
        JsonNode result = client.post("cangtoushi/index", params);
        if (result == null) return "";
        String content = TianApiClient.text(result, "content");
        if (StringUtils.hasText(content)) return content;
        JsonNode first = TianApiClient.toList(result).stream().findFirst().orElse(null);
        return first != null ? TianApiClient.text(first, "content") : "";
    }

    private String getContent(String path, String... keys) {
        JsonNode result = client.get(path);
        if (result == null) return "";
        for (String key : keys) {
            String v = TianApiClient.text(result, key);
            if (key.equals("content") && StringUtils.hasText(v)) return v;
        }
        return TianApiClient.text(result, "content");
    }

    @Override
    public List<Map<String, String>> hotWord(String word, int num) {
        if (!StringUtils.hasText(word)) return Collections.emptyList();
        int n = Math.max(1, Math.min(20, num));
        Map<String, String> params = Map.of("word", word, "num", String.valueOf(n));
        JsonNode result = client.post("hotword/index", params);
        List<JsonNode> items = TianApiClient.toList(result);
        return items.stream()
                .map(node -> {
                    Map<String, String> m = new HashMap<>();
                    m.put("title", TianApiClient.text(node, "title"));
                    m.put("content", TianApiClient.text(node, "content"));
                    return m;
                })
                .filter(m -> StringUtils.hasText(m.get("content")))
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, String>> dictum(int num) {
        int n = Math.max(1, Math.min(20, num));
        Map<String, String> params = Map.of("num", String.valueOf(n));
        JsonNode result = client.post("dictum/index", params);
        List<JsonNode> items = TianApiClient.toList(result);
        return items.stream()
                .map(node -> {
                    Map<String, String> m = new HashMap<>();
                    m.put("mrname", TianApiClient.text(node, "mrname"));
                    m.put("content", TianApiClient.text(node, "content"));
                    return m;
                })
                .filter(m -> StringUtils.hasText(m.get("content")))
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, String>> mingyan(int num, Integer typeid) {
        int n = Math.max(1, Math.min(20, num));
        Map<String, String> params = new HashMap<>();
        params.put("num", String.valueOf(n));
        if (typeid != null && typeid >= 1 && typeid <= 24) {
            params.put("typeid", String.valueOf(typeid));
        }
        JsonNode result = client.post("mingyan/index", params);
        List<JsonNode> items = TianApiClient.toList(result);
        return items.stream()
                .map(node -> {
                    Map<String, String> m = new HashMap<>();
                    m.put("author", TianApiClient.text(node, "author"));
                    m.put("content", TianApiClient.text(node, "content"));
                    m.put("typeid", String.valueOf(TianApiClient.num(node, "typeid", 0)));
                    return m;
                })
                .filter(m -> StringUtils.hasText(m.get("content")))
                .collect(Collectors.toList());
    }

    @Override
    public String tiangouRiji() {
        return getContent("tiangou/index", "content");
    }

    @Override
    public List<Map<String, String>> joke(int num) {
        int n = Math.max(1, Math.min(10, num));
        Map<String, String> params = Map.of("num", String.valueOf(n));
        JsonNode result = client.post("joke/index", params);
        return parseListToMap(result, "title", "content");
    }

    @Override
    public List<Map<String, String>> xiehouyu(int num) {
        int n = Math.max(1, Math.min(10, num));
        Map<String, String> params = Map.of("num", String.valueOf(n));
        JsonNode result = client.post("xiehou/index", params);
        List<JsonNode> items = TianApiClient.toList(result);
        return items.stream()
                .map(node -> {
                    Map<String, String> m = new HashMap<>();
                    String quest = TianApiClient.text(node, "quest");
                    String answer = TianApiClient.text(node, "result");
                    String content = StringUtils.hasText(quest) && StringUtils.hasText(answer)
                            ? quest + " — " + answer : (quest + answer);
                    m.put("quest", quest);
                    m.put("content", content);
                    return m;
                })
                .filter(m -> StringUtils.hasText(m.get("content")))
                .collect(Collectors.toList());
    }

    @Override
    public String moodPoetry() {
        return getContent("moodpoetry/index", "content");
    }

    @Override
    public List<Map<String, String>> msDuilian(int num, String fenlei) {
        int n = Math.max(1, Math.min(10, num));
        Map<String, String> params = new HashMap<>();
        params.put("num", String.valueOf(n));
        if (StringUtils.hasText(fenlei)) params.put("fenlei", fenlei);
        JsonNode result = client.post("msdl/index", params);
        List<JsonNode> items = TianApiClient.toList(result);
        return items.stream()
                .map(node -> {
                    Map<String, String> m = new HashMap<>();
                    String shang = TianApiClient.text(node, "shanglian");
                    String xia = TianApiClient.text(node, "xialian");
                    String heng = TianApiClient.text(node, "hengpi");
                    StringBuilder sb = new StringBuilder();
                    if (StringUtils.hasText(shang)) sb.append(shang);
                    if (StringUtils.hasText(xia)) sb.append(sb.length() > 0 ? "\n" + xia : xia);
                    if (StringUtils.hasText(heng)) sb.append(sb.length() > 0 ? "\n" + heng : heng);
                    m.put("content", sb.toString());
                    m.put("fenlei", TianApiClient.text(node, "fenlei"));
                    return m;
                })
                .filter(m -> StringUtils.hasText(m.get("content")))
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, String>> flMingju(String type, int num) {
        if (!StringUtils.hasText(type)) return Collections.emptyList();
        int n = Math.max(1, Math.min(20, num));
        Map<String, String> params = Map.of("type", type, "num", String.valueOf(n));
        JsonNode result = client.post("flmj/index", params);
        return parseListToMap(result, "source", "content");
    }

    @Override
    public String zuiMeiSongci() {
        return getContent("zmsc/index", "content");
    }

    @Override
    public String guJiMingju() {
        return getContent("gjmj/index", "content");
    }

    @Override
    public String liZhiGuyan() {
        JsonNode result = client.get("lzmy/index");
        if (result == null) return "";
        String saying = TianApiClient.text(result, "saying");
        if (StringUtils.hasText(saying)) {
            String source = TianApiClient.text(result, "source");
            return StringUtils.hasText(source) ? saying + " — " + source : saying;
        }
        return TianApiClient.text(result, "content");
    }

    @Override
    public String jokeOne() {
        List<Map<String, String>> list = joke(1);
        if (list != null && !list.isEmpty()) {
            String title = list.get(0).getOrDefault("title", "");
            String content = list.get(0).getOrDefault("content", "");
            return StringUtils.hasText(title) ? title + "\n" + content : content;
        }
        return "";
    }

    @Override
    public String hotReview() {
        return getContent("hotreview/index", "content");
    }

    @Override
    public String xiaoDuanzi() {
        return getContent("mnpara/index", "content");
    }

    @Override
    public String shunKouliu() {
        return getContent("skl/index", "content");
    }

    @Override
    public String jingMeiJuzi() {
        return getContent("sentence/index", "content");
    }

    @Override
    public String guDaiQingshi() {
        return getContent("qingshi/index", "content");
    }

    @Override
    public String shiLianFenshou() {
        return getContent("hsjz/index", "content");
    }

    @Override
    public String raoKouling() {
        // TianAPI 官方路径为 rkl/index，见 https://www.tianapi.com/apiview/37
        return getContent("rkl/index", "content");
    }

    private List<Map<String, String>> parseListToMap(JsonNode result, String titleKey, String contentKey) {
        if (result == null) return Collections.emptyList();
        List<JsonNode> items = TianApiClient.toList(result);
        return items.stream()
                .map(node -> {
                    Map<String, String> m = new HashMap<>();
                    m.put("title", TianApiClient.text(node, titleKey));
                    m.put("content", TianApiClient.text(node, contentKey));
                    return m;
                })
                .filter(m -> StringUtils.hasText(m.get("content")))
                .collect(Collectors.toList());
    }

    // ─── 合规/审核（按次计费） ──────────────────────────────────────

    @Override
    public AdReviewResultVO adReview(String content) {
        if (!StringUtils.hasText(content)) {
            return new AdReviewResultVO(true, "合规", Collections.emptyList());
        }
        String trimmed = content.length() > 1000 ? content.substring(0, 1000) : content;
        JsonNode result = client.post("adreview/index", Map.of("content", trimmed));
        if (result == null) {
            return new AdReviewResultVO(true, "检测异常，请重试", Collections.emptyList());
        }
        int conType = TianApiClient.num(result, "con_type", 1);
        String con = TianApiClient.text(result, "con");
        List<String> words = new ArrayList<>();
        JsonNode arr = result.path("words");
        if (arr.isArray()) {
            arr.forEach(n -> words.add(n.asText()));
        }
        boolean compliant = conType == 1;
        return new AdReviewResultVO(compliant, con, words);
    }

    @Override
    public TextAuditResultVO textAudit(String content) {
        if (!StringUtils.hasText(content)) {
            return new TextAuditResultVO(true, "合规", "", Collections.emptyList());
        }
        String trimmed = content.length() > 1000 ? content.substring(0, 1000) : content;
        JsonNode result = client.post("antispam/index", Map.of("content", trimmed));
        if (result == null) {
            return new TextAuditResultVO(true, "审核异常，请重试", "", Collections.emptyList());
        }
        int conType = TianApiClient.num(result, "con_type", 1);
        String con = TianApiClient.text(result, "con");
        // 官方文档：msg 在 result.list 内，兼容顶层
        JsonNode listNode = result.path("list");
        String msg = listNode.isObject() ? TianApiClient.text(listNode, "msg") : "";
        if (!StringUtils.hasText(msg)) msg = TianApiClient.text(result, "msg");
        List<String> words = new ArrayList<>();
        JsonNode arr = listNode.isObject() ? listNode.path("words") : result.path("words");
        if (arr.isArray()) {
            arr.forEach(n -> words.add(n.asText()));
        }
        boolean compliant = conType == 1;
        return new TextAuditResultVO(compliant, con, msg, words);
    }

    // ─── 智能文案 ──────────────────────────────────────

    @Override
    public String aiTextGenerate(String text) {
        if (!StringUtils.hasText(text)) return "";
        String t = text.length() > 60 ? text.substring(0, 60) : text;
        JsonNode result = client.post("aitextcall/index", Map.of("text", t));
        return result == null ? "" : TianApiClient.text(result, "content");
    }

    // ─── 节假日/简报 ──────────────────────────────────────

    @Override
    public JsonNode jiejiari(String date, int type) {
        Map<String, String> params = new HashMap<>();
        params.put("date", date);
        params.put("type", String.valueOf(type));
        JsonNode result = client.post("jiejiari/index", params);
        if (result == null) {
            // code 160 等：未申请接口或 TianAPI 返回空，返回友好结构避免 null
            ObjectNode node = objectMapper.createObjectNode();
            node.put("tip", "TianAPI 返回空，请在天聚数行控制台申请「节假日」接口");
            node.putArray("list");
            return node;
        }
        return result;
    }

    @Override
    public List<BulletinItemVO> bulletin() {
        JsonNode result = client.get("bulletin/index");
        List<JsonNode> items = TianApiClient.toList(result);
        return items.stream()
                .map(n -> new BulletinItemVO(
                        TianApiClient.text(n, "title"),
                        TianApiClient.text(n, "digest"),
                        TianApiClient.text(n, "mtime")
                ))
                .collect(Collectors.toList());
    }
}
