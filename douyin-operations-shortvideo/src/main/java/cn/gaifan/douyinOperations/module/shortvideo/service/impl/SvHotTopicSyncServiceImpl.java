package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.module.guiguiya.client.GuiguiyaHotClient;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvHotTopic;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvHotTopicRepository;
import cn.gaifan.douyinOperations.module.shortvideo.service.SvHotTopicSyncService;
import cn.gaifan.douyinOperations.module.tianapi.service.TianApiService;
import cn.gaifan.douyinOperations.module.tianapi.vo.HotItemVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Optional;

/**
 * 热点话题同步：抖音热搜（鬼鬼鸭/TianAPI）→ sv_hot_topic
 */
@Service
public class SvHotTopicSyncServiceImpl implements SvHotTopicSyncService {

    private static final Logger log = LoggerFactory.getLogger(SvHotTopicSyncServiceImpl.class);
    private static final String SOURCE_DOUYIN_HOT = "douyin_hot";

    @Resource
    private TianApiService tianApiService;
    @Resource
    private SvHotTopicRepository svHotTopicRepository;

    @Value("${app.hot-topic.sync-max-items:50}")
    private int maxItems = 50;

    @Override
    public int syncFromDouyinHot() {
        if (tianApiService == null) return 0;
        List<HotItemVO> list = tianApiService.douyinHot();
        if (list == null || list.isEmpty()) return 0;

        int synced = 0;
        int limit = Math.min(list.size(), maxItems);
        for (int i = 0; i < limit; i++) {
            HotItemVO item = list.get(i);
            String hotId = item.getLink() != null ? GuiguiyaHotClient.extractHotId(item.getLink()) : null;
            if (hotId == null) hotId = "tianapi_" + String.valueOf(item.getWord() != null ? item.getWord().hashCode() : i).replace("-", "n");

            try {
                Optional<SvHotTopic> existing = svHotTopicRepository.findByDouyinHotId(hotId);
                SvHotTopic topic;
                if (existing.isPresent()) {
                    topic = existing.get();
                    topic.setTitle(crop(item.getWord(), 256));
                    topic.setHeatScore(item.getHotIndex() != null ? item.getHotIndex() : 0L);
                    topic.setStatus("active");
                } else {
                    topic = new SvHotTopic();
                    topic.setSource(SOURCE_DOUYIN_HOT);
                    topic.setDouyinHotId(hotId);
                    topic.setTitle(crop(item.getWord(), 256));
                    topic.setHeatScore(item.getHotIndex() != null ? item.getHotIndex() : 0L);
                    topic.setStatus("active");
                }
                svHotTopicRepository.save(topic);
                synced++;
            } catch (Exception e) {
                log.warn("同步热点失败: word={}, hotId={}, err={}", item.getWord(), hotId, e.getMessage());
            }
        }
        log.info("[HotTopicSync] 抖音热搜同步完成: {} 条", synced);
        return synced;
    }

    private static String crop(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen);
    }
}
