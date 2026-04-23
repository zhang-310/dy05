package cn.gaifan.douyinOperations.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 统一日期工具类
 * 包含日期解析、格式化、计算和辅助方法
 *
 * @author gaifan
 * @version 3.0
 */
public class DateUtil {

    private static final Logger log = LoggerFactory.getLogger(DateUtil.class);

    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final String DATETIME_COMPACT_FORMAT = "yyyyMMddHHmmss";
    private static final String DATE_SHORT_FORMAT = "yy-MM-dd";
    private static final String DATE_COMPACT_FORMAT = "yyyyMMdd";
    private static final long MILLIS_PER_DAY = 24 * 3600 * 1000L;
    private static final AtomicLong index = new AtomicLong(0L);

    private static final ThreadLocal<ConcurrentHashMap<String, SimpleDateFormat>> DATE_FORMAT_CACHE =
            ThreadLocal.withInitial(ConcurrentHashMap::new);

    private static SimpleDateFormat getDateFormat(String pattern) {
        ConcurrentHashMap<String, SimpleDateFormat> cache = DATE_FORMAT_CACHE.get();
        return cache.computeIfAbsent(pattern, SimpleDateFormat::new);
    }

    // ============ Parsing ============

    private static Date parseDate(String dateStr, String pattern) {
        if (dateStr == null || dateStr.trim().isEmpty()) return null;
        if (pattern == null || pattern.trim().isEmpty()) {
            log.warn("日期格式模式为空，无法解析日期字符串");
            return null;
        }
        try {
            return getDateFormat(pattern).parse(dateStr);
        } catch (ParseException e) {
            log.error("日期解析失败: pattern={}, dateStr={}", pattern, dateStr, e);
            return null;
        }
    }

    public static Date strToDateYyyyMmDd(String dateStr) {
        return parseDate(dateStr, DATE_FORMAT);
    }

    public static Timestamp stringToTimestamp(String dateStr) {
        Date date = parseDate(dateStr, DATETIME_FORMAT);
        if (date == null) {
            log.warn("日期时间字符串解析失败，返回null: dateStr={}", dateStr);
            return null;
        }
        return new Timestamp(date.getTime());
    }

    public static Date parseDateByPattern(String dateStr, String pattern) {
        return parseDate(dateStr, pattern);
    }

    // ============ Formatting ============

    private static String formatDate(Date date, String pattern) {
        if (date == null) return "";
        String finalPattern = (pattern == null || pattern.trim().isEmpty()) ? DATETIME_FORMAT : pattern;
        try {
            return getDateFormat(finalPattern).format(date);
        } catch (Exception e) {
            log.error("日期格式化失败: pattern={}, date={}", finalPattern, date, e);
            return "";
        }
    }

    public static String dateToString(Date date, String dateFormat) {
        return formatDate(date, dateFormat);
    }

    public static String dateToString(Date date) {
        return formatDate(date, DATETIME_FORMAT);
    }

    public static String dateToStringCKFinder(Date date) {
        return formatDate(date, DATETIME_COMPACT_FORMAT);
    }

    public static String dateToStringShort(Date date) {
        return formatDate(date, DATE_SHORT_FORMAT);
    }

    // ============ Calculation ============

    private static String getDaysBefore(int days) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -days);
        return formatDate(cal.getTime(), DATE_FORMAT);
    }

    private static String getDaysAfter(int days) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, days);
        return formatDate(cal.getTime(), DATE_FORMAT);
    }

    public static String getYesterday() { return getDaysBefore(1); }
    public static String getTheDayBeforeYesterday() { return getDaysBefore(2); }
    public static String getBefore3Days() { return getDaysBefore(3); }
    public static String getBefore4Days() { return getDaysBefore(4); }
    public static String getTomorrow() { return getDaysAfter(1); }

    public static String getDateAfter(int day) {
        return getDaysAfter(day);
    }

    public static String getDateAfter(Date date, int day) {
        if (date == null) return "";
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DAY_OF_MONTH, day);
        return formatDate(cal.getTime(), DATE_FORMAT);
    }

    public static String getDateAfterNoUnderline(Date date, int day) {
        if (date == null) return "";
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DAY_OF_MONTH, day);
        return formatDate(cal.getTime(), DATE_COMPACT_FORMAT);
    }

    public static int daysBetween(Date startDate, Date endDate) {
        if (startDate == null || endDate == null) {
            log.warn("计算日期差时参数为null: startDate={}, endDate={}", startDate, endDate);
            return 0;
        }
        try {
            Calendar cal1 = Calendar.getInstance();
            cal1.setTime(startDate);
            cal1.set(Calendar.HOUR_OF_DAY, 0); cal1.set(Calendar.MINUTE, 0);
            cal1.set(Calendar.SECOND, 0); cal1.set(Calendar.MILLISECOND, 0);

            Calendar cal2 = Calendar.getInstance();
            cal2.setTime(endDate);
            cal2.set(Calendar.HOUR_OF_DAY, 0); cal2.set(Calendar.MINUTE, 0);
            cal2.set(Calendar.SECOND, 0); cal2.set(Calendar.MILLISECOND, 0);

            return (int) ((cal2.getTimeInMillis() - cal1.getTimeInMillis()) / MILLIS_PER_DAY);
        } catch (Exception e) {
            log.error("计算日期差时发生异常: startDate={}, endDate={}", startDate, endDate, e);
            return 0;
        }
    }

    public static int daysBetween(String startDateStr, String endDateStr) {
        Date startDate = strToDateYyyyMmDd(startDateStr);
        Date endDate = strToDateYyyyMmDd(endDateStr);
        if (startDate == null || endDate == null) return 0;
        return daysBetween(startDate, endDate);
    }

    // ============ Utility ============

    public static Timestamp getDayBegin() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 1);
        return new Timestamp(cal.getTimeInMillis());
    }

    public static String getNewsDir() {
        Calendar cal = Calendar.getInstance();
        return String.format("/%d/%02d/%02d/", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
    }

    public static String getFileName(String type) {
        return new Date().getTime() + type;
    }

    public static String getImageName(String type) {
        return new Date().getTime() + "-" + index.incrementAndGet() + type;
    }

    public static Date getDate(long date) { return new Date(date); }

    public static String getToday() {
        return formatDate(new Date(), DATE_FORMAT);
    }

    public static String getNextMonth() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, 1);
        return formatDate(cal.getTime(), DATE_FORMAT);
    }

    public static Timestamp date2Timestamp(Date date) {
        return date == null ? null : new Timestamp(date.getTime());
    }

    public static Date timestamp2Date(Timestamp date) {
        return date == null ? null : new Date(date.getTime());
    }

    public static Timestamp getLocalTimestamp() {
        return new Timestamp(System.currentTimeMillis());
    }

    public static Timestamp getEndTimestamp() {
        Date date = strToDateYyyyMmDd("2999-01-01");
        return date == null ? null : new Timestamp(date.getTime());
    }
}
