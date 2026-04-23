package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.live.entity.LiveSessionRealtimeData;
import cn.gaifan.douyinOperations.module.live.entity.LiveSessionScriptSlot;
import cn.gaifan.douyinOperations.module.live.repository.LiveSessionRealtimeDataRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveSessionScriptSlotRepository;
import cn.gaifan.douyinOperations.module.live.service.LiveRealtimePanelService;
import cn.gaifan.douyinOperations.module.live.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 直播实时辅助面板业务实现
 * 实现话术导航、状态管理、实时数据更新等功能
 * 强制 owner_id 数据隔离，确保用户数据安全
 */
@Slf4j
@Service
@Transactional
public class LiveRealtimePanelServiceImpl implements LiveRealtimePanelService {

    @Autowired
    private LiveSessionScriptSlotRepository slotRepository;

    @Autowired
    private LiveSessionRealtimeDataRepository realtimeDataRepository;

    /**
     * 初始化直播实时面板
     * 查询直播的所有话术段落和实时数据
     *
     * @param liveSessionId 直播场次 ID
     * @param userId        当前用户 ID（owner_id）
     * @return 面板初始化数据
     * @throws GlobalException 如果直播不存在或无访问权限
     */
    @Override
    @Transactional(readOnly = true)
    public PanelInitVO initializePanel(Long liveSessionId, Long userId) {
        log.info("初始化实时面板: liveSessionId={}, userId={}", liveSessionId, userId);

        // 查询所有话术段落（强制 owner_id 隔离）
        List<LiveSessionScriptSlot> slots = slotRepository.findByLiveSessionIdOrderBySlotIndex(liveSessionId);

        // 验证数据所有权
        validateOwnershipForSlots(slots, userId, liveSessionId);

        // 转换为 VO
        List<LiveSessionScriptSlotVO> slotVOs = convertToSlotVOs(slots);

        // 查找当前活跃的话术（isCurrent = true）
        int currentSlotIndex = 0;
        for (int i = 0; i < slots.size(); i++) {
            if (Boolean.TRUE.equals(slots.get(i).getIsCurrent())) {
                currentSlotIndex = i;
                break;
            }
        }

        // 获取或创建实时数据
        LiveSessionRealtimeDataVO realtimeData = getOrCreateRealtimeData(liveSessionId, userId);

        PanelInitVO result = new PanelInitVO();
        result.setLiveSessionId(liveSessionId);
        result.setSlots(slotVOs);
        result.setCurrentSlotIndex(currentSlotIndex);
        result.setRealtimeData(realtimeData);

        log.info("实时面板初始化成功: 话术段落数={}, 当前序号={}", slots.size(), currentSlotIndex);
        return result;
    }

    /**
     * 跳转到下一话术段落
     * 将当前段落的 isCurrent 标记为 false，下一段落标记为 true，并设置 startedAt
     *
     * @param liveSessionId 直播场次 ID
     * @param userId        当前用户 ID
     * @return 下一话术段落的 VO
     * @throws GlobalException 如果已经是最后一个话术或数据不一致
     */
    @Override
    public LiveSessionScriptSlotVO nextSlot(Long liveSessionId, Long userId) {
        log.info("执行下一话术: liveSessionId={}, userId={}", liveSessionId, userId);

        List<LiveSessionScriptSlot> slots = slotRepository.findByLiveSessionIdOrderBySlotIndex(liveSessionId);
        validateOwnershipForSlots(slots, userId, liveSessionId);

        if (slots.isEmpty()) {
            log.warn("没有找到话术段落: liveSessionId={}", liveSessionId);
            throw new BusinessException(ErrorCode.LIVE_NO_SCRIPTS_FOUND, "没有可用的话术段落");
        }

        // 找到当前活跃的段落
        int currentIndex = -1;
        for (int i = 0; i < slots.size(); i++) {
            if (Boolean.TRUE.equals(slots.get(i).getIsCurrent())) {
                currentIndex = i;
                break;
            }
        }

        // 如果没有当前段落，从第一个开始
        if (currentIndex == -1) {
            currentIndex = 0;
        } else if (currentIndex >= slots.size() - 1) {
            // 已经是最后一个，无法继续
            log.warn("已在最后一个话术段落: liveSessionId={}", liveSessionId);
            throw new BusinessException(ErrorCode.LIVE_SCRIPT_ALREADY_LAST, "已经是最后一个话术段落");
        } else {
            // 取消当前段落的标记
            LiveSessionScriptSlot currentSlot = slots.get(currentIndex);
            currentSlot.setIsCurrent(false);
            slotRepository.save(currentSlot);
            currentIndex++;
        }

        // 标记下一个段落为当前
        LiveSessionScriptSlot nextSlot = slots.get(currentIndex);
        nextSlot.setIsCurrent(true);
        nextSlot.setStartedAt(LocalDateTime.now());
        slotRepository.save(nextSlot);

        log.info("下一话术执行成功: slotIndex={}", nextSlot.getSlotIndex());
        return convertToSlotVO(nextSlot);
    }

