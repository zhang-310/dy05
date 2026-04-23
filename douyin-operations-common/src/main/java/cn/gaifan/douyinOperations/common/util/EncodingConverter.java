package cn.gaifan.douyinOperations.common.util;

import cn.gaifan.douyinOperations.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 文件编码转换工具类
 * 负责文件编码的转换
 *
 * @author system
 */
public class EncodingConverter {

    private static final Logger log = LoggerFactory.getLogger(EncodingConverter.class);

    /**
     * 转换字符串编码
     *
     * @param source 源字符串
     * @param fromEncoding 源编码
     * @param toEncoding 目标编码
     * @return 转换后的字符串
     * @throws BusinessException 转换失败时抛出
     */
    public static String convertEncoding(String source, String fromEncoding, String toEncoding) {
        if (source == null || source.isEmpty()) {
            return source;
        }

        try {
            byte[] bytes = source.getBytes(fromEncoding);
            return new String(bytes, toEncoding);
        } catch (Exception e) {
            log.error("字符串编码转换失败: from={}, to={}", fromEncoding, toEncoding, e);
            throw new BusinessException("字符串编码转换失败: " + e.getMessage());
        }
    }

    /**
     * 转换文件编码
     *
     * @param filePath 文件路径
     * @param targetEncoding 目标编码
     * @throws BusinessException 转换失败时抛出
     */
    public static void convertFileEncoding(String filePath, String targetEncoding) {
        if (filePath == null || filePath.isEmpty()) {
            throw new BusinessException("文件路径不能为空");
        }

        File file = new File(filePath);
        if (!file.exists()) {
            throw new BusinessException("文件不存在: " + filePath);
        }

        if (!file.isFile()) {
            throw new BusinessException("路径不是有效文件: " + filePath);
        }

        try {
            String currentEncoding = EncodingDetector.detectEncoding(filePath);
            if (currentEncoding == null) {
                currentEncoding = "UTF-8";
            }

            if (currentEncoding.equalsIgnoreCase(targetEncoding)) {
                log.debug("文件已是目标编码，无需转换: {}", filePath);
                return;
            }

            byte[] content = readFileBytes(filePath);
            String text = new String(content, currentEncoding);
            writeFileBytes(filePath, text.getBytes(targetEncoding));

            log.info("文件编码转换成功: {} (from {} to {})", filePath, currentEncoding, targetEncoding);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("文件编码转换失败: {}", filePath, e);
            throw new BusinessException("文件编码转换失败: " + e.getMessage());
        }
    }

    /**
     * 读取文件字节
     */
    private static byte[] readFileBytes(String filePath) throws IOException {
        File file = new File(filePath);
        byte[] content = new byte[(int) file.length()];
        try (FileInputStream fis = new FileInputStream(file)) {
            int totalRead = 0;
            int bytesRead;
            while ((bytesRead = fis.read(content, totalRead, content.length - totalRead)) > 0) {
                totalRead += bytesRead;
            }
        }
        return content;
    }

    /**
     * 写入文件字节
     */
    private static void writeFileBytes(String filePath, byte[] content) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            fos.write(content);
        }
    }
}
