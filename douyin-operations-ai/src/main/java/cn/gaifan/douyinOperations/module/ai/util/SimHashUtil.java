package cn.gaifan.douyinOperations.module.ai.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 64 位 SimHash，用于文档近似重复检测（海明距离 &lt; 3 视为疑似重复）。
 */
public final class SimHashUtil {

    private static final int HASH_BITS = 64;

    private SimHashUtil() {
    }

    /**
     * 计算文本的 64 位 SimHash 值。
     * 采用字符级 n-gram（长度 4）加权累加位向量的方式。
     */
    public static long compute(String text) {
        if (text == null || text.isBlank()) return 0L;
        text = text.trim();
        int[] bits = new int[HASH_BITS];
        int len = text.length();
        for (int i = 0; i <= len - 4; i++) {
            String gram = text.substring(i, i + 4);
            long h = hash64(gram);
            for (int b = 0; b < HASH_BITS; b++) {
                bits[b] += ((h >> b) & 1) == 1 ? 1 : -1;
            }
        }
        if (len < 4) {
            long h = hash64(text);
            for (int b = 0; b < HASH_BITS; b++) {
                bits[b] = ((h >> b) & 1) == 1 ? 1 : -1;
            }
        }
        long result = 0;
        for (int b = 0; b < HASH_BITS; b++) {
            if (bits[b] > 0) result |= (1L << b);
        }
        return result;
    }

    /**
     * 海明距离（两个 64 位值中不同位的个数）
     */
    public static int hammingDistance(long a, long b) {
        long x = a ^ b;
        int n = 0;
        while (x != 0) {
            n++;
            x &= (x - 1);
        }
        return n;
    }

    private static long hash64(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(s.getBytes(StandardCharsets.UTF_8));
            long lo = (bytes[0] & 0xFFL) | ((bytes[1] & 0xFFL) << 8) | ((bytes[2] & 0xFFL) << 16) | ((bytes[3] & 0xFFL) << 24)
                    | ((bytes[4] & 0xFFL) << 32) | ((bytes[5] & 0xFFL) << 40) | ((bytes[6] & 0xFFL) << 48) | ((bytes[7] & 0xFFL) << 56);
            return lo;
        } catch (Exception e) {
            return s.hashCode() & 0xFFFFFFFFL;
        }
    }
}