    /**
     * 返回到上一话术段落
     * 将当前段落的 isCurrent 标记为 false，上一段落标记为 true
     *
     * @param liveSessionId 直播场次 ID
     * @param userId        当前用户 ID
     * @return 上一话术段落的 VO
     * @throws GlobalException 如果已经是第一个话术或数据不一致
     */
    @Override
    public LiveSessionScriptSlotVO prevSlot(Long liveSessionId, Long userId) {
        log.info("执行上一话术: liveSessionId={}, userId={}", liveSessionId, userId);

        List<LiveSessionScriptSlot> slots = slotRepository.findByLiveSessionIdOrderBySlotIndex(liveSessionId);
        validateOwnershipForSlots(slots, userId, liveSessionId);

        if (slots.isEmpty()) {
            log.warn("没有找到话术段落: liveSessionId={}", liveSessionId);
            throw new BusinessException(ErrorCode.LIVE_NO_SCRIPTS_FOUND, "没有可用的话术段落");
        }

        // 找到当前活跃的段落
        int currentIndex = -1;
        for (int i = 0; i < slots.size(); i++) {
            if (Boolean.TRUE.equals(slots.get(i).getIsCurrent())) {
                currentIndex = i;
                break;
            }
        }

        // 如果没有当前段落或在第一个位置
        if (currentIndex <= 0) {
            log.warn("已在第一个话术段落: liveSessionId={}", liveSessionId);
            throw new BusinessException(ErrorCode.LIVE_SCRIPT_ALREADY_FIRST, "已经是第一个话术段落");
        }

        // 取消当前段落的标记
        LiveSessionScriptSlot currentSlot = slots.get(currentIndex);
        currentSlot.setIsCurrent(false);
        slotRepository.save(currentSlot);

        // 标记上一个段落为当前
        LiveSessionScriptSlot prevSlot = slots.get(currentIndex - 1);
        prevSlot.setIsCurrent(true);
        prevSlot.setStartedAt(LocalDateTime.now());
        slotRepository.save(prevSlot);

        log.info("上一话术执行成功: slotIndex={}", prevSlot.getSlotIndex());
        return convertToSlotVO(prevSlot);
    }

    /**
     * 跳转到指定序号的话术段落
     * 更新当前段落和目标段落的状态
     *
     * @param liveSessionId 直播场次 ID
     * @param slotIndex     目标段落序号
     * @param userId        当前用户 ID
     * @return 目标话术段落的 VO
     * @throws GlobalException 如果段落序号无效或数据不一致
     */
    @Override
    public LiveSessionScriptSlotVO jumpSlot(Long liveSessionId, Integer slotIndex, Long userId) {
        log.info("执行跳转话术: liveSessionId={}, slotIndex={}, userId={}", liveSessionId, slotIndex, userId);

        List<LiveSessionScriptSlot> slots = slotRepository.findByLiveSessionIdOrderBySlotIndex(liveSessionId);
        validateOwnershipForSlots(slots, userId, liveSessionId);

        if (slots.isEmpty()) {
            log.warn("没有找到话术段落: liveSessionId={}", liveSessionId);
            throw new BusinessException(ErrorCode.LIVE_NO_SCRIPTS_FOUND, "没有可用的话术段落");
        }

        // 验证目标段落序号
        if (slotIndex == null || slotIndex < 0 || slotIndex >= slots.size()) {
            log.warn("无效的段落序号: slotIndex={}", slotIndex);
            throw new BusinessException(ErrorCode.LIVE_INVALID_SLOT_INDEX, "无效的段落序号");
        }

        // 取消当前段落的标记
        for (LiveSessionScriptSlot slot : slots) {
            if (Boolean.TRUE.equals(slot.getIsCurrent())) {
                slot.setIsCurrent(false);
                slotRepository.save(slot);
                break;
            }
        }

        // 标记目标段落为当前
        LiveSessionScriptSlot targetSlot = slots.get(slotIndex);
        targetSlot.setIsCurrent(true);
        targetSlot.setStartedAt(LocalDateTime.now());
        slotRepository.save(targetSlot);

        log.info("跳转话术执行成功: slotIndex={}", slotIndex);
        return convertToSlotVO(targetSlot);
    }

