package cn.gaifan.douyinOperations.module.product.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.product.entity.DyProduct;
import cn.gaifan.douyinOperations.module.product.repository.DyProductRepository;
import cn.gaifan.douyinOperations.module.product.service.ProductLinkExtractService;
import cn.gaifan.douyinOperations.module.product.vo.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import jakarta.persistence.criteria.Predicate;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements cn.gaifan.douyinOperations.module.product.service.ProductService {

    private static final Set<String> SORTABLE = Set.of("id", "userId", "productName", "price", "inventory", "status", "featured", "createTime");

    @Resource
    private DyProductRepository dyProductRepository;
    @Resource
    private ProductLinkExtractService productLinkExtractService;

    public PageResultVO<ProductVO> search(ProductSearchVO vo) {
        vo.validateParams();
        // 默认按推荐优先、创建时间倒序（文档 4.3.3：推荐商品在直播选品中优先展示）
        String sortName = SORTABLE.contains(vo.getSortName()) ? vo.getSortName() : "createTime";
        String sortOrder = vo.getSortOrder() != null && !vo.getSortOrder().isBlank() ? vo.getSortOrder() : "desc";
        Sort sort = Sort.by(
                Sort.Order.desc("featured"),
                new Sort.Order("desc".equalsIgnoreCase(sortOrder) ? Sort.Direction.DESC : Sort.Direction.ASC, sortName)
        );
        Pageable pageable = PageRequest.of(vo.getPage(), vo.getRows(), sort);

        Specification<DyProduct> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("deleted"), 0));
            if (vo.getUserId() != null && vo.getUserId() > 0) {
                predicates.add(cb.equal(root.get("userId"), vo.getUserId()));
            } else if (vo.getUserIds() != null && !vo.getUserIds().isEmpty()) {
                predicates.add(root.get("userId").in(vo.getUserIds()));
            }
            if (vo.getKeyword() != null && !vo.getKeyword().isBlank()) {
                String kw = "%" + vo.getKeyword().trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("productName"), kw),
                        cb.like(root.get("sku"), kw),
                        cb.like(root.get("barcode"), kw)
                ));
            }
            if (vo.getProductCategory() != null && !vo.getProductCategory().isBlank()) {
                predicates.add(cb.equal(root.get("productCategory"), vo.getProductCategory().trim()));
            }
            if (vo.getStatus() != null) predicates.add(cb.equal(root.get("status"), vo.getStatus()));
            if (vo.getFeatured() != null) predicates.add(cb.equal(root.get("featured"), vo.getFeatured()));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<DyProduct> page = dyProductRepository.findAll(spec, pageable);
        return PageResultVO.of(page.getTotalElements(),
                page.getContent().stream().map(this::toVO).collect(Collectors.toList()),
                vo.getPage(), vo.getRows());
    }

    public ProductVO getById(Long id) {
        if (id == null || id <= 0) throw new BusinessException(ErrorCode.VALIDATION_FAIL, "商品 ID 无效");
        return toVO(dyProductRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "商品不存在")));
    }

    @Transactional(rollbackFor = Exception.class)
    public long save(ProductSaveVO vo) {
        DyProduct entity;
        if (vo.getId() != null && vo.getId() > 0) {
            entity = dyProductRepository.findByIdAndDeleted(vo.getId(), 0)
                    .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "商品不存在"));
        } else if (vo.getUserId() != null && vo.getSku() != null && !vo.getSku().isBlank()) {
            entity = dyProductRepository.findByUserIdAndSkuAndDeleted(vo.getUserId(), vo.getSku(), 0)
                    .orElseGet(() -> {
                        DyProduct e = new DyProduct();
                        e.setUserId(vo.getUserId());
                        return e;
                    });
        } else {
            entity = new DyProduct();
            entity.setUserId(vo.getUserId());
        }
        entity.setProductName(vo.getProductName());
        entity.setPrice(vo.getPrice());
        if (vo.getProductCategory() != null) entity.setProductCategory(vo.getProductCategory());
        if (vo.getDescription() != null) entity.setDescription(vo.getDescription());
        if (vo.getImageUrl() != null) entity.setImageUrl(vo.getImageUrl());
        if (vo.getCostPrice() != null) entity.setCostPrice(vo.getCostPrice());
        if (vo.getInventory() != null) entity.setInventory(vo.getInventory());
        if (vo.getSku() != null) entity.setSku(vo.getSku());
        if (vo.getBarcode() != null) entity.setBarcode(vo.getBarcode());
        if (vo.getManufacturer() != null) entity.setManufacturer(vo.getManufacturer());
        if (vo.getTags() != null) entity.setTags(vo.getTags());
        if (vo.getStatus() != null) entity.setStatus(vo.getStatus());
        if (vo.getFeatured() != null) entity.setFeatured(vo.getFeatured());
        if (vo.getProfitMarginPct() != null) entity.setProfitMarginPct(vo.getProfitMarginPct());
        if (vo.getLossPerUnit() != null) entity.setLossPerUnit(vo.getLossPerUnit());
        if (vo.getControlStrategy() != null) entity.setControlStrategy(vo.getControlStrategy());
        if (vo.getProductLink() != null) entity.setProductLink(vo.getProductLink());
        if (vo.getAiSellingPoints() != null) entity.setAiSellingPoints(vo.getAiSellingPoints());
        long id = dyProductRepository.save(entity).getId();
        // 若有链接且卖点为空，异步提取关键信息（不阻塞保存）
        if (vo.getProductLink() != null && !vo.getProductLink().isBlank()
                && (vo.getAiSellingPoints() == null || vo.getAiSellingPoints().isBlank())) {
            productLinkExtractService.extractAndSaveAsync(id);
        }
        return id;
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, Long userId) {
        DyProduct entity = dyProductRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "商品不存在"));

        // P0-1: 数据所有权校验
        if (!entity.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权限删除该商品");
        }

        entity.setDeleted(1);
        dyProductRepository.save(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(java.util.List<Long> ids, Long userId) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "商品 ID 列表不能为空");
        }
        List<DyProduct> entities = dyProductRepository.findAllById(ids);
        for (DyProduct entity : entities) {
            // P0-1: 数据所有权校验 - 仅删除自己的数据
            if (entity.getDeleted() == 0 && entity.getUserId().equals(userId)) {
                entity.setDeleted(1);
            }
        }
        dyProductRepository.saveAll(entities);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateInventory(Long id, Long quantity) {
        DyProduct entity = dyProductRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "商品不存在"));
        long newInventory = (entity.getInventory() != null ? entity.getInventory() : 0) + quantity;
        if (newInventory < 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "库存不足");
        }
        entity.setInventory(newInventory);
        try {
            dyProductRepository.save(entity);
        } catch (jakarta.persistence.OptimisticLockException e) {
            throw new BusinessException(ErrorCode.INVENTORY_CONFLICT, "库存已被其他操作更新，请刷新后重试");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void publish(Long id) {
        dyProductRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "商品不存在"));
        dyProductRepository.updateStatus(id, 1);
    }

    @Transactional(rollbackFor = Exception.class)
    public void unpublish(Long id) {
        dyProductRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "商品不存在"));
        dyProductRepository.updateStatus(id, 0);
    }

    @Transactional(rollbackFor = Exception.class)
    public void setFeatured(Long id, Integer featured) {
        dyProductRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "商品不存在"));
        dyProductRepository.updateFeatured(id, featured);
    }

    @Override
    public String inferProductType(Long productId) {
        if (productId == null) return null;
        DyProduct product = dyProductRepository.findByIdAndDeleted(productId, 0).orElse(null);
        if (product == null) return null;

        List<String> types = new ArrayList<>();

        // 1. 根据利润率判断利润品（>30%）
        if (product.getProfitMarginPct() != null && product.getProfitMarginPct().compareTo(new BigDecimal("0.3")) > 0) {
            types.add("profit");
        }
        // 2. 根据每单亏损判断亏品
        if (product.getLossPerUnit() != null && product.getLossPerUnit().compareTo(BigDecimal.ZERO) > 0) {
            types.add("loss");
        }
        // 3. 根据 featured 标识爆品
        if (product.getFeatured() != null && product.getFeatured() == 1) {
            types.add("hot");
        }
        // 4. 根据 control_strategy 判断控单产品
        if (product.getControlStrategy() != null && !product.getControlStrategy().isBlank()) {
            types.add("control");
        }
        // 5. 默认平价品
        if (types.isEmpty()) {
            types.add("flat");
        }

        return String.join(",", types);
    }

    private ProductVO toVO(DyProduct e) {
        ProductVO vo = new ProductVO();
        vo.setId(e.getId());
        vo.setUserId(e.getUserId());
        vo.setProductName(e.getProductName());
        vo.setProductCategory(e.getProductCategory());
        vo.setDescription(e.getDescription());
        vo.setImageUrl(e.getImageUrl());
        vo.setPrice(e.getPrice());
        vo.setCostPrice(e.getCostPrice());
        vo.setInventory(e.getInventory());
        vo.setSku(e.getSku());
        vo.setBarcode(e.getBarcode());
        vo.setManufacturer(e.getManufacturer());
        vo.setTags(e.getTags());
        vo.setStatus(e.getStatus());
        vo.setFeatured(e.getFeatured());
        vo.setProfitMarginPct(e.getProfitMarginPct());
        vo.setLossPerUnit(e.getLossPerUnit());
        vo.setControlStrategy(e.getControlStrategy());
        vo.setProductLink(e.getProductLink());
        vo.setAiSellingPoints(e.getAiSellingPoints());
        vo.setCreateTime(e.getCreateTime());
        vo.setUpdateTime(e.getUpdateTime());
        return vo;
    }
}
