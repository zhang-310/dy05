package cn.gaifan.douyinOperations.module.guiguiya.client;

import cn.gaifan.douyinOperations.module.guiguiya.config.GuiguiyaProperties;
import cn.gaifan.douyinOperations.module.tianapi.vo.HotItemVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 鬼鬼鸭 API 客户端（抖音热搜实时上升热点）
 * API: http://api.guiguiya.com/api/hotlist/dy
 */
@Component
public class GuiguiyaHotClient {

    private static final Logger log = LoggerFactory.getLogger(GuiguiyaHotClient.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Pattern HOT_ID_PATTERN = Pattern.compile("/hot/(\\d+)(?:\\?|$|/)");

    @Resource
    private RestTemplate restTemplate;
    @Resource
    private GuiguiyaProperties properties;

    /**
     * 拉取抖音热搜（实时上升热点）
     * 返回与 HotItemVO 兼容格式，含 link、hotZh、position 扩展字段
     */
    public List<HotItemVO> fetchDouyinHot() {
        if (!properties.isConfigured()) {
            log.debug("鬼鬼鸭未启用，跳过拉取");
            return List.of();
        }
        try {
            ResponseEntity<String> resp = restTemplate.getForEntity(properties.getDouyinHotUrl(), String.class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                return parseResponse(resp.getBody());
            }
        } catch (Exception e) {
            log.warn("鬼鬼鸭抖音热搜请求失败: {}", e.getMessage());
        }
        return List.of();
    }

    private List<HotItemVO> parseResponse(String json) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        int code = root.path("code").asInt(0);
        if (code != 200) {
            log.warn("鬼鬼鸭返回非成功: code={}, msg={}", code, root.path("msg").asText(""));
            return List.of();
        }
        JsonNode data = root.path("data");
        if (!data.isArray()) return List.of();

        List<HotItemVO> list = new ArrayList<>();
        for (JsonNode item : data) {
            String word = item.path("word").asText("");
            if (word.isBlank()) continue;

            int position = item.path("position").asInt(0);
            long hot = item.path("hot").asLong(0);
            String hotZh = item.has("hot_zh") ? item.path("hot_zh").asText("") : "";
            String link = item.has("vieo_link") ? item.path("vieo_link").asText("") :  // 接口拼写
                    (item.has("video_link") ? item.path("video_link").asText("") : "");

            HotItemVO vo = new HotItemVO(word, String.valueOf(position), hot, "douyin");
            vo.setPosition(position);
            vo.setHotZh(hotZh);
            vo.setLink(link);
            list.add(vo);
        }
        log.debug("鬼鬼鸭抖音热搜拉取成功: {} 条", list.size());
        return list;
    }

    /** 从链接提取 douyin_hot_id，如 https://www.douyin.com/hot/2428956 -> 2428956 */
    public static String extractHotId(String link) {
        if (link == null || link.isBlank()) return null;
        Matcher m = HOT_ID_PATTERN.matcher(link);
        return m.find() ? m.group(1) : null;
    }
}