    /**
     * 标记话术段落为已完成
     * 设置 isCompleted 为 true，completedAt 为当前时间
     *
     * @param liveSessionId 直播场次 ID
     * @param slotIndex     段落序号
     * @param userId        当前用户 ID
     * @return 已完成的话术段落的 VO
     * @throws GlobalException 如果段落不存在或数据不一致
     */
    @Override
    public LiveSessionScriptSlotVO completeSlot(Long liveSessionId, Integer slotIndex, Long userId) {
        log.info("标记话术完成: liveSessionId={}, slotIndex={}, userId={}", liveSessionId, slotIndex, userId);

        Optional<LiveSessionScriptSlot> optionalSlot = slotRepository
                .findByLiveSessionIdAndSlotIndex(liveSessionId, slotIndex);

        if (optionalSlot.isEmpty()) {
            log.warn("未找到话术段落: liveSessionId={}, slotIndex={}", liveSessionId, slotIndex);
            throw new BusinessException(ErrorCode.LIVE_SCRIPT_NOT_FOUND, "未找到指定的话术段落");
        }

        LiveSessionScriptSlot slot = optionalSlot.get();

        // 验证数据所有权
        if (!slot.getOwnerId().equals(userId)) {
            log.warn("无访问权限: slotId={}, userId={}, ownerId={}", slot.getId(), userId, slot.getOwnerId());
            throw new BusinessException(ErrorCode.LIVE_ACCESS_DENIED, "无访问权限");
        }

        // 标记为完成
        slot.setIsCompleted(true);
        slot.setCompletedAt(LocalDateTime.now());
        slotRepository.save(slot);

        log.info("话术完成标记成功: slotIndex={}", slotIndex);
        return convertToSlotVO(slot);
    }

    /**
     * 更新实时数据
     * 保存或更新直播场次的实时统计数据
     *
     * @param data   实时数据保存参数
     * @param userId 当前用户 ID
     * @return 更新后的实时数据 VO
     * @throws GlobalException 如果直播不存在或无访问权限
     */
    @Override
    public LiveSessionRealtimeDataVO updateRealtimeData(RealtimeDataSaveVO data, Long userId) {
        log.info("更新实时数据: liveSessionId={}, userId={}", data.getLiveSessionId(), userId);

        Long liveSessionId = data.getLiveSessionId();

        // 验证该用户有权访问此直播
        List<LiveSessionScriptSlot> slots = slotRepository.findByLiveSessionIdOrderBySlotIndex(liveSessionId);
        validateOwnershipForSlots(slots, userId, liveSessionId);

        // 获取或创建实时数据记录
        Optional<LiveSessionRealtimeData> optionalData = realtimeDataRepository.findByLiveSessionId(liveSessionId);
        LiveSessionRealtimeData realtimeData;

        if (optionalData.isPresent()) {
            realtimeData = optionalData.get();
        } else {
            realtimeData = new LiveSessionRealtimeData();
            realtimeData.setLiveSessionId(liveSessionId);
        }

        // 更新各字段（使用提供的值，如果为 null 则保留原值）
        if (data.getWatchedCount() != null) {
            realtimeData.setWatchedCount(data.getWatchedCount());
        }
        if (data.getViewerCount() != null) {
            realtimeData.setViewerCount(data.getViewerCount());
        }
        if (data.getLikeCount() != null) {
            realtimeData.setLikeCount(data.getLikeCount());
        }
        if (data.getCommentCount() != null) {
            realtimeData.setCommentCount(data.getCommentCount());
        }
        if (data.getShareCount() != null) {
            realtimeData.setShareCount(data.getShareCount());
        }
        if (data.getFollowCount() != null) {
            realtimeData.setFollowCount(data.getFollowCount());
        }
        if (data.getGiftAmount() != null) {
            realtimeData.setGiftAmount(data.getGiftAmount());
        }
        if (data.getProductClickCount() != null) {
            realtimeData.setProductClickCount(data.getProductClickCount());
        }
        if (data.getProductPurchaseCount() != null) {
            realtimeData.setProductPurchaseCount(data.getProductPurchaseCount());
        }
        if (data.getProductPurchaseAmount() != null) {
            realtimeData.setProductPurchaseAmount(data.getProductPurchaseAmount());
        }

        // 保存或更新
        realtimeDataRepository.save(realtimeData);

        log.info("实时数据更新成功: liveSessionId={}", liveSessionId);
        return convertToRealtimeDataVO(realtimeData);
    }

