package cn.gaifan.douyinOperations.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 文件读取工具类
 * 负责文件内容的读取操作
 *
 * @author system
 */
public class FileReader {

    private static final Logger log = LoggerFactory.getLogger(FileReader.class);

    private static final String DEFAULT_CHARSET = "UTF-8";
    private static final String LINE_SEPARATOR_WIN = "\r\n";

    /**
     * 读取文件内容
     *
     * @param file 文件对象
     * @param charset 字符编码
     * @return 文件内容，读取失败返回空字符串
     */
    public static String readFile(File file, String charset) {
        if (file == null) {
            log.warn("文件对象为 null，无法读取");
            return "";
        }
        if (charset == null || charset.trim().isEmpty()) {
            log.warn("字符编码为空，使用默认编码: {}", DEFAULT_CHARSET);
            charset = DEFAULT_CHARSET;
        }
        if (!file.exists()) {
            log.warn("文件不存在: {}", file.getAbsolutePath());
            return "";
        }
        if (!file.isFile()) {
            log.warn("路径不是文件: {}", file.getAbsolutePath());
            return "";
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), charset))) {
            String read = br.readLine();
            while (read != null) {
                sb.append(read).append(LINE_SEPARATOR_WIN);
                read = br.readLine();
            }
        } catch (IOException e) {
            log.error("读取文件失败: {}", file.getAbsolutePath(), e);
            return sb.toString();
        }
        return sb.toString();
    }

    /**
     * 读取文件内容
     *
     * @param filePath 文件路径
     * @param charset 字符编码
     * @return 文件内容，读取失败返回空字符串
     */
    public static String readFile(String filePath, String charset) {
        if (filePath == null || filePath.trim().isEmpty()) {
            log.warn("文件路径为空，无法读取");
            return "";
        }
        return readFile(new File(filePath), charset);
    }

    /**
     * 读取文件内容并返回 StringBuilder
     *
     * @param filePath 文件路径
     * @param encoding 字符编码
     * @return 文件内容的 StringBuilder
     */
    public static StringBuilder readFileForStringBuilder(String filePath, String encoding) {
        if (filePath == null || filePath.trim().isEmpty()) {
            log.warn("文件路径为空，无法读取");
            return new StringBuilder();
        }
        if (encoding == null || encoding.trim().isEmpty()) {
            log.warn("字符编码为空，使用默认编码: {}", DEFAULT_CHARSET);
            encoding = DEFAULT_CHARSET;
        }

        StringBuilder buffered = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(filePath), encoding))) {
            String read;
            while ((read = br.readLine()) != null) {
                buffered.append(read).append(LINE_SEPARATOR_WIN);
            }
        } catch (IOException e) {
            log.error("读取文件失败: {}", filePath, e);
        }
        return buffered;
    }

    /**
     * 读取文本文件并按行分割为列表
     *
     * @param filePath 文件路径
     * @param charset 字符编码
     * @return 行列表，读取失败返回空列表
     */
    public static List<String> getListForTxt(String filePath, String charset) {
        if (filePath == null || filePath.trim().isEmpty()) {
            log.warn("文件路径为空，无法读取");
            return new ArrayList<>();
        }

        String contentString = readFile(new File(filePath), charset);
        if (contentString == null || contentString.isEmpty()) {
            return new ArrayList<>();
        }

        contentString = contentString.replaceAll("(\\r\\n|\\r|\\n)$", "");

        List<String> list = new ArrayList<>();
        String[] temp = contentString.split("\\r?\\n|\\r");
        for (String s : temp) {
            String trimmed = s.trim();
            if (!trimmed.isEmpty()) {
                list.add(trimmed);
            }
        }
        return list;
    }
}
