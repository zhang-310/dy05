package cn.gaifan.douyinOperations.common.util;

import org.apache.commons.lang3.StringUtils;
import lombok.extern.log4j.Log4j2;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * 文件哈希验证工具类
 * 负责文件MD5的计算和验证
 *
 * @author gaifan
 */
@Log4j2
public class FileHashValidator {

    /**
     * 计算文件的MD5值
     *
     * @param filePath 文件路径，不能为空
     * @return MD5字符串（32位16进制，小写）
     * @throws IOException 文件不存在或读取失败
     */
    public static String computeFileMd5(String filePath) throws IOException {
        if (StringUtils.isBlank(filePath)) {
            throw new IllegalArgumentException("文件路径不能为空");
        }
        try (FileInputStream fis = new FileInputStream(filePath)) {
            return Md5Hasher.computeMd5(fis);
        }
    }

    /**
     * 验证文件的MD5值是否匹配
     *
     * @param filePath 文件路径
     * @param expectedMd5 期望的MD5值（不区分大小写）
     * @return 如果匹配返回true
     * @throws IOException 读取文件失败
     */
    public static boolean verifyFileMd5(String filePath, String expectedMd5) throws IOException {
        if (StringUtils.isBlank(filePath)) {
            throw new IllegalArgumentException("文件路径不能为空");
        }
        if (StringUtils.isBlank(expectedMd5)) {
            throw new IllegalArgumentException("期望的MD5值不能为空");
        }
        String actualMd5 = computeFileMd5(filePath);
        return actualMd5 != null && actualMd5.equalsIgnoreCase(expectedMd5.trim());
    }

    /**
     * 获取上传文件的md5（兼容方法，不推荐使用）
     *
     * @param mergedFile 文件输入流，调用者负责关闭
     * @return MD5字符串
     * @deprecated 建议使用 {@link Md5Hasher#computeMd5(java.io.InputStream)}
     */
    @Deprecated
    public static String getMd5(FileInputStream mergedFile) {
        try {
            return Md5Hasher.computeMd5(mergedFile);
        } catch (IOException | RuntimeException e) {
            log.error("计算文件MD5失败", e);
            return null;
        }
    }
}
