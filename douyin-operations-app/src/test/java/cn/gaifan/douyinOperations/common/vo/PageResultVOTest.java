package cn.gaifan.douyinOperations.common.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PageResultVO 分页结果测试")
class PageResultVOTest {

    @Test
    void of_shouldCreatePageResult() {
        List<String> data = Arrays.asList("a", "b", "c");
        PageResultVO<String> page = PageResultVO.of(100L, data, 0, 30);

        assertThat(page.getTotal()).isEqualTo(100L);
        assertThat(page.getList()).hasSize(3);
        assertThat(page.getPageNum()).isEqualTo(0);
        assertThat(page.getPageSize()).isEqualTo(30);
    }

    @Test
    void of_withNullList_shouldReturnEmptyList() {
        PageResultVO<String> page = PageResultVO.of(0L, null, 0, 30);
        assertThat(page.getList()).isNotNull().isEmpty();
    }

    @Test
    void of_withEmptyList_shouldReturnEmptyList() {
        PageResultVO<String> page = PageResultVO.of(0L, Collections.emptyList(), 0, 30);
        assertThat(page.getList()).isEmpty();
        assertThat(page.getTotal()).isEqualTo(0L);
    }
}
