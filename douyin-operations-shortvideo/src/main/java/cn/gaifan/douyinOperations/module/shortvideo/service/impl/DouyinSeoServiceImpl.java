package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.module.shortvideo.service.DouyinSeoService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 抖音 SEO 服务实现 (Phase 8)
 * 占位实现，后续可接入 LLM/数据分析
 */
@Service
public class DouyinSeoServiceImpl implements DouyinSeoService {

    @Override
    public List<String> suggestTags(String title, String description, String industry) {
        List<String> tags = new ArrayList<>();
        if (title != null && !title.isBlank()) {
            for (String w : title.split("[\\s#]+")) {
                if (w.length() >= 2 && w.length() <= 8) tags.add(w);
            }
        }
        if (industry != null && !industry.isBlank()) tags.add(industry);
        return tags.isEmpty() ? List.of("短视频", "抖音") : tags.subList(0, Math.min(5, tags.size()));
    }

    @Override
    public List<String> suggestPublishTime(Long accountId) {
        return List.of("18:00-20:00", "12:00-13:00", "21:00-23:00");
    }

    @Override
    public String suggestCover(List<String> frameUrls) {
        return (frameUrls != null && !frameUrls.isEmpty()) ? frameUrls.get(0) : null;
    }

    @Override
    public List<String> suggestAbTestTitles(String baseTitle) {
        return baseTitle != null ? List.of(baseTitle, baseTitle + "｜必看") : List.of();
    }
}
