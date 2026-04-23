package cn.gaifan.douyinOperations.module.shortvideo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * LF-05 垂类爆款采集配置，绑定 {@code app.shortvideo.vertical-collector.*}
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.shortvideo.vertical-collector")
public class ShortVideoVerticalCollectorProperties {

    private List<String> industryKeywords = new ArrayList<>(List.of(
            "护肤", "彩妆", "美妆", "面膜", "精华", "粉底", "口红", "防晒",
            "眼影", "底妆", "化妆", "卸妆", "美白", "抗老", "祛痘", "保湿"
    ));

    private long minHeatScore = 50L;

    private int batchLimit = 30;

    private boolean visionAnalysisEnabled = true;

    private String visionModelName = "gpt-4o";
}
