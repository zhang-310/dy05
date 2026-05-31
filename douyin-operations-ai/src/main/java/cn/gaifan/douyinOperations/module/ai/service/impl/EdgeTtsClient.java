package cn.gaifan.douyinOperations.module.ai.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Edge-TTS 原生 HTTP Client
 * 通过本地/远程 Edge-TTS HTTP 服务（edge-tts-go 或 Python edge-tts 的 HTTP 包装）生成语音
 */
@Component
public class EdgeTtsClient {
    private static final Logger log = LoggerFactory.getLogger(EdgeTtsClient.class);

    @Value("${app.tts.edge-tts.url:}")
    private String edgeTtsUrl;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public boolean isAvailable() {
        return edgeTtsUrl != null && !edgeTtsUrl.isBlank();
    }

    public byte[] synthesize(String text, String voice) {
        if (!isAvailable()) {
            throw new IllegalStateException("Edge-TTS 服务未配置");
        }
        try {
            String url = edgeTtsUrl + "/api/tts?text=" + java.net.URLEncoder.encode(text, "UTF-8") + "&voice=" + voice;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(60))
                    .GET()
                    .build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() == 200) {
                return response.body();
            }
            log.error("Edge-TTS 请求失败: status={}", response.statusCode());
            throw new RuntimeException("Edge-TTS 请求失败: " + response.statusCode());
        } catch (Exception e) {
            log.error("Edge-TTS 合成失败: {}", e.getMessage());
            throw new RuntimeException("Edge-TTS 合成失败", e);
        }
    }

    public String[] listVoices() {
        return new String[]{
            "zh-CN-XiaoxiaoNeural", "zh-CN-YunxiNeural", "zh-CN-YunyangNeural",
            "zh-CN-XiaoyiNeural", "zh-CN-YunjianNeural", "zh-CN-XiaochenNeural"
        };
    }
}
