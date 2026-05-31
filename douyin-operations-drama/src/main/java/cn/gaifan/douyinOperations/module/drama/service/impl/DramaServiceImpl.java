package cn.gaifan.douyinOperations.module.drama.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.drama.entity.DramaProject;
import cn.gaifan.douyinOperations.module.drama.repository.DramaProjectRepository;
import cn.gaifan.douyinOperations.module.drama.service.DramaService;
import cn.gaifan.douyinOperations.module.drama.vo.*;
import cn.gaifan.douyinOperations.contract.product.FeatureCode;
import cn.gaifan.douyinOperations.contract.product.ProductCode;
import cn.gaifan.douyinOperations.contract.product.ProductIntegrationInvocationRequest;
import cn.gaifan.douyinOperations.contract.product.ProductIntegrationInvocationResult;
import cn.gaifan.douyinOperations.module.platform.credit.CommercialProductChargeService;
import cn.gaifan.douyinOperations.module.platform.product.DeliveryLedgerService;
import cn.gaifan.douyinOperations.module.platform.product.ProductIntegrationService;
import cn.gaifan.douyinOperations.module.platform.product.DeliveryProduct;
import cn.gaifan.douyinOperations.common.config.RequestIdentityHolder;
import cn.gaifan.douyinOperations.module.platform.identity.CommercialIdentityBridge;
import cn.gaifan.douyinOperations.contract.identity.IdentityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import jakarta.annotation.Resource;
import jakarta.persistence.criteria.Predicate;
import java.sql.Timestamp;
import java.util.*;

@Service("appDramaServiceImpl")
public class DramaServiceImpl implements DramaService {

    private static final Set<String> SORTABLE = Set.of("id", "title", "status", "genre", "createTime");
    private static final List<String> STATUS_FLOW = List.of("draft", "planning", "shooting", "post", "done");
    private static final Set<String> VALID_STATUSES = new LinkedHashSet<>(STATUS_FLOW);

    @Resource
    private DramaProjectRepository repo;

    @Autowired(required = false)
    private CommercialProductChargeService commercialProductChargeService;

    @Autowired(required = false)
    private ProductIntegrationService productIntegrationService;

    @Autowired(required = false)
    private DeliveryLedgerService deliveryLedgerService;

    private static final String MAKER_EXPORT_FEATURE = FeatureCode.SHORTVIDEO_EXPORT;

    @Override
    public Map<String, Object> overview(Long userId) {
        List<DramaProject> projects = repo.findByUserIdAndDeletedOrderByCreateTimeDesc(userId, 0);
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (DramaProject p : projects) {
            byStatus.merge(p.getStatus(), 1L, Long::sum);
        }
        return Map.of(
                "productCode", "drama-ai",
                "projectCount", projects.size(),
                "byStatus", byStatus
        );
    }

