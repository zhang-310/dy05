package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.repository.AiModelRepository;
import cn.gaifan.douyinOperations.module.ai.service.LlmClient;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvCompetitor;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvCompetitorSnapshot;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvCompetitorRepository;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvCompetitorSnapshotRepository;
import cn.gaifan.douyinOperations.module.shortvideo.service.CompetitorMonitorService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

/**
 * 竞品监测服务实现（Phase 5：内存→数据库持久化）
 */
@Service
public class CompetitorMonitorServiceImpl implements CompetitorMonitorService {

    private static final Logger log = LoggerFactory.getLogger(CompetitorMonitorServiceImpl.class);

    @Resource
    private LlmClient llmClient;
    @Resource
    private AiModelRepository modelRepository;
    @Resource
    private SvCompetitorRepository competitorRepository;
    @Resource
    private SvCompetitorSnapshotRepository snapshotRepository;

    @Override
    @Transactional
    public void addCompetitor(Long ownerId, String accountId, String accountName, String platform) {
        SvCompetitor entity = new SvCompetitor();
        entity.setOwnerId(ownerId);
        entity.setAccountId(accountId);
        entity.setCompetitorName(accountName != null ? accountName : accountId);
        entity.setPlatform(platform != null ? platform : "douyin");
        entity.setDeleted(0);
        competitorRepository.save(entity);
        log.info("竞品已添加: ownerId={}, name={}", ownerId, entity.getCompetitorName());
    }

    @Override
    public List<Map<String, Object>> listCompetitors(Long ownerId) {
        List<SvCompetitor> list = competitorRepository.findByOwnerIdAndDeletedOrderByCreateTimeDesc(ownerId, 0);
        List<Map<String, Object>> result = new ArrayList<>();
        for (SvCompetitor c : list) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", c.getId());
            m.put("accountId", c.getAccountId());
            m.put("accountName", c.getCompetitorName());
            m.put("platform", c.getPlatform());
            m.put("addTime", c.getCreateTime());
            result.add(m);
        }
        return result;
    }

    @Override
    @Transactional
    public void removeCompetitor(Long ownerId, Long competitorId) {
        competitorRepository.findById(competitorId)
                .filter(c -> ownerId.equals(c.getOwnerId()))
                .ifPresent(c -> {
                    c.setDeleted(1);
                    competitorRepository.save(c);
                    log.info("竞品已移除: id={}", competitorId);
                });
    }

    @Override
    @Transactional
    public Map<String, Object> analyzeCompetitor(Long ownerId, Long competitorId) {
        SvCompetitor competitor = competitorRepository.findById(competitorId)
                .filter(c -> ownerId.equals(c.getOwnerId()))
                .orElse(null);
        if (competitor == null) return Map.of("error", "竞品不存在");

        String prompt = String.format("分析竞品账号「%s」(平台: %s) 的内容策略，包括：1.选题方向 2.发布频率 3.内容风格 4.互动策略 5.可借鉴之处",
                competitor.getCompetitorName(), competitor.getPlatform());
        List<AiModel> models = modelRepository.findByStatusAndDeleted(1, 0).stream().limit(3).toList();
        if (models.isEmpty()) return Map.of("error", "无可用模型");
        LlmClient.LlmResponse resp = llmClient.chatWithFallback(models, "你是专业的短视频运营分析师", prompt);
        String analysis = resp.success() ? resp.content() : resp.errorMsg();

        SvCompetitorSnapshot snapshot = new SvCompetitorSnapshot();
        snapshot.setCompetitorId(competitorId);
        snapshot.setSnapshotDate(LocalDate.now());
        snapshot.setContentStrategySummary(analysis);
        snapshotRepository.save(snapshot);

        return Map.of("accountName", competitor.getCompetitorName(), "analysis", analysis);
    }

    @Override
    public String generateWeeklyReport(Long ownerId) {
        List<SvCompetitor> competitors = competitorRepository.findByOwnerIdAndDeletedOrderByCreateTimeDesc(ownerId, 0);
        if (competitors.isEmpty()) return "暂无监控的竞品账号";
        StringBuilder sb = new StringBuilder("请生成本周竞品分析报告，监控的竞品账号有：\n");
        for (SvCompetitor c : competitors) {
            sb.append("- ").append(c.getCompetitorName()).append("(").append(c.getPlatform()).append(")\n");
        }
        sb.append("\n请分析：1.整体趋势 2.各竞品表现 3.选题建议 4.差异化机会");
        List<AiModel> models = modelRepository.findByStatusAndDeleted(1, 0).stream().limit(3).toList();
        if (models.isEmpty()) return "无可用模型";
        LlmClient.LlmResponse resp = llmClient.chatWithFallback(models, "你是短视频行业分析专家", sb.toString());
        return resp.success() ? resp.content() : "报告生成失败: " + resp.errorMsg();
    }
}
