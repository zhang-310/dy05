package cn.gaifan.douyinOperations.common.util;

import java.sql.Timestamp;
import java.util.Date;

/**
 * 时间戳工具类（Facade）
 * 为了向后兼容，将所有功能委托到具体的工具类：
 * - TimestampParser: 字符串解析
 * - TimestampFormatter: 格式化输出
 * - TimestampCalculator: 时间计算
 * - TimestampValidator: 验证和比较
 *
 * <p>使用示例：
 * <pre>{@code
 * // 获取当前时间戳
 * Timestamp now = TimestampUtil.getLocalTimestamp();
 *
 * // 解析日期字符串
 * Timestamp ts = TimestampUtil.stringToTimestamp("2024-01-15 10:30:00");
 *
 * // 格式化当前时间
 * String formatted = TimestampUtil.formatNow("yyyy-MM-dd HH:mm:ss");
 *
 * // 计算3天后的时间
 * Timestamp future = TimestampUtil.getAfterDay(3);
 * }</pre>
 *
 * @author gaifan
 */
public class TimestampUtil {

    // ============ Parsing Methods (TimestampParser) ============

    public static Timestamp stringToTimestamp(String dateStr) {
        return TimestampParser.stringToTimestamp(dateStr);
    }

    public static Timestamp stringToTimestampOrThrow(String dateStr) {
        return TimestampParser.stringToTimestampOrThrow(dateStr);
    }

    public static Timestamp parseTimestamp(String time, String format) {
        return TimestampParser.parseTimestamp(time, format);
    }

    public static Timestamp parseTimestampOrThrow(String time, String format) {
        return TimestampParser.parseTimestampOrThrow(time, format);
    }

    public static Timestamp getTimestamp(String time) {
        return TimestampParser.stringToTimestamp(time);
    }

    @Deprecated
    public static Timestamp getLongDate(String time, String format) {
        return TimestampParser.parseTimestamp(time, format);
    }

    // ============ Formatting Methods (TimestampFormatter) ============

    public static String formatTimestamp(Timestamp timestamp, String format) {
        return TimestampFormatter.formatTimestamp(timestamp, format);
    }

    public static String formatTimestamp(Timestamp timestamp) {
        return TimestampFormatter.formatTimestamp(timestamp);
    }

    public static String formatTimestampOrThrow(Timestamp timestamp, String format) {
        return TimestampFormatter.formatTimestampOrThrow(timestamp, format);
    }

    public static String formatNow(String format) {
        return TimestampFormatter.formatNow(format);
    }

    public static String longToString(Long time) {
        return TimestampFormatter.longToString(time);
    }

    @Deprecated
    public static String getNowDate(String format) {
        return TimestampFormatter.formatNow(format);
    }

    @Deprecated
    public static String long2String(Long time) {
        return TimestampFormatter.longToString(time);
    }

    // ============ Calculation Methods (TimestampCalculator) ============

    public static long getTodayStartTimeMillis() {
        return TimestampCalculator.getTodayStartTimeMillis();
    }

    public static Timestamp getTodayStartTimestamp() {
        return TimestampCalculator.getTodayStartTimestamp();
    }

    @Deprecated
    public static Long getTodayStartTime() {
        return TimestampCalculator.getTodayStartTimeMillis();
    }

    public static long getTodayEndTimeMillis() {
        return TimestampCalculator.getTodayEndTimeMillis();
    }

    public static Timestamp getTodayEndTimestamp() {
        return TimestampCalculator.getTodayEndTimestamp();
    }

    @Deprecated
    public static Long getTodayEndTime() {
        return TimestampCalculator.getTodayEndTimeMillis();
    }

    public static Timestamp getLocalTimestamp() {
        return TimestampCalculator.getLocalTimestamp();
    }

    public static Timestamp longToTimestamp(Long time) {
        return TimestampCalculator.longToTimestamp(time);
    }

    @Deprecated
    public static Timestamp long2Timestamp(Long time) {
        return TimestampCalculator.longToTimestamp(time);
    }

    public static long getSecondsFromNow(Timestamp date) {
        return TimestampCalculator.getSecondsFromNow(date);
    }

    @Deprecated
    public static Long toNowTime(Timestamp date) {
        return TimestampCalculator.getSecondsFromNow(date);
    }

    public static Timestamp getAfterYear(Integer year) {
        return TimestampCalculator.getAfterYear(year);
    }

    public static Timestamp getAfterYear(Timestamp startDate, Integer year) {
        return TimestampCalculator.getAfterYear(startDate, year);
    }

    public static Timestamp getAfterMonth(Integer month) {
        return TimestampCalculator.getAfterMonth(month);
    }

    public static Timestamp getAfterMonth(Timestamp startDate, Integer month) {
        return TimestampCalculator.getAfterMonth(startDate, month);
    }

    public static Timestamp getAfterDay(Integer day) {
        return TimestampCalculator.getAfterDay(day);
    }

    public static Timestamp getAfterDay(Timestamp startDate, Integer day) {
        return TimestampCalculator.getAfterDay(startDate, day);
    }

    public static Timestamp getEndTimestamp() {
        return TimestampCalculator.getEndTimestamp();
    }

    public static Timestamp getDayBegin() {
        return TimestampCalculator.getDayBegin();
    }

    // ============ Validation Methods (TimestampValidator) ============

    public static boolean isFuture(Timestamp timestamp) {
        return TimestampValidator.isFuture(timestamp);
    }

    @Deprecated
    public static Boolean compareNow(Timestamp timestamp) {
        return TimestampValidator.compareNow(timestamp);
    }

    public static Timestamp dateToTimestamp(Date date) {
        return TimestampValidator.dateToTimestamp(date);
    }

    @Deprecated
    public static Timestamp date2Timestamp(Date date) {
        return TimestampValidator.dateToTimestamp(date);
    }

    // ============ Exception Classes ============

    /**
     * 时间戳格式化异常
     * 当时间戳格式化失败时抛出此异常
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

    /**
     * 时间戳解析异常
     * 当时间戳字符串解析失败时抛出此异常
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
