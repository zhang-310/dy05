package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.ai.entity.EvolutionFitnessRecord;
import cn.gaifan.douyinOperations.module.ai.repository.EvolutionFitnessRecordRepository;
import cn.gaifan.douyinOperations.module.ai.service.EvolutionFitnessService;
import cn.gaifan.douyinOperations.module.ai.service.KnowledgeBaseService;
import cn.gaifan.douyinOperations.module.ai.vo.EvolutionFitnessListVO;
import cn.gaifan.douyinOperations.module.ai.vo.EvolutionFitnessRecordVO;
import jakarta.annotation.Resource;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Service
public class EvolutionFitnessServiceImpl implements EvolutionFitnessService {

    @Resource
    private EvolutionFitnessRecordRepository repository;

    @Resource
    private KnowledgeBaseService knowledgeBaseService;

    @Value("${app.ai.evolution-fitness.record-enabled:true}")
    private boolean recordEnabled;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void record(Long kbId, String taskId, String parentTaskId, String metricName, Double metricValue, String payloadJson) {
        record(kbId, taskId, parentTaskId, metricName, metricValue, payloadJson, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void record(Long kbId, String taskId, String parentTaskId, String metricName, Double metricValue, String payloadJson, String experimentId) {
        if (!recordEnabled || taskId == null || taskId.isBlank() || metricName == null || metricName.isBlank()) {
            return;
        }
        EvolutionFitnessRecord row = new EvolutionFitnessRecord();
        row.setKbId(kbId);
        row.setTaskId(taskId.trim());
        row.setParentTaskId(parentTaskId != null ? parentTaskId.trim() : null);
        row.setMetricName(metricName.trim());
        row.setMetricValue(metricValue);
        row.setPayloadJson(payloadJson);
        row.setExperimentId(experimentId != null && !experimentId.isBlank() ? experimentId.trim() : null);
        repository.save(row);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResultVO<EvolutionFitnessRecordVO> listForKb(Long userId, Long kbId, EvolutionFitnessListVO vo) {
        knowledgeBaseService.assertKbOwnership(kbId, userId);
        final EvolutionFitnessListVO queryVo = vo != null ? vo : new EvolutionFitnessListVO();
        queryVo.validateParams();

        Specification<EvolutionFitnessRecord> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("kbId"), kbId));
            if (StringUtils.hasText(queryVo.getTaskId())) {
                predicates.add(cb.equal(root.get("taskId"), queryVo.getTaskId().trim()));
            }
            if (StringUtils.hasText(queryVo.getMetricName())) {
                predicates.add(cb.equal(root.get("metricName"), queryVo.getMetricName().trim()));
            }
            if (queryVo.getStartTimeMs() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createTime"), new Timestamp(queryVo.getStartTimeMs())));
            }
            if (queryVo.getEndTimeMs() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createTime"), new Timestamp(queryVo.getEndTimeMs())));
            }
            if (StringUtils.hasText(queryVo.getExperimentId())) {
                predicates.add(cb.equal(root.get("experimentId"), queryVo.getExperimentId().trim()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<EvolutionFitnessRecord> page = repository.findAll(spec,
                PageRequest.of(queryVo.getPage(), queryVo.getRows(), Sort.by(Sort.Direction.DESC, "createTime")));
        List<EvolutionFitnessRecordVO> list = page.getContent().stream().map(this::toVo).toList();
        return PageResultVO.of(page.getTotalElements(), list, queryVo.getPage(), queryVo.getRows());
    }

    private EvolutionFitnessRecordVO toVo(EvolutionFitnessRecord e) {
        EvolutionFitnessRecordVO v = new EvolutionFitnessRecordVO();
        v.setId(e.getId());
        v.setKbId(e.getKbId());
        v.setTaskId(e.getTaskId());
        v.setParentTaskId(e.getParentTaskId());
        v.setMetricName(e.getMetricName());
        v.setMetricValue(e.getMetricValue());
        v.setPayloadJson(e.getPayloadJson());
        v.setExperimentId(e.getExperimentId());
        v.setCreateTime(e.getCreateTime());
        return v;
    }
}
