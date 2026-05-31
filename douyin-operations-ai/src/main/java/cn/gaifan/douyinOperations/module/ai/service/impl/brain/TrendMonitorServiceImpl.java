package cn.gaifan.douyinOperations.module.ai.service.impl.brain;

import cn.gaifan.douyinOperations.common.config.BusinessParamConfig;
import cn.gaifan.douyinOperations.module.ai.service.brain.HostPersonaService;
import cn.gaifan.douyinOperations.module.ai.service.brain.TrendMonitorService;
import cn.gaifan.douyinOperations.module.ai.vo.HotspotWindow;
import cn.gaifan.douyinOperations.module.ai.vo.TrendLifecycle;
import cn.gaifan.douyinOperations.module.ai.vo.TrendPredictionVO;
import cn.gaifan.douyinOperations.module.tianapi.service.TianApiService;
import cn.gaifan.douyinOperations.module.tianapi.vo.HotItemVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 实时趋势感知服务实现（Phase1）
 * 对接 TianAPI 热搜（抖音/微博/头条/全网等），定时轮询；抖音热门视频模块暂搁置
 */
@Service
public class TrendMonitorServiceImpl implements TrendMonitorService {

    private static final Logger log = LoggerFactory.getLogger(TrendMonitorServiceImpl.class);

    @Value("${app.ai.brain.trend-monitor.enabled:true}")
    private boolean enabled;

    @Value("${app.ai.brain.trend-monitor.interval-sec:60}")
    private int intervalSec;

    @Autowired(required = false)
    private TianApiService tianApiService;

    @Autowired(required = false)
    private HostPersonaService hostPersonaService;

    @Autowired(required = false)
    private BusinessParamConfig businessParamConfig;

    private final List<TrendSignal> cachedTrends = Collections.synchronizedList(new ArrayList<>());
    private volatile long lastFetchAt = 0;

    /** 热度历史：key=category_title, value=[(timestamp, heat), ...] 用于计算 momentum */
    private final Map<String, LinkedList<HeatPoint>> heatHistory = new ConcurrentHashMap<>();

    private record HeatPoint(long timestamp, double heat) {}

    @Override
    public List<TrendSignal> getCurrentTrends(String category, int limit) {
        if (!enabled) return List.of();
        ensureCached();
        List<TrendSignal> list = new ArrayList<>(cachedTrends);
        if (category != null && !category.isBlank()) {
            list = list.stream().filter(t -> category.equals(t.category())).toList();
        }
        return list.size() > limit ? list.subList(0, limit) : list;
    }

    @Override
    public List<TrendPredictionVO> getTrendsWithLifecycle(String category, int limit) {
        List<TrendSignal> list = getCurrentTrends(category, limit);
        List<TrendPredictionVO> result = new ArrayList<>();
        for (TrendSignal s : list) {
            TrendLifecycle lc = computeLifecycle(s);
            HotspotWindow win = computeWindow(lc);
            result.add(new TrendPredictionVO(s, lc, win));
        }
        result.sort((a, b) -> {
            int g = windowPriority(b.getWindow()) - windowPriority(a.getWindow());
            if (g != 0) return g;
            return Double.compare(b.getLifecycle().getMomentum(), a.getLifecycle().getMomentum());
        });
        return result.size() > limit ? result.subList(0, limit) : result;
    }

    private int windowPriority(HotspotWindow w) {
        if (w == null) return 0;
        return switch (w.getWindowType() != null ? w.getWindowType() : "") {
            case "golden" -> 4;
            case "silver" -> 3;
            case "bronze" -> 2;
            default -> 1;
        };
    }

