package cn.gaifan.douyinOperations.module.photoavatar.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.ai.service.ImageGenerationService;
import cn.gaifan.douyinOperations.module.ai.service.ImageGenerationService.ImageToImageRequest;
import cn.gaifan.douyinOperations.module.photoavatar.entity.PhotoAvatarTask;
import cn.gaifan.douyinOperations.module.photoavatar.repository.PhotoAvatarTaskRepository;
import cn.gaifan.douyinOperations.module.photoavatar.service.PhotoAvatarService;
import cn.gaifan.douyinOperations.module.photoavatar.vo.PhotoAvatarSaveVO;
import cn.gaifan.douyinOperations.module.photoavatar.vo.PhotoAvatarSearchVO;
import cn.gaifan.douyinOperations.module.photoavatar.vo.PhotoAvatarTaskVO;
import cn.gaifan.douyinOperations.contract.product.FeatureCode;
import cn.gaifan.douyinOperations.contract.product.ProductCode;
import cn.gaifan.douyinOperations.module.platform.credit.CommercialProductChargeService;
import cn.gaifan.douyinOperations.module.platform.product.DeliveryProduct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import jakarta.persistence.criteria.Predicate;
import java.sql.Timestamp;
import java.util.*;

@Service
public class PhotoAvatarServiceImpl implements PhotoAvatarService {

    private static final Logger log = LoggerFactory.getLogger(PhotoAvatarServiceImpl.class);
    private static final Set<String> SORTABLE = Set.of("id", "status", "outfitStyle", "background", "createTime");

    @Resource
    private PhotoAvatarTaskRepository repo;

    @Autowired(required = false)
    private ImageGenerationService imageGenerationService;

    @Autowired(required = false)
    private CommercialProductChargeService commercialProductChargeService;

    @Override
    public Map<String, Object> overview(Long userId) {
        List<PhotoAvatarTask> tasks = repo.findByUserIdAndDeletedOrderByCreateTimeDesc(userId, 0);
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (PhotoAvatarTask t : tasks) {
            byStatus.merge(t.getStatus(), 1L, Long::sum);
        }
        return Map.of(
                "productCode", "photo-avatar-video",
                "taskCount", tasks.size(),
                "byStatus", byStatus
        );
    }