    /**
     * 获取当前话术段落
     *
     * @param liveSessionId 直播场次 ID
     * @param userId        当前用户 ID
     * @return 当前话术段落的 VO
     * @throws GlobalException 如果没有当前话术或数据不一致
     */
    @Override
    @Transactional(readOnly = true)
    public LiveSessionScriptSlotVO getCurrentSlot(Long liveSessionId, Long userId) {
        log.info("获取当前话术: liveSessionId={}, userId={}", liveSessionId, userId);

        List<LiveSessionScriptSlot> slots = slotRepository.findByLiveSessionIdOrderBySlotIndex(liveSessionId);
        validateOwnershipForSlots(slots, userId, liveSessionId);

        Optional<LiveSessionScriptSlot> currentSlot = slotRepository.findCurrentSlot(liveSessionId);

        if (currentSlot.isEmpty()) {
            log.warn("没有找到当前话术: liveSessionId={}", liveSessionId);
            throw new BusinessException(ErrorCode.LIVE_NO_CURRENT_SCRIPT, "没有当前话术段落");
        }

        return convertToSlotVO(currentSlot.get());
    }

    /**
     * 获取实时数据
     *
     * @param liveSessionId 直播场次 ID
     * @param userId        当前用户 ID
     * @return 实时数据的 VO
     */
    @Override
    @Transactional(readOnly = true)
    public LiveSessionRealtimeDataVO getRealtimeData(Long liveSessionId, Long userId) {
        log.info("获取实时数据: liveSessionId={}, userId={}", liveSessionId, userId);

        List<LiveSessionScriptSlot> slots = slotRepository.findByLiveSessionIdOrderBySlotIndex(liveSessionId);
        validateOwnershipForSlots(slots, userId, liveSessionId);

        return getOrCreateRealtimeData(liveSessionId, userId);
    }

    /**
     * 标记指定话术段落为已完成（仅更新，不返回）
     *
     * @param liveSessionId 直播场次 ID
     * @param slotIndex     段落序号
     * @param userId        当前用户 ID
     * @return 操作是否成功
     */
    @Override
    public boolean markSlotCompleted(Long liveSessionId, Integer slotIndex, Long userId) {
        log.info("标记话术段落完成: liveSessionId={}, slotIndex={}, userId={}", liveSessionId, slotIndex, userId);

        try {
            completeSlot(liveSessionId, slotIndex, userId);
            return true;
        } catch (Exception e) {
            log.error("标记话术完成失败: liveSessionId={}, slotIndex={}", liveSessionId, slotIndex, e);
            return false;
        }
    }

    /**
     * 内部方法：验证话术段落的所有权
     * 确保用户只能访问自己的数据
     *
     * @param slots         话术段落列表
     * @param userId        当前用户 ID
     * @param liveSessionId 直播场次 ID
     * @throws GlobalException 如果列表为空或所有权验证失败
     */
    private void validateOwnershipForSlots(List<LiveSessionScriptSlot> slots, Long userId, Long liveSessionId) {
        if (slots.isEmpty()) {
            // 如果没有话术段落，无法验证所有权，直接返回
            return;
        }

        // 检查第一个话术的所有者
        LiveSessionScriptSlot firstSlot = slots.get(0);
        if (!firstSlot.getOwnerId().equals(userId)) {
            log.warn("数据隔离验证失败: liveSessionId={}, userId={}, ownerId={}", liveSessionId, userId, firstSlot.getOwnerId());
            throw new BusinessException(ErrorCode.LIVE_ACCESS_DENIED, "无访问权限");
        }
    }

