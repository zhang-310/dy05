package cn.gaifan.douyinOperations.module.douyin.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.douyin.entity.DouyinAccount;
import cn.gaifan.douyinOperations.module.douyin.entity.DyFanProfile;
import cn.gaifan.douyinOperations.module.douyin.entity.DyFanProfileStats;
import cn.gaifan.douyinOperations.module.douyin.repository.DouyinAccountRepository;
import cn.gaifan.douyinOperations.module.douyin.repository.DyFanProfileRepository;
import cn.gaifan.douyinOperations.module.douyin.repository.DyFanProfileStatsRepository;
import cn.gaifan.douyinOperations.module.douyin.service.FanProfileService;
import cn.gaifan.douyinOperations.module.douyin.vo.FanProfileVO;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FanProfileServiceImpl implements FanProfileService {

    private static final Logger log = LoggerFactory.getLogger(FanProfileServiceImpl.class);

    @Resource
    private DouyinAccountRepository accountRepository;

    @Resource
    private DyFanProfileRepository fanProfileRepository;

    @Resource
    private DyFanProfileStatsRepository statsRepository;

    @Value("${douyin.api.base-url}")
    private String douyinApiBaseUrl;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncFanProfile(Long accountId) {
        DouyinAccount account = accountRepository.findById(accountId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "账号不存在"));

        // Token 在 OAuthToken 表，此处仅检查账号存在；实际 API 调用需 OAuthTokenService
        // 若需严格校验，可注入 OAuthTokenService.getToken(account.getUserId(), "douyin")

        try {
            // 调用抖音 API 获取粉丝画像数据
            JSONObject fanData = fetchFanDataFromDouyin(account);

            if (fanData == null || fanData.isEmpty()) {
                log.warn("账号 {} 粉丝画像数据为空", accountId);
                return;
            }

            Timestamp syncTime = new Timestamp(System.currentTimeMillis());

            // 解析并保存统计数据
            saveFanStats(accountId, fanData, syncTime);

            log.info("账号 {} 粉丝画像同步成功", accountId);
        } catch (Exception e) {
            log.error("账号 {} 粉丝画像同步失败", accountId, e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "粉丝画像同步失败: " + e.getMessage());
        }
    }

    @Override
    public FanProfileVO getFanProfile(Long accountId, Long userId) {
        // 验证账号权限
        DouyinAccount account = verifyAccountAccess(accountId, userId);

        // 获取统计数据
        List<DyFanProfileStats> allStats = statsRepository.findByAccountIdAndDeletedOrderByCountDesc(accountId, 0);

        if (allStats.isEmpty()) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "暂无粉丝画像数据，请先同步");
        }

        // 构建 VO
        FanProfileVO vo = new FanProfileVO();
        vo.setAccountId(accountId);
        vo.setAccountName(account.getAccountName());
        vo.setTotalFans(account.getFanCount());

        // 按类型分组
        Map<String, List<DyFanProfileStats>> statsByType = allStats.stream()
                .collect(Collectors.groupingBy(DyFanProfileStats::getStatType));

        vo.setAgeDistribution(convertToStatItems(statsByType.get("age")));
        vo.setGenderDistribution(convertToStatItems(statsByType.get("gender")));
        vo.setProvinceDistribution(convertToStatItems(statsByType.get("province")));
        vo.setCityDistribution(convertToStatItems(statsByType.get("city")));
        vo.setInterestTags(convertToStatItems(statsByType.get("interest")));
        vo.setActiveTimeDistribution(convertToStatItems(statsByType.get("active_time")));
        vo.setDeviceDistribution(convertToStatItems(statsByType.get("device")));

        // 同步时间
        if (!allStats.isEmpty() && allStats.get(0).getSyncTime() != null) {
            vo.setSyncTime(allStats.get(0).getSyncTime().getTime());
        }

        return vo;
    }

    @Override
    public List<DyFanProfileStats> getStats(Long accountId, String statType, Long userId) {
        // 验证账号权限
        verifyAccountAccess(accountId, userId);

        if (statType == null || statType.isBlank()) {
            return statsRepository.findByAccountIdAndDeletedOrderByCountDesc(accountId, 0);
        } else {
            return statsRepository.findByAccountIdAndStatTypeAndDeletedOrderByCountDesc(accountId, statType, 0);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void manualSync(Long accountId, Long userId) {
        // 验证账号权限
        verifyAccountAccess(accountId, userId);

        syncFanProfile(accountId);
    }

    // ─── 私有方法 ──────────────────────────────────────

    private JSONObject fetchFanDataFromDouyin(DouyinAccount account) {
        try {
            // 模拟调用抖音 API（实际需要根据抖音开放平台文档实现）
            // String url = douyinApiBaseUrl + "/fans/data?open_id=" + account.getOpenId();

            // 这里返回模拟数据
            return generateMockFanData();
        } catch (Exception e) {
            log.error("调用抖音 API 失败", e);
            return null;
        }
    }

    private void saveFanStats(Long accountId, JSONObject fanData, Timestamp syncTime) {
        // 查找账号所有者
        Long ownerId = accountRepository.findById(accountId)
                .map(DouyinAccount::getUserId).orElse(null);

        // 删除旧数据
        statsRepository.deleteOldStatsByAccountId(accountId);

        List<DyFanProfileStats> statsList = new ArrayList<>();

        // 年龄分布
        if (fanData.containsKey("age_distribution")) {
            JSONArray ageData = fanData.getJSONArray("age_distribution");
            statsList.addAll(parseStats(accountId, ownerId, "age", ageData, syncTime));
        }

        // 性别分布
        if (fanData.containsKey("gender_distribution")) {
            JSONArray genderData = fanData.getJSONArray("gender_distribution");
            statsList.addAll(parseStats(accountId, ownerId, "gender", genderData, syncTime));
        }

        // 地域分布
        if (fanData.containsKey("province_distribution")) {
            JSONArray provinceData = fanData.getJSONArray("province_distribution");
            statsList.addAll(parseStats(accountId, ownerId, "province", provinceData, syncTime));
        }

        if (fanData.containsKey("city_distribution")) {
            JSONArray cityData = fanData.getJSONArray("city_distribution");
            statsList.addAll(parseStats(accountId, ownerId, "city", cityData, syncTime));
        }

        // 兴趣标签
        if (fanData.containsKey("interest_tags")) {
            JSONArray interestData = fanData.getJSONArray("interest_tags");
            statsList.addAll(parseStats(accountId, ownerId, "interest", interestData, syncTime));
        }

        // 活跃时段
        if (fanData.containsKey("active_time")) {
            JSONArray activeTimeData = fanData.getJSONArray("active_time");
            statsList.addAll(parseStats(accountId, ownerId, "active_time", activeTimeData, syncTime));
        }

        // 设备类型
        if (fanData.containsKey("device_distribution")) {
            JSONArray deviceData = fanData.getJSONArray("device_distribution");
            statsList.addAll(parseStats(accountId, ownerId, "device", deviceData, syncTime));
        }

        // 批量保存
        if (!statsList.isEmpty()) {
            statsRepository.saveAll(statsList);
        }
    }

    private List<DyFanProfileStats> parseStats(Long accountId, Long ownerId, String statType, JSONArray data, Timestamp syncTime) {
        List<DyFanProfileStats> statsList = new ArrayList<>();

        if (data == null || data.isEmpty()) {
            return statsList;
        }

        for (int i = 0; i < data.size(); i++) {
            JSONObject item = data.getJSONObject(i);

            DyFanProfileStats stats = new DyFanProfileStats();
            stats.setAccountId(accountId);
            stats.setOwnerId(ownerId);
            stats.setStatType(statType);
            stats.setStatKey(item.getString("key"));
            stats.setStatValue(item.getString("value"));
            stats.setCount(item.getLong("count"));

            if (item.containsKey("percentage")) {
                stats.setPercentage(BigDecimal.valueOf(item.getDoubleValue("percentage")));
            }

            stats.setSyncTime(syncTime);
            statsList.add(stats);
        }

        return statsList;
    }

    private List<FanProfileVO.StatItem> convertToStatItems(List<DyFanProfileStats> stats) {
        if (stats == null || stats.isEmpty()) {
            return new ArrayList<>();
        }

        return stats.stream()
                .limit(10) // 最多返回 10 条
                .map(s -> {
                    FanProfileVO.StatItem item = new FanProfileVO.StatItem();
                    item.setKey(s.getStatKey());
                    item.setValue(s.getStatValue());
                    item.setCount(s.getCount());
                    if (s.getPercentage() != null) {
                        item.setPercentage(s.getPercentage().doubleValue());
                    }
                    return item;
                })
                .collect(Collectors.toList());
    }

    private DouyinAccount verifyAccountAccess(Long accountId, Long userId) {
        DouyinAccount account = accountRepository.findById(accountId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "账号不存在"));

        if (!account.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权限访问此账号");
        }

        return account;
    }

    // 生成模拟数据（用于测试）
    private JSONObject generateMockFanData() {
        JSONObject data = new JSONObject();

        // 年龄分布
        JSONArray ageData = new JSONArray();
        ageData.add(createStatItem("18-24", "18-24岁", 3500L, 35.0));
        ageData.add(createStatItem("25-30", "25-30岁", 2800L, 28.0));
        ageData.add(createStatItem("31-40", "31-40岁", 2200L, 22.0));
        ageData.add(createStatItem("41-50", "41-50岁", 1000L, 10.0));
        ageData.add(createStatItem("50+", "50岁以上", 500L, 5.0));
        data.put("age_distribution", ageData);

        // 性别分布
        JSONArray genderData = new JSONArray();
        genderData.add(createStatItem("female", "女性", 6000L, 60.0));
        genderData.add(createStatItem("male", "男性", 3500L, 35.0));
        genderData.add(createStatItem("unknown", "未知", 500L, 5.0));
        data.put("gender_distribution", genderData);

        // 省份分布
        JSONArray provinceData = new JSONArray();
        provinceData.add(createStatItem("guangdong", "广东", 2000L, 20.0));
        provinceData.add(createStatItem("beijing", "北京", 1500L, 15.0));
        provinceData.add(createStatItem("shanghai", "上海", 1200L, 12.0));
        provinceData.add(createStatItem("zhejiang", "浙江", 1000L, 10.0));
        provinceData.add(createStatItem("jiangsu", "江苏", 800L, 8.0));
        data.put("province_distribution", provinceData);

        // 兴趣标签
        JSONArray interestData = new JSONArray();
        interestData.add(createStatItem("beauty", "美妆", 3000L, 30.0));
        interestData.add(createStatItem("fashion", "时尚", 2500L, 25.0));
        interestData.add(createStatItem("food", "美食", 2000L, 20.0));
        interestData.add(createStatItem("travel", "旅游", 1500L, 15.0));
        interestData.add(createStatItem("fitness", "健身", 1000L, 10.0));
        data.put("interest_tags", interestData);

        // 活跃时段
        JSONArray activeTimeData = new JSONArray();
        activeTimeData.add(createStatItem("morning", "早上(6-12点)", 2000L, 20.0));
        activeTimeData.add(createStatItem("afternoon", "下午(12-18点)", 3000L, 30.0));
        activeTimeData.add(createStatItem("evening", "晚上(18-24点)", 4000L, 40.0));
        activeTimeData.add(createStatItem("night", "深夜(0-6点)", 1000L, 10.0));
        data.put("active_time", activeTimeData);

        // 设备类型
        JSONArray deviceData = new JSONArray();
        deviceData.add(createStatItem("ios", "iOS", 5500L, 55.0));
        deviceData.add(createStatItem("android", "Android", 4500L, 45.0));
        data.put("device_distribution", deviceData);

        return data;
    }

    private JSONObject createStatItem(String key, String value, Long count, Double percentage) {
        JSONObject item = new JSONObject();
        item.put("key", key);
        item.put("value", value);
        item.put("count", count);
        item.put("percentage", percentage);
        return item;
    }
}
