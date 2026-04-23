package cn.gaifan.douyinOperations.common.util;

import lombok.extern.slf4j.Slf4j;
import java.io.*;

/**
 * 文件验证工具类
 * 负责文件存在性验证和文件内容验证
 *
 * @author system
 */
@Slf4j
public class FileValidator {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(FileValidator.class);
    private static final String DEFAULT_DETECT_CHARSET = "GBK";

    /**
     * 检查文件或目录是否存在
     *
     * @param path 文件或目录路径
     * @return 存在返回 true，否则返回 false
     */
    public static boolean exists(String path) {
        if (path == null || path.trim().isEmpty()) {
            return false;
        }
        File f = new File(path);
        return f.exists();
    }

    /**
     * 检查文件编码（通过 BOM 判断）
     *
     * @param fileName 文件名
     * @return 检测到的编码，无法检测时返回 GBK
     * @throws IOException 文件读取异常
     */
    public static String getEncoding(String fileName) throws IOException {
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        File file = new File(fileName);
        if (!file.exists()) {
            throw new IOException("文件不存在: " + fileName);
        }
        if (!file.isFile()) {
            throw new IOException("路径不是文件: " + fileName);
        }

        try (BufferedInputStream bin = new BufferedInputStream(new FileInputStream(fileName))) {
            int firstByte = bin.read();
            int secondByte = bin.read();

            if (firstByte == -1 || secondByte == -1) {
                logger.debug("文件小于2字节，返回默认编码 {}: {}", DEFAULT_DETECT_CHARSET, fileName);
                return DEFAULT_DETECT_CHARSET;
            }

            // UTF-8 BOM: EF BB BF
            if (firstByte == 0xEF && secondByte == 0xBB) {
                int thirdByte = bin.read();
                if (thirdByte == 0xBF) {
                    logger.debug("检测到 UTF-8 BOM: {}", fileName);
                    return "UTF-8";
                }
            }
            // UTF-16 LE BOM: FF FE
            else if (firstByte == 0xFF && secondByte == 0xFE) {
                logger.debug("检测到 UTF-16LE BOM: {}", fileName);
                return "UTF-16LE";
            }
            // UTF-16 BE BOM: FE FF
            else if (firstByte == 0xFE && secondByte == 0xFF) {
                logger.debug("检测到 UTF-16BE BOM: {}", fileName);
                return "UTF-16BE";
            }

            logger.debug("未检测到 BOM，返回默认编码 {}: {}", DEFAULT_DETECT_CHARSET, fileName);
            return DEFAULT_DETECT_CHARSET;
        }
    }
}
