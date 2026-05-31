package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvAccountCollectTask;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvViralVideo;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvAccountCollectTaskRepository;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvAccountCollectWorkerNodeRepository;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvViralVideoRepository;
import cn.gaifan.douyinOperations.module.shortvideo.service.AccountCollectWorkerGatewayService;
import cn.gaifan.douyinOperations.module.shortvideo.vo.AccountCollectWorkerClaimVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.AccountCollectWorkerFailVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.AccountCollectWorkerHeartbeatVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.AccountCollectWorkerSubmitResultVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.AccountCollectWorkerSubmitVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.AccountCollectWorkerTaskVO;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class AccountCollectWorkerGatewayServiceImpl implements AccountCollectWorkerGatewayService {

    @Resource
    private SvAccountCollectTaskRepository taskRepository;
    @Resource
    private SvAccountCollectWorkerNodeRepository workerNodeRepository;
    @Resource
    private SvViralVideoRepository viralVideoRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AccountCollectWorkerTaskVO claim(AccountCollectWorkerClaimVO vo) {
        String workerId = requireWorkerId(vo != null ? vo.getWorkerId() : null);
        String workerRegion = vo != null && StringUtils.hasText(vo.getWorkerRegion()) ? vo.getWorkerRegion().trim() : "remote";
        int leaseSeconds = normalizeLeaseSeconds(vo != null ? vo.getLeaseSeconds() : null);
        Timestamp leaseUntil = new Timestamp(System.currentTimeMillis() + leaseSeconds * 1000L);
        List<Long> ids = taskRepository.findOneClaimableRemoteTaskId();
        if (ids == null || ids.isEmpty()) {
            workerNodeRepository.markSeen(workerId, workerRegion, null, leaseUntil);
            return null;
        }
        Long taskId = ids.get(0);
        int updated = taskRepository.claimForCollect(taskId, workerId, workerRegion, leaseUntil);
        if (updated <= 0) {
            workerNodeRepository.markSeen(workerId, workerRegion, null, leaseUntil);
            return null;
        }
        workerNodeRepository.markClaimed(workerId, workerRegion, taskId, leaseUntil);
        SvAccountCollectTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "采集任务不存在"));
        return toWorkerTask(task);
    }

    @Override
    public boolean heartbeat(AccountCollectWorkerHeartbeatVO vo) {
        String workerId = requireWorkerId(vo != null ? vo.getWorkerId() : null);
        String workerRegion = vo != null && StringUtils.hasText(vo.getWorkerRegion()) ? vo.getWorkerRegion().trim() : null;
        Timestamp leaseUntil = new Timestamp(System.currentTimeMillis() + normalizeLeaseSeconds(vo.getLeaseSeconds()) * 1000L);
        workerNodeRepository.markSeen(workerId, workerRegion, vo != null ? vo.getTaskId() : null, leaseUntil);
        if (vo.getTaskId() == null) {
            return true;
        }
        return taskRepository.heartbeat(vo.getTaskId(), workerId, leaseUntil) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AccountCollectWorkerSubmitResultVO submit(AccountCollectWorkerSubmitVO vo) {
        String workerId = requireWorkerId(vo != null ? vo.getWorkerId() : null);
        if (vo.getTaskId() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "taskId 不能为空");
        }
        SvAccountCollectTask task = lockedWorkerTask(vo.getTaskId(), workerId);
        List<AccountCollectWorkerSubmitVO.CollectedVideoVO> videos = vo.getVideos() == null
                ? List.of()
                : vo.getVideos();
        List<SvViralVideo> created = createViralVideos(task, videos);
        String status = videos.isEmpty() ? "collected" : "collected";
        String message = videos.isEmpty() ? "采集节点返回空列表" : null;
        int updated = taskRepository.completeRemoteCollect(task.getId(), workerId, trimToNull(vo.getAccountName()), trimToNull(vo.getSecUid()),
                videos.size(), created.size(), status, message);
        if (updated <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "任务已被其他采集节点接管或已结束");
        }
        workerNodeRepository.markSubmitted(workerId);
        return result(task.getId(), videos.size(), created.size(), status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AccountCollectWorkerSubmitResultVO fail(AccountCollectWorkerFailVO vo) {
        String workerId = requireWorkerId(vo != null ? vo.getWorkerId() : null);
        if (vo.getTaskId() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "taskId 不能为空");
        }
        lockedWorkerTask(vo.getTaskId(), workerId);
        String msg = truncate(StringUtils.hasText(vo.getErrorMessage()) ? vo.getErrorMessage() : "采集节点上报失败");
        int updated;
        if (Boolean.TRUE.equals(vo.getRetryable())) {
            updated = taskRepository.releaseWorkerTaskForRetry(vo.getTaskId(), workerId, new Timestamp(System.currentTimeMillis()), msg);
            if (updated > 0) {
                workerNodeRepository.markFailed(workerId, msg);
                return result(vo.getTaskId(), 0, 0, "pending");
            }
        }
        updated = taskRepository.markWorkerTaskFailedTerminal(vo.getTaskId(), workerId, msg);
        if (updated <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "任务已被其他采集节点接管或已结束");
        }
        workerNodeRepository.markFailed(workerId, msg);
        return result(vo.getTaskId(), 0, 0, "failed");
    }

    private List<SvViralVideo> createViralVideos(SvAccountCollectTask task,
                                                 List<AccountCollectWorkerSubmitVO.CollectedVideoVO> videoInfos) {
        List<SvViralVideo> created = new ArrayList<>();
        for (AccountCollectWorkerSubmitVO.CollectedVideoVO vi : videoInfos) {
            if (vi == null) {
                continue;
            }
            if (StringUtils.hasText(vi.getVideoId())) {
                Optional<SvViralVideo> existing = viralVideoRepository
                        .findByOwnerIdAndDouyinVideoIdAndDeleted(task.getOwnerId(), vi.getVideoId(), 0);
                if (existing.isPresent()) {
                    SvViralVideo ex = existing.get();
                    if (ex.getCollectTaskId() == null) {
                        ex.setCollectTaskId(task.getId());
                    }
                    if (ex.getSvAccountId() == null && task.getSvAccountId() != null) {
                        ex.setSvAccountId(task.getSvAccountId());
                    }
                    viralVideoRepository.save(ex);
                    created.add(ex);
                    continue;
                }
            }
            SvViralVideo viral = new SvViralVideo();
            viral.setOwnerId(task.getOwnerId());
            viral.setSvAccountId(task.getSvAccountId());
            viral.setDouyinVideoId(trimToNull(vi.getVideoId()));
            viral.setTitle(truncate(vi.getTitle(), 512));
            viral.setCoverUrl(truncate(vi.getCoverUrl(), 512));
            viral.setVideoUrl(truncate(vi.getVideoUrl(), 512));
            viral.setViewCount(vi.getViewCount());
            viral.setLikeCount(vi.getLikeCount());
            viral.setCommentCount(vi.getCommentCount());
            viral.setShareCount(vi.getShareCount());
            viral.setFavoriteCount(vi.getFavoriteCount());
            viral.setVideoDuration(vi.getDuration());
            viral.setPublishTime(vi.getPublishTime());
            viral.setAutoCollected(true);
            viral.setCollectSource("remote_account_collect");
            viral.setCollectTaskId(task.getId());
            viralVideoRepository.save(viral);
            created.add(viral);
        }
        return created;
    }

    private SvAccountCollectTask lockedWorkerTask(Long taskId, String workerId) {
        SvAccountCollectTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "采集任务不存在"));
        if (!"collecting".equals(task.getStatus()) || !workerId.equals(task.getWorkerId())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "任务未由当前采集节点领取或已结束");
        }
        return task;
    }

    private AccountCollectWorkerTaskVO toWorkerTask(SvAccountCollectTask task) {
        AccountCollectWorkerTaskVO vo = new AccountCollectWorkerTaskVO();
        BeanUtils.copyProperties(task, vo);
        vo.setTaskId(task.getId());
        return vo;
    }

    private AccountCollectWorkerSubmitResultVO result(Long taskId, int submitted, int created, String status) {
        AccountCollectWorkerSubmitResultVO vo = new AccountCollectWorkerSubmitResultVO();
        vo.setTaskId(taskId);
        vo.setSubmittedVideos(submitted);
        vo.setCreatedVideos(created);
        vo.setStatus(status);
        return vo;
    }

    private String requireWorkerId(String workerId) {
        if (!StringUtils.hasText(workerId)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "workerId 不能为空");
        }
        return workerId.trim();
    }

    private int normalizeLeaseSeconds(Integer leaseSeconds) {
        int value = leaseSeconds == null || leaseSeconds <= 0 ? 900 : leaseSeconds;
        return Math.max(60, Math.min(value, 3600));
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String truncate(String value) {
        return truncate(value, 500);
    }

    private String truncate(String value, int maxLen) {
        if (value == null) {
            return null;
        }
        return value.length() > maxLen ? value.substring(0, maxLen) : value;
    }
}
