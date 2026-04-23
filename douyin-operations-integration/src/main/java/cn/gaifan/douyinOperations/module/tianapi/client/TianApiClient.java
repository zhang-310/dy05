package cn.gaifan.douyinOperations.module.tianapi.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import cn.gaifan.douyinOperations.module.tianapi.exception.TianApiQuotaExceededException;
import jakarta.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * TianAPI（天聚数行）统一 HTTP 客户端
 * 文档：https://www.tianapi.com
 */
@Component
public class TianApiClient {

    private static final Logger log = LoggerFactory.getLogger(TianApiClient.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Resource
    private RestTemplate restTemplate;
    @Resource
    private cn.gaifan.douyinOperations.module.tianapi.config.TianApiProperties properties;

    /**
     * 通用 GET 请求
     *
     * @param path 如 douyinhot/index
     * @return 解析后的 result 节点，失败返回 null
     */
    public JsonNode get(String path) {
        return request(path, null);
    }

    /**
     * 通用 POST 请求（表单）
     *
     * @param path 如 adreview/index
     * @param params key-value 表单参数
     * @return 解析后的 result 节点，失败返回 null
     */
    public JsonNode post(String path, Map<String, String> params) {
        return request(path, params);
    }

    private JsonNode request(String path, Map<String, String> formParams) {
        if (!properties.isConfigured()) {
            log.warn("TianAPI 未配置或已禁用，跳过调用: {}", path);
            return null;
        }
        String url = properties.getBaseUrl() + "/" + path + "?key=" + properties.getApiKey();
        try {
            ResponseEntity<String> resp;
            if (formParams != null && !formParams.isEmpty()) {
                MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
                formParams.forEach(map::add);
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
                HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(map, headers);
                resp = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            } else {
                resp = restTemplate.getForEntity(url, String.class);
            }
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                JsonNode root = objectMapper.readTree(resp.getBody());
                int code = root.path("code").asInt(0);
                if (code == 200) {
                    return root.path("result");
                }
                String msg = root.path("msg").asText("");
                // code 150：该类目当日配额用尽（每类约1万次/天），抛异常以便该分类停止、继续其他类目
                if (code == 150) {
                    throw new TianApiQuotaExceededException(path, code, msg);
                }
                // code 250: 暂无数据（正常情况，降级 DEBUG）
                if (code == 250) {
                    log.debug("TianAPI 暂无数据: path={}, msg={}", path, msg);
                } else {
                    log.warn("TianAPI 业务异常: path={}, code={}, msg={}", path, code, msg);
                }
            }
        } catch (TianApiQuotaExceededException e) {
            throw e;
        } catch (Exception e) {
            log.error("TianAPI 请求失败: path={}", path, e);
        }
        return null;
    }

    /**
     * 将 result 转为数组，兼容 list、result.list、单对象
     */
    public static List<JsonNode> toList(JsonNode result) {
        if (result == null) return Collections.emptyList();
        if (result.isArray()) {
            return StreamSupport.stream(result.spliterator(), false).collect(Collectors.toList());
        }
        JsonNode list = result.path("list");
        if (list.isArray()) {
            return StreamSupport.stream(list.spliterator(), false).collect(Collectors.toList());
        }
        return Collections.singletonList(result);
    }

    public static String text(JsonNode n, String key) {
        if (n == null) return "";
        JsonNode v = n.path(key);
        return v.isMissingNode() ? "" : v.asText("");
    }

    public static int num(JsonNode n, String key, int def) {
        if (n == null) return def;
        JsonNode v = n.path(key);
        return v.isMissingNode() ? def : v.asInt(def);
    }
}
