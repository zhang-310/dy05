package cn.gaifan.douyinOperations.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.DateTimeException;

/**
 * 时间戳计算工具类
 * 负责时间戳的算术运算和时间计算
 *
 * @author gaifan
 */
public class TimestampCalculator {

    private static final Logger log = LoggerFactory.getLogger(TimestampCalculator.class);

    private static final int END_YEAR = 2999;
    private static final int END_MONTH = 1;
    private static final int END_DAY = 1;
    private static final long END_TIMESTAMP_MILLIS = getEndTimestampMillisValue();

    private static long getEndTimestampMillisValue() {
        try {
            LocalDate endDate = LocalDate.of(END_YEAR, END_MONTH, END_DAY);
            LocalDateTime dateTime = endDate.atStartOfDay();
            return dateTime.atZone(ZoneOffset.UTC).toInstant().toEpochMilli();
        } catch (Exception e) {
            try {
                log.error("计算结束时间戳失败，使用后备值", e);
            } catch (Exception ignored) {
                // 日志系统初始化失败，这是在日志系统启动期间的特殊情况
                // 该错误将被忽略，使用默认日志处理
            }
            return 32454410400000L;
        }
    }

    /**
     * 获取今天开始时间（00:00:00.000）的毫秒时间戳
     *
     * @return 今天开始时间的毫秒时间戳
     */
    public static long getTodayStartTimeMillis() {
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        return toEpochMilli(todayStart);
    }

    /**
     * 获取今天开始时间的Timestamp对象
     *
     * @return 今天开始时间的Timestamp对象
     */
    public static Timestamp getTodayStartTimestamp() {
        return new Timestamp(getTodayStartTimeMillis());
    }

    /**
     * 获取今天结束时间（23:59:59.999）的毫秒时间戳
     *
     * @return 今天结束时间的毫秒时间戳
     */
    public static long getTodayEndTimeMillis() {
        LocalDateTime todayEnd = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
        return toEpochMilli(todayEnd);
    }

    /**
     * 获取今天结束时间的Timestamp对象
     *
     * @return 今天结束时间的Timestamp对象
     */
    public static Timestamp getTodayEndTimestamp() {
        return new Timestamp(getTodayEndTimeMillis());
    }

    /**
     * 获取结束时间戳（2999-01-01 00:00:00）
     *
     * @return 2999-01-01的Timestamp对象
     */
    public static Timestamp getEndTimestamp() {
        try {
            LocalDate endDate = LocalDate.of(END_YEAR, END_MONTH, END_DAY);
            LocalDateTime dateTime = endDate.atStartOfDay();
            return Timestamp.valueOf(dateTime);
        } catch (DateTimeException e) {
            log.error("创建结束时间戳失败", e);
            return new Timestamp(END_TIMESTAMP_MILLIS);
        }
    }

    /**
     * 获取当前时间的Timestamp对象
     *
     * @return 当前时间的Timestamp对象
     */
    public static Timestamp getLocalTimestamp() {
        return new Timestamp(System.currentTimeMillis());
    }

    /**
     * 将毫秒时间戳转换为Timestamp对象
     *
     * @param time 毫秒时间戳，如果为null返回null
     * @return Timestamp对象
     */
    public static Timestamp longToTimestamp(Long time) {
        if (time == null) {
            return null;
        }
        return new Timestamp(time);
    }

    /**
     * 计算指定时间距离现在多少秒
     *
     * @param date 指定的时间戳，如果为null返回0
     * @return 距离现在的秒数（如果指定时间在未来，返回负数）
     */
    public static long getSecondsFromNow(Timestamp date) {
        if (date == null) {
            return 0L;
        }
        long now = System.currentTimeMillis();
        long diff = now - date.getTime();
        return diff / 1000;
    }

    /**
     * 获取指定年数后的日期
     *
     * @param year 年数（可以为负数）
     * @return 计算后的Timestamp对象
     */
    public static Timestamp getAfterYear(Integer year) {
        return getAfterYear(getLocalTimestamp(), year);
    }

