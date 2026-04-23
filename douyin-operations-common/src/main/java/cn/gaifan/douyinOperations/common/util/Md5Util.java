package cn.gaifan.douyinOperations.common.util;

import java.io.FileInputStream;
import java.io.InputStream;

/**
 * MD5工具类（Facade）
 * <p>
 * 为了向后兼容，将所有功能委托到具体的工具类：
 * - Md5Hasher: 哈希计算
 * - FileHashValidator: 文件验证
 * - HexParser: 十六进制解析
 * </p>
 *
 * @author gaifan
 */
public class Md5Util {

    // ============ Hash Computation (Md5Hasher) ============

    public static String computeMd5(InputStream inputStream) throws java.io.IOException {
        return Md5Hasher.computeMd5(inputStream);
    }

    public static String computeMd5(String content) {
        return Md5Hasher.computeMd5(content);
    }

    public static String computeMd5(byte[] bytes) {
        return Md5Hasher.computeMd5(bytes);
    }

    public static Long computeMd5HashForIndex(String content) {
        return Md5Hasher.computeMd5HashForIndex(content);
    }

    // ============ File Hash Validation (FileHashValidator) ============

    public static String computeFileMd5(String filePath) throws java.io.IOException {
        return FileHashValidator.computeFileMd5(filePath);
    }

    public static boolean verifyFileMd5(String filePath, String expectedMd5) throws java.io.IOException {
        return FileHashValidator.verifyFileMd5(filePath, expectedMd5);
    }

    @Deprecated
    public static String getMd5(FileInputStream mergedFile) {
        return FileHashValidator.getMd5(mergedFile);
    }

    // ============ Hex Parsing (HexParser) ============

    public static Long getLongMD5(String content) {
        return HexParser.getLongMD5(content);
    }

    public static long parseHexToLong(String hexString) {
        return HexParser.parseHexToLong(hexString);
    }

    @Deprecated
    public static long parseMd5L16ToLong(String md5L16) {
        return HexParser.parseMd5L16ToLong(md5L16);
    }

    @Deprecated
    public static long parseString16ToLong(String str16) {
        return HexParser.parseString16ToLong(str16);
    }
}
