package cn.gaifan.douyinOperations.common.util;

import org.mozilla.universalchardet.UniversalDetector;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * 文件编码检测工具类
 * 负责检测文本文件的字符编码
 *
 * @author system
 */
public class EncodingDetector {

    private static final Logger log = LoggerFactory.getLogger(EncodingDetector.class);

    private static final int BUFFER_SIZE = 4096;    private static final int MAX_DETECT_BYTES = 128 * 1024;
    private static final String EXCEL_ENCODING = "UTF-16LE";
    private static final String DEFAULT_ENCODING = "UTF-8";
    private static final String FILE_TYPE_XLS = "xls";
    private static final String FILE_TYPE_XLSX = "xlsx";

    private static final FileTypeInfo EXCEL_XLS_TYPE = new FileTypeInfo(
            new byte[]{(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1},
            "xls", EXCEL_ENCODING
    );

    private static final FileTypeInfo EXCEL_XLSX_TYPE = new FileTypeInfo(
            new byte[]{0x50, 0x4B, 0x03, 0x04},
            "xlsx", EXCEL_ENCODING
    );

    private static class FileTypeInfo {
        final byte[] magicBytes;
        final String extension;
        final String encoding;

        FileTypeInfo(byte[] magicBytes, String extension, String encoding) {
            this.magicBytes = magicBytes;
            this.extension = extension;
            this.encoding = encoding;
        }
    }

    /**
     * 检测文件编码
     *
     * @param filePath 文件路径
     * @return 文件编码，如果无法检测则返回 null
     * @throws BusinessException 文件不存在或无法读取时抛出
     */
    public static String detectEncoding(String filePath) {
        validateFilePath(filePath);

        File file = new File(filePath);
        validateFile(file, filePath);

        if (file.length() == 0) {
            log.warn("文件为空，无法检测编码: {}", filePath);
            return null;
        }

        FileTypeInfo detectedType = detectFileTypeByMagicNumber(filePath);
        if (detectedType != null) {
            log.debug("通过文件头检测到文件类型: {} (文件: {})", detectedType.extension, filePath);
            return detectedType.encoding;
        }

        String fileExtension = getFileExtension(filePath);

        if (FILE_TYPE_XLS.equalsIgnoreCase(fileExtension) || FILE_TYPE_XLSX.equalsIgnoreCase(fileExtension)) {
            return getExcelEncoding(filePath);
        } else {
            return detectTextFileEncoding(filePath);
        }
    }

    /**
     * 通过文件头检测文件类型
     */
    private static FileTypeInfo detectFileTypeByMagicNumber(String filePath) {
        try (FileInputStream fis = new FileInputStream(filePath)) {
            byte[] header = new byte[8];
            int bytesRead = fis.read(header);

            if (bytesRead < 4) {
                return null;
            }

            if (bytesRead >= 8 && matchesMagicBytes(header, EXCEL_XLS_TYPE.magicBytes)) {
                return EXCEL_XLS_TYPE;
            }

            if (bytesRead >= 4 && matchesMagicBytes(header, EXCEL_XLSX_TYPE.magicBytes)) {
                String extension = getFileExtension(filePath);
                if (FILE_TYPE_XLSX.equalsIgnoreCase(extension)) {
                    return EXCEL_XLSX_TYPE;
                }
            }

        } catch (IOException e) {
            log.debug("通过文件头检测文件类型失败: {}", filePath, e);
        }

        return null;
    }

    /**
     * 检查字节数组是否匹配魔法字节
     */
    private static boolean matchesMagicBytes(byte[] header, byte[] magicBytes) {
        if (header == null || magicBytes == null || header.length < magicBytes.length) {
            return false;
        }
        for (int i = 0; i < magicBytes.length; i++) {
            if (header[i] != magicBytes[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * 检测文本文件编码
     */
    private static String detectTextFileEncoding(String filePath) {
        UniversalDetector detector = null;
        try (FileInputStream fis = new FileInputStream(filePath)) {
            detector = new UniversalDetector(null);
            byte[] buf = new byte[BUFFER_SIZE];
            int totalRead = 0;
            int nread;
            while ((nread = fis.read(buf)) > 0 && !detector.isDone() && totalRead < MAX_DETECT_BYTES) {
                detector.handleData(buf, 0, nread);
                totalRead += nread;
            }

            detector.dataEnd();
            String encoding = detector.getDetectedCharset();

            if (encoding == null) {
                encoding = detectEncodingByBOM(filePath);
                if (encoding == null) {
                    log.warn("无法检测文件编码: {}", filePath);
                }
            } else {
                log.debug("检测到文件编码: {} (文件: {})", encoding, filePath);
            }

            return encoding;
        } catch (IOException e) {
            log.error("文件编码检测失败, 文件路径: {}", filePath, e);
            throw new BusinessException("文件编码检测失败: " + e.getMessage());
        } finally {
            if (detector != null) {
                detector.reset();
            }
        }
    }

    /**
     * 通过文件BOM检测编码
     */
    private static String detectEncodingByBOM(String filePath) {
        try (FileInputStream fis = new FileInputStream(filePath)) {
            byte[] bom = new byte[4];
            int bytesRead = fis.read(bom);

            if (bytesRead < 2) {
                return null;
            }

            if (bom[0] == (byte) 0xFF && bom[1] == (byte) 0xFE) {
                if (bytesRead >= 4 && bom[2] == 0x00 && bom[3] == 0x00) {
                    return "UTF-32LE";
                }
                return "UTF-16LE";
            }

            if (bom[0] == (byte) 0xFE && bom[1] == (byte) 0xFF) {
                return "UTF-16BE";
            }

            if (bytesRead >= 3 && bom[0] == (byte) 0xEF && bom[1] == (byte) 0xBB && bom[2] == (byte) 0xBF) {
                return "UTF-8";
            }

            if (bytesRead >= 4 && bom[0] == 0x00 && bom[1] == 0x00 && bom[2] == (byte) 0xFE && bom[3] == (byte) 0xFF) {
                return "UTF-32BE";
            }

        } catch (IOException e) {
            log.debug("通过BOM检测编码失败: {}", filePath, e);
        }

        return null;
    }

    /**
     * 获取Excel文件编码标识
     */
    private static String getExcelEncoding(String filePath) {
        String extension = getFileExtension(filePath);
        String fileType = FILE_TYPE_XLS.equalsIgnoreCase(extension) ? "XLS" : "XLSX";
        log.debug("Excel文件编码标识: {} (文件类型: {})", filePath, fileType);
        return EXCEL_ENCODING;
    }

    /**
     * 验证文件路径参数
     */
    private static void validateFilePath(String filePath) {
        if (filePath == null) {
            throw new BusinessException("文件路径不能为null");
        }
        if (filePath.trim().isEmpty()) {
            throw new BusinessException("文件路径不能为空");
        }
    }

    /**
     * 验证文件对象
     */
    private static void validateFile(File file, String filePath) {
        if (!file.exists()) {
            throw new BusinessException("文件不存在: " + filePath);
        }
        if (!file.isFile()) {
            throw new BusinessException("路径不是有效文件: " + filePath);
        }
        if (!file.canRead()) {
            throw new BusinessException("文件不可读: " + filePath);
        }
    }

    /**
     * 获取文件扩展名
     */
    private static String getFileExtension(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return "";
        }

        int lastDotIndex = filePath.lastIndexOf('.');
        int lastSeparatorIndex = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));

        if (lastDotIndex > lastSeparatorIndex && lastDotIndex < filePath.length() - 1) {
            return filePath.substring(lastDotIndex + 1).toLowerCase();
        }

        return "";
    }

    /**
     * 检测文件编码，如果无法检测则返回默认编码
     */
    public static String detectEncodingWithDefault(String filePath) {
        String encoding = detectEncoding(filePath);
        return encoding != null ? encoding : DEFAULT_ENCODING;
    }

    /**
     * 检查文件是否为文本文件
     */
    public static boolean isTextFile(String filePath) {
        String extension = getFileExtension(filePath);
        if (FILE_TYPE_XLS.equalsIgnoreCase(extension) || FILE_TYPE_XLSX.equalsIgnoreCase(extension)) {
            return false;
        }
        return true;
    }
}
