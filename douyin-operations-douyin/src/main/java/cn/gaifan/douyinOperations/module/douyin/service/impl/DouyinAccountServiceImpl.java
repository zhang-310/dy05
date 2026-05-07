package cn.gaifan.douyinOperations.module.douyin.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.douyin.entity.DouyinAccount;
import cn.gaifan.douyinOperations.module.douyin.repository.DouyinAccountRepository;
import cn.gaifan.douyinOperations.module.douyin.repository.DouyinVideoRepository;
import cn.gaifan.douyinOperations.module.douyin.service.DouyinAccountService;
import cn.gaifan.douyinOperations.module.douyin.vo.*;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import jakarta.persistence.criteria.Predicate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 抖音账号服务实现
 */
@Service
public class DouyinAccountServiceImpl implements DouyinAccountService {

    private static final Set<String> SORTABLE_FIELDS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("id", "userId", "createTime", "updateTime", "fanCount", "videoCount", "totalLikes")));

    @Resource
    private DouyinAccountRepository douyinAccountRepository;

    @Resource
    private DouyinVideoRepository douyinVideoRepository;

    @Override
    public PageResultVO<DouyinAccountVO> search(DouyinAccountSearchVO vo) {
        vo.validateParams();
        String sortName = SORTABLE_FIELDS.contains(vo.getSortName()) ? vo.getSortName() : "id";
        Pageable pageable = PageRequest.of(vo.getPage(), vo.getRows(),
                Sort.by("desc".equalsIgnoreCase(vo.getSortOrder()) ? Sort.Direction.DESC : Sort.Direction.ASC, sortName));

        Specification<DouyinAccount> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("deleted"), 0));

            if (vo.getUserId() != null && vo.getUserId() > 0) {
                predicates.add(cb.equal(root.get("userId"), vo.getUserId()));
            } else if (vo.getUserIds() != null && !vo.getUserIds().isEmpty()) {
                predicates.add(root.get("userId").in(vo.getUserIds()));
            }
            if (vo.getAccountName() != null && !vo.getAccountName().trim().isEmpty()) {
                predicates.add(cb.like(root.get("accountName"), "%" + vo.getAccountName().trim() + "%"));
            }
            if (vo.getAccountId() != null && !vo.getAccountId().trim().isEmpty()) {
                predicates.add(cb.like(root.get("accountId"), "%" + vo.getAccountId().trim() + "%"));
            }
            if (vo.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), vo.getStatus()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<DouyinAccount> page = douyinAccountRepository.findAll(spec, pageable);
        List<DouyinAccountVO> list = page.getContent().stream().map(this::toAccountVO).collect(Collectors.toList());
        return PageResultVO.of(page.getTotalElements(), list, vo.getPage(), vo.getRows());
    }

    @Override
    public DouyinAccountVO getAccount(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "账号 ID 无效");
        }
        DouyinAccount account = douyinAccountRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_FAIL, "账号不存在"));
        return toAccountVO(account);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long saveAccount(DouyinAccountSaveVO vo) {
        DouyinAccount account;
        if (vo.getId() != null && vo.getId() > 0) {
            account = douyinAccountRepository.findByIdAndDeleted(vo.getId(), 0)
                    .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_FAIL, "账号不存在"));
        } else {
            if (douyinAccountRepository.existsByAccountIdAndDeleted(vo.getAccountId(), 0)) {
                throw new BusinessException(ErrorCode.VALIDATION_FAIL, "账号 ID 已存在");
            }
            account = new DouyinAccount();
            account.setUserId(vo.getUserId());
            account.setAccountId(vo.getAccountId());
        }
        account.setAccountName(vo.getAccountName());
        if (vo.getFollowCount() != null) account.setFollowCount(vo.getFollowCount());
        if (vo.getFanCount() != null) account.setFanCount(vo.getFanCount());
        if (vo.getVideoCount() != null) account.setVideoCount(vo.getVideoCount());
        if (vo.getTotalLikes() != null) account.setTotalLikes(vo.getTotalLikes());
        if (vo.getDescription() != null) account.setDescription(vo.getDescription());
        if (vo.getStatus() != null) account.setStatus(vo.getStatus());
        account = douyinAccountRepository.save(account);
        return account.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAccount(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "账号 ID 无效");
        }
        DouyinAccount account = douyinAccountRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_FAIL, "账号不存在"));
        account.setDeleted(1);
        douyinAccountRepository.save(account);
    }

    @Override
    @Cacheable(value = "accountStatistics", key = "#id", unless = "#result == null")
    public DouyinAccountStatisticsVO getAccountStatistics(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "账号 ID 无效");
        }
        DouyinAccount account = douyinAccountRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_FAIL, "账号不存在"));

        // 使用聚合查询统计数据，避免加载全部视频到内存
        long totalVideos = douyinVideoRepository.countByAccountIdAndDeleted(id, 0);

        DouyinAccountStatisticsVO stats = new DouyinAccountStatisticsVO();
        stats.setAccountId(id);
        stats.setTotalVideos(totalVideos);

        if (totalVideos == 0) {
            stats.setTotalViews(0L);
            stats.setTotalLikes(0L);
            stats.setTotalShares(0L);
            stats.setTotalComments(0L);
            stats.setTotalDownloads(0L);
            stats.setAvgViewsPerVideo(0.0);
            stats.setAvgLikesPerVideo(0.0);
        } else {
            Long totalViews = douyinVideoRepository.sumViewCountByAccountIdAndDeleted(id, 0);
            Long totalLikes = douyinVideoRepository.sumLikeCountByAccountIdAndDeleted(id, 0);
            Long totalShares = douyinVideoRepository.sumShareCountByAccountIdAndDeleted(id, 0);
            Long totalComments = douyinVideoRepository.sumCommentCountByAccountIdAndDeleted(id, 0);
            Long totalDownloads = douyinVideoRepository.sumDownloadCountByAccountIdAndDeleted(id, 0);

            totalViews = totalViews != null ? totalViews : 0L;
            totalLikes = totalLikes != null ? totalLikes : 0L;
            totalShares = totalShares != null ? totalShares : 0L;
            totalComments = totalComments != null ? totalComments : 0L;
            totalDownloads = totalDownloads != null ? totalDownloads : 0L;

            stats.setTotalViews(totalViews);
            stats.setTotalLikes(totalLikes);
            stats.setTotalShares(totalShares);
            stats.setTotalComments(totalComments);
            stats.setTotalDownloads(totalDownloads);
            stats.setAvgViewsPerVideo((double) totalViews / totalVideos);
            stats.setAvgLikesPerVideo((double) totalLikes / totalVideos);
        }

        return stats;
    }

    private DouyinAccountVO toAccountVO(DouyinAccount account) {
        DouyinAccountVO vo = new DouyinAccountVO();
        vo.setId(account.getId());
        vo.setUserId(account.getUserId());
        vo.setAccountName(account.getAccountName());
        vo.setAccountId(account.getAccountId());
        vo.setFollowCount(account.getFollowCount());
        vo.setFanCount(account.getFanCount());
        vo.setVideoCount(account.getVideoCount());
        vo.setTotalLikes(account.getTotalLikes());
        vo.setDescription(account.getDescription());
        vo.setStatus(account.getStatus());
        vo.setBindTime(account.getBindTime());
        vo.setCreateTime(account.getCreateTime());
        vo.setUpdateTime(account.getUpdateTime());
        return vo;
    }
}
