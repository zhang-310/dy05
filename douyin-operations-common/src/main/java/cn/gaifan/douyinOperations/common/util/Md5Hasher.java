package cn.gaifan.douyinOperations.common.util;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * MD5哈希计算工具类
 * 负责MD5值的计算
 *
 * @author gaifan
 */
@Log4j2
public class Md5Hasher {

    private static final char[] HEX_DIGITS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
    private static final int BUFFER_SIZE = 8192;
    private static final int HEX_STRING_LENGTH = 32;

    /**
     * 计算输入流的MD5值
     *
     * @param inputStream 输入流，调用者负责关闭
     * @return MD5字符串（32位16进制，小写）
     * @throws java.io.IOException IO错误
     */
    public static String computeMd5(InputStream inputStream) throws java.io.IOException {
        if (inputStream == null) {
            throw new IllegalArgumentException("输入流不能为null");
        }
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                md5.update(buffer, 0, bytesRead);
            }
            return bytesToHex(md5.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5算法不可用", e);
        }
    }

    /**
     * 计算字符串的MD5值
     *
     * @param content 要计算MD5的字符串内容
     * @return MD5字符串（32位16进制，小写），null或空字符串返回null
     */
    public static String computeMd5(String content) {
        if (StringUtils.isBlank(content)) {
            return null;
        }
        return computeMd5(content.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 计算字节数组的MD5值
     *
     * @param bytes 要计算MD5的字节数组
     * @return MD5字符串（32位16进制，小写）
     */
    public static String computeMd5(byte[] bytes) {
        if (bytes == null) {
            throw new IllegalArgumentException("字节数组不能为null");
        }
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(bytes);
            return bytesToHex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5算法不可用", e);
        }
    }

    /**
     * 计算字符串的MD5哈希值（64位，用于数据库索引优化）
     *
     * @param content 要计算哈希的字符串内容
     * @return MD5前16位转Long值（64位哈希）
     */
    public static Long computeMd5HashForIndex(String content) {
        if (StringUtils.isBlank(content)) {
            return null;
        }
        String md5 = computeMd5(content);
        if (md5 == null) {
            return null;
        }
        try {
            return HexParser.parseHexToLong(md5.substring(0, 16));
        } catch (NumberFormatException e) {
            log.error("转换MD5哈希值为Long失败: " + content, e);
            return null;
        }
    }

    /**
     * 将字节数组转换为16进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        if (bytes == null) {
            throw new IllegalArgumentException("字节数组不能为null");
        }
        StringBuilder hexString = new StringBuilder(HEX_STRING_LENGTH);
        for (byte b : bytes) {
            int value = 0xff & b;
            hexString.append(HEX_DIGITS[value >>> 4]);
            hexString.append(HEX_DIGITS[value & 0xf]);
        }
        return hexString.toString();
    }
}