    private TrendLifecycle computeLifecycle(TrendSignal s) {
        String key = (s.category() != null ? s.category() : "") + "_" + (s.title() != null ? s.title() : s.id());
        double currentHeat = s.heatScore();
        double momentum = 0.2;
        int shortWindowHours = getShortWindowHours();
        long shortWindowMs = shortWindowHours * 60L * 60 * 1000;
        LinkedList<HeatPoint> hist = heatHistory.get(key);
        if (hist != null && hist.size() >= 2) {
            HeatPoint now = hist.getLast();
            long cutoff = now.timestamp() - shortWindowMs;
            HeatPoint past = null;
            for (HeatPoint p : hist) {
                if (p.timestamp() <= cutoff) past = p;
            }
            if (past != null && past.heat() > 0) {
                momentum = (currentHeat - past.heat()) / past.heat();
            }
        } else {
            int rank = parseRank(s.id());
            if (rank <= 3) momentum = 0.5;
            else if (rank <= 10) momentum = 0.2;
            else if (rank <= 20) momentum = 0.05;
        }
        String phase = "rising";
        if (momentum > 0.5) phase = "emerging";
        else if (momentum > 0.1) phase = "rising";
        else if (momentum >= -0.1) phase = "peak";
        else if (momentum >= -0.5) phase = "declining";
        else phase = "dead";
        if (currentHeat < 0.1) phase = "dead";
        int estimatedPeakHours = phase.equals("emerging") ? 6 : phase.equals("rising") ? 12 : phase.equals("peak") ? 0 : -1;
        double predictedPeak = phase.equals("emerging") || phase.equals("rising") ? currentHeat * 1.3 : currentHeat;
        return new TrendLifecycle(phase, momentum, estimatedPeakHours, currentHeat, predictedPeak);
    }

    private int parseRank(String id) {
        if (id == null) return 99;
        try {
            String num = id.replaceAll("\\D", "");
            return num.isEmpty() ? 99 : Integer.parseInt(num);
        } catch (Exception e) {
            return 99;
        }
    }

    private HotspotWindow computeWindow(TrendLifecycle lc) {
        if (lc == null) return new HotspotWindow("expired", 0, "热点已过，不建议追");
        String phase = lc.getPhase();
        int remaining = lc.getEstimatedPeakHours() >= 0 ? lc.getEstimatedPeakHours() : 0;
        int goldenHours = getGoldenWindowHours();
        int silverHours = getSilverWindowHours();
        if ("emerging".equals(phase) || "rising".equals(phase)) {
            if (remaining <= goldenHours) return new HotspotWindow("golden", remaining, "黄金窗口，建议立即创作");
            if (remaining <= silverHours) return new HotspotWindow("silver", remaining, "白银窗口，仍有较大流量");
            return new HotspotWindow("bronze", remaining, "青铜窗口，需差异化角度");
        }
        if ("peak".equals(phase)) return new HotspotWindow("silver", 12, "峰值附近，抓紧创作");
        return new HotspotWindow("expired", 0, "热点已过，不建议追");
    }

    @Override
    public List<TrendSignal> getTrendsForHost(String hostCode, int limit) {
        List<TrendSignal> all = getCurrentTrends(null, limit * 2);
        if (hostCode == null || hostPersonaService == null) {
            return all.size() > limit ? all.subList(0, limit) : all;
        }
        List<String> priorities = hostPersonaService.getAiPriorities(hostCode);
        if (priorities.isEmpty()) return all.size() > limit ? all.subList(0, limit) : all;
        return all.stream()
                .sorted((a, b) -> {
                    double scoreA = hostRelevanceScore(a, priorities);
                    double scoreB = hostRelevanceScore(b, priorities);
                    return Double.compare(scoreB, scoreA);
                })
                .limit(limit)
                .toList();
    }

    private double hostRelevanceScore(TrendSignal t, List<String> priorities) {
        double base = t.heatScore();
        String title = t.title() != null ? t.title().toLowerCase() : "";
        for (int i = 0; i < priorities.size(); i++) {
            String p = priorities.get(i).toLowerCase();
            if (title.contains(p) || p.contains("热点") || p.contains("趋势")) base += (priorities.size() - i) * 0.1;
        }
        return base;
    }

    @Override
    public List<TrendSignal> detectNewTrends() {
        if (!enabled) return List.of();
        ensureCached();
        return new ArrayList<>(cachedTrends);
    }

    @Override
    @Scheduled(fixedDelay = 60000)
    public void monitorAndPersist() {
        if (!enabled) return;
        try {
            fetchAndCache();
        } catch (Exception e) {
            log.warn("[TrendMonitor] monitorAndPersist failed: {}", e.getMessage());
        }
    }

    @Override
    public boolean isAvailable() {
        return enabled;
    }

    private void ensureCached() {
        if (System.currentTimeMillis() - lastFetchAt > intervalSec * 1000L) {
            fetchAndCache();
        }
    }

