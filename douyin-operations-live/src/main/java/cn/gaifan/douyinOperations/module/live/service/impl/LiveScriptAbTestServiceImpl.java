package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.live.entity.LiveScriptAbTest;
import cn.gaifan.douyinOperations.module.live.repository.LiveScriptAbTestRepository;
import cn.gaifan.douyinOperations.module.live.service.LiveScriptAbTestService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LiveScriptAbTestServiceImpl implements LiveScriptAbTestService {

    private final LiveScriptAbTestRepository repository;
    /** session 级变体缓存：key=testId:sessionId, value=A|B */
    private final Map<String, String> sessionVariantCache = new ConcurrentHashMap<>();

    public LiveScriptAbTestServiceImpl(LiveScriptAbTestRepository repository) {
        this.repository = repository;
    }

    @Override
    public String getVariantForSession(Long testId, Long sessionId) {
        if (testId == null || sessionId == null) return "A";
        String key = testId + ":" + sessionId;
        return sessionVariantCache.computeIfAbsent(key, k -> {
            LiveScriptAbTest t = repository.findById(testId).orElse(null);
            if (t == null) return "A";
            int split = t.getTrafficSplit() != null ? t.getTrafficSplit() : 50;
            return Math.random() < split / 100.0 ? "A" : "B";
        });
    }

    @Override
    @Transactional
    public void calculateSignificance(Long testId) {
        LiveScriptAbTest t = repository.findById(testId).orElse(null);
        if (t == null) return;
        double pA = t.getaConversionRate() != null ? t.getaConversionRate() / 100.0 : 0;
        double pB = t.getbConversionRate() != null ? t.getbConversionRate() / 100.0 : 0;
        int nA = t.getaImpressions() != null ? t.getaImpressions() : 0;
        int nB = t.getbImpressions() != null ? t.getbImpressions() : 0;
        if (nA < 30 || nB < 30) return;

        double pPool = (pA * nA + pB * nB) / (nA + nB);
        if (pPool <= 0 || pPool >= 1) return;
        double se = Math.sqrt(pPool * (1 - pPool) * (1.0 / nA + 1.0 / nB));
        if (se <= 0) return;
        double z = (pA - pB) / se;
        double pValue = 2 * (1 - normalCDF(Math.abs(z)));

        t.setpValue(pValue);
        t.setConfidenceLevel(1 - pValue);
        if (pValue < 0.05 && Math.min(nA, nB) >= 100) {
            t.setWinner(pA > pB ? "A" : "B");
            t.setStatus("completed");
            t.setEndTime(LocalDateTime.now());
        }
        repository.save(t);
    }

    private static double normalCDF(double z) {
        double a1 = 0.254829592, a2 = -0.284496736, a3 = 1.421413741, a4 = -1.453152027, a5 = 1.061405429;
        double t = 1.0 / (1.0 + 0.2316419 * Math.abs(z));
        double d = 1.0 - 1.0 / Math.sqrt(2 * Math.PI) * Math.exp(-z * z / 2) * (a1 * t + a2 * t * t + a3 * t * t * t + a4 * t * t * t * t + a5 * t * t * t * t * t);
        return z < 0 ? 1 - d : d;
    }

    @Override
    @Transactional
    public LiveScriptAbTest createTest(Long scriptId, Long versionAId, Long versionBId, int trafficSplit, Long ownerId) {
        LiveScriptAbTest t = new LiveScriptAbTest();
        t.setOwnerId(ownerId);
        t.setScriptId(scriptId);
        t.setVersionAId(versionAId);
        t.setVersionBId(versionBId);
        t.setTrafficSplit(Math.max(1, Math.min(99, trafficSplit)));
        return repository.save(t);
    }

    @Override
    @Transactional
    public void recordImpression(Long testId, String variant) {
        LiveScriptAbTest t = repository.findById(testId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "测试不存在"));
        if ("A".equalsIgnoreCase(variant)) {
            t.setaImpressions((t.getaImpressions() != null ? t.getaImpressions() : 0) + 1);
        } else if ("B".equalsIgnoreCase(variant)) {
            t.setbImpressions((t.getbImpressions() != null ? t.getbImpressions() : 0) + 1);
        }
        repository.save(t);
    }

    @Override
    @Transactional
    public void updateMetrics(Long testId, String variant, double conversionRate, double retentionRate, double interactionRate) {
        LiveScriptAbTest t = repository.findById(testId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "测试不存在"));
        if ("A".equalsIgnoreCase(variant)) {
            t.setaConversionRate(conversionRate);
            t.setaRetentionRate(retentionRate);
            t.setaInteractionRate(interactionRate);
        } else if ("B".equalsIgnoreCase(variant)) {
            t.setbConversionRate(conversionRate);
            t.setbRetentionRate(retentionRate);
            t.setbInteractionRate(interactionRate);
        }
        repository.save(t);
    }

    @Override
    public PageResultVO<LiveScriptAbTest> listTests(Long ownerId, int page, int rows) {
        Page<LiveScriptAbTest> p = repository.findByOwnerIdAndDeletedOrderByCreateTimeDesc(ownerId, 0, PageRequest.of(page, Math.min(100, Math.max(1, rows))));
        return new PageResultVO<>(p.getTotalElements(), p.getContent(), page + 1, p.getSize());
    }
}
