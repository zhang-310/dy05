package cn.gaifan.douyinOperations.module.shortvideo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * LF-06 自动二创编排：命中敏感/高风险题材时跳过整条流水线，节省算力与模型调用。
 * 绑定 {@code app.shortvideo.auto-orchestration.content-policy.*}
 * <p>
 * 说明：与「儿童出镜」相关规则区分——本列表侧重新闻/军政国际/政治/明星八卦等不宜自动二创类目；
 * 儿童<strong>直播</strong>禁止见 {@code app.live.content-policy}（短视频拍摄不在直播策略拦截范围内）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.shortvideo.auto-orchestration.content-policy")
public class ShortVideoAutoRemakeContentPolicyProperties {

    private boolean enabled = true;

    /**
     * 在爆款标题、tags、industryTags 拼接文本中命中任一子串（忽略大小写）则跳过自动二创。
     */
    private List<String> skipAutoRemakeSubstrings = new ArrayList<>(List.of(
            "军事", "军情", "军演", "武器", "战场", "战争",
            "外交", "国际关系", "地缘政治", "联合国",
            "时政", "政治新闻", "选举", "国会", "议会",
            "时事新闻", "突发新闻", "热点新闻", "新闻联播",
            "明星绯闻", "娱乐圈", "八卦", "塌房", "丑闻",
            "两岸关系", "俄乌", "巴以", "中东局势",
            "色情", "赌博", "毒品", "恐怖"
    ));
}
