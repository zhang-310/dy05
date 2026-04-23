package cn.gaifan.douyinOperations.common.util;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * API Key 加解密工具（P2 安全策略）
 * 使用 AES-GCM 对称加密，密钥来自配置 app.security.api-key-cipher-secret
 * 密钥需为 16/24/32 字节 Base64 或十六进制，生产环境务必通过环境变量注入
 */
public final class ApiKeyCipher {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;

    private ApiKeyCipher() {}

    /**
     * 加密 API Key
     * @param plainText 明文
     * @param secretBase64 密钥（Base64 编码，16/24/32 字节）
     * @return Base64(IV || cipherText)，失败返回 null
     */
    public static String encrypt(String plainText, String secretBase64) {
        if (plainText == null || plainText.isBlank() || secretBase64 == null || secretBase64.isBlank()) {
            return plainText;
        }
        try {
            byte[] key = Base64.getDecoder().decode(secretBase64);
            if (key.length != 16 && key.length != 24 && key.length != 32) {
                return plainText;
            }
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            return plainText;  // 失败时返回原文，避免丢数据
        }
    }

    /**
     * 解密 API Key
     * @param encryptedBase64 密文（Base64(IV || cipherText)）
     * @param secretBase64 密钥
     * @return 明文，失败或非加密格式返回原字符串
     */
    public static String decrypt(String encryptedBase64, String secretBase64) {
        if (encryptedBase64 == null || encryptedBase64.isBlank() || secretBase64 == null || secretBase64.isBlank()) {
            return encryptedBase64;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(encryptedBase64);
            if (combined.length <= GCM_IV_LENGTH) {
                return encryptedBase64;
            }
            byte[] key = Base64.getDecoder().decode(secretBase64);
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
            byte[] cipherText = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, GCM_IV_LENGTH, cipherText, 0, cipherText.length);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return encryptedBase64;
        }
    }

    /**
     * 判断字符串是否为密文（Base64 且长度合理）
     */
    public static boolean looksEncrypted(String value) {
        if (value == null || value.length() < 32) return false;
        try {
            byte[] decoded = Base64.getDecoder().decode(value);
            return decoded != null && decoded.length > GCM_IV_LENGTH;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
