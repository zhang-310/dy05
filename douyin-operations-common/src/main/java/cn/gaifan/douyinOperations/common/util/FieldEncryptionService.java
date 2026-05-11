package cn.gaifan.douyinOperations.common.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Component;

/**
 * 字段加密服务
 * 用于敏感字段的透明加解密（如 secret、AES key）
 *
 * P0-001: 敏感数据明文存储修复
 */
@Component
public class FieldEncryptionService {

    private final TextEncryptor encryptor;

    public FieldEncryptionService(
            @Value("${field.encryption.password:dy05-default-encryption-password-change-in-production}") String password,
            @Value("${field.encryption.salt:deadbeef}") String salt) {
        this.encryptor = Encryptors.text(password, salt);
    }

    /**
     * 加密明文
     * @param plaintext 明文
     * @return 密文（Base64 编码）
     */
    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            return plaintext;
        }
        return encryptor.encrypt(plaintext);
    }

    /**
     * 解密密文
     * @param ciphertext 密文（Base64 编码）
     * @return 明文
     */
    public String decrypt(String ciphertext) {
        if (ciphertext == null || ciphertext.isEmpty()) {
            return ciphertext;
        }
        try {
            return encryptor.decrypt(ciphertext);
        } catch (Exception e) {
            // 解密失败可能是因为数据未加密（迁移期间）
            // 返回原值，由调用方判断
            return ciphertext;
        }
    }
}
