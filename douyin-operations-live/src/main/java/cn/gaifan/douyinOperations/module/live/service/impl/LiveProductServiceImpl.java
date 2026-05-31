package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.live.entity.LiveProduct;
import cn.gaifan.douyinOperations.module.live.repository.LiveProductRepository;
import cn.gaifan.douyinOperations.module.live.service.LiveProductService;
import cn.gaifan.douyinOperations.module.live.vo.*;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 直播产品服务实现
 */
@Service
public class LiveProductServiceImpl implements LiveProductService {

    @Resource
    private LiveProductRepository liveProductRepository;

    private static final Set<String> SORTABLE_FIELDS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("id", "sessionId", "position", "createTime")));

    @Override
    public PageResultVO<LiveProductVO> search(LiveProductSearchVO vo) {
        vo.validateParams();
        String sortName = SORTABLE_FIELDS.contains(vo.getSortName()) ? vo.getSortName() : "id";
        Pageable pageable = PageRequest.of(vo.getPage(), vo.getRows(),
                Sort.by("desc".equalsIgnoreCase(vo.getSortOrder()) ? Sort.Direction.DESC : Sort.Direction.ASC, sortName));

        if (vo.getSessionId() == null && vo.getSessionIds() != null && vo.getSessionIds().isEmpty()) {
            return PageResultVO.of(0L, Collections.emptyList(), vo.getPage(), vo.getRows());
        }

        Page<LiveProduct> page;
        if (vo.getSessionId() != null && vo.getSessionId() > 0) {
            page = liveProductRepository.findBySessionId(vo.getSessionId(), pageable);
        } else if (vo.getSessionIds() != null && !vo.getSessionIds().isEmpty()) {
            page = liveProductRepository.findBySessionIdIn(vo.getSessionIds(), pageable);
        } else {
            page = liveProductRepository.findAll(pageable);
        }

        List<LiveProductVO> list = page.getContent().stream().map(this::toLiveProductVO).collect(Collectors.toList());
        return PageResultVO.of(page.getTotalElements(), list, vo.getPage(), vo.getRows());
    }

    // P0-3: 添加缓存 - 产品详情查询
    @Override
    @Cacheable(value = "live:product", key = "#id")
    public LiveProductVO getById(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "直播产品 ID 无效");
        }
        LiveProduct product = liveProductRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "直播产品不存在"));
        return toLiveProductVO(product);
    }

    // P0-3: 保存时清除缓存
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "live:product", key = "#vo.id", condition = "#vo.id != null")
    public long save(LiveProductSaveVO vo) {
        if (vo.getSessionId() == null || vo.getSessionId() <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "直播场次 ID 无效");
        }
        if (vo.getProductId() == null || vo.getProductId() <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "产品 ID 无效");
        }

        LiveProduct product;
        if (vo.getId() != null && vo.getId() > 0) {
            product = liveProductRepository.findById(vo.getId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "直播产品不存在"));
        } else {
            product = new LiveProduct();
            product.setSessionId(vo.getSessionId());
            product.setProductId(vo.getProductId());
        }
        product.setProductName(vo.getProductName());
        product.setSaleQuantity(vo.getSaleQuantity());
        product.setPosition(vo.getPosition());
        product.setProductType(trimToNull(vo.getProductType()));
        if (vo.getScriptSource() != null) {
            product.setScriptSource(trimToNull(vo.getScriptSource()));
        }
        product.setProductScriptId(vo.getProductScriptId());
        product = liveProductRepository.save(product);
        return product.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    // P0-3: 删除时清除缓存
    @CacheEvict(value = "live:product", key = "#id")
    public void delete(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "直播产品 ID 无效");
        }
        liveProductRepository.deleteById(id);
    }

    @Override
    public List<LiveProductVO> getBySessionId(Long sessionId) {
        if (sessionId == null || sessionId <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "直播场次 ID 无效");
        }
        return liveProductRepository.findBySessionIdOrderByPositionAscIdAsc(sessionId).stream()
                .map(this::toLiveProductVO).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBySessionId(Long sessionId) {
        if (sessionId == null || sessionId <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "直播场次 ID 无效");
        }
        liveProductRepository.deleteBySessionId(sessionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchSort(Long sessionId, List<Long> productIds) {
        if (sessionId == null || productIds == null || productIds.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "sessionId 和 productIds 不能为空");
        }
        List<LiveProduct> products = liveProductRepository.findAllById(productIds);
        Map<Long, Integer> positionMap = new HashMap<>();
        for (int i = 0; i < productIds.size(); i++) {
            positionMap.put(productIds.get(i), i);
        }
        for (LiveProduct p : products) {
            if (p.getSessionId() != null && p.getSessionId().equals(sessionId)) {
                Integer newPos = positionMap.get(p.getId());
                if (newPos != null) {
                    p.setPosition(newPos);
                }
            }
        }
        liveProductRepository.saveAll(products);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchAdd(Long sessionId, List<LiveProductBatchAddItemVO> items, Long userId) {
        if (sessionId == null || items == null || items.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "sessionId 和 items 不能为空");
        }

        // 获取当前场次已有产品的最大 position
        List<LiveProduct> existing = liveProductRepository.findBySessionIdOrderByPositionAscIdAsc(sessionId);
        int maxPosition = existing.stream()
                .mapToInt(p -> p.getPosition() != null ? p.getPosition() : 0)
                .max()
                .orElse(-1);

        // 批量创建产品
        List<LiveProduct> newProducts = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            LiveProductBatchAddItemVO item = items.get(i);
            LiveProduct product = new LiveProduct();
            product.setSessionId(sessionId);
            product.setProductId(item.getProductId());
            product.setProductName(item.getProductName());
            product.setProductType(item.getProductType());
            product.setProductScriptId(item.getProductScriptId());
            product.setPosition(maxPosition + i + 1);
            newProducts.add(product);
        }

        liveProductRepository.saveAll(newProducts);
        return newProducts.size();
    }

    private LiveProductVO toLiveProductVO(LiveProduct product) {
        LiveProductVO vo = new LiveProductVO();
        vo.setId(product.getId());
        vo.setSessionId(product.getSessionId());
        vo.setProductId(product.getProductId());
        vo.setProductName(product.getProductName());
        vo.setSaleQuantity(product.getSaleQuantity());
        vo.setRevenue(product.getRevenue());
        vo.setPosition(product.getPosition());
        vo.setProductType(product.getProductType());
        vo.setScriptSource(product.getScriptSource());
        vo.setProductScriptId(product.getProductScriptId());
        vo.setCreateTime(product.getCreateTime());
        return vo;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
