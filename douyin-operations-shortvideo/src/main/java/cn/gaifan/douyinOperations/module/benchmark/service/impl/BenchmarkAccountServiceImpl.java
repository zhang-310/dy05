package cn.gaifan.douyinOperations.module.benchmark.service.impl;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.benchmark.config.BenchmarkCacheConfig;
import cn.gaifan.douyinOperations.module.benchmark.entity.BenchmarkAccount;
import cn.gaifan.douyinOperations.module.benchmark.metrics.BenchmarkMetrics;
import cn.gaifan.douyinOperations.module.benchmark.repository.BenchmarkAccountRepository;
import cn.gaifan.douyinOperations.module.benchmark.service.BenchmarkAccountService;
import cn.gaifan.douyinOperations.module.benchmark.service.DouyinCookieService;
import cn.gaifan.douyinOperations.module.benchmark.vo.*;
import cn.gaifan.douyinOperations.module.shortvideo.service.impl.AccountVideoScraper;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 对标账号管理服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BenchmarkAccountServiceImpl implements BenchmarkAccountService {

    private final BenchmarkAccountRepository accountRepository;
    private final DouyinCookieService cookieService;
    private final AccountVideoScraper accountVideoScraper;
    private final BenchmarkMetrics metrics;

    @Override
    @Cacheable(value = BenchmarkCacheConfig.CACHE_ACCOUNT_SEARCH,
               key = "#ownerId + ':' + #searchVO.hashCode()",
               unless = "#result == null || #result.total == 0")
    public PageResultVO<BenchmarkAccountVO> search(BenchmarkAccountSearchVO searchVO, Long ownerId) {
        searchVO.validateParams();

        Specification<BenchmarkAccount> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 数据隔离
            predicates.add(cb.equal(root.get("ownerId"), ownerId));

            // 关键词搜索
            if (StringUtils.hasText(searchVO.getKeyword())) {
                String keyword = "%" + searchVO.getKeyword() + "%";
                predicates.add(cb.like(root.get("accountName"), keyword));
            }

            // 平台过滤
            if (StringUtils.hasText(searchVO.getPlatform())) {
                predicates.add(cb.equal(root.get("platform"), searchVO.getPlatform()));
            }

            // 分类过滤
            if (StringUtils.hasText(searchVO.getCategory())) {
                predicates.add(cb.equal(root.get("category"), searchVO.getCategory()));
            }

            // 激活状态过滤
            if (searchVO.getIsActive() != null) {
                predicates.add(cb.equal(root.get("isActive"), searchVO.getIsActive()));
            }

            // 粉丝数范围过滤
            if (searchVO.getMinFanCount() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("fanCount"), searchVO.getMinFanCount()));
            }
            if (searchVO.getMaxFanCount() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("fanCount"), searchVO.getMaxFanCount()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Sort sort = Sort.by(Sort.Direction.DESC, "createTime");
        Pageable pageable = PageRequest.of(searchVO.getPage(), searchVO.getRows(), sort);
        Page<BenchmarkAccount> page = accountRepository.findAll(spec, pageable);

        List<BenchmarkAccountVO> voList = page.getContent().stream()
                .map(this::entityToVO)
                .toList();

        return new PageResultVO<>(page.getTotalElements(), voList, searchVO.getPage(), searchVO.getRows());
    }

    @Override
    @Cacheable(value = BenchmarkCacheConfig.CACHE_ACCOUNT_DETAIL,
               key = "#id + ':' + #ownerId",
               unless = "#result == null")
    public BenchmarkAccountVO getById(Long id, Long ownerId) {
        BenchmarkAccount account = accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("账号不存在"));

        if (!account.getOwnerId().equals(ownerId)) {
            throw new RuntimeException("无权访问该账号");
        }

        return entityToVO(account);
    }

    @Override
    @Transactional
    @CacheEvict(value = {BenchmarkCacheConfig.CACHE_ACCOUNT_SEARCH,
                         BenchmarkCacheConfig.CACHE_ACCOUNT_DETAIL},
                allEntries = true)
    public BenchmarkAccountVO save(BenchmarkAccountSaveVO saveVO, Long ownerId) {
        BenchmarkAccount account;

        if (saveVO.getId() != null) {
            // 更新
            account = accountRepository.findById(saveVO.getId())
                    .orElseThrow(() -> new RuntimeException("账号不存在"));

            if (!account.getOwnerId().equals(ownerId)) {
                throw new RuntimeException("无权修改该账号");
            }
        } else {
            // 新增
            account = new BenchmarkAccount();
            account.setOwnerId(ownerId);
        }

        BeanUtils.copyProperties(saveVO, account, "id", "ownerId");
        account = accountRepository.save(account);

        return entityToVO(account);
    }

    @Override
    @Transactional
    @CacheEvict(value = {BenchmarkCacheConfig.CACHE_ACCOUNT_SEARCH,
                         BenchmarkCacheConfig.CACHE_ACCOUNT_DETAIL},
                allEntries = true)
    public void delete(Long id, Long ownerId) {
        BenchmarkAccount account = accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("账号不存在"));

        if (!account.getOwnerId().equals(ownerId)) {
            throw new RuntimeException("无权删除该账号");
        }

        account.setDeleted(1);
        accountRepository.save(account);
    }

    @Override
    @Transactional
    public List<BenchmarkAccountVO> searchByKeyword(SearchAccountByKeywordVO searchVO, Long ownerId) {
        log.info("按关键词搜索账号: keyword={}, minFanCount={}", searchVO.getKeyword(), searchVO.getMinFanCount());
        long startTime = System.currentTimeMillis();

        if (!accountVideoScraper.isAvailable()) {
            metrics.recordPlaywrightError("searchByKeyword", "unavailable");
            throw new RuntimeException("Playwright 不可用，无法搜索账号");
        }

        // 获取可用Cookie
        String cookie = getCookieValue(searchVO.getCookieId(), ownerId);
        if (cookie == null) {
            metrics.recordAccountSearch("douyin", false);
            throw new RuntimeException("没有可用的Cookie，请先添加Cookie");
        }

        List<BenchmarkAccountVO> results = new ArrayList<>();

        try {
            // 使用 Playwright 搜索账号
            String accountUrl = accountVideoScraper.searchDouyinAccount(searchVO.getKeyword(), ownerId);

            if (accountUrl != null) {
                // 抓取账号详细信息
                AccountVideoScraper.ScrapeResult scrapeResult = accountVideoScraper.scrapeAccountVideos(accountUrl, ownerId);

                if (scrapeResult != null && scrapeResult.getAccountName() != null) {
                    // 检查是否已存在
                    BenchmarkAccount existingAccount = accountRepository.findBySecUidAndOwnerId(
                            scrapeResult.getSecUid(), ownerId);

                    if (existingAccount == null) {
                        // 创建新账号
                        BenchmarkAccount account = new BenchmarkAccount();
                        account.setOwnerId(ownerId);
                        account.setAccountName(scrapeResult.getAccountName());
                        account.setAccountUrl(accountUrl);
                        account.setSecUid(scrapeResult.getSecUid());
                        account.setPlatform("douyin");
                        account.setVideoCount(scrapeResult.getVideos().size());

                        // 计算平均点赞数
                        if (!scrapeResult.getVideos().isEmpty()) {
                            long totalLikes = scrapeResult.getVideos().stream()
                                    .filter(v -> v.getLikeCount() != null)
                                    .mapToLong(AccountVideoScraper.ScrapedVideo::getLikeCount)
                                    .sum();
                            account.setAvgLikeCount((int) (totalLikes / scrapeResult.getVideos().size()));
                        }

                        account = accountRepository.save(account);
                        results.add(entityToVO(account));

                        metrics.recordAccountCreated("douyin");
                        log.info("保存新账号: {}", account.getAccountName());
                    } else {
                        results.add(entityToVO(existingAccount));
                        log.info("账号已存在: {}", existingAccount.getAccountName());
                    }
                }
            }

            metrics.recordAccountSearch("douyin", true);
            metrics.recordAccountSearchDuration("douyin", System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            metrics.recordAccountSearch("douyin", false);
            metrics.recordPlaywrightError("searchByKeyword", e.getClass().getSimpleName());
            log.error("搜索账号失败: {}", e.getMessage(), e);
            throw new RuntimeException("搜索账号失败: " + e.getMessage());
        }

        log.info("搜索到 {} 个符合条件的账号", results.size());
        return results;
    }

    @Override
    @Transactional
    public BenchmarkAccountVO analyzeByUrl(AnalyzeAccountByUrlVO analyzeVO, Long ownerId) {
        log.info("按URL分析账号: url={}", analyzeVO.getAccountUrl());

        if (!accountVideoScraper.isAvailable()) {
            throw new RuntimeException("Playwright 不可用，无法分析账号");
        }

        // 获取可用Cookie
        String cookie = getCookieValue(analyzeVO.getCookieId(), ownerId);
        if (cookie == null) {
            throw new RuntimeException("没有可用的Cookie，请先添加Cookie");
        }

        try {
            // 提取 sec_uid
            String secUid = accountVideoScraper.extractSecUid(analyzeVO.getAccountUrl());
            if (secUid == null) {
                throw new RuntimeException("无法从URL提取sec_uid");
            }

            // 检查是否已存在
            BenchmarkAccount existingAccount = accountRepository.findBySecUidAndOwnerId(secUid, ownerId);
            if (existingAccount != null) {
                log.info("账号已存在，返回现有记录: {}", existingAccount.getAccountName());
                return entityToVO(existingAccount);
            }

            // 抓取账号信息
            AccountVideoScraper.ScrapeResult scrapeResult =
                    accountVideoScraper.scrapeAccountVideos(analyzeVO.getAccountUrl(), ownerId);

            if (scrapeResult == null || scrapeResult.getAccountName() == null) {
                throw new RuntimeException("无法获取账号信息");
            }

            // 保存账号信息
            BenchmarkAccount account = new BenchmarkAccount();
            account.setOwnerId(ownerId);
            account.setAccountName(scrapeResult.getAccountName());
            account.setAccountUrl(analyzeVO.getAccountUrl());
            account.setSecUid(scrapeResult.getSecUid());
            account.setPlatform("douyin");
            account.setVideoCount(scrapeResult.getVideos().size());

            // 计算平均点赞数
            if (!scrapeResult.getVideos().isEmpty()) {
                long totalLikes = scrapeResult.getVideos().stream()
                        .filter(v -> v.getLikeCount() != null)
                        .mapToLong(AccountVideoScraper.ScrapedVideo::getLikeCount)
                        .sum();
                account.setAvgLikeCount((int) (totalLikes / scrapeResult.getVideos().size()));
            }

            account.setLastCollectTime(LocalDateTime.now());
            account = accountRepository.save(account);

            log.info("成功分析并保存账号: {}, 视频数: {}", account.getAccountName(), account.getVideoCount());
            return entityToVO(account);

        } catch (Exception e) {
            log.error("分析账号失败: {}", e.getMessage(), e);
            throw new RuntimeException("分析账号失败: " + e.getMessage());
        }
    }

    /**
     * 获取Cookie值
     */
    private String getCookieValue(Long cookieId, Long ownerId) {
        if (cookieId != null) {
            DouyinCookieVO cookie = cookieService.getById(cookieId, ownerId);
            return cookie.getCookieValue();
        } else {
            return cookieService.getAvailableCookie(ownerId, "douyin");
        }
    }

    private BenchmarkAccountVO entityToVO(BenchmarkAccount entity) {
        BenchmarkAccountVO vo = new BenchmarkAccountVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
