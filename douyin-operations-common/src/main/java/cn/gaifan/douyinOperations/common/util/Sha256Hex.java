package cn.gaifan.douyinOperations.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * UTF-8 字符串对的 SHA-256 十六进制指纹（system 与 user 段以 0x00 分隔，避免歧义）。
 */
public final class Sha256Hex {

    private Sha256Hex() {}

    public static String fingerprintSystemAndUser(String systemPrompt, String userPrompt) {
        String sys = systemPrompt != null ? systemPrompt : "";
        String user = userPrompt != null ? userPrompt : "";
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(sys.getBytes(StandardCharsets.UTF_8));
            md.update((byte) 0);
            md.update(user.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
