package cn.gaifan.douyinOperations.module.ai.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 文档内容指纹（MD5），用于导入/上传时内容级去重，与 KnowledgeBaseServiceImpl 逻辑一致。
 */
public final class ContentFingerprintUtil {

    private ContentFingerprintUtil() {}

    /**
     * 对正文做规范化后计算 MD5，空内容返回 null。
     */
    public static String compute(String content) {
        if (content == null || content.isBlank()) return null;
        String norm = content.trim().replaceAll("\\s+", "");
        if (norm.isEmpty()) return null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(norm.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(32);
            for (byte b : digest) sb.append(String.format("%02x", b & 0xff));
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }
}