    @Override
    public PageResultVO<DramaProjectVO> search(DramaSearchVO vo, Long userId) {
        vo.validateParams();
        String sortName = SORTABLE.contains(vo.getSortName()) ? vo.getSortName() : "createTime";
        Pageable pageable = PageRequest.of(vo.getPage(), vo.getRows(),
                Sort.by("desc".equalsIgnoreCase(vo.getSortOrder()) ? Sort.Direction.DESC : Sort.Direction.ASC, sortName));

        Specification<DramaProject> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("userId"), userId));
            predicates.add(cb.equal(root.get("deleted"), 0));
            if (vo.getStatus() != null && !vo.getStatus().isBlank()) {
                predicates.add(cb.equal(root.get("status"), vo.getStatus().trim()));
            }
            if (vo.getGenre() != null && !vo.getGenre().isBlank()) {
                predicates.add(cb.equal(root.get("genre"), vo.getGenre().trim()));
            }
            if (vo.getTitle() != null && !vo.getTitle().isBlank()) {
                predicates.add(cb.like(root.get("title"), "%" + vo.getTitle().trim() + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<DramaProject> page = repo.findAll(spec, pageable);
        return PageResultVO.of(page.getTotalElements(),
                page.getContent().stream().map(this::toVO).toList(),
                vo.getPage(), vo.getRows());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createProject(DramaSaveVO saveVO, Long userId) {
        if (commercialProductChargeService != null) {
            commercialProductChargeService.charge(
                    CommercialProductChargeService.CommercialProductChargeCommand.of(
                            ProductCode.DRAMA_AI,
                            FeatureCode.DRAMA_STORYBOARD,
                            "短剧项目创建 userId=" + userId,
                            DeliveryProduct.DRAMA_AI
                    ));
        }
        DramaProject project = new DramaProject();
        project.setUserId(userId);
        project.setTitle(saveVO.getTitle() != null ? saveVO.getTitle() : "未命名项目");
        project.setDescription(saveVO.getDescription());
        project.setGenre(saveVO.getGenre() != null ? saveVO.getGenre() : "other");
        project.setEpisodeCount(saveVO.getEpisodeCount() != null ? saveVO.getEpisodeCount() : 0);
        project.setStatus("draft");
        project.setVisibility("private");
        project.setCostCredits(100L);
        project.setCreateTime(new Timestamp(System.currentTimeMillis()));
        return repo.save(project).getId();
    }

    @Override
    public DramaProjectVO getDetail(Long projectId, Long userId) {
        DramaProject project = repo.findById(projectId)
                .filter(p -> p.getDeleted() == 0 && p.getUserId().equals(userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "短剧项目不存在"));
        return toVO(project);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long update(DramaUpdateVO updateVO, Long userId) {
        DramaProject project = repo.findById(updateVO.getId())
                .filter(p -> p.getDeleted() == 0 && p.getUserId().equals(userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "短剧项目不存在"));

        if (updateVO.getTitle() != null && !updateVO.getTitle().isBlank()) {
            project.setTitle(updateVO.getTitle().trim());
        }
        if (updateVO.getDescription() != null) {
            project.setDescription(updateVO.getDescription());
        }
        if (updateVO.getGenre() != null && !updateVO.getGenre().isBlank()) {
            project.setGenre(updateVO.getGenre().trim());
        }
        if (updateVO.getScript() != null) {
            boolean scriptNew = project.getScript() == null || project.getScript().isBlank();
            if (scriptNew && !updateVO.getScript().isBlank() && commercialProductChargeService != null) {
                commercialProductChargeService.charge(
                        CommercialProductChargeService.CommercialProductChargeCommand.of(
                                ProductCode.DRAMA_AI,
                                FeatureCode.DRAMA_SCRIPT,
                                "短剧剧本更新 projectId=" + project.getId(),
                                DeliveryProduct.DRAMA_AI
                        ));
            }
            project.setScript(updateVO.getScript());
        }
        if (updateVO.getEpisodeCount() != null) {
            project.setEpisodeCount(updateVO.getEpisodeCount());
        }
        return repo.save(project).getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long projectId, Long userId) {
        DramaProject project = repo.findById(projectId)
                .filter(p -> p.getDeleted() == 0 && p.getUserId().equals(userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "短剧项目不存在"));
        project.setDeleted(1);
        repo.save(project);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(Long projectId, String newStatus, Long userId) {
        if (!VALID_STATUSES.contains(newStatus)) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "无效状态: " + newStatus + "，有效值: " + String.join(", ", VALID_STATUSES));
        }

        DramaProject project = repo.findById(projectId)
                .filter(p -> p.getDeleted() == 0 && p.getUserId().equals(userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "短剧项目不存在"));

        int currentIdx = STATUS_FLOW.indexOf(project.getStatus());
        int targetIdx = STATUS_FLOW.indexOf(newStatus);
        if (targetIdx < currentIdx) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "不允许回退状态: " + project.getStatus() + " → " + newStatus);
        }

        project.setStatus(newStatus);
        repo.save(project);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> exportToShortvideoMaker(Long projectId, Long userId, String traceId) {
        DramaProject project = repo.findById(projectId)
                .filter(p -> p.getDeleted() == 0 && p.getUserId().equals(userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "短剧项目不存在"));

        IdentityContext ctx = RequestIdentityHolder.current();
        String tenantId = CommercialIdentityBridge.resolveTenantId(ctx);
        if (tenantId == null || tenantId.isBlank() || "default".equals(tenantId)) {
            tenantId = "demo-tenant";
        }
        String resolvedTrace = traceId != null && !traceId.isBlank()
                ? traceId
                : CommercialIdentityBridge.resolveTraceId(ctx);
        if (resolvedTrace == null || resolvedTrace.isBlank()) {
            resolvedTrace = "drama-export-" + projectId;
        }

        if (productIntegrationService != null) {
            ProductIntegrationInvocationResult gate = productIntegrationService.invoke(
                    new ProductIntegrationInvocationRequest(
                            tenantId,
                            String.valueOf(userId),
                            null,
                            ProductCode.DRAMA_AI,
                            FeatureCode.DRAMA_SCRIPT,
                            ProductCode.SHORTVIDEO_MAKER,
                            MAKER_EXPORT_FEATURE,
                            ctx != null && ctx.channel() != null ? ctx.channel() : "WEB",
                            BigDecimal.ONE,
                            null,
                            resolvedTrace,
                            "drama 导出成片 projectId=" + projectId,
                            false,
                            null,
                            null,
                            String.valueOf(projectId),
                            null,
                            false
                    ));
            if (!gate.allowed()) {
                if ("CREDIT_DENIED".equals(gate.status())) {
                    throw new BusinessException(ErrorCode.INSUFFICIENT_CREDITS,
                            gate.message() != null ? gate.message() : "积分不足");
                }
                throw new BusinessException(ErrorCode.FORBIDDEN,
                        gate.message() != null ? gate.message() : "产品互调被拒绝");
            }
            if (deliveryLedgerService != null) {
                deliveryLedgerService.recordShortvideoMakerDelivery(tenantId, resolvedTrace, "EXPORT_FROM_DRAMA");
            }
        }

        return Map.of(
                "productCode", ProductCode.DRAMA_AI,
                "targetProduct", ProductCode.SHORTVIDEO_MAKER,
                "projectId", projectId,
                "traceId", resolvedTrace,
                "title", project.getTitle()
        );
    }

    private DramaProjectVO toVO(DramaProject entity) {
        DramaProjectVO vo = new DramaProjectVO();
        vo.setId(entity.getId());
        vo.setUserId(entity.getUserId());
        vo.setTitle(entity.getTitle());
        vo.setDescription(entity.getDescription());
        vo.setGenre(entity.getGenre());
        vo.setStatus(entity.getStatus());
        vo.setScript(entity.getScript());
        vo.setEpisodeCount(entity.getEpisodeCount());
        vo.setVisibility(entity.getVisibility());
        vo.setCostCredits(entity.getCostCredits());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }
}
