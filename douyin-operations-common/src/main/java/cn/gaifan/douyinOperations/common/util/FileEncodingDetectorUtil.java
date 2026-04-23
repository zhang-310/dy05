package cn.gaifan.douyinOperations.common.util;

import lombok.extern.slf4j.Slf4j;
import cn.gaifan.douyinOperations.common.exception.BusinessException;

/**
 * 文件编码检测工具类（Facade）
 * 为了向后兼容，将所有功能委托到具体的工具类：
 * - EncodingDetector: 编码检测
 * - EncodingConverter: 编码转换
 *
 * @author system
 */
@Slf4j
public class FileEncodingDetectorUtil {

    // ============ Detection Methods (EncodingDetector) ============

    /**
     * 检测文件编码
     *
     * @param filePath 文件路径
     * @return 文件编码，如果无法检测则返回 null
     * @throws BusinessException 文件不存在、无法读取或检测失败时抛出
     */
    public static String detectEncoding(String filePath) {
        return EncodingDetector.detectEncoding(filePath);
    }

    /**
     * 检测文件编码，如果无法检测则返回默认编码
     *
     * @param filePath 文件路径
     * @return 文件编码，无法检测时返回默认编码（UTF-8）
     * @throws BusinessException 文件不存在或读取失败时抛出
     */
    public static String detectEncodingWithDefault(String filePath) {
        return EncodingDetector.detectEncodingWithDefault(filePath);
    }

    /**
     * 检查文件是否为文本文件
     *
     * @param filePath 文件路径
     * @return 如果可能是文本文件返回true，否则返回false
     */
    public static boolean isTextFile(String filePath) {
        return EncodingDetector.isTextFile(filePath);
    }

    // ============ Conversion Methods (EncodingConverter) ============

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
        return EncodingConverter.convertEncoding(source, fromEncoding, toEncoding);
    }

    /**
     * 转换文件编码
     *
     * @param filePath 文件路径
     * @param targetEncoding 目标编码
     * @throws BusinessException 转换失败时抛出
     */
    public static void convertFileEncoding(String filePath, String targetEncoding) {
        EncodingConverter.convertFileEncoding(filePath, targetEncoding);
    }
}