    /**
     * 从指定日期开始，计算指定年数后的日期
     *
     * @param startDate 基准日期，如果为null使用当前时间
     * @param year 年数（可以为负数）
     * @return 计算后的Timestamp对象
     */
    public static Timestamp getAfterYear(Timestamp startDate, Integer year) {
        return addTimePeriod(startDate, year, null, null, PeriodType.YEAR);
    }

    /**
     * 获取指定月数后的日期
     *
     * @param month 月数（可以为负数）
     * @return 计算后的Timestamp对象
     */
    public static Timestamp getAfterMonth(Integer month) {
        return getAfterMonth(getLocalTimestamp(), month);
    }

    /**
     * 从指定日期开始，计算指定月数后的日期
     *
     * @param startDate 基准日期，如果为null使用当前时间
     * @param month 月数（可以为负数）
     * @return 计算后的Timestamp对象
     */
    public static Timestamp getAfterMonth(Timestamp startDate, Integer month) {
        return addTimePeriod(startDate, null, month, null, PeriodType.MONTH);
    }

    /**
     * 获取指定天数后的日期
     *
     * @param day 天数（可以为负数）
     * @return 计算后的Timestamp对象
     */
    public static Timestamp getAfterDay(Integer day) {
        return getAfterDay(getLocalTimestamp(), day);
    }

    /**
     * 从指定日期开始，计算指定天数后的日期
     *
     * @param startDate 基准日期，如果为null使用当前时间
     * @param day 天数（可以为负数）
     * @return 计算后的Timestamp对象
     */
    public static Timestamp getAfterDay(Timestamp startDate, Integer day) {
        return addTimePeriod(startDate, null, null, day, PeriodType.DAY);
    }

    /**
     * 时间周期类型枚举
     */
    private enum PeriodType {
        YEAR, MONTH, DAY
    }

    /**
     * 添加时间周期的公共方法
     */
    private static Timestamp addTimePeriod(Timestamp startDate, Integer year,
                                          Integer month, Integer day, PeriodType periodType) {
        if (startDate == null) {
            startDate = getLocalTimestamp();
        }

        if (periodType == null) {
            log.warn("periodType为null，返回startDate的副本");
            return new Timestamp(startDate.getTime());
        }

        try {
            LocalDateTime dateTime = startDate.toLocalDateTime();
            LocalDateTime result;

            switch (periodType) {
                case YEAR:
                    if (year == null || year == 0) {
                        return new Timestamp(startDate.getTime());
                    }
                    if (Math.abs(year) > 10000) {
                        log.warn("年份值过大: year={}", year);
                    }
                    result = dateTime.plusYears(year);
                    break;
                case MONTH:
                    if (month == null || month == 0) {
                        return new Timestamp(startDate.getTime());
                    }
                    if (Math.abs(month) > 120000) {
                        log.warn("月份值过大: month={}", month);
                    }
                    result = dateTime.plusMonths(month);
                    break;
                case DAY:
                    if (day == null || day == 0) {
                        return new Timestamp(startDate.getTime());
                    }
                    if (Math.abs(day) > 3650000) {
                        log.warn("天数值过大: day={}", day);
                    }
                    result = dateTime.plusDays(day);
                    break;
                default:
                    log.warn("未知的periodType: {}", periodType);
                    return new Timestamp(startDate.getTime());
            }

            return Timestamp.valueOf(result);
        } catch (DateTimeException e) {
            log.error("添加时间周期失败", e);
            return new Timestamp(startDate.getTime());
        } catch (Exception e) {
            log.error("添加时间周期时发生未知异常", e);
            return new Timestamp(startDate.getTime());
        }
    }

    /**
     * 将LocalDateTime转换为毫秒时间戳
     */
    private static long toEpochMilli(LocalDateTime dateTime) {
        if (dateTime == null) {
            throw new NullPointerException("dateTime不能为null");
        }
        try {
            return dateTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (DateTimeException e) {
            log.error("LocalDateTime转换为时间戳失败", e);
            throw e;
        }
    }

    /**
     * 获取当天开始时间的Timestamp对象（毫秒设置为1）
     *
     * @return 当天开始时间的Timestamp对象
     */
    public static Timestamp getDayBegin() {
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime dayBegin = todayStart.plusNanos(1_000_000L);
        return Timestamp.valueOf(dayBegin);
    }
}
