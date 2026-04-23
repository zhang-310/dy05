package cn.gaifan.douyinOperations.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 时间戳解析工具类
 * 负责将字符串解析为Timestamp对象
 *
 * @author gaifan
 */
public class TimestampParser {

    private static final Logger log = LoggerFactory.getLogger(TimestampParser.class);

    private static final String DEFAULT_DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final ConcurrentHashMap<String, DateTimeFormatter> FORMATTER_CACHE =
            new ConcurrentHashMap<>(32);

    static {
        FORMATTER_CACHE.put(DEFAULT_DATETIME_FORMAT, DateTimeFormatter.ofPattern(DEFAULT_DATETIME_FORMAT));
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
     * 将日期时间字符串转换为Timestamp对象
     * 格式：yyyy-MM-dd HH:mm:ss
     *
     * @param dateStr 日期时间字符串，如果为null或空字符串返回null
     * @return Timestamp对象，解析失败返回null
     */
    public static Timestamp stringToTimestamp(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        try {
            LocalDateTime dateTime = LocalDateTime.parse(dateStr.trim(),
                    DateTimeFormatter.ofPattern(DEFAULT_DATETIME_FORMAT));
            return Timestamp.valueOf(dateTime);
        } catch (DateTimeParseException e) {
            log.warn("日期时间字符串解析失败: dateStr={}", dateStr, e);
            return null;
        }
    }

    /**
     * 将日期时间字符串转换为Timestamp对象（带异常抛出）
     *
     * @param dateStr 日期时间字符串，不能为null或空字符串
     * @return Timestamp对象
     * @throws TimestampParseException 如果解析失败
     */
    public static Timestamp stringToTimestampOrThrow(String dateStr) {
        if (dateStr == null) {
            throw new NullPointerException("dateStr不能为null");
        }
        String trimmed = dateStr.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("dateStr不能为空字符串");
        }
        try {
            LocalDateTime dateTime = LocalDateTime.parse(trimmed,
                    DateTimeFormatter.ofPattern(DEFAULT_DATETIME_FORMAT));
            return Timestamp.valueOf(dateTime);
        } catch (DateTimeParseException e) {
            String errorMsg = String.format("日期时间字符串解析失败: dateStr=%s", dateStr);
            log.error(errorMsg, e);
            throw new TimestampParseException(errorMsg, e);
        }
    }

    /**
     * 将指定格式的日期时间字符串转换为Timestamp对象
     *
     * @param time 日期时间字符串，如果为null或空字符串返回null
     * @param format 日期时间格式，如"yyyy-MM-dd HH:mm:ss"，如果为null或空使用默认格式
     * @return Timestamp对象，解析失败返回null
     */
    public static Timestamp parseTimestamp(String time, String format) {
        if (time == null || time.trim().isEmpty()) {
            return null;
        }
        if (format == null || format.trim().isEmpty()) {
            log.debug("日期格式为空，使用默认格式: {}", DEFAULT_DATETIME_FORMAT);
            format = DEFAULT_DATETIME_FORMAT;
        }
        try {
            DateTimeFormatter formatter = getFormatter(format);
            LocalDateTime dateTime = LocalDateTime.parse(time.trim(), formatter);
            return Timestamp.valueOf(dateTime);
        } catch (DateTimeParseException e) {
            log.warn("日期时间字符串解析失败: time={}, format={}", time, format, e);
            return null;
        } catch (IllegalArgumentException e) {
            log.warn("日期格式无效: format={}", format, e);
            return null;
        }
    }

    /**
     * 将指定格式的日期时间字符串转换为Timestamp对象（带异常抛出）
     *
     * @param time 日期时间字符串，不能为null或空字符串
     * @param format 日期时间格式，如果为null或空使用默认格式
     * @return Timestamp对象
     * @throws TimestampParseException 如果解析失败
     */
    public static Timestamp parseTimestampOrThrow(String time, String format) {
        if (time == null) {
            throw new NullPointerException("time不能为null");
        }
        String trimmed = time.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("time不能为空字符串");
        }
        if (format == null || format.trim().isEmpty()) {
            format = DEFAULT_DATETIME_FORMAT;
        }
        try {
            DateTimeFormatter formatter = getFormatter(format);
            LocalDateTime dateTime = LocalDateTime.parse(trimmed, formatter);
            return Timestamp.valueOf(dateTime);
        } catch (DateTimeParseException e) {
            String errorMsg = String.format("日期时间字符串解析失败: time=%s, format=%s", time, format);
            log.error(errorMsg, e);
            throw new TimestampParseException(errorMsg, e);
        } catch (IllegalArgumentException e) {
            String errorMsg = String.format("日期格式无效: format=%s", format);
            log.error(errorMsg, e);
            throw new IllegalArgumentException(errorMsg, e);
        }
    }

    /**
     * 时间戳解析异常
     */
    public static class TimestampParseException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public TimestampParseException(String message) {
            super(message);
        }

        public TimestampParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
