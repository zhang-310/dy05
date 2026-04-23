package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.service.LlmClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("OpenAiCompatibleLlmClient tools 调用测试")
class OpenAiCompatibleLlmClientTest {

    private OpenAiCompatibleLlmClient client;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        client = new OpenAiCompatibleLlmClient();
        ReflectionTestUtils.setField(client, "openaiUrlDefault", "https://api.openai.com");
        ReflectionTestUtils.setField(client, "deepseekUrlDefault", "https://api.deepseek.com");
        ReflectionTestUtils.setField(client, "ollamaUrlDefault", "http://localhost:11434");
        ReflectionTestUtils.setField(client, "backoff429Seconds", "1,1,1");
        ReflectionTestUtils.setField(client, "deepseekApiKeyEnv", "");

        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(client, "restTemplate");
        server = MockRestServiceServer.bindTo(restTemplate).build();
    }

    @Test
    void chatWithToolsStructured_shouldParseToolCalls() {
        server.expect(requestTo("https://api.openai.com/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer sk-test"))
                .andExpect(content().string(containsString("\"tool_choice\":\"auto\"")))
                .andExpect(content().string(containsString("\"kb_rag_search\"")))
                .andRespond(withSuccess("""
                        {
                          "choices": [
                            {
                              "message": {
                                "role": "assistant",
                                "content": "",
                                "tool_calls": [
                                  {
                                    "id": "call_1",
                                    "type": "function",
                                    "function": {
                                      "name": "kb_rag_search",
                                      "arguments": "{\\"query\\":\\"护肤\\"}"
                                    }
                                  }
                                ]
                              }
                            }
                          ],
                          "usage": {
                            "total_tokens": 42
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        LlmClient.LlmToolResponse response = client.chatWithToolsStructured(
                createModel(),
                List.of(
                        Map.of("role", "system", "content", "你是助手"),
                        Map.of("role", "user", "content", "帮我查护肤知识")
                ),
                """
                [
                  {
                    "type": "function",
                    "function": {
                      "name": "kb_rag_search",
                      "description": "检索知识库",
                      "parameters": {
                        "type": "object",
                        "properties": {
                          "query": { "type": "string" }
                        },
                        "required": ["query"]
                      }
                    }
                  }
                ]
                """
        );

        assertThat(response.success()).isTrue();
        assertThat(response.tokensUsed()).isEqualTo(42);
        assertThat(response.toolCallsJson()).contains("kb_rag_search");
        assertThat(response.rawJson()).contains("tool_calls");
        server.verify();
    }

    @Test
    void chatWithToolsStructured_shouldWrapLegacyFunctionCall() {
        server.expect(requestTo("https://api.openai.com/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "choices": [
                            {
                              "message": {
                                "role": "assistant",
                                "content": null,
                                "function_call": {
                                  "name": "competitor_analysis",
                                  "arguments": "{\\"category\\":\\"护肤\\"}"
                                }
                              }
                            }
                          ],
                          "usage": {
                            "prompt_tokens": 20,
                            "completion_tokens": 12
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        LlmClient.LlmToolResponse response = client.chatWithToolsStructured(
                createModel(),
                List.of(Map.of("role", "user", "content", "分析护肤竞品")),
                """
                [
                  {
                    "type": "function",
                    "function": {
                      "name": "competitor_analysis",
                      "description": "竞品分析",
                      "parameters": {
                        "type": "object"
                      }
                    }
                  }
                ]
                """
        );

        assertThat(response.success()).isTrue();
        assertThat(response.tokensUsed()).isEqualTo(32);
        assertThat(response.toolCallsJson()).contains("competitor_analysis");
        assertThat(response.toolCallsJson()).contains("call_legacy_0");
        server.verify();
    }

    private AiModel createModel() {
        AiModel model = new AiModel();
        model.setModelProvider("openai");
        model.setModelVersion("gpt-4o-mini");
        model.setApiKey("sk-test");
        model.setMaxTokens(256);
        model.setTemperature(new BigDecimal("0.30"));
        return model;
    }
}
