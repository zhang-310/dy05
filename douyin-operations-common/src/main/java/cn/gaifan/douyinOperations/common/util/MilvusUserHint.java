package cn.gaifan.douyinOperations.common.util;

/**
 * 将 Milvus 底层英文错误转换为用户可理解的补充说明（不改变原始 message 前缀）。
 */
public final class MilvusUserHint {

    private MilvusUserHint() {
    }

    public static String appendRecoveryHint(String message) {
        if (message == null || message.isBlank()) {
            return message;
        }
        String lower = message.toLowerCase();
        if (lower.contains("recovering")
                || lower.contains("do not found any channel")
                || lower.contains("collection on recovering")
                || (lower.contains("collection") && lower.contains("channel"))) {
            return message + "（提示：Milvus 集合可能处于加载/恢复中，请稍后重试或检查 Milvus 与集合 load 状态）";
        }
        return message;
    }
}
