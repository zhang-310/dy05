package cn.gaifan.douyinOperations.module.ai.service.impl.brain;

import cn.gaifan.douyinOperations.common.config.IpBusinessConfig;
import cn.gaifan.douyinOperations.module.ai.service.brain.IpGrowthStageService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class IpGrowthStageServiceImpl implements IpGrowthStageService {

    @Resource
    private IpBusinessConfig config;

    @Override
    public String getPhenomenalStage(long followerCount) {
        IpBusinessConfig.IpGrowth g = config.getIpGrowth();
        if (followerCount < g.getPhenomenalSeedMax()) return "seed";
        if (followerCount < g.getPhenomenalBoostMax()) return "boost";
        if (followerCount < g.getPhenomenalStableMax()) return "stable";
        return "super_ip";
    }

    @Override
    public String getTopStage(int operatingMonths) {
        IpBusinessConfig.IpGrowth g = config.getIpGrowth();
        if (operatingMonths < g.getTopFoundationMonths()) return "foundation";
        if (operatingMonths < g.getTopExpansionMonths()) return "expansion";
        return "ecosystem";
    }

    @Override
    public Map<String, Object> getStageStrategy(String ipType, String stage) {
        Map<String, Object> strategy = new LinkedHashMap<>();
        strategy.put("ipType", ipType);
        strategy.put("stage", stage);

        if ("phenomenal".equals(ipType)) {
            switch (stage) {
                case "seed" -> {
                    strategy.put("label", "种子期");
                    strategy.put("followerRange", "< 10万");
                    strategy.put("contentFocus", "数量优先，日更2-3条，快速测试爆款公式");
                    strategy.put("talkingPointPriority", "涨粉话术为主，强调关注引导");
                    strategy.put("conversionHint", "暂不强推转化，以涨粉为核心KPI");
                }
                case "boost" -> {
                    strategy.put("label", "爆发期");
                    strategy.put("followerRange", "10万 - 50万");
                    strategy.put("contentFocus", "质量提升，每条精打细磨，强化爆款复刻");
                    strategy.put("talkingPointPriority", "互动话术为主，提升停留和完播");
                    strategy.put("conversionHint", "开始试水带货，小额高频测试转化率");
                }
                case "stable" -> {
                    strategy.put("label", "稳定期");
                    strategy.put("followerRange", "50万 - 100万");
                    strategy.put("contentFocus", "矩阵化运营，分流量号+内容号+带货号");
                    strategy.put("talkingPointPriority", "转化话术为主，强化逼单和价值塑造");
                    strategy.put("conversionHint", "稳定变现，客单价逐步提升，复购率是关键");
                }
                case "super_ip" -> {
                    strategy.put("label", "超级IP");
                    strategy.put("followerRange", "> 100万");
                    strategy.put("contentFocus", "品牌化运营，IP衍生+跨平台+私域沉淀");
                    strategy.put("talkingPointPriority", "品牌话术，输出价值观和生活方式");
                    strategy.put("conversionHint", "高客单+定制合作+品牌联名");
                }
            }
        } else if ("top".equals(ipType)) {
            switch (stage) {
                case "foundation" -> {
                    strategy.put("label", "根基期");
                    strategy.put("monthRange", "< 6个月");
                    strategy.put("contentFocus", "建立专业认知，输出硬核内容，积累信任");
                    strategy.put("talkingPointPriority", "专业教育话术，展示专业深度");
                    strategy.put("conversionHint", "低价引流产品，建立首次购买信任");
                }
                case "expansion" -> {
                    strategy.put("label", "扩张期");
                    strategy.put("monthRange", "6-12个月");
                    strategy.put("contentFocus", "横向扩展品类，纵向深耕服务，打造生态");
                    strategy.put("talkingPointPriority", "场景化话术，从单品到整套方案");
                    strategy.put("conversionHint", "中高客单为主，会员体系+复购激励");
                }
                case "ecosystem" -> {
                    strategy.put("label", "生态期");
                    strategy.put("monthRange", "> 12个月");
                    strategy.put("contentFocus", "平台生态构建，线上线下融合，行业标准制定");
                    strategy.put("talkingPointPriority", "行业权威话术，输出方法论和行业见解");
                    strategy.put("conversionHint", "高客单定制+B端合作+知识付费");
                }
            }
        }
        return strategy;
    }

    @Override
    public Map<String, Object> getMetricsBaseline(String ipType) {
        Map<String, Object> baseline = new LinkedHashMap<>();
        IpBusinessConfig.IpMetrics metrics = config.getIpMetrics();

        if ("phenomenal".equals(ipType)) {
            var p = metrics.getPhenomenal();
            baseline.put("dailyFollowerGrowth", p.getDailyFollowerGrowth());
            baseline.put("minViewPerVideo", p.getMinViewPerVideo());
            baseline.put("minInteractionRate", p.getMinInteractionRate());
            baseline.put("liveWatchMinutes", p.getLiveWatchMinutes());
            baseline.put("liveConversionRate", p.getLiveConversionRate());
            baseline.put("avgUnitPrice", p.getAvgUnitPrice());
        } else if ("top".equals(ipType)) {
            var t = metrics.getTop();
            baseline.put("monthlyFollowerGrowth", t.getMonthlyFollowerGrowth());
            baseline.put("minRepurchaseRate", t.getMinRepurchaseRate());
            baseline.put("avgUnitPrice", t.getAvgUnitPrice());
            baseline.put("customerLtv", t.getCustomerLtv());
            baseline.put("liveWatchMinutes", t.getLiveWatchMinutes());
            baseline.put("liveConversionRate", t.getLiveConversionRate());
        }
        return baseline;
    }
}
