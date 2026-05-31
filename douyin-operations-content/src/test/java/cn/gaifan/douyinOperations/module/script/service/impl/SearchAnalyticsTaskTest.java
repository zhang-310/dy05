package cn.gaifan.douyinOperations.module.script.service.impl;

import cn.gaifan.douyinOperations.module.script.service.SearchSuggestionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SearchAnalyticsTaskTest {

    @Mock
    private SearchSuggestionService searchSuggestionService;

    @InjectMocks
    private SearchAnalyticsTask task;

    @Test
    void updateSearchSuggestions_shouldSkipWhenDisabled() {
        ReflectionTestUtils.setField(task, "schedulerEnabled", false);

        task.updateSearchSuggestions();

        verify(searchSuggestionService, never()).updateSuggestions();
    }

    @Test
    void updateTrendingScores_shouldSkipWhenDisabled() {
        ReflectionTestUtils.setField(task, "schedulerEnabled", false);

        task.updateTrendingScores();

        verify(searchSuggestionService, never()).updateTrendingScores();
    }
}
