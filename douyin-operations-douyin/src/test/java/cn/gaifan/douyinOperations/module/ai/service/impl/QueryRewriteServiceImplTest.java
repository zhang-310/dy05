package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.entity.AiTaskModelConfig;
import cn.gaifan.douyinOperations.module.ai.repository.AiModelRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiTaskModelConfigRepository;
import cn.gaifan.douyinOperations.module.ai.service.LlmClient;
import cn.gaifan.douyinOperations.module.douyin.entity.DouyinAccount;
import cn.gaifan.douyinOperations.module.douyin.repository.DouyinAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("QueryRewriteServiceImpl 查询改写服务测试")
class QueryRewriteServiceImplTest {

    @InjectMocks
    private QueryRewriteServiceImpl queryRewriteService;

    @Mock
    private DouyinAccountRepository accountRepository;

    @Mock
    private AiModelRepository modelRepository;

    @Mock
    private AiTaskModelConfigRepository taskModelConfigRepository;

    @Mock
    private LlmClient llmClient;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    @BeforeEach
    void setUp() {
        org.springframework.test.util.ReflectionTestUtils.setField(queryRewriteService, "rewriteEnabled", true);
        org.springframework.test.util.ReflectionTestUtils.setField(queryRewriteService, "maxSubQueries", 3);
    }

    @Nested
    @DisplayName("rewrite 输入校验")
    class InputValidationTests {

        @Test
        void rewrite_nullUserId_shouldReturnOriginalQuery() {
            List<String> result = queryRewriteService.rewrite(null, "测试查询");
            assertThat(result).containsExactly("测试查询");
            verify(llmClient, never()).chatWithFallback(any(), any(), any());
        }

        @Test
        void rewrite_blankQuery_shouldReturnOriginal() {
            List<String> result = queryRewriteService.rewrite(1L, "   ");
            assertThat(result).containsExactly("   ");
            verify(llmClient, never()).chatWithFallback(any(), any(), any());
        }

        @Test
        void rewrite_nullQuery_shouldReturnEmpty() {
            List<String> result = queryRewriteService.rewrite(1L, null);
            assertThat(result).containsExactly("");
        }

        @Test
        void rewrite_disabled_shouldReturnOriginal() {
            org.springframework.test.util.ReflectionTestUtils.setField(queryRewriteService, "rewriteEnabled", false);
            List<String> result = queryRewriteService.rewrite(1L, "查询");
            assertThat(result).containsExactly("查询");
            verify(llmClient, never()).chatWithFallback(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("rewrite 模型链")
    class ModelChainTests {

        @Test
        void rewrite_useTaskConfigModels_whenConfigExists() {
            DouyinAccount account = buildAccount();
            when(accountRepository.findByUserIdAndDeleted(1L, 0, PageRequest.of(0, 1)))
                    .thenReturn(new PageImpl<>(List.of(account)));
            when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
            when(valueOps.get(anyString())).thenReturn(null);

            AiTaskModelConfig config = new AiTaskModelConfig();
            config.setTaskCode("query_rewrite");
            config.setPrimaryModelId(10L);
            config.setFallbackModelId(11L);
            when(taskModelConfigRepository.findByTaskCodeAndStatusAndDeleted("query_rewrite", 1, 0))
                    .thenReturn(Optional.of(config));

            AiModel m1 = buildModel(10L);
            AiModel m2 = buildModel(11L);
            when(modelRepository.findById(10L)).thenReturn(Optional.of(m1));
            when(modelRepository.findById(11L)).thenReturn(Optional.of(m2));

            when(llmClient.chatWithFallback(anyList(), anyString(), anyString()))
                    .thenReturn(new LlmClient.LlmResponse("子查询1\n子查询2", 10, true, null));

            List<String> result = queryRewriteService.rewrite(1L, "直播话术优化");
            assertThat(result).containsExactly("子查询1", "子查询2");
            verify(llmClient).chatWithFallback(argThat((List<AiModel> models) -> models.size() == 2), anyString(), anyString());
        }

        @Test
        void rewrite_fallbackToAnyModels_whenNoTaskConfig() {
            DouyinAccount account = buildAccount();
            when(accountRepository.findByUserIdAndDeleted(1L, 0, PageRequest.of(0, 1)))
                    .thenReturn(new PageImpl<>(List.of(account)));
            when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
            when(valueOps.get(anyString())).thenReturn(null);
            when(taskModelConfigRepository.findByTaskCodeAndStatusAndDeleted("query_rewrite", 1, 0))
                    .thenReturn(Optional.empty());

            AiModel m = buildModel(1L);
            when(modelRepository.findByStatusAndDeleted(1, 0)).thenReturn(List.of(m));
            when(llmClient.chatWithFallback(anyList(), anyString(), anyString()))
                    .thenReturn(new LlmClient.LlmResponse("改写1", 5, true, null));

            List<String> result = queryRewriteService.rewrite(1L, "测试");
            assertThat(result).containsExactly("改写1");
        }

        @Test
        void rewrite_noAccounts_shouldReturnOriginal() {
            when(accountRepository.findByUserIdAndDeleted(1L, 0, PageRequest.of(0, 1)))
                    .thenReturn(new PageImpl<>(Collections.emptyList()));
            List<String> result = queryRewriteService.rewrite(1L, "查询");
            assertThat(result).containsExactly("查询");
            verify(llmClient, never()).chatWithFallback(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("rewrite 缓存")
    class CacheTests {

        @Test
        void rewrite_cacheHit_shouldNotCallLlm() {
            when(accountRepository.findByUserIdAndDeleted(1L, 0, PageRequest.of(0, 1)))
                    .thenReturn(new PageImpl<>(List.of(buildAccount())));
            when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
            when(valueOps.get(anyString())).thenReturn("缓存子查询1\n缓存子查询2");

            List<String> result = queryRewriteService.rewrite(1L, "测试");
            assertThat(result).containsExactly("缓存子查询1", "缓存子查询2");
            verify(llmClient, never()).chatWithFallback(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("rewrite 超长输入")
    class LongInputTests {

        @Test
        void rewrite_queryOver500Chars_shouldTruncate() {
            String longQuery = "a".repeat(600);
            when(accountRepository.findByUserIdAndDeleted(1L, 0, PageRequest.of(0, 1)))
                    .thenReturn(new PageImpl<>(List.of(buildAccount())));
            when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
            when(valueOps.get(anyString())).thenReturn(null);
            when(taskModelConfigRepository.findByTaskCodeAndStatusAndDeleted("query_rewrite", 1, 0))
                    .thenReturn(Optional.empty());
            when(modelRepository.findByStatusAndDeleted(1, 0)).thenReturn(List.of(buildModel(1L)));
            when(llmClient.chatWithFallback(anyList(), anyString(), anyString())).thenAnswer(inv -> {
                String prompt = inv.getArgument(2);
                assertThat(prompt).contains("a".repeat(500));
                return new LlmClient.LlmResponse("ok", 1, true, null);
            });
            queryRewriteService.rewrite(1L, longQuery);
        }
    }

    private DouyinAccount buildAccount() {
        DouyinAccount a = new DouyinAccount();
        a.setId(100L);
        a.setUserId(1L);
        a.setFanCount(1000L);
        a.setDescription("测试账号");
        a.setDeleted(0);
        return a;
    }

    private AiModel buildModel(Long id) {
        AiModel m = new AiModel();
        m.setId(id);
        m.setModelProvider("ollama");
        m.setModelVersion("qwen2.5:7b");
        m.setStatus(1);
        m.setDeleted(0);
        return m;
    }
}
