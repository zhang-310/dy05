package cn.gaifan.douyinOperations.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.DateTimeException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 时间戳格式化工具类
 * 负责Timestamp对象的格式化输出
 *
 * @author gaifan
 */
public class TimestampFormatter {

    private static final Logger log = LoggerFactory.getLogger(TimestampFormatter.class);

    private static final String DEFAULT_DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final String DATETIME_WITH_MILLIS_FORMAT = "yyyy-MM-dd HH:mm:ss:SSS";
    private static final ZoneId SYSTEM_ZONE = ZoneId.systemDefault();

    private static final DateTimeFormatter DEFAULT_DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern(DEFAULT_DATETIME_FORMAT);
    private static final DateTimeFormatter DATETIME_WITH_MILLIS_FORMATTER =
            DateTimeFormatter.ofPattern(DATETIME_WITH_MILLIS_FORMAT);

    private static final ConcurrentHashMap<String, DateTimeFormatter> FORMATTER_CACHE =
            new ConcurrentHashMap<>(32);

    static {
        FORMATTER_CACHE.put(DEFAULT_DATETIME_FORMAT, DEFAULT_DATETIME_FORMATTER);
        FORMATTER_CACHE.put(DATETIME_WITH_MILLIS_FORMAT, DATETIME_WITH_MILLIS_FORMATTER);
        FORMATTER_CACHE.put("yyyy-MM-dd", DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        FORMATTER_CACHE.put("yyyyMMdd", DateTimeFormatter.ofPattern("yyyyMMdd"));
        FORMATTER_CACHE.put("yyyy-MM-dd HH:mm", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    private static DateTimeFormatter getFormatter(String pattern) {
        if (pattern == null || pattern.trim().isEmpty()) {
            throw new IllegalArgumentException("日期格式模式不能为空");
        }
        String trimmedPattern = pattern.trim();
        return FORMATTER_CACHE.computeIfAbsent(trimmedPattern,
                p -> {
                    try {
                        return DateTimeFormatter.ofPattern(p);
                    } catch (IllegalArgumentException e) {
                        log.error("创建日期格式化器失败，格式无效: pattern={}", p, e);
                        throw e;
                    }
                });
    }

    /**
     * 将Timestamp对象格式化为字符串
     *
     * @param timestamp Timestamp对象，如果为null返回空字符串
     * @param format 日期时间格式，如果为null或空使用默认格式
     * @return 格式化后的字符串
     */
    public static String formatTimestamp(Timestamp timestamp, String format) {
        if (timestamp == null) {
            return "";
        }
        if (format == null || format.trim().isEmpty()) {
            format = DEFAULT_DATETIME_FORMAT;
        }
        try {
            LocalDateTime dateTime = timestamp.toLocalDateTime();
            DateTimeFormatter formatter = getFormatter(format);
            return dateTime.format(formatter);
        } catch (IllegalArgumentException e) {
            String errorMsg = String.format("日期格式无效: format=%s", format);
            log.error(errorMsg, e);
            return "";
        } catch (DateTimeException e) {
            String errorMsg = String.format("日期格式化失败: format=%s, timestamp=%s", format, timestamp);
            log.error(errorMsg, e);
            return "";
        }
    }

    /**
     * 将Timestamp对象格式化为字符串（使用默认格式）
     *
     * @param timestamp Timestamp对象
     * @return 格式化后的字符串
     */
    public static String formatTimestamp(Timestamp timestamp) {
        return formatTimestamp(timestamp, null);
    }

    /**
     * 将Timestamp对象格式化为字符串（带异常抛出）
     *
     * @param timestamp Timestamp对象，不能为null
     * @param format 日期时间格式
     * @return 格式化后的字符串
     * @throws TimestampFormatException 如果格式化失败
     */
    public static String formatTimestampOrThrow(Timestamp timestamp, String format) {
        if (timestamp == null) {
            throw new NullPointerException("timestamp不能为null");
        }
        if (format == null || format.trim().isEmpty()) {
            format = DEFAULT_DATETIME_FORMAT;
        }
        try {
            LocalDateTime dateTime = timestamp.toLocalDateTime();
            DateTimeFormatter formatter = getFormatter(format);
            return dateTime.format(formatter);
        } catch (IllegalArgumentException e) {
            String errorMsg = String.format("日期格式无效: format=%s", format);
            log.error(errorMsg, e);
            throw new IllegalArgumentException(errorMsg, e);
        } catch (DateTimeException e) {
            String errorMsg = String.format("日期格式化失败: format=%s, timestamp=%s", format, timestamp);
            log.error(errorMsg, e);
            throw new TimestampFormatException(errorMsg, e);
        }
    }

    /**
     * 获取当前时间的格式化字符串
     *
     * @param format 日期时间格式，如果为null或空返回空字符串
     * @return 格式化后的字符串
     */
    public static String formatNow(String format) {
        if (format == null || format.trim().isEmpty()) {
            log.warn("日期格式为空，返回空字符串");
            return "";
        }
        try {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = getFormatter(format);
            return now.format(formatter);
        } catch (IllegalArgumentException e) {
            log.error("日期格式无效: format={}", format, e);
            return "";
        } catch (DateTimeException e) {
            log.error("日期格式化失败: format={}", format, e);
            return "";
        }
    }

    /**
     * 将毫秒时间戳转换为格式化的字符串
     * 格式：yyyy-MM-dd HH:mm:ss:SSS
     *
     * @param time 毫秒时间戳，如果为null返回空字符串
     * @return 格式化后的字符串
     */
    public static String longToString(Long time) {
        if (time == null) {
            return "";
        }
        try {
            Instant instant = Instant.ofEpochMilli(time);
            LocalDateTime dateTime = LocalDateTime.ofInstant(instant, SYSTEM_ZONE);
            return dateTime.format(DATETIME_WITH_MILLIS_FORMATTER);
        } catch (DateTimeException e) {
            log.error("时间戳格式化失败（日期时间异常）: time={}", time, e);
            return "";
        } catch (ArithmeticException e) {
            log.error("时间戳格式化失败（算术溢出）: time={}", time, e);
            return "";
        } catch (Exception e) {
            log.error("时间戳格式化失败（未知异常）: time={}", time, e);
            return "";
        }
    }

    /**
     * 时间戳格式化异常
     */
    public static class TimestampFormatException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public TimestampFormatException(String message) {
            super(message);
        }

        public TimestampFormatException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
