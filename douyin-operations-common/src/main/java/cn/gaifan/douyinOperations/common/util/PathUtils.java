package cn.gaifan.douyinOperations.common.util;

import lombok.extern.slf4j.Slf4j;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 路径和目录工具类
 * 负责目录操作和路径处理
 *
 * @author system
 */
@Slf4j
public class PathUtils {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PathUtils.class);

    /**
     * 创建目录（如果不存在）
     *
     * @param dir 目录路径
     */
    public static void createDirs(String dir) {
        if (dir == null || dir.trim().isEmpty()) {
            logger.warn("目录路径为空，无法创建目录");
            return;
        }
        File file = new File(dir);
        if (!file.exists()) {
            boolean created = file.mkdirs();
            if (!created) {
                logger.warn("创建目录失败: {}", dir);
            }
        }
    }

    /**
     * 创建目录（如果不存在）
     *
     * @param dir 目录对象
     */
    public static void createDirs(File dir) {
        if (dir != null && !dir.exists()) {
            boolean created = dir.mkdirs();
            if (!created) {
                logger.warn("创建目录失败: {}", dir.getAbsolutePath());
            }
        }
    }

    /**
     * 获取指定目录下的所有子目录名称
     *
     * @param dirPath 目录路径
     * @return 子目录名称数组
     */
    public static String[] getDirectories(String dirPath) {
        if (dirPath == null || dirPath.trim().isEmpty()) {
            return new String[0];
        }
        File dir = new File(dirPath);
        if (!dir.exists() || !dir.isDirectory()) {
            return new String[0];
        }
        File[] contents = dir.listFiles();
        if (contents == null) {
            logger.warn("无法读取目录内容，可能是权限问题: {}", dirPath);
            return new String[0];
        }
        List<String> dirList = new ArrayList<>();
        for (File content : contents) {
            if (content.isDirectory()) {
                dirList.add(content.getName());
            }
        }
        return dirList.toArray(new String[0]);
    }

    /**
     * 获取指定目录下的所有文件名
     *
     * @param dirPath 目录路径
     * @return 文件名数组
     */
    public static String[] getDirectoryFiles(String dirPath) {
        if (dirPath == null || dirPath.trim().isEmpty()) {
            return new String[0];
        }
        File dir = new File(dirPath);
        if (!dir.exists() || !dir.isDirectory()) {
            return new String[0];
        }
        File[] contents = dir.listFiles();
        if (contents == null) {
            logger.warn("无法读取目录内容，可能是权限问题: {}", dirPath);
            return new String[0];
        }
        List<String> fileList = new ArrayList<>();
        for (File content : contents) {
            if (content.isFile()) {
                fileList.add(content.getName());
            }
        }
        return fileList.toArray(new String[0]);
    }

    /**
     * 根据前缀和后缀删除目录中的文件
     *
     * @param dir 目录路径
     * @param prefix 文件名前缀
     * @param suffix 文件名后缀
     * @return 至少删除一个文件返回 true
     */
    public static boolean deleteByPrefixAndSuffix(String dir, String prefix, String suffix) {
        if (dir == null || dir.trim().isEmpty()) {
            logger.warn("目录路径为空，无法删除文件");
            return false;
        }
        if (prefix == null) {
            prefix = "";
        }
        if (suffix == null) {
            suffix = "";
        }

        try {
            File directory = new File(dir);
            if (!directory.exists() || !directory.isDirectory()) {
                logger.warn("目录不存在或不是目录: {}", dir);
                return false;
            }
            File[] fileList = directory.listFiles();
            if (fileList == null) {
                logger.warn("无法读取目录内容: {}", dir);
                return false;
            }
            boolean deleted = false;
            for (File f : fileList) {
                if (f.isFile()) {
                    String fileName = f.getName();
                    if (fileName.startsWith(prefix) && fileName.endsWith(suffix)) {
                        if (f.delete()) {
                            deleted = true;
                            logger.debug("删除文件成功: {}", f.getAbsolutePath());
                        } else {
                            logger.warn("删除文件失败: {}", f.getAbsolutePath());
                        }
                    }
                }
            }
            return deleted;
        } catch (Exception ex) {
            logger.error("删除文件时发生异常: dir={}, prefix={}, suffix={}", dir, prefix, suffix, ex);
        }
        return false;
    }

    /**
     * 递归删除目录及其所有内容
     *
     * @param file 文件或目录对象
     */
    public static void deleteDir(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        if (file.isFile()) {
            boolean deleted = file.delete();
            if (!deleted) {
                logger.warn("删除文件失败: {}", file.getAbsolutePath());
            }
        } else if (file.isDirectory()) {
            File[] subFiles = file.listFiles();
            if (subFiles != null) {
                for (File subFile : subFiles) {
                    deleteDir(subFile);
                }
            }
            boolean deleted = file.delete();
            if (!deleted) {
                logger.warn("删除目录失败: {}", file.getAbsolutePath());
            }
        }
    }

    /**
     * 删除文件
     *
     * @param path 文件路径
     * @return 删除成功返回 true
     */
    public static boolean delete(String path) {
        if (path == null || path.trim().isEmpty()) {
            logger.warn("文件路径为空，无法删除");
            return false;
        }
        try {
            File file = new File(path);
            if (file.exists() && file.isFile()) {
                boolean deleted = file.delete();
                if (!deleted) {
                    logger.warn("删除文件失败: {}", path);
                }
                return deleted;
            } else if (file.exists() && !file.isFile()) {
                logger.warn("路径不是文件，无法删除: {}", path);
            }
        } catch (Exception ex) {
            logger.error("删除文件时发生异常: {}", path, ex);
        }
        return false;
    }
}
