package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.ai.entity.AiEvolveTopic;
import cn.gaifan.douyinOperations.module.ai.repository.AiEvolveTopicRepository;
import cn.gaifan.douyinOperations.module.ai.service.EvolveTopicImportService;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 从老系统 JSON/TXT 导入进化主题
 * JSON 支持格式：["主题1","主题2"] 或 [{"topic":"主题1","category":"live"},...]
 * TXT 格式：每行一个主题
 */
@Service
public class EvolveTopicImportServiceImpl implements EvolveTopicImportService {

    private static final Logger log = LoggerFactory.getLogger(EvolveTopicImportServiceImpl.class);
    private static final String SOURCE_IMPORT = "manual";

    @Resource
    private AiEvolveTopicRepository topicRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TopicImportResult importFromFile(String sourcePath, Long kbId) {
        if (sourcePath == null || sourcePath.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "导入路径不能为空");
        }

        Path file = Path.of(sourcePath.trim());
        if (!Files.isRegularFile(file)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "路径不是有效文件: " + sourcePath);
        }

        String name = file.getFileName().toString().toLowerCase();
        if (!name.endsWith(".json") && !name.endsWith(".txt")) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "仅支持 .json 或 .txt 文件");
        }

        List<RawTopic> rawTopics;
        try {
            String content = Files.readString(file, StandardCharsets.UTF_8);
            rawTopics = name.endsWith(".json")
                    ? parseJson(content)
                    : parseTxt(content);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "读取文件失败: " + e.getMessage());
        }

        List<String> errors = new ArrayList<>();
        int success = 0, skipped = 0;

        for (RawTopic raw : rawTopics) {
            String topic = raw.topic == null ? null : raw.topic.trim();
            if (topic == null || topic.isEmpty()) {
                skipped++;
                continue;
            }
            if (topic.length() > 256) {
                topic = topic.substring(0, 256);
            }

            if (topicRepository.findByTopicAndKbIdAndDeleted(topic, kbId, 0).isPresent()) {
                skipped++;
                continue;
            }

            try {
                AiEvolveTopic t = new AiEvolveTopic();
                t.setKbId(kbId);
                t.setTopic(topic);
                t.setCategory(raw.category != null && !raw.category.isBlank() ? raw.category.trim() : "basic");
                t.setPriority(raw.priority != null ? raw.priority : 100);
                t.setSource(SOURCE_IMPORT);
                t.setStatus(1);
                topicRepository.save(t);
                success++;
                log.info("导入主题: {} (category={}) -> kbId={}", topic, t.getCategory(), kbId);
            } catch (Exception e) {
                errors.add(topic + ": " + e.getMessage());
            }
        }

        List<String> consolidatedErrors = consolidateErrors(errors);
        int total = rawTopics.size();
        String hint = null;
        if (total == 0) {
            hint = "解析后为空，请检查文件格式（支持根数组 [\"主题1\",...] 或 {\"queries\":[...]}）";
            log.warn("主题导入: 文件 {} {}", sourcePath, hint);
        } else if (success == 0 && skipped == total) {
            hint = "共 " + total + " 条主题均已存在（可能已由系统默认初始化），已跳过";
            log.info("主题导入: 文件 {} {}", sourcePath, hint);
        } else if (success == 0 && !consolidatedErrors.isEmpty()) {
            hint = "全部导入失败，请查看下方错误明细";
        }
        log.info("主题导入完成: 文件={}, 总数={}, 成功={}, 跳过={}, 失败={}", sourcePath, total, success, skipped, consolidatedErrors.size());
        return new TopicImportResult(total, success, skipped, consolidatedErrors, hint);
    }

    private List<RawTopic> parseJson(String content) {
        List<RawTopic> list = new ArrayList<>();
        Object parsed = JSON.parse(content);
        JSONArray arr = null;
        if (parsed instanceof JSONArray a) {
            arr = a;
        } else if (parsed instanceof JSONObject obj) {
            // 老系统常见格式：{"queries":[...]}、{"topics":[...]}、{"list":[...]}
            for (String key : new String[]{"queries", "topics", "list", "items", "data"}) {
                Object val = obj.get(key);
                if (val instanceof JSONArray a) {
                    arr = a;
                    break;
                }
            }
        }
        if (arr == null) {
            log.warn("JSON 格式无法识别，期望根数组或含 queries/topics/list 的对象");
            return list;
        }
        for (int i = 0; i < arr.size(); i++) {
            Object item = arr.get(i);
            if (item instanceof String s) {
                list.add(new RawTopic(s, "basic", 100));
            } else if (item instanceof JSONObject obj) {
                String topic = obj.getString("topic");
                if (topic == null) topic = obj.getString("query");
                if (topic == null) topic = obj.getString("text");
                if (topic == null) topic = obj.getString("name");
                String category = obj.getString("category");
                Integer priority = obj.getInteger("priority");
                list.add(new RawTopic(topic, category, priority));
            } else if (item != null) {
                list.add(new RawTopic(String.valueOf(item), "basic", 100));
            }
        }
        return list;
    }

    private List<RawTopic> parseTxt(String content) {
        List<RawTopic> list = new ArrayList<>();
        for (String line : content.split("\\r?\\n")) {
            String t = line.trim();
            if (!t.isEmpty() && !t.startsWith("#")) {
                list.add(new RawTopic(t, "basic", 100));
            }
        }
        return list;
    }

    private record RawTopic(String topic, String category, Integer priority) {}

    private List<String> consolidateErrors(List<String> raw) {
        if (raw == null || raw.isEmpty()) return raw;
        Map<String, List<String>> byReason = new LinkedHashMap<>();
        for (String e : raw) {
            String reason = e;
            int colon = e.indexOf(": ");
            if (colon > 0) reason = e.substring(colon + 2).trim();
            String norm = reason.length() > 60 ? reason.substring(0, 60) + "…" : reason;
            byReason.computeIfAbsent(norm, k -> new ArrayList<>()).add(colon > 0 ? e.substring(0, colon).trim() : e);
        }
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, List<String>> ent : byReason.entrySet()) {
            if (result.size() >= 3) break;
            String reason = ent.getKey();
            List<String> titles = ent.getValue();
            if (titles.size() >= 2) {
                result.add(String.format("以下 %d 条：%s", titles.size(), reason));
            } else {
                result.add(titles.get(0) + ": " + reason);
            }
        }
        if (byReason.size() > 3) result.add("……及其他 " + (byReason.size() - 3) + " 类错误");
        return result;
    }
}