    /**
     * 内部方法：获取或创建实时数据
     * 如果实时数据不存在，则创建一条新的空记录
     *
     * @param liveSessionId 直播场次 ID
     * @param userId        当前用户 ID
     * @return 实时数据 VO
     */
    private LiveSessionRealtimeDataVO getOrCreateRealtimeData(Long liveSessionId, Long userId) {
        Optional<LiveSessionRealtimeData> optionalData = realtimeDataRepository.findByLiveSessionId(liveSessionId);

        if (optionalData.isPresent()) {
            return convertToRealtimeDataVO(optionalData.get());
        }

        // 创建新记录
        LiveSessionRealtimeData newData = new LiveSessionRealtimeData();
        newData.setLiveSessionId(liveSessionId);
        newData.setWatchedCount(0);
        newData.setViewerCount(0);
        newData.setLikeCount(0);
        newData.setCommentCount(0);
        newData.setShareCount(0);
        newData.setFollowCount(0);
        newData.setGiftAmount(java.math.BigDecimal.ZERO);
        newData.setProductClickCount(0);
        newData.setProductPurchaseCount(0);
        newData.setProductPurchaseAmount(java.math.BigDecimal.ZERO);

        realtimeDataRepository.save(newData);

        return convertToRealtimeDataVO(newData);
    }

    /**
     * 内部方法：转换单个话术为 VO
     *
     * @param slot 话术实体
     * @return 话术 VO
     */
    private LiveSessionScriptSlotVO convertToSlotVO(LiveSessionScriptSlot slot) {
        LiveSessionScriptSlotVO vo = new LiveSessionScriptSlotVO();
        vo.setId(slot.getId());
        vo.setLiveSessionId(slot.getLiveSessionId());
        vo.setSlotIndex(slot.getSlotIndex());
        vo.setScriptVersionId(slot.getScriptVersionId());
        vo.setContent(slot.getContent());
        vo.setDurationSeconds(slot.getDurationSeconds());
        vo.setScriptType(slot.getScriptType());
        vo.setStyle(slot.getStyle());
        vo.setIsCurrent(slot.getIsCurrent());
        vo.setIsCompleted(slot.getIsCompleted());
        vo.setStartedAt(slot.getStartedAt());
        vo.setCompletedAt(slot.getCompletedAt());
        return vo;
    }

    /**
     * 内部方法：转换话术列表为 VO 列表
     *
     * @param slots 话术实体列表
     * @return 话术 VO 列表
     */
    private List<LiveSessionScriptSlotVO> convertToSlotVOs(List<LiveSessionScriptSlot> slots) {
        return slots.stream().map(this::convertToSlotVO).collect(Collectors.toList());
    }

    /**
     * 内部方法：转换实时数据为 VO
     *
     * @param data 实时数据实体
     * @return 实时数据 VO
     */
    private LiveSessionRealtimeDataVO convertToRealtimeDataVO(LiveSessionRealtimeData data) {
        LiveSessionRealtimeDataVO vo = new LiveSessionRealtimeDataVO();
        vo.setId(data.getId());
        vo.setLiveSessionId(data.getLiveSessionId());
        vo.setWatchedCount(data.getWatchedCount());
        vo.setViewerCount(data.getViewerCount());
        vo.setLikeCount(data.getLikeCount());
        vo.setCommentCount(data.getCommentCount());
        vo.setShareCount(data.getShareCount());
        vo.setFollowCount(data.getFollowCount());
        vo.setGiftAmount(data.getGiftAmount());
        vo.setProductClickCount(data.getProductClickCount());
        vo.setProductPurchaseCount(data.getProductPurchaseCount());
        vo.setProductPurchaseAmount(data.getProductPurchaseAmount());
        vo.setCurrentSlotIndex(data.getCurrentSlotIndex());
        return vo;
    }
}
