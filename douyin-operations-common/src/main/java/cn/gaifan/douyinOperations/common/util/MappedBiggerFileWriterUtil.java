package cn.gaifan.douyinOperations.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.nio.channels.FileChannel;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 大文件写入工具类（Facade）
 * 为了向后兼容，将所有功能委托到具体的工具类：
 * - MappedFileWriter: 内存映射文件写入
 * - BufferedChunkWriter: 块写入管理
 * - FileWriterPool: 对象池管理
 *
 * 使用内存映射（MappedByteBuffer）技术实现高效的大文件追加写入。
 *
 * @author gaifan
 */
public class MappedBiggerFileWriterUtil {

    private static final Logger log = LoggerFactory.getLogger(MappedBiggerFileWriterUtil.class);

    /**
     * FileChannel缓存，按文件路径存储
     */
    private static final Map<String, ChannelWrapper> CHANNEL_CACHE = new ConcurrentHashMap<>();

    /**
     * 锁缓存，按文件路径分段锁
     */
    private static final Map<String, ReentrantLock> LOCK_CACHE = new ConcurrentHashMap<>();

    /**
     * Channel缓存的最大数量
     */
    @SuppressWarnings("unused")
    private static final int MAX_CACHE_SIZE = getMaxCacheSize();

    /**
     * 按访问时间排序的缓存映射
     */
    private static final TreeMap<Long, String> ACCESS_TIME_MAP = new TreeMap<>();

    /**
     * 获取缓存最大数量，支持配置化
     */
    private static int getMaxCacheSize() {
        try {
            String sysProp = System.getProperty("mapped.filewriter.max.cache.size");
            if (sysProp != null && !sysProp.trim().isEmpty()) {
                int size = Integer.parseInt(sysProp.trim());
                if (size > 0 && size <= 1000) {
                    log.info("从系统属性读取缓存大小配置: {}", size);
                    return size;
                }
                log.warn("系统属性配置的缓存大小无效: {}，使用默认值50", sysProp);
            }

            String envVar = System.getenv("MAPPED_FILEWRITER_MAX_CACHE_SIZE");
            if (envVar != null && !envVar.trim().isEmpty()) {
                int size = Integer.parseInt(envVar.trim());
                if (size > 0 && size <= 1000) {
                    log.info("从环境变量读取缓存大小配置: {}", size);
                    return size;
                }
                log.warn("环境变量配置的缓存大小无效: {}，使用默认值50", envVar);
            }
        } catch (NumberFormatException e) {
            log.warn("缓存大小配置格式错误，使用默认值50: {}", e.getMessage());
        }
        log.debug("使用默认缓存大小: 50");
        return 50;
    }

    /**
     * 以指定编码字符串向目标文件追加写入内容
     *
     * @param to 目标文件全路径
     * @param content 待写入内容
     * @param charsetString 编码字符串
     * @throws IOException IO异常
     */
    public static void write(String to, String content, String charsetString) throws IOException {
        MappedFileWriter.write(to, content, charsetString);
    }

    /**
     * 以指定Charset编码向目标文件追加写入内容
     *
     * @param to 目标文件全路径
     * @param content 待写入内容
     * @param charset 字符编码
     * @throws IOException IO异常
     */
    public static void write(String to, String content, Charset charset) throws IOException {
        MappedFileWriter.write(to, content, charset);
    }

    /**
     * 以默认编码向目标文件追加写入内容
     *
     * @param to 目标文件全路径
     * @param content 待写入内容
     * @throws IOException IO异常
     */
    public static void write(String to, String content) throws IOException {
        MappedFileWriter.write(to, content);
    }

    /**
     * 向目标文件追加写入内容（字节数组）
     *
     * @param to 目标文件全路径
     * @param bs 待写入内容字节数组
     * @throws IOException IO异常
     */
    public static void write(String to, byte[] bs) throws IOException {
        MappedFileWriter.write(to, bs);
    }

    /**
     * 清理所有缓存的Channel
     */
    public static void cleanup() {
        for (ChannelWrapper wrapper : CHANNEL_CACHE.values()) {
            try {
                wrapper.close();
            } catch (IOException e) {
                log.warn("关闭Channel失败: {}", e.getMessage());
            }
        }
        CHANNEL_CACHE.clear();
        LOCK_CACHE.clear();
        ACCESS_TIME_MAP.clear();
        log.info("已清理所有缓存的FileChannel");
    }

    // ============ Internal Methods for MappedFileWriter ============

    protected static ReentrantLock getLock(String absolutePath) {
        return LOCK_CACHE.computeIfAbsent(absolutePath, k -> new ReentrantLock());
    }

    protected static ChannelWrapper getOrCreateChannel(File file) throws IOException {
        String absolutePath = file.getAbsolutePath();
        return CHANNEL_CACHE.computeIfAbsent(absolutePath, k -> {
            try {
                RandomAccessFile raf = new RandomAccessFile(file, "rw");
                FileChannel channel = raf.getChannel();
                return new ChannelWrapper(raf, channel, absolutePath);
            } catch (IOException e) {
                log.error("创建FileChannel失败，文件: {}", absolutePath, e);
                throw new RuntimeException("创建FileChannel失败: " + absolutePath, e);
            }
        });
    }

    protected static void removeChannel(String absolutePath) {
        ChannelWrapper wrapper = CHANNEL_CACHE.remove(absolutePath);
        if (wrapper != null) {
            try {
                wrapper.close();
                log.debug("移除缓存的Channel: {}", absolutePath);
            } catch (IOException e) {
                log.warn("关闭Channel失败，文件: {}", absolutePath, e);
            }
        }
    }

    protected static void recordWrite(long bytes) {
        // 统计记录
    }
}
