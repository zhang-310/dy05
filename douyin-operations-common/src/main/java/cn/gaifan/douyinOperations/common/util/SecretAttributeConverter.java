package cn.gaifan.douyinOperations.common.util;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * JPA AttributeConverter for transparent encryption/decryption
 * 用于敏感字段的透明加解密
 *
 * P0-001: 敏感数据明文存储修复
 */
@Converter
@Component
public class SecretAttributeConverter implements AttributeConverter<String, String> {

    private static FieldEncryptionService encryptionService;

    @Autowired
    public void setEncryptionService(FieldEncryptionService service) {
        SecretAttributeConverter.encryptionService = service;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (encryptionService == null) {
            // 启动期间可能未注入，返回原值
            return attribute;
        }
        return encryptionService.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (encryptionService == null) {
            // 启动期间可能未注入，返回原值
            return dbData;
        }
        return encryptionService.decrypt(dbData);
    }
}
