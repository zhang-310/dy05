package cn.gaifan.douyinOperations.module.script.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.repository.AiModelRepository;
import cn.gaifan.douyinOperations.module.ai.service.LlmClient;
import cn.gaifan.douyinOperations.module.script.entity.UserViolationWord;
import cn.gaifan.douyinOperations.module.script.entity.ViolationWord;
import cn.gaifan.douyinOperations.module.script.repository.UserViolationWordRepository;
import cn.gaifan.douyinOperations.module.script.repository.ViolationWordRepository;
import cn.gaifan.douyinOperations.module.script.service.ViolationWordService;
import cn.gaifan.douyinOperations.module.script.vo.*;
import com.github.benmanes.caffeine.cache.Cache;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import jakarta.persistence.criteria.Predicate;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ViolationWordServiceImpl implements ViolationWordService {

    private static final Logger log = LoggerFactory.getLogger(ViolationWordServiceImpl.class);
    private static final Set<String> SORTABLE = Set.of("id", "word", "level", "status", "createTime");

    @Resource
    private ViolationWordRepository violationWordRepository;
    @Resource
    private UserViolationWordRepository userViolationWordRepository;
    @Resource(name = "violationWordCache")
    private Cache<String, Object> violationWordCache;

    @Resource
    private LlmClient llmClient;

    @Resource
    private AiModelRepository aiModelRepository;

    @Override
    public PageResultVO<ViolationWordVO> search(ViolationWordSearchVO vo) {
        vo.validateParams();
        String sortName = SORTABLE.contains(vo.getSortName()) ? vo.getSortName() : "id";
        Pageable pageable = PageRequest.of(vo.getPage(), vo.getRows(),
                Sort.by("desc".equalsIgnoreCase(vo.getSortOrder()) ? Sort.Direction.DESC : Sort.Direction.ASC, sortName));

        Specification<ViolationWord> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("deleted"), 0));
            if (vo.getKeyword() != null && !vo.getKeyword().isBlank()) {
                predicates.add(cb.like(root.get("word"), "%" + vo.getKeyword().trim() + "%"));
            }
            if (vo.getLevel() != null) predicates.add(cb.equal(root.get("level"), vo.getLevel()));
            if (vo.getReason() != null && !vo.getReason().isBlank()) {
                predicates.add(cb.equal(root.get("reason"), vo.getReason().trim()));
            }
            if (vo.getStatus() != null) predicates.add(cb.equal(root.get("status"), vo.getStatus()));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<ViolationWord> page = violationWordRepository.findAll(spec, pageable);
        return PageResultVO.of(page.getTotalElements(),
                page.getContent().stream().map(this::toVO).collect(Collectors.toList()),
                vo.getPage(), vo.getRows());
    }

    @Override
    public ViolationWordVO getById(Long id) {
        if (id == null || id <= 0) throw new BusinessException(ErrorCode.VALIDATION_FAIL, "违规词 ID 无效");
        return toVO(violationWordRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "违规词不存在")));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long save(ViolationWordSaveVO vo) {
        ViolationWord entity;
        if (vo.getId() != null && vo.getId() > 0) {
            entity = violationWordRepository.findByIdAndDeleted(vo.getId(), 0)
                    .orElseThrow(() -> new BusinessException(ErrorCode.VIOLATION_WORD_NOT_FOUND, "违规词不存在"));
        } else {
            if (violationWordRepository.existsByWordAndDeleted(vo.getWord(), 0)) {
                throw new BusinessException(ErrorCode.VIOLATION_WORD_EXISTS, "违规词已存在");
            }
            entity = new ViolationWord();
        }
        entity.setWord(vo.getWord());
        entity.setLevel(vo.getLevel());
        if (vo.getReason() != null) entity.setReason(vo.getReason());
        if (vo.getReplacement() != null) entity.setReplacement(vo.getReplacement());
        if (vo.getScope() != null && !vo.getScope().isBlank()) entity.setScope(vo.getScope().trim());
        else if (entity.getScope() == null) entity.setScope("all");
        if (vo.getStatus() != null) entity.setStatus(vo.getStatus());
        ViolationWord saved = violationWordRepository.save(entity);
        violationWordCache.invalidateAll();
        return saved.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (id == null || id <= 0) throw new BusinessException(ErrorCode.VALIDATION_FAIL, "违规词 ID 无效");
        ViolationWord entity = violationWordRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.VIOLATION_WORD_NOT_FOUND, "违规词不存在"));
        entity.setDeleted(1);
        violationWordRepository.save(entity);
        violationWordCache.invalidateAll();
    }

    @Override
    public List<ViolationWordVO> listActive() {
        return violationWordRepository.findByStatusAndDeleted(1, 0)
                .stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public ViolationCheckResultVO check(String text, String scope, Long userId) {
        if (text == null || text.isBlank()) {
            ViolationCheckResultVO result = new ViolationCheckResultVO();
            result.setHasViolation(false);
            result.setTotalCount(0);
            result.setViolations(Collections.emptyList());
            return result;
        }
        Map<String, WordEntry> merged = buildMergedWordMap(scope, userId);
        List<ViolationCheckResultVO.ViolationHitVO> hits = scanTextWithMerged(text, merged);
        ViolationCheckResultVO result = new ViolationCheckResultVO();
        result.setHasViolation(!hits.isEmpty());
        result.setTotalCount(hits.size());
        result.setViolations(hits);
        return result;
    }

    @Override
    public ViolationCheckBatchResultVO checkBatch(ViolationCheckBatchVO vo, Long userId) {
        if (vo == null || vo.getTexts() == null || vo.getTexts().isEmpty()) {
            ViolationCheckBatchResultVO result = new ViolationCheckBatchResultVO();
            result.setResults(Collections.emptyMap());
            result.setTotalViolations(0);
            return result;
        }
        String scope = vo.getScope() != null ? vo.getScope() : "all";
        Map<String, WordEntry> merged = buildMergedWordMap(scope, userId);
        Map<String, ViolationCheckResultVO> results = new LinkedHashMap<>();
        int totalViolations = 0;
        for (ViolationCheckBatchVO.TextItem item : vo.getTexts()) {
            String key = item.getKey();
            String text = item.getText() != null ? item.getText() : "";
            List<ViolationCheckResultVO.ViolationHitVO> hits = scanTextWithMerged(text, merged);
            ViolationCheckResultVO single = new ViolationCheckResultVO();
            single.setHasViolation(!hits.isEmpty());
            single.setTotalCount(hits.size());
            single.setViolations(hits);
            results.put(key, single);
            totalViolations += hits.size();
        }
        ViolationCheckBatchResultVO result = new ViolationCheckBatchResultVO();
        result.setResults(results);
        result.setTotalViolations(totalViolations);
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, WordEntry> buildMergedWordMap(String scope, Long userId) {
        List<String> scopes = resolveScopes(scope);
        String cacheKey = "public:" + String.join(",", scopes);
        List<ViolationWord> publicWords = (List<ViolationWord>) violationWordCache.get(cacheKey, k ->
                violationWordRepository.findByStatusAndDeletedAndScopeIn(1, 0, scopes));
        List<UserViolationWord> userWords = userId != null && userId > 0
                ? userViolationWordRepository.findByUserIdAndStatusAndDeletedAndScopeIn(userId, 1, 0, scopes)
                : Collections.emptyList();
        Map<String, WordEntry> merged = new LinkedHashMap<>();
        for (ViolationWord vw : publicWords) {
            String key = vw.getWord().toLowerCase();
            merged.putIfAbsent(key, new WordEntry(vw.getWord(), vw.getLevel(), vw.getReason(), vw.getReplacement(), "public"));
        }
        for (UserViolationWord uv : userWords) {
            String key = uv.getWord().toLowerCase();
            WordEntry existing = merged.get(key);
            if (existing == null || uv.getLevel() != null && (existing.level == null || uv.getLevel() > existing.level)) {
                merged.put(key, new WordEntry(uv.getWord(), uv.getLevel(), uv.getReason(), uv.getReplacement(), "user"));
            }
        }
        return merged;
    }

    private List<ViolationCheckResultVO.ViolationHitVO> scanTextWithMerged(String text, Map<String, WordEntry> merged) {
        if (text == null || text.isBlank()) return Collections.emptyList();
        String textLower = text.toLowerCase();
        List<ViolationCheckResultVO.ViolationHitVO> hits = new ArrayList<>();
        for (WordEntry entry : merged.values()) {
            String wordLower = entry.word.toLowerCase();
            int idx = 0;
            while ((idx = textLower.indexOf(wordLower, idx)) != -1) {
                ViolationCheckResultVO.ViolationHitVO hit = new ViolationCheckResultVO.ViolationHitVO();
                hit.setWord(entry.word);
                hit.setPosition(idx);
                hit.setLength(entry.word.length());
                hit.setReason(entry.reason);
                hit.setLevel(entry.level);
                hit.setReplacement(entry.replacement);
                hit.setSource(entry.source);
                hits.add(hit);
                idx += entry.word.length();
            }
        }
        return hits;
    }

    private List<String> resolveScopes(String scope) {
        if (scope == null || scope.isBlank()) return List.of("all", "live_only", "video_only");
        return switch (scope.toLowerCase()) {
            case "live" -> List.of("all", "live_only");
            case "video" -> List.of("all", "video_only");
            default -> List.of("all", "live_only", "video_only");
        };
    }

    private record WordEntry(String word, Integer level, String reason, String replacement, String source) {}

    @Override
    public ViolationReplacementResultVO suggestReplacement(ViolationReplacementRequestVO vo) {
        if (vo.getText() == null || vo.getText().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "文本不能为空");
        }
        if (vo.getViolationWords() == null || vo.getViolationWords().isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "违规词列表不能为空");
        }

        long startTime = System.currentTimeMillis();

        // 查找可用的 AI 模型
        AiModel model = findAvailableModel();
        if (model == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "无可用的 AI 模型");
        }

        // 构建 prompt
        String systemPrompt = buildReplacementSystemPrompt();
        String userPrompt = buildReplacementUserPrompt(vo);

        try {
            LlmClient.LlmResponse response = llmClient.chat(model, systemPrompt, userPrompt);

            if (!response.success() || response.content() == null || response.content().isBlank()) {
                log.error("LLM 生成替换建议失败: {}", response.errorMsg());
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "AI 生成失败: " + response.errorMsg());
            }

            // 更新模型用量
            if (response.tokensUsed() > 0) {
                aiModelRepository.incrementQuotaUsed(model.getId(), response.tokensUsed());
            }

            // 解析 JSON 响应
            ViolationReplacementResultVO result = parseReplacementResponse(response.content(), vo.getText());
            result.setGenerationTime(System.currentTimeMillis() - startTime);
            result.setTokenUsage((int) Math.min(response.tokensUsed(), Integer.MAX_VALUE));

            log.info("违规词替换建议生成成功: violations={}, tokens={}", vo.getViolationWords().size(), response.tokensUsed());
            return result;

        } catch (Exception e) {
            log.error("生成违规词替换建议失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "生成替换建议失败: " + e.getMessage());
        }
    }

    private AiModel findAvailableModel() {
        List<AiModel> models = aiModelRepository.findByStatusAndDeleted(1, 0);
        if (models.isEmpty()) return null;
        return models.stream()
                .filter(m -> m.getQuotaLimit() == null || m.getQuotaLimit() == 0 || m.getQuotaUsed() < m.getQuotaLimit())
                .findFirst()
                .orElse(models.get(0));
    }

    private String buildReplacementSystemPrompt() {
        return """
                你是一位专业的文案合规专家，擅长为违规词提供合适的替换建议。

                任务：为给定的违规词提供 3-5 个替换选项，每个选项包含：
                1. replacement: 替换词（保持原意和语境）
                2. reason: 替换理由（简短说明为什么这个替换合适）
                3. score: 推荐度（1-5，5 最高）

                要求：
                1. 替换词必须符合广告法和平台规范
                2. 保持原文的语气和风格
                3. 替换后语句通顺自然
                4. 优先推荐最合适的替换词（score=5）

                输出格式（严格 JSON）：
                {
                  "suggested_text": "替换后的完整文本（使用最优替换词）",
                  "replacements": [
                    {
                      "violation_word": "违规词1",
                      "options": [
                        {"replacement": "替换词1", "reason": "理由1", "score": 5},
                        {"replacement": "替换词2", "reason": "理由2", "score": 4}
                      ]
                    }
                  ]
                }
                """;
    }

    private String buildReplacementUserPrompt(ViolationReplacementRequestVO vo) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("原始文本：\n").append(vo.getText()).append("\n\n");
        prompt.append("违规词列表：\n");
        for (String word : vo.getViolationWords()) {
            prompt.append("- ").append(word).append("\n");
        }

        if (vo.getContext() != null && !vo.getContext().isBlank()) {
            prompt.append("\n上下文信息：\n").append(vo.getContext()).append("\n");
        }

        if (vo.getScriptType() != null && !vo.getScriptType().isBlank()) {
            prompt.append("\n话术类型：").append(vo.getScriptType()).append("\n");
        }

        prompt.append("\n请为每个违规词提供替换建议，并输出替换后的完整文本。");
        return prompt.toString();
    }

    private ViolationReplacementResultVO parseReplacementResponse(String content, String originalText) {
        try {
            // 提取 JSON（可能包含在 markdown 代码块中）
            String jsonStr = content.trim();
            if (jsonStr.startsWith("```json")) {
                jsonStr = jsonStr.substring(7);
            }
            if (jsonStr.startsWith("```")) {
                jsonStr = jsonStr.substring(3);
            }
            if (jsonStr.endsWith("```")) {
                jsonStr = jsonStr.substring(0, jsonStr.length() - 3);
            }
            jsonStr = jsonStr.trim();

            JSONObject json = JSON.parseObject(jsonStr);

            ViolationReplacementResultVO result = new ViolationReplacementResultVO();
            result.setOriginalText(originalText);
            result.setSuggestedText(json.getString("suggested_text"));

            List<ViolationReplacementResultVO.ReplacementDetail> details = new ArrayList<>();
            JSONArray replacements = json.getJSONArray("replacements");

            if (replacements != null) {
                for (int i = 0; i < replacements.size(); i++) {
                    JSONObject item = replacements.getJSONObject(i);
                    ViolationReplacementResultVO.ReplacementDetail detail = new ViolationReplacementResultVO.ReplacementDetail();
                    detail.setViolationWord(item.getString("violation_word"));

                    List<ViolationReplacementResultVO.ReplacementOption> options = new ArrayList<>();
                    JSONArray optionsArray = item.getJSONArray("options");

                    if (optionsArray != null) {
                        for (int j = 0; j < optionsArray.size(); j++) {
                            JSONObject opt = optionsArray.getJSONObject(j);
                            ViolationReplacementResultVO.ReplacementOption option = new ViolationReplacementResultVO.ReplacementOption();
                            option.setReplacement(opt.getString("replacement"));
                            option.setReason(opt.getString("reason"));
                            option.setScore(opt.getInteger("score"));
                            options.add(option);
                        }
                    }

                    detail.setOptions(options);
                    details.add(detail);
                }
            }

            result.setReplacements(details);
            return result;

        } catch (Exception e) {
            log.error("解析 AI 响应失败: {}", content, e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "解析 AI 响应失败");
        }
    }

    @Override
    public Map<String, Object> importFromCsv(byte[] csvBytes) {
        if (csvBytes == null || csvBytes.length == 0) {
            throw new BusinessException(ErrorCode.CSV_FORMAT_ERROR, "CSV 文件为空");
        }
        String content = new String(csvBytes, StandardCharsets.UTF_8).replace("\r\n", "\n").replace("\r", "\n");
        String[] lines = content.split("\n");
        int imported = 0, skipped = 0;
        List<String> errors = new ArrayList<>();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;
            String[] parts = parseCsvLine(line);
            if (parts.length < 2) {
                if (i == 0 && line.toLowerCase().startsWith("word")) continue; // skip header
                errors.add("第" + (i + 1) + "行格式错误: " + line);
                continue;
            }
            String word = parts[0].trim();
            if (word.isEmpty()) {
                errors.add("第" + (i + 1) + "行违规词为空");
                continue;
            }
            if (i == 0 && "word".equalsIgnoreCase(word)) continue; // skip header
            // P1-5: CSV 注入防护 - 过滤公式字符
            word = sanitizeCsvValue(word);
            Integer level = parseLevel(parts.length > 1 ? parts[1].trim() : "2");
            String scope = parts.length > 2 ? parts[2].trim() : "all";
            if (scope.isEmpty()) scope = "all";
            String reason = parts.length > 3 ? sanitizeCsvValue(parts[3].trim()) : null;
            String replacement = parts.length > 4 ? sanitizeCsvValue(parts[4].trim()) : null;
            if (violationWordRepository.existsByWordAndDeleted(word, 0)) {
                skipped++;
                continue;
            }
            ViolationWord entity = new ViolationWord();
            entity.setWord(word);
            entity.setLevel(level != null ? level : 2);
            entity.setScope(scope);
            entity.setReason(reason);
            entity.setReplacement(replacement);
            entity.setStatus(1);
            violationWordRepository.save(entity);
            imported++;
        }
        if (imported > 0) violationWordCache.invalidateAll();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("imported", imported);
        result.put("skipped", skipped);
        result.put("total", imported + skipped);
        result.put("errors", errors);
        return result;
    }

    @Override
    public byte[] exportToCsv() {
        List<ViolationWord> list = violationWordRepository.findByStatusAndDeleted(1, 0);
        StringBuilder sb = new StringBuilder();
        sb.append("word,level,scope,reason,replacement\n");
        for (ViolationWord e : list) {
            // P1-5: CSV 注入防护 - 导出时也过滤公式字符
            sb.append(escapeCsv(sanitizeCsvValue(e.getWord()))).append(",");
            sb.append(e.getLevel() != null ? e.getLevel() : 2).append(",");
            sb.append(escapeCsv(e.getScope() != null ? e.getScope() : "all")).append(",");
            sb.append(escapeCsv(sanitizeCsvValue(e.getReason()))).append(",");
            sb.append(escapeCsv(sanitizeCsvValue(e.getReplacement()))).append("\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * P1-5: CSV 注入防护 - 过滤公式字符
     * 如果值以 =, +, -, @, \t, \r 开头，添加单引号前缀阻止公式执行
     */
    private String sanitizeCsvValue(String value) {
        if (value == null || value.isEmpty()) return value;
        char first = value.charAt(0);
        if (first == '=' || first == '+' || first == '-' || first == '@' || first == '\t' || first == '\r') {
            return "'" + value;
        }
        return value;
    }

    private String[] parseCsvLine(String line) {
        List<String> parts = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cur.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                parts.add(cur.toString());
                cur = new StringBuilder();
            } else {
                cur.append(c);
            }
        }
        parts.add(cur.toString());
        return parts.toArray(new String[0]);
    }

    private Integer parseLevel(String s) {
        if (s == null || s.isEmpty()) return 2;
        if (s.matches("\\d+")) return Integer.parseInt(s);
        return switch (s.toLowerCase()) {
            case "forbidden" -> 3;
            case "warning" -> 2;
            case "suggest" -> 1;
            default -> 2;
        };
    }

    private String escapeCsv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    private ViolationWordVO toVO(ViolationWord e) {
        ViolationWordVO vo = new ViolationWordVO();
        vo.setId(e.getId());
        vo.setWord(e.getWord());
        vo.setLevel(e.getLevel());
        vo.setReason(e.getReason());
        vo.setReplacement(e.getReplacement());
        vo.setScope(e.getScope());
        vo.setStatus(e.getStatus());
        vo.setCreateTime(e.getCreateTime());
        vo.setUpdateTime(e.getUpdateTime());
        return vo;
    }
}
