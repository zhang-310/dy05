package cn.gaifan.douyinOperations.module.product.repository;

import cn.gaifan.douyinOperations.module.product.entity.ProductScriptSnapshot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 商品话术快照数据访问层
 *
 * @author Claude Code
 * @since 2026-03-06
 */
@Repository
public interface ProductScriptSnapshotRepository extends JpaRepository<ProductScriptSnapshot, Long>,
        JpaSpecificationExecutor<ProductScriptSnapshot> {

    /**
     * 查询特定直播场次的所有快照
     *
     * @param liveSessionId 直播场次 ID
     * @return 快照列表
     */
    List<ProductScriptSnapshot> findByLiveSessionId(Long liveSessionId);

    /**
     * 查询特定直播场次的所有快照（分页）
     *
     * @param liveSessionId 直播场次 ID
     * @param pageable 分页参数
     * @return 快照分页结果
     */
    Page<ProductScriptSnapshot> findByLiveSessionId(Long liveSessionId, Pageable pageable);

    /**
     * 查询特定话术版本的所有快照
     *
     * @param productScriptVersionId 话术版本 ID
     * @return 快照列表
     */
    List<ProductScriptSnapshot> findByProductScriptVersionId(Long productScriptVersionId);

    /**
     * 查询特定所有者的所有快照
     *
     * @param ownerId 所有者 ID
     * @param pageable 分页参数
     * @return 快照分页结果
     */
    Page<ProductScriptSnapshot> findByOwnerId(Long ownerId, Pageable pageable);

    /**
     * 查询特定所有者和直播场次的快照
     *
     * @param ownerId 所有者 ID
     * @param liveSessionId 直播场次 ID
     * @return 快照列表
     */
    List<ProductScriptSnapshot> findByOwnerIdAndLiveSessionId(Long ownerId, Long liveSessionId);
}
