package cn.gaifan.douyinOperations.common.util;

import lombok.extern.log4j.Log4j2;

/**
 * 十六进制字符串解析工具类
 * 负责16进制字符串转换为整数类型
 *
 * @author gaifan
 */
@Log4j2
public class HexParser {

    private static final int MAX_HEX_STRING_LENGTH = 16;
    private static final char[] NUM_DIGITS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};

    /**
     * 将字符串内容转换为MD5的长整型值（只取高位，生成16位数字字符串）
     *
     * @param content 要计算的字符串内容
     * @return MD5转换后的长整型值
     *         - null或空字符串：返回0L
     *         - 计算成功：返回16位数字字符串对应的Long值
     *         - 计算失败：返回null
     */
    public static Long getLongMD5(String content) {
        if (content == null || content.trim().isEmpty()) {
            return 0L;
        }
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            md.update(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            byte[] digest = md.digest();
            char[] str = new char[16];
            for (int i = 0; i < 16; i++) {
                str[i] = NUM_DIGITS[(digest[i] >>> 4 & 0xf) % 10];
            }
            return Long.parseLong(new String(str));
        } catch (java.security.NoSuchAlgorithmException e) {
            log.error("MD5算法不可用", e);
            return null;
        } catch (NumberFormatException e) {
            log.error("解析MD5长整型值失败，内容: " + (content.length() > 50 ? content.substring(0, 50) + "..." : content), e);
            return null;
        }
    }

    /**
     * 将16进制字符串转换为long值
     *
     * @param hexString 16进制字符串（不区分大小写）
     * @return 对应的long值
     * @throws NumberFormatException 如果字符串无效
     */
    public static long parseHexToLong(String hexString) {
        if (hexString == null) {
            throw new NumberFormatException("输入字符串不能为null");
        }
        hexString = hexString.toLowerCase().trim();
        if (hexString.startsWith("0x")) {
            hexString = hexString.substring(2).trim();
        }
        if (hexString.isEmpty()) {
            throw new NumberFormatException("输入字符串不能为空");
        }
        if (hexString.length() > MAX_HEX_STRING_LENGTH) {
            throw new NumberFormatException("输入字符串过长，最大支持16位16进制数: " + hexString);
        }
        return parseHexToLongInternal(hexString);
    }

    /**
     * 将16进制字符串转换为long值（内部实现）
     */
    private static long parseHexToLongInternal(String hexString) {
        char[] chars = hexString.toCharArray();
        long result = 0L;
        for (char c : chars) {
            result <<= 4;
            int digit;
            if (c >= '0' && c <= '9') {
                digit = c - '0';
            } else if (c >= 'a' && c <= 'f') {
                digit = c - 'a' + 10;
            } else {
                throw new NumberFormatException("无效的16进制字符 '" + c + "' 在字符串: " + hexString);
            }
            result += digit;
        }
        return result;
    }

    /**
     * 将16位的md5转化为long值（兼容方法）
     *
     * @param md5L16 16位16进制字符串（0-9, a-f）
     * @return long值
     * @deprecated 建议使用 {@link #parseHexToLong(String)}
     */
    @Deprecated
    public static long parseMd5L16ToLong(String md5L16) {
        if (md5L16 == null) {
            throw new NumberFormatException("输入字符串不能为null");
        }
        return parseHexToLongInternal(md5L16.toLowerCase());
    }

    /**
     * 将16进制的字符串转化为long值（兼容方法）
     *
     * @param str16 16进制字符串（支持0x前缀）
     * @return long值
     * @deprecated 建议使用 {@link #parseHexToLong(String)}
     */
    @Deprecated
    public static long parseString16ToLong(String str16) {
        return parseHexToLong(str16);
    }
}