    private void fetchAndCache() {
        cachedTrends.clear();
        if (tianApiService == null) {
            lastFetchAt = System.currentTimeMillis();
            log.debug("[TrendMonitor] TianAPI 服务未注入，跳过拉取");
            return;
        }
        // 抖音热搜优先用鬼鬼鸭（免 key），TianAPI 兜底；微博/全网仍依赖 TianAPI
        try {
            List<HotItemVO> douyin = safeCall(() -> tianApiService.douyinHot());
            List<HotItemVO> weibo = safeCall(() -> tianApiService.weiboHot());
            List<HotItemVO> network = safeCall(() -> tianApiService.networkHot());

            long now = System.currentTimeMillis();
            int rank = 0;
            for (HotItemVO h : douyin) {
                double heat = h.getHotIndex() != null ? h.getHotIndex().doubleValue() / 10000.0 : 0.5;
                String key = "douyin_" + (h.getWord() != null ? h.getWord() : ("dy_" + rank));
                heatHistory.computeIfAbsent(key, k -> new LinkedList<>()).add(new HeatPoint(now, heat));
                pruneHistory(key);
                cachedTrends.add(new TrendSignal(
                        "dy_" + (++rank),
                        h.getWord(),
                        "douyin",
                        heat,
                        now,
                        "tianapi",
                        (h.getLabel() != null ? "[" + h.getLabel() + "] " : "") + "抖音热搜"
                ));
            }
            rank = 0;
            for (HotItemVO h : weibo) {
                double heat = h.getHotIndex() != null ? h.getHotIndex().doubleValue() / 10000.0 : 0.5;
                String key = "weibo_" + (h.getWord() != null ? h.getWord() : ("wb_" + rank));
                heatHistory.computeIfAbsent(key, k -> new LinkedList<>()).add(new HeatPoint(now, heat));
                pruneHistory(key);
                cachedTrends.add(new TrendSignal(
                        "wb_" + (++rank),
                        h.getWord(),
                        "weibo",
                        heat,
                        now,
                        "tianapi",
                        "微博热搜"
                ));
            }
            rank = 0;
            for (HotItemVO h : network) {
                double heat = h.getHotIndex() != null ? h.getHotIndex().doubleValue() / 10000.0 : 0.5;
                String key = "network_" + (h.getWord() != null ? h.getWord() : ("net_" + rank));
                heatHistory.computeIfAbsent(key, k -> new LinkedList<>()).add(new HeatPoint(now, heat));
                pruneHistory(key);
                cachedTrends.add(new TrendSignal(
                        "net_" + (++rank),
                        h.getWord(),
                        "network",
                        heat,
                        now,
                        "tianapi",
                        "全网热搜"
                ));
            }
            lastFetchAt = now;
            log.info("[TrendMonitor] 趋势拉取完成: 抖音{} 微博{} 全网{} 条", douyin.size(), weibo.size(), network.size());
        } catch (Exception e) {
            log.warn("[TrendMonitor] fetchAndCache failed: {}", e.getMessage());
            lastFetchAt = System.currentTimeMillis();
        }
    }

    private int getShortWindowHours() {
        return businessParamConfig != null && businessParamConfig.getTrendMonitor() != null
                ? businessParamConfig.getTrendMonitor().getShortWindowHours() : 6;
    }

    private int getLongWindowHours() {
        return businessParamConfig != null && businessParamConfig.getTrendMonitor() != null
                ? businessParamConfig.getTrendMonitor().getLongWindowHours() : 24;
    }

    private int getGoldenWindowHours() {
        return businessParamConfig != null && businessParamConfig.getTrendMonitor() != null
                ? businessParamConfig.getTrendMonitor().getGoldenWindowHours() : 6;
    }

    private int getSilverWindowHours() {
        return businessParamConfig != null && businessParamConfig.getTrendMonitor() != null
                ? businessParamConfig.getTrendMonitor().getSilverWindowHours() : 24;
    }

    private void pruneHistory(String key) {
        LinkedList<HeatPoint> list = heatHistory.get(key);
        if (list == null) return;
        long longWindowMs = getLongWindowHours() * 60L * 60 * 1000;
        long cutoff = System.currentTimeMillis() - longWindowMs;
        while (list.size() > 1 && list.getFirst().timestamp() < cutoff) {
            list.removeFirst();
        }
        while (list.size() > 48) list.removeFirst();
    }

    private List<HotItemVO> safeCall(java.util.function.Supplier<List<HotItemVO>> supplier) {
        try {
            List<HotItemVO> r = supplier.get();
            return r != null ? r : List.of();
        } catch (Exception e) {
            log.debug("[TrendMonitor] 单源拉取失败: {}", e.getMessage());
            return List.of();
        }
    }
}
