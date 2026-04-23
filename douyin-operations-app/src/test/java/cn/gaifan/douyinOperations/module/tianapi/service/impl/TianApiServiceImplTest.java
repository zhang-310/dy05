package cn.gaifan.douyinOperations.module.tianapi.service.impl;

import cn.gaifan.douyinOperations.module.guiguiya.client.GuiguiyaHotClient;
import cn.gaifan.douyinOperations.module.tianapi.client.TianApiClient;
import cn.gaifan.douyinOperations.module.tianapi.config.TianApiProperties;
import cn.gaifan.douyinOperations.module.tianapi.vo.HotItemVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TianApiServiceImpl 单元测试")
class TianApiServiceImplTest {

    @Mock
    private TianApiClient client;
    @Mock
    private GuiguiyaHotClient guiguiyaHotClient;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private TianApiProperties properties;
    private TianApiServiceImpl service;

    @BeforeEach
    void setUp() {
        properties = new TianApiProperties();
        properties.setEnabled(true);
        properties.setApiKey("test-key");
        properties.setHotCacheMinutes(5);

        service = new TianApiServiceImpl();
        ReflectionTestUtils.setField(service, "client", client);
        ReflectionTestUtils.setField(service, "properties", properties);
        ReflectionTestUtils.setField(service, "guiguiyaHotClient", guiguiyaHotClient);
        ReflectionTestUtils.setField(service, "redisTemplate", null);
    }

    @Test
    void isEnabled_shouldFollowConfiguredState() {
        assertThat(service.isEnabled()).isTrue();

        properties.setApiKey("");
        assertThat(service.isEnabled()).isFalse();
    }

    @Test
    void douyinHot_shouldPreferGuiguiyaWhenAvailable() {
        HotItemVO item = new HotItemVO("修护精华", "1", 100000L, "douyin");
        when(guiguiyaHotClient.fetchDouyinHot()).thenReturn(List.of(item));

        List<HotItemVO> result = service.douyinHot();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getWord()).isEqualTo("修护精华");
        verify(guiguiyaHotClient).fetchDouyinHot();
        verifyNoInteractions(client);
    }

    @Test
    void douyinHot_shouldFallbackToTianApiWhenGuiguiyaReturnsEmpty() throws Exception {
        when(guiguiyaHotClient.fetchDouyinHot()).thenReturn(List.of());
        JsonNode resultNode = objectMapper.readTree("""
            [
              { "word": "护肤热点", "label": 3, "hotindex": 123456 }
            ]
            """);
        when(client.get("douyinhot/index")).thenReturn(resultNode);

        List<HotItemVO> result = service.douyinHot();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getWord()).isEqualTo("护肤热点");
        assertThat(result.get(0).getLabel()).isEqualTo("3");
        assertThat(result.get(0).getHotIndex()).isEqualTo(123456L);
        assertThat(result.get(0).getSource()).isEqualTo("douyin");
        verify(client).get("douyinhot/index");
    }
}
