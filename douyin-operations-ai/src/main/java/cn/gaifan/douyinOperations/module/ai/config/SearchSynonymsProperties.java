package cn.gaifan.douyinOperations.module.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * 知识库检索同义词扩展（S-3）：按组配置领域等价词，在子查询改写之后注入有限变体，参与混合检索 RRF。
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.ai.search.synonyms")
public class SearchSynonymsProperties {

    private boolean enabled = true;

    /** 相对「改写后」列表额外注入的变体条数上限（避免子查询爆炸） */
    private int maxExtraQueries = 6;

    private List<Group> groups = new ArrayList<>();

    @Data
    public static class Group {
        private List<String> terms = new ArrayList<>();
    }
}
