package cn.gaifan.douyinOperations.module.agent.service;

import cn.gaifan.douyinOperations.module.agent.entity.AgentUserPreference;
import cn.gaifan.douyinOperations.module.agent.repository.AgentUserPreferenceRepository;
import cn.gaifan.douyinOperations.module.agent.service.impl.UserPreferenceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserPreferenceService 单元测试")
class UserPreferenceServiceImplTest {

    @Mock
    private AgentUserPreferenceRepository repository;

    @InjectMocks
    private UserPreferenceServiceImpl userPreferenceService;

    private Long userId = 1L;
    private AgentUserPreference mockPreference;

    @BeforeEach
    void setUp() {
        mockPreference = new AgentUserPreference();
        mockPreference.setId(1L);
        mockPreference.setUserId(userId);
        mockPreference.setPrefKey("tone");
        mockPreference.setPrefValue("professional");
        mockPreference.setUsageCount(1);
        mockPreference.setLastUsedAt(new Timestamp(System.currentTimeMillis()));
        mockPreference.setDeleted(0);
    }

    @Test
    @DisplayName("记录偏好 - 新偏好应创建记录")
    void record_newPreference_shouldCreateRecord() {
        // Given
        when(repository.findByUserIdAndPrefKeyAndPrefValueAndDeleted(userId, "tone", "professional", 0))
                .thenReturn(Optional.empty());
        when(repository.save(any(AgentUserPreference.class)))
                .thenReturn(mockPreference);

        // When
        userPreferenceService.record(userId, "tone", "professional");

        // Then
        verify(repository).findByUserIdAndPrefKeyAndPrefValueAndDeleted(userId, "tone", "professional", 0);
        verify(repository).save(argThat(pref ->
                pref.getUserId().equals(userId) &&
                pref.getPrefKey().equals("tone") &&
                pref.getPrefValue().equals("professional") &&
                pref.getUsageCount() == 1
        ));
    }

    @Test
    @DisplayName("记录偏好 - 已存在偏好应增加使用次数")
    void record_existingPreference_shouldIncrementUsageCount() {
        // Given
        when(repository.findByUserIdAndPrefKeyAndPrefValueAndDeleted(userId, "tone", "professional", 0))
                .thenReturn(Optional.of(mockPreference));
        when(repository.save(any(AgentUserPreference.class)))
                .thenReturn(mockPreference);

        // When
        userPreferenceService.record(userId, "tone", "professional");

        // Then
        verify(repository).findByUserIdAndPrefKeyAndPrefValueAndDeleted(userId, "tone", "professional", 0);
        verify(repository).save(argThat(pref ->
                pref.getUsageCount() == 2 &&
                pref.getLastUsedAt() != null
        ));
    }

    @Test
    @DisplayName("记录偏好 - userId 为 null 应忽略")
    void record_nullUserId_shouldIgnore() {
        // When
        userPreferenceService.record(null, "tone", "professional");

        // Then
        verify(repository, never()).findByUserIdAndPrefKeyAndPrefValueAndDeleted(anyLong(), anyString(), anyString(), anyInt());
        verify(repository, never()).save(any(AgentUserPreference.class));
    }

    @Test
    @DisplayName("记录偏好 - key 为 null 应忽略")
    void record_nullKey_shouldIgnore() {
        // When
        userPreferenceService.record(userId, null, "professional");

        // Then
        verify(repository, never()).findByUserIdAndPrefKeyAndPrefValueAndDeleted(anyLong(), anyString(), anyString(), anyInt());
        verify(repository, never()).save(any(AgentUserPreference.class));
    }

    @Test
    @DisplayName("记录偏好 - value 为 null 应忽略")
    void record_nullValue_shouldIgnore() {
        // When
        userPreferenceService.record(userId, "tone", null);

        // Then
        verify(repository, never()).findByUserIdAndPrefKeyAndPrefValueAndDeleted(anyLong(), anyString(), anyString(), anyInt());
        verify(repository, never()).save(any(AgentUserPreference.class));
    }

