package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.douyin.entity.DouyinAccount;
import cn.gaifan.douyinOperations.module.douyin.repository.DouyinAccountRepository;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvPublishTimeAnalysis;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvPublishTimeAnalysisRepository;
import cn.gaifan.douyinOperations.module.shortvideo.service.PublishTimeRecommendationService;
import jakarta.annotation.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 发布时间推荐 Service 实现
 */
@Service
public class PublishTimeRecommendationServiceImpl implements PublishTimeRecommendationService {

    @Resource
    private SvPublishTimeAnalysisRepository publishTimeAnalysisRepository;

    @Resource
    private DouyinAccountRepository douyinAccountRepository;

    @Override
    public List<Map<String, Object>> getRecommendedTimes(Long accountId, List<Long> visibleOwnerIds) {
        if (accountId == null) throw new BusinessException(ErrorCode.VALIDATION_FAIL, "accountId 不能为空");
        if (visibleOwnerIds != null && !visibleOwnerIds.isEmpty()) {
            DouyinAccount account = douyinAccountRepository.findByIdAndDeleted(accountId, 0)
                    .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "账号不存在"));
            if (!visibleOwnerIds.contains(account.getOwnerId())) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权限查看该账号的发布时间分析");
            }
        }
        List<SvPublishTimeAnalysis> list = publishTimeAnalysisRepository
                .findByAccountIdAndRecommendedOrderByAvgViewCountDesc(accountId, true);
        List<Map<String, Object>> result = new ArrayList<>();
        for (SvPublishTimeAnalysis a : list) {
            result.add(Map.of(
                    "dayOfWeek", a.getDayOfWeek() != null ? a.getDayOfWeek() : 0,
                    "hourOfDay", a.getHourOfDay() != null ? a.getHourOfDay() : 0,
                    "avgViewCount", a.getAvgViewCount() != null ? a.getAvgViewCount() : 0L,
                    "videoCount", a.getVideoCount() != null ? a.getVideoCount() : 0,
                    "label", formatLabel(a.getDayOfWeek(), a.getHourOfDay())
            ));
        }
        if (result.isEmpty()) {
            list = publishTimeAnalysisRepository.findByAccountIdOrderByAvgViewCountDesc(accountId, PageRequest.of(0, 5));
            for (SvPublishTimeAnalysis a : list) {
                result.add(Map.of(
                        "dayOfWeek", a.getDayOfWeek() != null ? a.getDayOfWeek() : 0,
                        "hourOfDay", a.getHourOfDay() != null ? a.getHourOfDay() : 0,
                        "avgViewCount", a.getAvgViewCount() != null ? a.getAvgViewCount() : 0L,
                        "videoCount", a.getVideoCount() != null ? a.getVideoCount() : 0,
                        "label", formatLabel(a.getDayOfWeek(), a.getHourOfDay())
                ));
            }
        }
        return result;
    }

    private static final String[] WEEK_NAMES = {"", "周一", "周二", "周三", "周四", "周五", "周六", "周日"};

    private String formatLabel(Integer dayOfWeek, Integer hourOfDay) {
        String day = (dayOfWeek != null && dayOfWeek >= 1 && dayOfWeek <= 7) ? WEEK_NAMES[dayOfWeek] : "周?";
        int h = (hourOfDay != null ? hourOfDay : 0);
        return day + " " + h + ":00";
    }
}
