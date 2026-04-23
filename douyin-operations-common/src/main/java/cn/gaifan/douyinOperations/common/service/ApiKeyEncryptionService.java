package cn.gaifan.douyinOperations.common.service;

import cn.gaifan.douyinOperations.common.util.ApiKeyCipher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * API Key 加解密服务（P2）
 * 存储时加密、读取时解密，未配置密钥时透传（向后兼容）
 */
@Service
public class ApiKeyEncryptionService {

    @Value("${app.security.api-key-cipher-secret:}")
    private String secret;

    public String encryptForStorage(String plainText) {
        if (!StringUtils.hasText(plainText)) return plainText;
        if (!StringUtils.hasText(secret)) return plainText;
        String encrypted = ApiKeyCipher.encrypt(plainText, secret);
        return encrypted != null ? encrypted : plainText;
    }

    public String decryptForUse(String encrypted) {
        if (!StringUtils.hasText(encrypted)) return encrypted;
        if (!StringUtils.hasText(secret)) return encrypted;
        if (!ApiKeyCipher.looksEncrypted(encrypted)) return encrypted;
        return ApiKeyCipher.decrypt(encrypted, secret);
    }
}
