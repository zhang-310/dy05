package cn.gaifan.douyinOperations.module.guiguiya.client;

import cn.gaifan.douyinOperations.module.guiguiya.config.GuiguiyaProperties;
import cn.gaifan.douyinOperations.module.tianapi.vo.HotItemVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GuiguiyaHotClient 单元测试")
class GuiguiyaHotClientTest {

    @Mock
    private RestTemplate restTemplate;

    private GuiguiyaProperties properties;
    private GuiguiyaHotClient client;

    @BeforeEach
    void setUp() {
        properties = new GuiguiyaProperties();
        properties.setEnabled(true);
        properties.setDouyinHotUrl("http://api.guiguiya.com/api/hotlist/dy");

        client = new GuiguiyaHotClient();
        ReflectionTestUtils.setField(client, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(client, "properties", properties);
    }

    @Test
    void fetchDouyinHot_shouldReturnEmptyWhenDisabled() {
        properties.setEnabled(false);

        List<HotItemVO> result = client.fetchDouyinHot();

        assertThat(result).isEmpty();
        verifyNoInteractions(restTemplate);
    }

    @Test
    void fetchDouyinHot_shouldParseSuccessfulResponse() {
        String body = """
            {
              "code": 200,
              "data": [
                {
                  "word": "修护精华",
                  "position": 1,
                  "hot": 123456,
                  "hot_zh": "12.3万",
                  "vieo_link": "https://www.douyin.com/hot/2428956"
                }
              ]
            }
            """;
        when(restTemplate.getForEntity(properties.getDouyinHotUrl(), String.class))
                .thenReturn(new ResponseEntity<>(body, HttpStatus.OK));

        List<HotItemVO> result = client.fetchDouyinHot();

        assertThat(result).hasSize(1);
        HotItemVO item = result.get(0);
        assertThat(item.getWord()).isEqualTo("修护精华");
        assertThat(item.getPosition()).isEqualTo(1);
        assertThat(item.getHotIndex()).isEqualTo(123456L);
        assertThat(item.getHotZh()).isEqualTo("12.3万");
        assertThat(item.getLink()).isEqualTo("https://www.douyin.com/hot/2428956");
        verify(restTemplate).getForEntity(properties.getDouyinHotUrl(), String.class);
    }

    @Test
    void extractHotId_shouldParseValidLink() {
        assertThat(GuiguiyaHotClient.extractHotId("https://www.douyin.com/hot/2428956")).isEqualTo("2428956");
        assertThat(GuiguiyaHotClient.extractHotId("")).isNull();
        assertThat(GuiguiyaHotClient.extractHotId(null)).isNull();
    }
}
