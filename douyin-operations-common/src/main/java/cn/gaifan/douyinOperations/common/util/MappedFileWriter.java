package cn.gaifan.douyinOperations.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.channels.FileChannel;
import java.lang.reflect.Method;

/**
 * 内存映射文件写入工具类
 * 负责使用MappedByteBuffer进行大文件写入
 *
 * @author gaifan
 */
public class MappedFileWriter {

    private static final Logger log = LoggerFactory.getLogger(MappedFileWriter.class);

    /**
     * 清理MappedByteBuffer，释放直接内存
     * 避免内存泄漏，特别是在DirectBuffer占用过多内存时
     *
     * @param buffer MappedByteBuffer对象
     */
    public static void clean(MappedByteBuffer buffer) {
        if (buffer == null) {
            return;
        }
        try {
            Method cleaner = buffer.getClass().getMethod("cleaner");
            cleaner.setAccessible(true);
            Object cleanerObj = cleaner.invoke(buffer);
            if (cleanerObj != null) {
                Method clean = cleanerObj.getClass().getMethod("clean");
                clean.setAccessible(true);
                clean.invoke(cleanerObj);
            }
        } catch (Exception e) {
            log.warn("释放MappedByteBuffer内存失败", e);
        }
    }

    /**
     * 以指定编码字符串向目标文件追加写入内容
     *
     * @param to 目标文件全路径
     * @param content 待写入内容
     * @param charsetString 编码字符串
     * @throws IOException IO异常或编码异常
     */
    public static void write(String to, String content, String charsetString) throws IOException {
        if (content == null) {
            log.warn("写入内容为空，文件路径: {}", to);
            return;
        }
        try {
            byte[] bs;
            if (charsetString != null && !charsetString.isEmpty()) {
                bs = content.getBytes(charsetString);
            } else {
                bs = content.getBytes(StandardCharsets.UTF_8);
            }
            write(to, bs);
        } catch (Exception e) {
            log.error("写入文件失败，文件路径: {}, 编码: {}", to, charsetString, e);
            throw new IOException("写入文件失败: " + to, e);
        }
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
        if (content == null) {
            log.warn("写入内容为空，文件路径: {}", to);
            return;
        }
        try {
            byte[] bs;
            if (charset != null) {
                bs = content.getBytes(charset);
            } else {
                bs = content.getBytes(StandardCharsets.UTF_8);
            }
            write(to, bs);
        } catch (Exception e) {
            log.error("写入文件失败，文件路径: {}, 编码: {}", to, charset, e);
            throw new IOException("写入文件失败: " + to, e);
        }
    }

    /**
     * 以默认编码向目标文件追加写入内容
     *
     * @param to 目标文件全路径
     * @param content 待写入内容
     * @throws IOException IO异常
     */
    public static void write(String to, String content) throws IOException {
        write(to, content, "");
    }

    /**
     * 向目标文件追加写入内容（字节数组）
     * 使用内存映射技术实现高效写入
     *
     * @param to 目标文件全路径
     * @param bs 待写入内容字节数组
     * @throws IOException IO异常
     */
    public static void write(String to, byte[] bs) throws IOException {
        if (bs == null || bs.length == 0) {
            log.debug("待写入数据为空，跳过写入操作，文件路径: {}", to);
            return;
        }

        if (to == null || to.trim().isEmpty()) {
            throw new IllegalArgumentException("文件路径不能为空");
        }

        File file = new File(to);
        String absolutePath = file.getAbsolutePath();

        java.util.concurrent.locks.ReentrantLock lock = MappedBiggerFileWriterUtil.getLock(absolutePath);
        lock.lock();
        try {
            prepareFile(file);

            ChannelWrapper wrapper = MappedBiggerFileWriterUtil.getOrCreateChannel(file);
            FileChannel fc = wrapper.getChannel();

            MappedByteBuffer mbuf = null;
            try {
                long offset = fc.size();
                mbuf = fc.map(FileChannel.MapMode.READ_WRITE, offset, bs.length);
                mbuf.put(bs);
                mbuf.force();

                MappedBiggerFileWriterUtil.recordWrite(bs.length);
                log.debug("成功写入 {} 字节到文件: {}", bs.length, absolutePath);
            } catch (IOException e) {
                log.error("写入文件失败，文件路径: {}, 写入字节数: {}", absolutePath, bs.length, e);
                MappedBiggerFileWriterUtil.removeChannel(absolutePath);
                throw new IOException("写入文件失败: " + absolutePath, e);
            } finally {
                if (mbuf != null) {
                    clean(mbuf);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * 准备文件：创建父目录和文件
     */
    private static void prepareFile(File file) throws IOException {
        File parentFile = file.getParentFile();
        if (parentFile != null && !parentFile.exists()) {
            boolean created = parentFile.mkdirs();
            if (!created) {
                log.warn("创建目录失败: {}", parentFile.getAbsolutePath());
            } else {
                log.debug("创建目录成功: {}", parentFile.getAbsolutePath());
            }
        }

        if (!file.exists()) {
            boolean created = file.createNewFile();
            if (!created) {
                log.warn("创建文件失败，可能已存在: {}", file.getAbsolutePath());
            } else {
                log.debug("创建文件成功: {}", file.getAbsolutePath());
            }
        }
    }
}

class ChannelWrapper {
    private final RandomAccessFile randomAccessFile;
    private final FileChannel channel;
    private final String filePath;
    private long lastAccessTime;
    private long lastHealthCheckTime = 0;
    private static final long HEALTH_CHECK_INTERVAL = 60000;

    public ChannelWrapper(RandomAccessFile randomAccessFile, FileChannel channel, String filePath) {
        this.randomAccessFile = randomAccessFile;
        this.channel = channel;
        this.filePath = filePath;
        this.lastAccessTime = System.currentTimeMillis();
    }

    public FileChannel getChannel() {
        this.lastAccessTime = System.currentTimeMillis();
        return channel;
    }

    public long getLastAccessTime() {
        return lastAccessTime;
    }

    public String getFilePath() {
        return filePath;
    }

    public boolean isHealthy() {
        long now = System.currentTimeMillis();
        if (now - lastHealthCheckTime < HEALTH_CHECK_INTERVAL) {
            return true;
        }
        lastHealthCheckTime = now;

        try {
            if (!channel.isOpen()) {
                Slf4jLoggerFactory.getLogger().debug("Channel健康检查失败：Channel已关闭，文件: {}", filePath);
                return false;
            }

            channel.size();

            File file = new File(filePath);
            if (!file.exists()) {
                Slf4jLoggerFactory.getLogger().debug("Channel健康检查失败：文件不存在，文件: {}", filePath);
                return false;
            }

            return true;
        } catch (IOException e) {
            Slf4jLoggerFactory.getLogger().debug("Channel健康检查失败，文件: {}, 错误: {}", filePath, e.getMessage());
            return false;
        }
    }

    public void close() throws IOException {
        try {
            if (channel != null) {
                channel.close();
            }
        } finally {
            if (randomAccessFile != null) {
                randomAccessFile.close();
            }
        }
    }
}

class Slf4jLoggerFactory {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ChannelWrapper.class);

    static org.slf4j.Logger getLogger() {
        return log;
    }
}
