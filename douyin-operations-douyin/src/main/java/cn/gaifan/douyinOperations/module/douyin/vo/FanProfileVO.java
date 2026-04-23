package cn.gaifan.douyinOperations.module.douyin.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 粉丝画像 VO
 */
@Data
public class FanProfileVO {

    /** 账号 ID */
    private Long accountId;

    /** 账号名称 */
    private String accountName;

    /** 粉丝总数 */
    private Long totalFans;

    /** 年龄分布 */
    private List<StatItem> ageDistribution;

    /** 性别分布 */
    private List<StatItem> genderDistribution;

    /** 地域分布（省份） */
    private List<StatItem> provinceDistribution;

    /** 地域分布（城市 Top 10） */
    private List<StatItem> cityDistribution;

    /** 兴趣标签 Top 10 */
    private List<StatItem> interestTags;

    /** 活跃时段分布 */
    private List<StatItem> activeTimeDistribution;

    /** 设备类型分布 */
    private List<StatItem> deviceDistribution;

    /** 同步时间 */
    private Long syncTime;

    @Data
    public static class StatItem {
        private String key;
        private String value;
        private Long count;
        private Double percentage;
    }
}
