package cn.gaifan.douyinOperations.module.product.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.contract.auth.DataScopeResolver;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.product.entity.DyProduct;
import cn.gaifan.douyinOperations.module.product.entity.DyProductSalesHistory;
import cn.gaifan.douyinOperations.module.product.repository.DyProductRepository;
import cn.gaifan.douyinOperations.module.product.repository.DyProductSalesHistoryRepository;
import cn.gaifan.douyinOperations.module.product.service.ProductService;
import cn.gaifan.douyinOperations.module.product.vo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SalesHistoryServiceImpl {

    private static final Logger log = LoggerFactory.getLogger(SalesHistoryServiceImpl.class);
    private static final Set<String> SORTABLE = Set.of("id", "productId", "saleAmount", "saleQuantity", "saleTime", "createTime");

    @Resource
    private DyProductSalesHistoryRepository salesHistoryRepository;
    @Resource
    private DyProductRepository dyProductRepository;
    @Resource
    private ProductService productService;
    @Resource
    private DataScopeResolver dataScopeService;

    /** 销售写入后是否自动扣减库存，默认 false（可选开启） */
    @Value("${product.sales.auto-deduct-inventory:false}")
    private boolean autoDeductInventory;

    /**
     * 分页搜索销售历史（含 DataScope 校验）
     */
    public PageResultVO<SalesHistoryVO> search(SalesHistorySearchVO vo, Long userId, String roleCode) {
        vo.validateParams();
        String sortName = SORTABLE.contains(vo.getSortName()) ? vo.getSortName() : "saleTime";
        Pageable pageable = PageRequest.of(vo.getPage(), vo.getRows(),
                Sort.by("desc".equalsIgnoreCase(vo.getSortOrder()) ? Sort.Direction.DESC : Sort.Direction.ASC, sortName));

        Specification<DyProductSalesHistory> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("deleted"), 0));

            // DataScope：仅返回可见用户商品的销售记录
            if (userId != null && roleCode != null) {
                List<Long> visibleUserIds = dataScopeService.getVisibleUserIds(userId, roleCode);
                if (visibleUserIds != null && !visibleUserIds.isEmpty()) {
                    Subquery<Long> sub = query.subquery(Long.class);
                    var prodRoot = sub.from(DyProduct.class);
                    sub.select(prodRoot.get("id"));
                    sub.where(prodRoot.get("userId").in(visibleUserIds), cb.equal(prodRoot.get("deleted"), 0));
                    predicates.add(root.get("productId").in(sub));
                }
            }

            if (vo.getProductId() != null) {
                predicates.add(cb.equal(root.get("productId"), vo.getProductId()));
                // productId 指定时额外校验归属
                if (userId != null && roleCode != null) {
                    checkProductInScope(vo.getProductId(), userId, roleCode);
                }
            }
            if (vo.getChannelSource() != null && !vo.getChannelSource().isBlank()) {
                predicates.add(cb.equal(root.get("channelSource"), vo.getChannelSource().trim()));
            }
            if (vo.getSessionId() != null && !vo.getSessionId().isBlank()) {
                predicates.add(cb.equal(root.get("sessionId"), vo.getSessionId().trim()));
            }
            if (vo.getStartTime() != null && !vo.getStartTime().isBlank()) {
                try {
                    Timestamp ts = parseTime(vo.getStartTime().trim(), true);
                    if (ts != null) predicates.add(cb.greaterThanOrEqualTo(root.get("saleTime"), ts));
                } catch (Exception ignored) { /* 解析失败忽略 */ }
            }
            if (vo.getEndTime() != null && !vo.getEndTime().isBlank()) {
                try {
                    Timestamp ts = parseTime(vo.getEndTime().trim(), false);
                    if (ts != null) predicates.add(cb.lessThanOrEqualTo(root.get("saleTime"), ts));
                } catch (Exception ignored) { /* 解析失败忽略 */ }
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<DyProductSalesHistory> page = salesHistoryRepository.findAll(spec, pageable);
        return PageResultVO.of(page.getTotalElements(),
                page.getContent().stream().map(this::toVO).collect(Collectors.toList()),
                vo.getPage(), vo.getRows());
    }

    /**
     * 获取销售记录详情（含 DataScope 校验）
     */
    public SalesHistoryVO getById(Long id, Long userId, String roleCode) {
        if (id == null || id <= 0) throw new BusinessException(ErrorCode.VALIDATION_FAIL, "销售记录 ID 无效");
        DyProductSalesHistory entity = salesHistoryRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "销售记录不存在"));
        if (userId != null && roleCode != null) {
            checkProductInScope(entity.getProductId(), userId, roleCode);
        }
        return toVO(entity);
    }

    /**
     * 新增/更新销售记录（含 DataScope 校验，可选自动扣减库存）
     */
    @Transactional(rollbackFor = Exception.class)
    public long save(SalesHistorySaveVO vo, Long userId, String roleCode) {
        if (userId != null && roleCode != null) {
            checkProductInScope(vo.getProductId(), userId, roleCode);
        }
        return doSave(vo);
    }

    /**
     * 内部保存（系统调用，如 live 场次结束回写），不做 DataScope 校验
     */
    @Transactional(rollbackFor = Exception.class)
    public long save(SalesHistorySaveVO vo) {
        return doSave(vo);
    }

    @Transactional(rollbackFor = Exception.class)
    private long doSave(SalesHistorySaveVO vo) {
        DyProductSalesHistory entity;
        if (vo.getId() != null && vo.getId() > 0) {
            entity = salesHistoryRepository.findByIdAndDeleted(vo.getId(), 0)
                    .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "销售记录不存在"));
        } else {
            entity = new DyProductSalesHistory();
            entity.setProductId(vo.getProductId());
        }
        entity.setSaleAmount(vo.getSaleAmount());
        if (vo.getSaleQuantity() != null) entity.setSaleQuantity(vo.getSaleQuantity());
        if (vo.getSaleTime() != null && !vo.getSaleTime().isBlank()) {
            Timestamp ts = parseTime(vo.getSaleTime().trim(), true);
            if (ts != null) entity.setSaleTime(ts);
        }
        if (vo.getChannelSource() != null) entity.setChannelSource(vo.getChannelSource());
        if (vo.getSessionId() != null) entity.setSessionId(vo.getSessionId());
        long savedId = salesHistoryRepository.save(entity).getId();

        // 可选：销售写入后自动扣减库存（通过 ProductService 使用乐观锁）
        if (autoDeductInventory && vo.getSaleQuantity() != null && vo.getSaleQuantity() > 0) {
            long delta = -vo.getSaleQuantity();
            try {
                productService.updateInventory(vo.getProductId(), delta);
            } catch (Exception e) {
                log.warn("销售写入后自动扣减库存失败 productId={}, delta={}", vo.getProductId(), delta, e);
            }
        }
        return savedId;
    }

    private void checkProductInScope(Long productId, Long userId, String roleCode) {
        if (productId == null) return;
        DyProduct product = dyProductRepository.findById(productId).orElse(null);
        if (product == null) return;
        List<Long> visible = dataScopeService.getVisibleUserIds(userId, roleCode);
        if (visible != null && !visible.contains(product.getUserId())) {
            throw new BusinessException(ErrorCode.PRODUCT_FORBIDDEN, "无权访问该商品的销售数据");
        }
    }

    private static Timestamp parseTime(String s, boolean startOfDay) {
        if (s == null || s.isBlank()) return null;
        try {
            s = s.trim().replace("T", " ");
            if (s.length() <= 10) {
                s = startOfDay ? s + " 00:00:00" : s + " 23:59:59";
            } else if (s.length() == 16) {
                s = s + ":00";
            }
            return Timestamp.valueOf(LocalDateTime.parse(s.substring(0, 19),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        } catch (Exception e) {
            return null;
        }
    }

    public BigDecimal totalSalesAmount(Long productId, Long userId, String roleCode) {
        if (userId != null && roleCode != null) {
            checkProductInScope(productId, userId, roleCode);
        }
        return salesHistoryRepository.sumSaleAmountByProductId(productId);
    }

    public Long totalSalesQuantity(Long productId, Long userId, String roleCode) {
        if (userId != null && roleCode != null) {
            checkProductInScope(productId, userId, roleCode);
        }
        return salesHistoryRepository.sumSaleQuantityByProductId(productId);
    }

    private SalesHistoryVO toVO(DyProductSalesHistory e) {
        SalesHistoryVO vo = new SalesHistoryVO();
        vo.setId(e.getId());
        vo.setProductId(e.getProductId());
        vo.setSaleQuantity(e.getSaleQuantity());
        vo.setSaleAmount(e.getSaleAmount());
        vo.setSaleTime(e.getSaleTime());
        vo.setChannelSource(e.getChannelSource());
        vo.setSessionId(e.getSessionId());
        vo.setCreateTime(e.getCreateTime());
        return vo;
    }
}
