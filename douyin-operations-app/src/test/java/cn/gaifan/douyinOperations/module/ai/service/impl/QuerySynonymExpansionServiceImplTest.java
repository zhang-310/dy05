package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.config.SearchSynonymsProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class QuerySynonymExpansionServiceImplTest {

    @InjectMocks
    private QuerySynonymExpansionServiceImpl service;

    @Mock
    private SearchSynonymsProperties props;

    @BeforeEach
    void setUp() {
        when(props.isEnabled()).thenReturn(true);
        when(props.getMaxExtraQueries()).thenReturn(6);
        List<SearchSynonymsProperties.Group> groups = new ArrayList<>();
        SearchSynonymsProperties.Group g1 = new SearchSynonymsProperties.Group();
        g1.setTerms(List.of("玻尿酸", "透明质酸", "HA"));
        groups.add(g1);
        when(props.getGroups()).thenReturn(groups);
    }

    @Test
    @DisplayName("命中同义词组时追加替换变体")
    void expandsMatchedGroup() {
        List<String> out = service.expandQueries(List.of("玻尿酸保湿成分"));
        assertThat(out).contains("透明质酸保湿成分", "HA保湿成分");
        assertThat(out.get(0)).isEqualTo("玻尿酸保湿成分");
    }

    @Test
    @DisplayName("关闭时不追加")
    void disabled_noop() {
        when(props.isEnabled()).thenReturn(false);
        List<String> in = List.of("玻尿酸保湿");
        assertThat(service.expandQueries(in)).containsExactlyElementsOf(in);
    }

    @Test
    @DisplayName("尊重 max-extra-queries")
    void respectsCap() {
        when(props.getMaxExtraQueries()).thenReturn(1);
        List<String> out = service.expandQueries(List.of("玻尿酸保湿成分"));
        assertThat(out).hasSize(2);
    }
}
