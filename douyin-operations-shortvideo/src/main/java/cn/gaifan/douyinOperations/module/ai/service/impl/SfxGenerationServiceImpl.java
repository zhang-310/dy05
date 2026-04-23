package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.util.ByteArrayMultipartFile;
import cn.gaifan.douyinOperations.module.ai.service.SfxGenerationService;
import cn.gaifan.douyinOperations.module.shortvideo.util.ShortVideoPathHelper;
import cn.gaifan.douyinOperations.module.storage.service.BosStorageService;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * AI 音效生成服务实现 (Phase 7)
 * 调用 ElevenLabs Sound Effects API，上传 BOS 后返回 URL
 */
@Service
public class SfxGenerationServiceImpl implements SfxGenerationService {

    private static final Logger log = LoggerFactory.getLogger(SfxGenerationServiceImpl.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String ELEVENLABS_SFX_URL = "https://api.elevenlabs.io/v1/sound-generation";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30)).build();

    @Value("${app.ai.elevenlabs.api-key:}")
    private String elevenLabsApiKey;

    @Resource
    private BosStorageService bosStorageService;

    private static final Map<String, String> SFX_KEYWORD_MAP = Map.ofEntries(
            Map.entry("雨", "rain falling on pavement"),
            Map.entry("雷", "distant thunder rumble"),
            Map.entry("风", "wind blowing through trees"),
            Map.entry("海", "ocean waves crashing on shore"),
            Map.entry("火", "fire crackling"),
            Map.entry("打斗", "martial arts punches and kicks impact"),
            Map.entry("奔跑", "running footsteps on pavement"),
            Map.entry("走路", "footsteps walking slowly"),
            Map.entry("开门", "door opening with creak"),
            Map.entry("关门", "door closing firmly"),
            Map.entry("汽车", "car engine running"),
            Map.entry("城市", "urban city ambient sounds, traffic"),
            Map.entry("森林", "forest ambiance, birds chirping"),
            Map.entry("夜晚", "nighttime crickets and ambient"),
            Map.entry("餐厅", "restaurant ambient chatter and clinking"),
            Map.entry("办公", "office ambient, keyboard typing"),
            Map.entry("爆炸", "explosion with debris"),
            Map.entry("枪", "gunshot echo"),
            Map.entry("哭", "soft crying"),
            Map.entry("笑", "laughter")
    );

    @Override
    public List<SfxResult> generateSfxFromScene(String sceneDescription, double durationSec, Long userId) {
        ensureConfigured();
        if (!StringUtils.hasText(sceneDescription)) {
            return List.of();
        }
        List<String> descriptions = extractSfxKeywords(sceneDescription);
        if (descriptions.isEmpty()) {
            return List.of();
        }
        List<SfxResult> results = new ArrayList<>();
        for (String desc : descriptions) {
            try {
                SfxResult r = generateSfx(desc, durationSec, userId);
                results.add(r);
            } catch (Exception e) {
                log.warn("音效生成失败: {} - {}", desc, e.getMessage());
            }
        }
        return results;
    }

    @Override
    public SfxResult generateSfx(String description, double durationSec, Long userId) {
        ensureConfigured();
        if (!StringUtils.hasText(description)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "音效描述不能为空");
        }
        double dur = durationSec > 0 ? Math.min(Math.max(durationSec, 0.5), 30) : 5.0;
        byte[] audioBytes = callElevenLabsSfx(description, dur);
        String audioUrl = uploadToBos(audioBytes, userId);
        return new SfxResult(description, audioUrl, dur);
    }

    private byte[] callElevenLabsSfx(String description, double durationSec) {
        String json = String.format("{\"text\":\"%s\",\"duration_seconds\":%.1f}",
                description.replace("\"", "\\\""), durationSec);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(ELEVENLABS_SFX_URL))
                .header("xi-api-key", elevenLabsApiKey)
                .header("Content-Type", "application/json")
                .header("Accept", "audio/mpeg")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .timeout(Duration.ofSeconds(90))
                .build();
        try {
            HttpResponse<byte[]> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofByteArray());
            if (resp.statusCode() != 200) {
                String body = resp.body().length > 0 ? new String(resp.body(), StandardCharsets.UTF_8) : "";
                throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                        "ElevenLabs SFX API 返回 " + resp.statusCode() + ": " + body);
            }
            return resp.body();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("ElevenLabs SFX 调用失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "音效生成失败: " + e.getMessage());
        }
    }

    private String uploadToBos(byte[] audioBytes, Long userId) {
        if (audioBytes == null || audioBytes.length == 0) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "音效数据为空");
        }
        if (bosStorageService.isConfigured()) {
            try {
                String date = LocalDate.now().format(DATE_FMT);
                String filename = "sfx_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12) + ".mp3";
                String key = ShortVideoPathHelper.sfxKey(userId, date, filename);
                var mf = new ByteArrayMultipartFile("file", filename, "audio/mpeg", audioBytes);
                return bosStorageService.upload(key, mf);
            } catch (Exception e) {
                log.warn("音效上传 BOS 失败: {}", e.getMessage());
            }
        }
        return "data:audio/mpeg;base64," + java.util.Base64.getEncoder().encodeToString(audioBytes);
    }

    private void ensureConfigured() {
        if (!StringUtils.hasText(elevenLabsApiKey)) {
            throw new BusinessException(ErrorCode.AI_TASK_MODEL_NOT_CONFIGURED,
                    "音效生成暂未配置。请配置 ELEVENLABS_API_KEY 后使用。");
        }
    }

    private List<String> extractSfxKeywords(String scene) {
        List<String> descriptions = new ArrayList<>();
        for (Map.Entry<String, String> entry : SFX_KEYWORD_MAP.entrySet()) {
            if (scene.contains(entry.getKey())) {
                descriptions.add(entry.getValue());
            }
        }
        return descriptions.size() > 3 ? descriptions.subList(0, 3) : descriptions;
    }
}
