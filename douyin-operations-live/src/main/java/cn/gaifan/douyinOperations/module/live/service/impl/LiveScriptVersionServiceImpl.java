package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.live.entity.LiveScriptVersion;
import cn.gaifan.douyinOperations.module.live.repository.LiveScriptVersionRepository;
import cn.gaifan.douyinOperations.module.live.service.LiveScriptVersionService;
import cn.gaifan.douyinOperations.module.live.vo.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LiveScriptVersionServiceImpl implements LiveScriptVersionService {
    @Resource private LiveScriptVersionRepository liveScriptVersionRepository;

    public PageResultVO<LiveScriptVersionVO> search(LiveScriptVersionSearchVO vo) {
        vo.validateParams();
        Pageable pageable = PageRequest.of(vo.getPage(), vo.getRows());
        var page = liveScriptVersionRepository.findAll(pageable);
        var list = page.getContent().stream().map(this::toVO).collect(Collectors.toList());
        return PageResultVO.of(page.getTotalElements(), list, vo.getPage(), vo.getRows());
    }

    public LiveScriptVersionVO getById(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "版本 ID 无效");
        }
        return liveScriptVersionRepository.findById(id).map(this::toVO)
            .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "版本不存在"));
    }

    @Transactional
    public long save(LiveScriptVersionSaveVO vo) {
        if (vo == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "参数不能为空");
        }
        if (vo.getScriptId() == null || vo.getScriptId() <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "脚本 ID 不能为空");
        }
        if (vo.getScriptContent() == null || vo.getScriptContent().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "脚本内容不能为空");
        }
        LiveScriptVersion v = new LiveScriptVersion();
        v.setScriptId(vo.getScriptId());
        v.setScriptContent(vo.getScriptContent());
        Long count = liveScriptVersionRepository.countByScriptIdAndDeleted(vo.getScriptId(), 0);
        v.setVersionNo((int)(count + 1));
        v.setVersionLabel("v" + v.getVersionNo());
        return liveScriptVersionRepository.save(v).getId();
    }

    @Transactional
    public void delete(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "版本 ID 无效");
        }
        var v = liveScriptVersionRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "版本不存在"));
        v.setDeleted(1);
        liveScriptVersionRepository.save(v);
    }

    public List<LiveScriptVersionVO> getVersionsByScriptId(Long scriptId) {
        return liveScriptVersionRepository.findByScriptIdAndDeletedOrderByVersionNoDesc(scriptId, 0)
            .stream().map(this::toVO).collect(Collectors.toList());
    }

    public LiveScriptVersionVO getLatestVersion(Long scriptId) {
        if (scriptId == null || scriptId <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "脚本 ID 无效");
        }
        return liveScriptVersionRepository.findLatestVersionByScriptId(scriptId)
            .map(this::toVO)
            .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "暂无版本"));
    }

    public VersionDiffVO diffVersions(Long oldId, Long newId) {
        if (oldId == null || oldId <= 0 || newId == null || newId <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "版本 ID 无效");
        }
        var old = liveScriptVersionRepository.findById(oldId)
            .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "旧版本不存在"));
        var neu = liveScriptVersionRepository.findById(newId)
            .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "新版本不存在"));
        var diff = new VersionDiffVO();
        diff.setOldVersionId(oldId);
        diff.setNewVersionId(newId);
        diff.setOldVersionNo(old.getVersionNo());
        diff.setNewVersionNo(neu.getVersionNo());
        diff.setOldContent(old.getScriptContent());
        diff.setNewContent(neu.getScriptContent());
        diff.setChangedFields(new ArrayList<>());
        diff.setSimilarity(100);
        diff.setRecommendNew(true);
        diff.setRecommendation("OK");
        return diff;
    }

    @Transactional
    public void setRecommended(Long versionId, String reason) {
        var v = liveScriptVersionRepository.findById(versionId).orElseThrow();
        v.setIsRecommended(1);
        liveScriptVersionRepository.save(v);
    }

    @Transactional
    public void cancelRecommended(Long versionId) {
        var v = liveScriptVersionRepository.findById(versionId).orElseThrow();
        v.setIsRecommended(0);
        liveScriptVersionRepository.save(v);
    }

    public List<LiveScriptVersionVO> getRecommendedVersions(Long scriptId) {
        return liveScriptVersionRepository.findByScriptIdAndIsRecommendedAndDeleted(scriptId, 1, 0)
            .stream().map(this::toVO).collect(Collectors.toList());
    }

    public List<LiveScriptVersionVO> recommendVersions(Long scriptId) {
        return liveScriptVersionRepository.findByScriptIdOrderByRecommendScoreDesc(scriptId)
            .stream().map(this::toVO).limit(3).collect(Collectors.toList());
    }

    @Transactional
    public void updateVersionStatus(Long versionId, String status) {
        var v = liveScriptVersionRepository.findById(versionId).orElseThrow();
        v.setVersionStatus(status);
        liveScriptVersionRepository.save(v);
    }

    @Transactional
    public void incrementUsageCount(Long versionId) {
        var v = liveScriptVersionRepository.findById(versionId).orElseThrow();
        v.setUsageCount((v.getUsageCount() != null ? v.getUsageCount() : 0) + 1);
        liveScriptVersionRepository.save(v);
    }

    @Transactional
    public void incrementLikedCount(Long versionId) {
        var v = liveScriptVersionRepository.findById(versionId).orElseThrow();
        v.setLikedCount((v.getLikedCount() != null ? v.getLikedCount() : 0) + 1);
        liveScriptVersionRepository.save(v);
    }

    public List<LiveScriptVersionVO> getLatestVersionsByScriptIds(List<Long> scriptIds) {
        return liveScriptVersionRepository.findLatestVersionsByScriptIds(scriptIds)
            .stream().map(this::toVO).collect(Collectors.toList());
    }

    public List<LiveScriptVersionVO> getUserVersionsBySession(Long sessionId, Long ownerId) {
        return liveScriptVersionRepository.findBySessionIdAndOwnerId(sessionId, ownerId)
            .stream().map(this::toVO).collect(Collectors.toList());
    }

    @Transactional
    public long createVersionFromExisting(Long sourceId, LiveScriptVersionSaveVO vo) {
        var source = liveScriptVersionRepository.findById(sourceId).orElseThrow();
        vo.setScriptId(source.getScriptId());
        return save(vo);
    }

    private LiveScriptVersionVO toVO(LiveScriptVersion v) {
        var vo = new LiveScriptVersionVO();
        vo.setId(v.getId());
        vo.setScriptId(v.getScriptId());
        vo.setSessionId(v.getSessionId());
        vo.setVersionNo(v.getVersionNo());
        vo.setVersionLabel(v.getVersionLabel());
        vo.setScriptContent(v.getScriptContent());
        vo.setScriptType(v.getScriptType());
        vo.setRemark(v.getRemark());
        vo.setVersionStatus(v.getVersionStatus());
        vo.setLikedCount(v.getLikedCount());
        vo.setUsageCount(v.getUsageCount());
        vo.setOwnerId(v.getOwnerId());
        vo.setIsRecommended(v.getIsRecommended());
        vo.setCreateTime(v.getCreateTime());
        vo.setUpdateTime(v.getUpdateTime());
        return vo;
    }
}
