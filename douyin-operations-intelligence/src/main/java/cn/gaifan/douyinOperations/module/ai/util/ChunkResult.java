package cn.gaifan.douyinOperations.module.ai.util;

import java.util.Set;

/**
 * 分块结果：文本 + 业务标签（type:/cat:），用于 ScriptAwareChunker / MixedDocumentProcessor。
 */
public record ChunkResult(String text, Set<String> labels) {
    public ChunkResult {
        labels = labels != null ? Set.copyOf(labels) : Set.of();
    }
}