    @Override
    public PageResultVO<PhotoAvatarTaskVO> search(PhotoAvatarSearchVO vo, Long userId) {
        vo.validateParams();
        String sortName = SORTABLE.contains(vo.getSortName()) ? vo.getSortName() : "createTime";
        Pageable pageable = PageRequest.of(vo.getPage(), vo.getRows(),
                Sort.by("desc".equalsIgnoreCase(vo.getSortOrder()) ? Sort.Direction.DESC : Sort.Direction.ASC, sortName));

        Specification<PhotoAvatarTask> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("userId"), userId));
            predicates.add(cb.equal(root.get("deleted"), 0));
            if (vo.getStatus() != null && !vo.getStatus().isBlank()) {
                predicates.add(cb.equal(root.get("status"), vo.getStatus().trim()));
            }
            if (vo.getOutfitStyle() != null && !vo.getOutfitStyle().isBlank()) {
                predicates.add(cb.equal(root.get("outfitStyle"), vo.getOutfitStyle().trim()));
            }
            if (vo.getBackground() != null && !vo.getBackground().isBlank()) {
                predicates.add(cb.equal(root.get("background"), vo.getBackground().trim()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<PhotoAvatarTask> page = repo.findAll(spec, pageable);
        return PageResultVO.of(page.getTotalElements(),
                page.getContent().stream().map(this::toVO).toList(),
                vo.getPage(), vo.getRows());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createVideo(PhotoAvatarSaveVO saveVO, Long userId) {
        if (saveVO.getPortraitConsentConfirmed() == null || !saveVO.getPortraitConsentConfirmed()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "请先确认肖像权授权");
        }
        if (commercialProductChargeService != null) {
            commercialProductChargeService.charge(
                    CommercialProductChargeService.CommercialProductChargeCommand.of(
                            ProductCode.PHOTO_AVATAR_VIDEO,
                            FeatureCode.PHOTO_AVATAR_GENERATE,
                            "照片口播任务 userId=" + userId
                                    + (saveVO.getInsightReportId() != null ? " insightReportId=" + saveVO.getInsightReportId() : ""),
                            DeliveryProduct.PHOTO_AVATAR_VIDEO
                    ));
        }
        PhotoAvatarTask task = new PhotoAvatarTask();
        task.setUserId(userId);
        task.setPhotoUrl(saveVO.getPhotoUrl());
        task.setOutfitStyle(saveVO.getOutfitStyle() != null ? saveVO.getOutfitStyle() : "casual");
        task.setBackground(saveVO.getBackground() != null ? saveVO.getBackground() : "studio");
        task.setStatus("pending");
        task.setCostCredits(80L);
        task.setProgress(0);
        task.setCreateTime(new Timestamp(System.currentTimeMillis()));
        task = repo.save(task);

        // 异步生成（简化：同步调用 imageGenerationService）
        if (imageGenerationService != null) {
            try {
                task.setStatus("processing");
                task.setProgress(30);
                repo.save(task);

                String prompt = buildImagePrompt(task.getOutfitStyle(), task.getBackground());
                ImageToImageRequest req = new ImageToImageRequest(
                        task.getPhotoUrl(), prompt, null, 0.6, 20, 7.0);
                var result = imageGenerationService.imageToImage(req, userId);
                task.setOutputUrl(result.imageUrl());
                task.setStatus("completed");
                task.setProgress(100);
            } catch (Exception e) {
                log.error("Photo avatar generation failed for task {}", task.getId(), e);
                task.setStatus("failed");
                task.setErrorMessage(e.getMessage() != null ? e.getMessage().substring(0, Math.min(e.getMessage().length(), 500)) : "Unknown error");
                task.setProgress(0);
            }
        } else {
            // 无生成服务时仅记录任务
            task.setStatus("pending");
        }
        return repo.save(task).getId();
    }

    @Override
    public PhotoAvatarTaskVO getStatus(Long taskId, Long userId) {
        PhotoAvatarTask task = repo.findById(taskId)
                .filter(t -> t.getDeleted() == 0 && t.getUserId().equals(userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "照片转视频任务不存在"));
        return toVO(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long taskId, Long userId) {
        PhotoAvatarTask task = repo.findById(taskId)
                .filter(t -> t.getDeleted() == 0 && t.getUserId().equals(userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "照片转视频任务不存在"));
        task.setDeleted(1);
        repo.save(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long retry(Long taskId, Long userId) {
        PhotoAvatarTask task = repo.findById(taskId)
                .filter(t -> t.getDeleted() == 0 && t.getUserId().equals(userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "照片转视频任务不存在"));
        if (!"failed".equals(task.getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED, "仅失败任务可重试");
        }
        task.setStatus("pending");
        task.setErrorMessage(null);
        task.setProgress(0);
        task = repo.save(task);

        if (imageGenerationService != null) {
            try {
                task.setStatus("processing");
                task.setProgress(30);
                repo.save(task);

                String prompt = buildImagePrompt(task.getOutfitStyle(), task.getBackground());
                ImageToImageRequest req = new ImageToImageRequest(
                        task.getPhotoUrl(), prompt, null, 0.6, 20, 7.0);
                var result = imageGenerationService.imageToImage(req, userId);
                task.setOutputUrl(result.imageUrl());
                task.setStatus("completed");
                task.setProgress(100);
            } catch (Exception e) {
                log.error("Photo avatar retry failed for task {}", task.getId(), e);
                task.setStatus("failed");
                task.setErrorMessage(e.getMessage() != null ? e.getMessage().substring(0, Math.min(e.getMessage().length(), 500)) : "Unknown error");
                task.setProgress(0);
            }
        }
        return repo.save(task).getId();
    }

    private String buildImagePrompt(String outfitStyle, String background) {
        String outfit = outfitStyle != null ? outfitStyle : "casual";
        String bg = background != null ? background : "studio";
        return String.format(
                "A high-quality portrait photo of a person wearing %s style outfit, with %s background, professional lighting, photorealistic, 8K",
                outfit, bg);
    }

    private PhotoAvatarTaskVO toVO(PhotoAvatarTask entity) {
        PhotoAvatarTaskVO vo = new PhotoAvatarTaskVO();
        vo.setId(entity.getId());
        vo.setUserId(entity.getUserId());
        vo.setPhotoUrl(entity.getPhotoUrl());
        vo.setOutfitStyle(entity.getOutfitStyle());
        vo.setBackground(entity.getBackground());
        vo.setStatus(entity.getStatus());
        vo.setOutputUrl(entity.getOutputUrl());
        vo.setErrorMessage(entity.getErrorMessage());
        vo.setProgress(entity.getProgress());
        vo.setCostCredits(entity.getCostCredits());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }
}
