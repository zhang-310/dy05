package cn.gaifan.douyinOperations.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ApiKeyCipher 加解密测试")
class ApiKeyCipherTest {

    private static final String SECRET_16 = Base64.getEncoder().encodeToString("1234567890123456".getBytes());
    private static final String SECRET_32 = Base64.getEncoder().encodeToString("12345678901234567890123456789012".getBytes());

    @Test
    void encryptDecrypt_roundtrip() {
        String plain = "sk-abc123xyz";
        String enc = ApiKeyCipher.encrypt(plain, SECRET_32);
        assertNotNull(enc);
        assertNotEquals(plain, enc);
        String dec = ApiKeyCipher.decrypt(enc, SECRET_32);
        assertEquals(plain, dec);
    }

    @Test
    void encrypt_withInvalidSecret_returnsPlainText() {
        String plain = "sk-test";
        String enc = ApiKeyCipher.encrypt(plain, "short");
        assertEquals(plain, enc); // key must be 16/24/32 bytes
    }

    @Test
    void decrypt_nonEncrypted_returnsOriginal() {
        String plain = "sk-plain-key";
        String result = ApiKeyCipher.decrypt(plain, SECRET_32);
        assertEquals(plain, result);
    }

    @Test
    void looksEncrypted_encryptedValue_returnsTrue() {
        String enc = ApiKeyCipher.encrypt("test", SECRET_32);
        assertTrue(ApiKeyCipher.looksEncrypted(enc));
    }

    @Test
    void looksEncrypted_plainValue_returnsFalse() {
        assertFalse(ApiKeyCipher.looksEncrypted("sk-plain"));
        assertFalse(ApiKeyCipher.looksEncrypted(null));
    }

    @Test
    void encrypt_nullOrBlank_returnsOriginal() {
        assertNull(ApiKeyCipher.encrypt(null, SECRET_32));
        assertTrue(ApiKeyCipher.encrypt("", SECRET_32).isEmpty());
    }
}