    @Test
    @DisplayName("记录偏好 - key 为空字符串应忽略")
    void record_blankKey_shouldIgnore() {
        // When
        userPreferenceService.record(userId, "  ", "professional");

        // Then
        verify(repository, never()).findByUserIdAndPrefKeyAndPrefValueAndDeleted(anyLong(), anyString(), anyString(), anyInt());
        verify(repository, never()).save(any(AgentUserPreference.class));
    }

    @Test
    @DisplayName("记录偏好 - value 为空字符串应忽略")
    void record_blankValue_shouldIgnore() {
        // When
        userPreferenceService.record(userId, "tone", "  ");

        // Then
        verify(repository, never()).findByUserIdAndPrefKeyAndPrefValueAndDeleted(anyLong(), anyString(), anyString(), anyInt());
        verify(repository, never()).save(any(AgentUserPreference.class));
    }

    @Test
    @DisplayName("获取热门偏好 - 应返回偏好列表")
    void getTopPreferences_shouldReturnPreferenceList() {
        // Given
        AgentUserPreference pref1 = new AgentUserPreference();
        pref1.setPrefValue("professional");
        pref1.setUsageCount(10);

        AgentUserPreference pref2 = new AgentUserPreference();
        pref2.setPrefValue("casual");
        pref2.setUsageCount(5);

        when(repository.findTopByUserIdAndKey(eq(userId), eq("tone"), any(PageRequest.class)))
                .thenReturn(List.of(pref1, pref2));

        // When
        List<String> result = userPreferenceService.getTopPreferences(userId, "tone", 5);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly("professional", "casual");
        verify(repository).findTopByUserIdAndKey(eq(userId), eq("tone"), any(PageRequest.class));
    }

    @Test
    @DisplayName("获取热门偏好 - userId 为 null 应返回空列表")
    void getTopPreferences_nullUserId_shouldReturnEmptyList() {
        // When
        List<String> result = userPreferenceService.getTopPreferences(null, "tone", 5);

        // Then
        assertThat(result).isEmpty();
        verify(repository, never()).findTopByUserIdAndKey(anyLong(), anyString(), any(PageRequest.class));
    }

    @Test
    @DisplayName("获取热门偏好 - key 为 null 应返回空列表")
    void getTopPreferences_nullKey_shouldReturnEmptyList() {
        // When
        List<String> result = userPreferenceService.getTopPreferences(userId, null, 5);

        // Then
        assertThat(result).isEmpty();
        verify(repository, never()).findTopByUserIdAndKey(anyLong(), anyString(), any(PageRequest.class));
    }

    @Test
    @DisplayName("获取热门偏好 - limit 为 0 应返回空列表")
    void getTopPreferences_zeroLimit_shouldReturnEmptyList() {
        // When
        List<String> result = userPreferenceService.getTopPreferences(userId, "tone", 0);

        // Then
        assertThat(result).isEmpty();
        verify(repository, never()).findTopByUserIdAndKey(anyLong(), anyString(), any(PageRequest.class));
    }

    @Test
    @DisplayName("获取热门偏好 - limit 为负数应返回空列表")
    void getTopPreferences_negativeLimit_shouldReturnEmptyList() {
        // When
        List<String> result = userPreferenceService.getTopPreferences(userId, "tone", -1);

        // Then
        assertThat(result).isEmpty();
        verify(repository, never()).findTopByUserIdAndKey(anyLong(), anyString(), any(PageRequest.class));
    }

    @Test
    @DisplayName("获取热门偏好 - 无数据应返回空列表")
    void getTopPreferences_noData_shouldReturnEmptyList() {
        // Given
        when(repository.findTopByUserIdAndKey(eq(userId), eq("tone"), any(PageRequest.class)))
                .thenReturn(List.of());

        // When
        List<String> result = userPreferenceService.getTopPreferences(userId, "tone", 5);

        // Then
        assertThat(result).isEmpty();
        verify(repository).findTopByUserIdAndKey(eq(userId), eq("tone"), any(PageRequest.class));
    }
}
