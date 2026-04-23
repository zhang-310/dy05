package cn.gaifan.douyinOperations.module.messaging.util;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;

/**
 * 企微回调消息加解密工具（参考官方说明）
 * 签名算法：sha1(concat(sort(token,timestamp,nonce,encrypt)))
 */
public final class WecomCryptoUtil {

    private WecomCryptoUtil() {}

    public static boolean verifySignature(String token, String timestamp, String nonce, String encrypt, String msgSignature) {
        try {
            String[] arr = new String[]{token, timestamp, nonce, encrypt};
            Arrays.sort(arr);
            StringBuilder sb = new StringBuilder();
            for (String s : arr) sb.append(s);
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) hex.append(String.format("%02x", b));
            return hex.toString().equalsIgnoreCase(msgSignature != null ? msgSignature : "");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 解密 echostr 或消息体
     * 企微格式：Base64 解码 -> 前16字节 IV -> AES/CBC 解密 -> 16B随机数+4B长度+消息+corpId
     */
    public static String decrypt(String encodingAesKey, String encrypted) throws Exception {
        if (encodingAesKey == null || encodingAesKey.length() != 43) {
            throw new IllegalArgumentException("EncodingAESKey 必须为 43 位");
        }
        byte[] keyBytes = java.util.Base64.getDecoder().decode(encodingAesKey + "=");
        byte[] encryptedBytes = java.util.Base64.getDecoder().decode(encrypted);
        if (encryptedBytes.length < 16) throw new IllegalArgumentException("密文过短");
        byte[] iv = Arrays.copyOfRange(encryptedBytes, 0, 16);
        byte[] cipherText = Arrays.copyOfRange(encryptedBytes, 16, encryptedBytes.length);
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
        byte[] decrypted = cipher.doFinal(cipherText);
        int pad = decrypted[decrypted.length - 1] & 0xFF;
        if (pad < 1 || pad > 32) pad = 0;
        byte[] raw = Arrays.copyOfRange(decrypted, 0, decrypted.length - pad);
        if (raw.length < 20) throw new IllegalArgumentException("解密后数据过短");
        int len = ((raw[16] & 0xFF) << 24) | ((raw[17] & 0xFF) << 16) | ((raw[18] & 0xFF) << 8) | (raw[19] & 0xFF);
        if (len < 0 || 20 + len > raw.length) return new String(raw, 20, raw.length - 20, StandardCharsets.UTF_8);
        return new String(raw, 20, len, StandardCharsets.UTF_8);
    }
}
