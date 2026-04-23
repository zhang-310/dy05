package cn.gaifan.douyinOperations.common.util;

import lombok.extern.slf4j.Slf4j;
import java.io.*;

/**
 * 文件写入工具类
 * 负责文件内容的写入操作
 *
 * @author system
 */
@Slf4j
public class FileWriter {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(FileWriter.class);
    private static final String DEFAULT_CHARSET = "UTF-8";
    private static final String CHARSET_GB2312 = "GB2312";

    /**
     * 写入文件内容
     *
     * @param filePath 文件路径
     * @param fileContent 文件内容
     * @param writeCharset 字符编码
     */
    public static void writeFile(String filePath, String fileContent, String writeCharset) {
        if (filePath == null || filePath.trim().isEmpty()) {
            logger.warn("文件路径为空，无法写入");
            return;
        }
        if (fileContent == null) {
            logger.warn("文件内容为 null，将写入空内容");
            fileContent = "";
        }
        if (writeCharset == null || writeCharset.trim().isEmpty()) {
            logger.warn("字符编码为空，使用默认编码: {}", DEFAULT_CHARSET);
            writeCharset = DEFAULT_CHARSET;
        }

        File file = new File(filePath);
        File parentFile = file.getParentFile();
        if (parentFile != null) {
            PathUtils.createDirs(parentFile);
        }
        try (BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(filePath), writeCharset))) {
            bw.write(fileContent);
        } catch (IOException e) {
            logger.error("写入文件失败: {}", filePath, e);
        }
    }

    /**
     * 向文件写入内容（覆盖模式，默认使用 GB2312 编码）
     *
     * @param fileName 文件名
     * @param content 内容
     */
    public static void writeToFile(String fileName, String content) {
        writeToFile(fileName, content, CHARSET_GB2312, false);
    }

    /**
     * 向文件写入内容
     *
     * @param fileName 文件名
     * @param content 内容
     * @param charset 字符编码
     * @param append 是否追加模式
     */
    public static void writeToFile(String fileName, String content, String charset, boolean append) {
        if (fileName == null || fileName.trim().isEmpty()) {
            logger.warn("文件名为空，无法写入");
            return;
        }
        if (content == null) {
            logger.warn("文件内容为 null，将写入空内容");
            content = "";
        }
        if (charset == null || charset.trim().isEmpty()) {
            logger.warn("字符编码为空，使用默认编码: {}", DEFAULT_CHARSET);
            charset = DEFAULT_CHARSET;
        }

        try (OutputStreamWriter osw = new OutputStreamWriter(
                new FileOutputStream(fileName, append), charset)) {
            osw.write(content);
        } catch (IOException e) {
            logger.error("写入文件错误: fileName={}, append={}", fileName, append, e);
        }
    }
}
