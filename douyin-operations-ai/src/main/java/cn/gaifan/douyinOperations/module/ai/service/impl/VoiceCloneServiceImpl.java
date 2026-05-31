package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.ai.service.TtsService;
import cn.gaifan.douyinOperations.module.ai.service.VoiceCloneService;
import cn.gaifan.douyinOperations.module.config.service.ConfigService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * 声音克隆服务实现 (ElevenLabs API)
 * POST /v1/voices/add - 创建克隆音色
 * POST /v1/text-to-speech/{voice_id} - 使用克隆音色合成
 */
@Service
public class VoiceCloneServiceImpl implements VoiceCloneService {

    private static final Logger log = LoggerFactory.getLogger(VoiceCloneServiceImpl.class);
    private static final String ELEVENLABS_VOICES_ADD = "https://api.elevenlabs.io/v1/voices/add";
    private static final String ELEVENLABS_TTS = "https://api.elevenlabs.io/v1/text-to-speech/";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(60)).build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.ai.elevenlabs.api-key:}")
    private String apiKeyFromConfig;

    @Resource
    private ConfigService configService;

    @Override
    public String cloneFromSample(String sampleUrl, String voiceName, Long userId) {
        ensureConfigured();
        if (!StringUtils.hasText(sampleUrl) || !StringUtils.hasText(voiceName)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "样本 URL 和音色名称不能为空");
        }
        try {
            String boundary = "----VoiceClone" + System.currentTimeMillis();
            String name = voiceName != null ? voiceName : "cloned_" + userId;
            String json = String.format("{\"name\":\"%s\",\"description\":\"Cloned voice\"}",
                    name.replace("\"", "\\\""));

            String body = "--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"name\"\r\n\r\n" + name + "\r\n"
                    + "--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"files\"; filename=\"sample.mp3\"\r\n"
                    + "Content-Type: audio/mpeg\r\n\r\n";
            // 注意：实际需下载 sampleUrl 并作为 multipart 上传，此处简化用 URL
            byte[] sampleBytes = downloadAudio(sampleUrl);
            if (sampleBytes == null || sampleBytes.length == 0) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "无法下载样本音频");
            }

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(ELEVENLABS_VOICES_ADD))
                    .header("xi-api-key", getApiKey())
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(buildMultipartBody(boundary, name, sampleBytes))
                    .timeout(Duration.ofSeconds(120))
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() != 200) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                        "ElevenLabs 声音克隆失败: " + resp.statusCode() + " " + resp.body());
            }
            JsonNode root = objectMapper.readTree(resp.body());
            String voiceId = root.path("voice_id").asText(null);
            if (voiceId == null || voiceId.isBlank()) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "ElevenLabs 未返回 voice_id");
            }
            log.info("声音克隆成功: voiceId={}, name={}", voiceId, name);
            return voiceId;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("声音克隆失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "声音克隆失败: " + e.getMessage());
        }
    }

    private HttpRequest.BodyPublisher buildMultipartBody(String boundary, String name, byte[] sampleBytes) {
        String header = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"name\"\r\n\r\n" + name + "\r\n"
                + "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"files\"; filename=\"sample.mp3\"\r\n"
                + "Content-Type: audio/mpeg\r\n\r\n";
        String footer = "\r\n--" + boundary + "--\r\n";
        return HttpRequest.BodyPublishers.ofByteArray(
                concat(header.getBytes(StandardCharsets.UTF_8), sampleBytes, footer.getBytes(StandardCharsets.UTF_8)));
    }

    private static byte[] concat(byte[] a, byte[] b, byte[] c) {
        byte[] r = new byte[a.length + b.length + c.length];
        System.arraycopy(a, 0, r, 0, a.length);
        System.arraycopy(b, 0, r, a.length, b.length);
        System.arraycopy(c, 0, r, a.length + b.length, c.length);
        return r;
    }

    private byte[] downloadAudio(String url) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(60))
                    .GET().build();
            HttpResponse<byte[]> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofByteArray());
            return resp.statusCode() == 200 ? resp.body() : null;
        } catch (Exception e) {
            log.warn("下载样本音频失败: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public TtsService.AudioResult synthesizeWithClonedVoice(String voiceId, String text, Long userId) {
        ensureConfigured();
        if (!StringUtils.hasText(voiceId) || !StringUtils.hasText(text)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "voiceId 和文本不能为空");
        }
        try {
            String json = String.format("{\"text\":\"%s\",\"model_id\":\"eleven_multilingual_v2\"}",
                    text.replace("\\", "\\\\").replace("\"", "\\\""));

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(ELEVENLABS_TTS + voiceId))
                    .header("xi-api-key", getApiKey())
                    .header("Content-Type", "application/json")
                    .header("Accept", "audio/mpeg")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .timeout(Duration.ofSeconds(60))
                    .build();

            HttpResponse<byte[]> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofByteArray());
            if (resp.statusCode() != 200) {
                String err = resp.body().length > 0 ? new String(resp.body(), StandardCharsets.UTF_8) : "";
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "ElevenLabs TTS 失败: " + resp.statusCode() + " " + err);
            }
            byte[] audio = resp.body();
            long fileSize = audio.length;
            long durationMs = fileSize > 0 ? (long) (fileSize / 16.0) : 0;
            return new TtsService.AudioResult("data:audio/mpeg;base64," + java.util.Base64.getEncoder().encodeToString(audio),
                    text, durationMs, fileSize);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("克隆音色 TTS 失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "TTS 失败: " + e.getMessage());
        }
    }

    @Override
    public boolean isConfigured() {
        return StringUtils.hasText(getApiKey());
    }

    private void ensureConfigured() {
        if (!isConfigured()) {
            throw new BusinessException(ErrorCode.AI_TASK_MODEL_NOT_CONFIGURED, "声音克隆未配置，请配置 ELEVENLABS_API_KEY");
        }
    }

    private String getApiKey() {
        String key = configService != null ? configService.getRawValueByKey("ai.elevenlabs.api-key") : null;
        if (!StringUtils.hasText(key)) key = apiKeyFromConfig;
        if (!StringUtils.hasText(key)) key = System.getenv("ELEVENLABS_API_KEY");
        return StringUtils.hasText(key) ? key.trim() : null;
    }
}
