package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.contract.asset.SvProjectBosCleanupSource;
import cn.gaifan.douyinOperations.contract.asset.SvProjectStorageCleanupRow;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvProject;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvProjectRepository;
import jakarta.annotation.Resource;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

@Service
public class SvProjectBosCleanupSourceImpl implements SvProjectBosCleanupSource {

    @Resource
    private SvProjectRepository projectRepository;

    @Override
    public List<SvProjectStorageCleanupRow> findAbandonedProjects(int daysAbandoned) {
        Instant cutoff = Instant.now().minus(daysAbandoned, ChronoUnit.DAYS);
        Specification<SvProject> spec = (root, query, cb) -> cb.and(
                root.get("status").in(Arrays.asList("draft", "processing", "failed")),
                cb.lessThan(root.get("createTime"), Timestamp.from(cutoff)),
                cb.isNotNull(root.get("ownerId")),
                cb.isNotNull(root.get("id"))
        );
        return projectRepository.findAll(spec).stream()
                .map(p -> new SvProjectStorageCleanupRow(
                        p.getOwnerId(),
                        p.getId(),
                        p.getCreateTime().toInstant()))
                .toList();
    }
}
