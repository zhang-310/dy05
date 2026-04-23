package cn.gaifan.douyinOperations.common.util;


import java.sql.Timestamp;
import java.util.Date;

/**
 * 时间戳验证工具类
 * 负责时间戳的验证和比较
 *
 * @author gaifan
 */
public class TimestampValidator {

    /**
     * 比较指定时间戳与当前时间
     * 判断指定时间戳是否在当前时间之后（未来时间）
     *
     * @param timestamp 要比较的时间戳，如果为null返回false
     * @return true表示时间戳在当前时间之后，false表示在当前时间或之前
     */
    public static boolean isFuture(Timestamp timestamp) {
        if (timestamp == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        return timestamp.getTime() > now;
    }

    /**
     * 验证时间戳是否有效（不为null且不为特定的无效值）
     *
     * @param timestamp 时间戳
     * @return true表示有效，false表示无效
     */
    public static boolean isValid(Timestamp timestamp) {
        return timestamp != null && timestamp.getTime() > 0;
    }

    /**
     * 验证日期字符串是否可以解析为Timestamp
     *
     * @param dateStr 日期字符串
     * @return true表示可解析，false表示不可解析
     */
    public static boolean canParse(String dateStr) {
        return TimestampParser.stringToTimestamp(dateStr) != null;
    }

    /**
     * 验证日期字符串是否可以按指定格式解析为Timestamp
     *
     * @param dateStr 日期字符串
     * @param format 日期格式
     * @return true表示可解析，false表示不可解析
     */
    public static boolean canParse(String dateStr, String format) {
        return TimestampParser.parseTimestamp(dateStr, format) != null;
    }

    /**
     * 验证时间戳是否为当天
     *
     * @param timestamp 时间戳
     * @return true表示是当天，false表示不是
     */
    public static boolean isToday(Timestamp timestamp) {
        if (timestamp == null) {
            return false;
        }
        long todayStart = TimestampCalculator.getTodayStartTimeMillis();
        long todayEnd = TimestampCalculator.getTodayEndTimeMillis();
        long time = timestamp.getTime();
        return time >= todayStart && time <= todayEnd;
    }

    /**
     * 验证两个时间戳是否为同一天
     *
     * @param ts1 时间戳1
     * @param ts2 时间戳2
     * @return true表示同一天，false表示不同天
     */
    public static boolean isSameDay(Timestamp ts1, Timestamp ts2) {
        if (ts1 == null || ts2 == null) {
            return false;
        }
        String date1 = TimestampFormatter.formatTimestamp(ts1, "yyyy-MM-dd");
        String date2 = TimestampFormatter.formatTimestamp(ts2, "yyyy-MM-dd");
        return date1.equals(date2);
    }

    /**
     * 验证时间戳是否在指定范围内
     *
     * @param timestamp 要验证的时间戳
     * @param startTime 开始时间（包含）
     * @param endTime 结束时间（包含）
     * @return true表示在范围内，false表示不在范围内
     */
    public static boolean isInRange(Timestamp timestamp, Timestamp startTime, Timestamp endTime) {
        if (timestamp == null || startTime == null || endTime == null) {
            return false;
        }
        long time = timestamp.getTime();
        long start = startTime.getTime();
        long end = endTime.getTime();
        return time >= start && time <= end;
    }

    /**
     * 将Date对象转换为Timestamp对象
     *
     * @param date Date对象，如果为null返回null
     * @return Timestamp对象
     */
    public static Timestamp dateToTimestamp(Date date) {
        if (date == null) {
            return null;
        }
        return new Timestamp(date.getTime());
    }

    /**
     * 获取兼容方法（已弃用，用于向后兼容）
     * @deprecated 使用 {@link #isFuture(Timestamp)} 代替
     */
    @Deprecated
    public static Boolean compareNow(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        return isFuture(timestamp);
    }
}
