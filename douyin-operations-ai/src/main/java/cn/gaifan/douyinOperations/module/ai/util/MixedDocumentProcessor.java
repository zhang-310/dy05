package cn.gaifan.douyinOperations.module.ai.util;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 混合文档分段处理：按 # 标题 / 【标题】/ 分隔线切分大区块，每区块再按 script/general 分别分块。
 */
@Component
public class MixedDocumentProcessor {

    /** 一级分隔：## 标题、【标题】、--- */
    private static final Pattern SECTION = Pattern.compile("(?m)^(?:#{1,6}\\s+[^\\n]+|[【\\[][^】\\]]+[】\\]]|[-=]{3,}\\s*)$");

    @Resource
    private DocumentTypeDetector documentTypeDetector;

    /**
     * 混合文档分块：先按一级分隔切段，每段检测 script/general 后分别用话术分块或通用分块。
     */
    public List<ChunkResult> process(String content) {
        if (content == null || content.isBlank()) return List.of();
        content = content.trim();

        List<String> sections = splitSections(content);
        List<ChunkResult> result = new ArrayList<>();
        for (String section : sections) {
            String type = documentTypeDetector.detect(section);
            if ("script".equals(type)) {
                result.addAll(ScriptAwareChunker.scriptSplit(section));
            } else {
                result.addAll(ScriptAwareChunker.generalSplit(section));
            }
        }
        return result;
    }

    private static List<String> splitSections(String content) {
        List<Integer> starts = new ArrayList<>();
        java.util.regex.Matcher m = SECTION.matcher(content);
        while (m.find()) starts.add(m.start());
        if (starts.isEmpty()) return List.of(content);
        if (!starts.contains(0)) {
            starts.add(0);
            java.util.Collections.sort(starts);
        }
        List<String> sections = new ArrayList<>();
        for (int i = 0; i < starts.size(); i++) {
            int end = i + 1 < starts.size() ? starts.get(i + 1) : content.length();
            String block = content.substring(starts.get(i), end).trim();
            if (!block.isEmpty()) sections.add(block);
        }
        return sections.isEmpty() ? List.of(content) : sections;
    }
}
