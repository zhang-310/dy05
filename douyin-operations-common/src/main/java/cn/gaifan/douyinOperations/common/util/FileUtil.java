package cn.gaifan.douyinOperations.common.util;

import java.io.File;
import java.util.List;

/**
 * 文件操作工具类（Facade）
 * <p>
 * 为了向后兼容，将所有功能委托到具体的工具类：
 * - FileReader: 文件读取
 * - FileWriter: 文件写入
 * - FileValidator: 文件验证
 * - PathUtils: 路径操作
 * </p>
 *
 * @author system
 */
public class FileUtil {

    // ============ Directory Methods (PathUtils) ============

    public static void createDirs(String dir) {
        PathUtils.createDirs(dir);
    }

    public static void createDirs(File dir) {
        PathUtils.createDirs(dir);
    }

    public static String[] getDirectories(String dirPath) {
        return PathUtils.getDirectories(dirPath);
    }

    @Deprecated
    public static String[] getDirectorys(String dirPath) {
        return PathUtils.getDirectories(dirPath);
    }

    public static String[] getDirectoryFiles(String dirPath) {
        return PathUtils.getDirectoryFiles(dirPath);
    }

    // ============ File Validation (FileValidator) ============

    public static boolean exists(String path) {
        return FileValidator.exists(path);
    }

    @Deprecated
    public static boolean isExists(String path) {
        return FileValidator.exists(path);
    }

    public static String getEncoding(String fileName) throws java.io.IOException {
        return FileValidator.getEncoding(fileName);
    }

    // ============ File Reading (FileReader) ============

    public static String readFile(File file, String charset) {
        return FileReader.readFile(file, charset);
    }

    public static String readFile(String filePath, String charset) {
        return FileReader.readFile(filePath, charset);
    }

    public static String readFile(File parentFile, String fileName, String charset) {
        if (parentFile == null || fileName == null || fileName.trim().isEmpty()) {
            return "";
        }
        File file = new File(parentFile, fileName);
        return FileReader.readFile(file, charset);
    }

    public static String readFile(String parentFile, String fileName, String charset) {
        if (parentFile == null || parentFile.trim().isEmpty() ||
            fileName == null || fileName.trim().isEmpty()) {
            return "";
        }
        File file = new File(parentFile, fileName);
        return FileReader.readFile(file, charset);
    }

    public static StringBuilder readFileForStringBuilder(String filePath, String encoding) {
        return FileReader.readFileForStringBuilder(filePath, encoding);
    }

    @Deprecated
    public static StringBuffer readFileForStringBuffer(String filePath, String encoding) {
        StringBuilder sb = FileReader.readFileForStringBuilder(filePath, encoding);
        return new StringBuffer(sb);
    }

    public static List<String> getListForTxt(String filePath, String charset) {
        return FileReader.getListForTxt(filePath, charset);
    }

    // ============ File Writing (FileWriter) ============

    public static void writeFile(String filePath, String fileContent, String writeCharset) {
        FileWriter.writeFile(filePath, fileContent, writeCharset);
    }

    public static void writeToFile(String fileName, String content) {
        FileWriter.writeToFile(fileName, content);
    }

    public static void writeToFile(String fileName, String content, String charset, boolean append) {
        FileWriter.writeToFile(fileName, content, charset, append);
    }

    // ============ File Deletion (PathUtils) ============

    public static boolean delete(String path) {
        return PathUtils.delete(path);
    }

    public static boolean deleteByPrefixAndSuffix(String dir, String prefix, String suffix) {
        return PathUtils.deleteByPrefixAndSuffix(dir, prefix, suffix);
    }

    public static void deleteDir(File file) {
        PathUtils.deleteDir(file);
    }
}
