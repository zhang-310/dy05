package cn.gaifan.douyinOperations.module.ai.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 话术文档智能分块：按数字序号/标题/分隔线/空行切分，块长 30–1000 字；超长回退 512 字通用分块。
 */
public final class ScriptAwareChunker {

    private static final int CHUNK_SIZE = 512;
    private static final int CHUNK_OVERLAP = 50;
    private static final int MIN_CHUNK_CHARS = 30;
    private static final int MAX_CHUNK_CHARS = 1000;

    /** 数字序号：1. 2、3） */
    private static final Pattern NUMBERED = Pattern.compile("(?m)^\\d+[.、）)]\\s*");
    /** 标题：【xxx】# xxx */
    private static final Pattern BRACKET_TITLE = Pattern.compile("(?m)^[【\\[#][^\\n]+");
    /** 分隔线 --- === */
    private static final Pattern SEPARATOR = Pattern.compile("(?m)^[-=]{3,}\\s*$");

    private ScriptAwareChunker() {
    }

    /**
     * 话术分块：优先按序号/标题/分隔线/空行切分，块 30–1000 字，不足合并、超长 512 切。
     */
    public static List<ChunkResult> scriptSplit(String content) {
        if (content == null || content.isBlank()) return List.of();
        content = content.trim();

        List<String> raw = splitByPrimaryDelimiter(content);
        List<String> merged = mergeShortChunks(raw);
        List<ChunkResult> result = new ArrayList<>();
        for (String block : merged) {
            if (block.length() > MAX_CHUNK_CHARS) {
                for (String sub : generalSplitPlain(block)) {
                    result.add(new ChunkResult(sub, ChunkLabeler.label(sub)));
                }
            } else {
                result.add(new ChunkResult(block, ChunkLabeler.label(block)));
            }
        }
        return result;
    }

    /**
     * 通用分块：512 字 + 50 重叠，尽量在句末切分，并为每块打标签。
     */
    public static List<ChunkResult> generalSplit(String content) {
        if (content == null || content.isBlank()) return List.of();
        content = content.trim();
        List<String> plain = generalSplitPlain(content);
        List<ChunkResult> result = new ArrayList<>();
        for (String text : plain) {
            result.add(new ChunkResult(text, ChunkLabeler.label(text)));
        }
        return result;
    }

    /** 仅做 512 字切分，不打标签（供 MixedDocumentProcessor 等复用） */
    public static List<String> generalSplitPlain(String text) {
        List<String> chunks = new ArrayList<>();
        text = text == null ? "" : text.trim();
        if (text.isEmpty()) return chunks;

        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + CHUNK_SIZE, text.length());
            if (end < text.length()) {
                int lastPeriod = text.lastIndexOf('。', end);
                int lastExcl = text.lastIndexOf('！', end);
                int lastQuest = text.lastIndexOf('？', end);
                int lastNewline = text.lastIndexOf('\n', end);
                int boundary = Math.max(Math.max(lastPeriod, lastExcl), Math.max(lastQuest, lastNewline));
                if (boundary > start + CHUNK_SIZE / 2) end = boundary + 1;
            }
            String chunk = text.substring(start, end).trim();
            if (!chunk.isEmpty()) chunks.add(chunk);
            start = Math.max(start + 1, end - CHUNK_OVERLAP);
        }
        return chunks;
    }

    /** 在 content 中按主分隔符（数字序号 > 标题 > 分隔线 > 空行）切分，返回各块文本 */
    private static List<String> splitByPrimaryDelimiter(String content) {
        List<Integer> starts = new ArrayList<>();
        Pattern usePattern = null;
        if (NUMBERED.matcher(content).results().count() >= 2) usePattern = NUMBERED;
        else if (BRACKET_TITLE.matcher(content).results().count() >= 2) usePattern = BRACKET_TITLE;
        else if (SEPARATOR.matcher(content).find()) usePattern = SEPARATOR;
        if (usePattern != null) {
            Matcher m = usePattern.matcher(content);
            while (m.find()) starts.add(m.start());
        }
        if (starts.isEmpty()) {
            String[] byBlank = content.split("\\n\\s*\\n");
            List<String> list = new ArrayList<>();
            for (String s : byBlank) {
                String t = s.trim();
                if (!t.isEmpty()) list.add(t);
            }
            return list.isEmpty() ? List.of(content) : list;
        }
        List<String> blocks = new ArrayList<>();
        for (int i = 0; i < starts.size(); i++) {
            int end = i + 1 < starts.size() ? starts.get(i + 1) : content.length();
            String block = content.substring(starts.get(i), end).trim();
            if (!block.isEmpty()) blocks.add(block);
        }
        return blocks;
    }

    /** 太短（<30 字）的块与下一块合并 */
    private static List<String> mergeShortChunks(List<String> raw) {
        List<String> merged = new ArrayList<>();
        StringBuilder acc = null;
        for (String s : raw) {
            if (s.length() < MIN_CHUNK_CHARS && acc != null) {
                acc.append("\n").append(s);
            } else {
                if (acc != null && acc.length() > 0) merged.add(acc.toString());
                acc = new StringBuilder(s);
            }
        }
        if (acc != null && acc.length() > 0) merged.add(acc.toString());
        return merged;
    }
}
