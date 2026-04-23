package cn.gaifan.douyinOperations.common.util;

/**
 * 查询字符串工具（LIKE 通配符转义等）
 */
public final class QueryUtils {

    private QueryUtils() {}

    /**
     * 转义 LIKE 中的 \、%、_，避免用户输入匹配全表。
     * 配合 JPA {@code cb.like(..., "%" + escapeLike(k) + "%")} 时需在方言中开启 escape（PostgreSQL 默认 \ 为转义符）。
     */
    public static String escapeLike(String keyword) {
        if (keyword == null) {
            return null;
        }
        String t = keyword.trim();
        return t.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    /** 两侧模糊匹配的安全 LIKE 字面量（不含 % 包裹时自行拼接） */
    public static String likeContains(String keyword) {
        if (keyword == null) {
            return null;
        }
        return "%" + escapeLike(keyword) + "%";
    }
}
