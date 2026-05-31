package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.ai.entity.AiTtsGeneration;
import cn.gaifan.douyinOperations.module.ai.repository.AiTtsGenerationRepository;
import cn.gaifan.douyinOperations.module.ai.service.TtsService;
import cn.gaifan.douyinOperations.module.ai.service.VoiceCloneService;
import cn.gaifan.douyinOperations.module.config.service.ConfigService;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 语音合成服务：优先讯飞，否则 HTTP 代理
 */
@Service
public class TtsServiceImpl implements TtsService {

    private static final Logger log = LoggerFactory.getLogger(TtsServiceImpl.class);

    /** 音色映射：前端 id -> 讯飞 vcn */
    private static final Map<String, String> VOICE_TO_IFLYTEK = Map.of(
            "zh-CN-XiaoxiaoNeural", "xiaoxiao",
            "zh-CN-YunxiNeural", "xiaoyu",
            "zh-CN-YunyangNeural", "xiaoyu",
            "zh-CN-XiaoyiNeural", "xiaoqi",
            "xiaoyan", "xiaoyan",
            "xiaoxiao", "xiaoxiao",
            "xiaoyu", "xiaoyu",
            "xiaoqi", "xiaoqi"
    );

    @Value("${app.ai.tts-url:http://localhost:5000}")
    private String ttsUrl;

    @Value("${app.ai.tts.iflytek.app-id:}")
    private String iflytekAppIdFromConfig;

    @Value("${app.ai.tts.iflytek.api-key:}")
    private String iflytekApiKeyFromConfig;

    @Value("${app.ai.tts.iflytek.api-secret:}")
    private String iflytekApiSecretFromConfig;

    @Value("${app.video-analysis.work-dir:/tmp/video-edit}")
    private String workDir;

    @Resource
    private AiTtsGenerationRepository ttsGenerationRepository;

    @Resource
    private ConfigService configService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private VoiceCloneService voiceCloneService;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    private boolean isIflytekConfigured() {
        String appId = getIflytekConfig("ai.tts.iflytek.app-id", "IFLYTEK_APP_ID", iflytekAppIdFromConfig);
        String apiKey = getIflytekConfig("ai.tts.iflytek.api-key", "IFLYTEK_API_KEY", iflytekApiKeyFromConfig);
        String apiSecret = getIflytekConfig("ai.tts.iflytek.api-secret", "IFLYTEK_API_SECRET", iflytekApiSecretFromConfig);
        return StringUtils.hasText(appId) && StringUtils.hasText(apiKey) && StringUtils.hasText(apiSecret);
    }

    private String getIflytekConfig(String configKey, String envKey, String configValue) {
        String v = configService != null ? configService.getRawValueByKey(configKey) : null;
        if (v == null || v.isBlank()) v = configValue;
        if (v == null || v.isBlank()) v = System.getenv(envKey);
        return v != null && !v.isBlank() ? v.trim() : null;
    }

    @Override
    public AudioResult textToSpeech(TtsRequest request, Long userId) {
        if (request.voiceId() != null && !request.voiceId().isBlank()
                && voiceCloneService != null && voiceCloneService.isConfigured()) {
            return voiceCloneService.synthesizeWithClonedVoice(request.voiceId(), request.text(), userId);
        }
        if (isIflytekConfigured()) {
            return textToSpeechIflytek(request, userId);
        }
        return textToSpeechHttp(request, userId);
    }

    private AudioResult textToSpeechIflytek(TtsRequest request, Long userId) {
        String appId = getIflytekConfig("ai.tts.iflytek.app-id", "IFLYTEK_APP_ID", iflytekAppIdFromConfig);
        String apiKey = getIflytekConfig("ai.tts.iflytek.api-key", "IFLYTEK_API_KEY", iflytekApiKeyFromConfig);
        String apiSecret = getIflytekConfig("ai.tts.iflytek.api-secret", "IFLYTEK_API_SECRET", iflytekApiSecretFromConfig);
        String voice = request.voice() != null ? request.voice() : "xiaoyan";
        String vcn = VOICE_TO_IFLYTEK.getOrDefault(voice, voice);
        int speed = (int) Math.round((request.speed() != null ? request.speed() : 1.0) * 50);
        int pitch = (int) Math.round((request.pitch() != null ? request.pitch() : 1.0) * 50);
        int speedClamped = Math.max(0, Math.min(100, speed));
        int pitchClamped = Math.max(0, Math.min(100, pitch));

        try {
            IflytekTtsClient client = new IflytekTtsClient(appId, apiKey, apiSecret);
            byte[] audio = client.synthesize(request.text(), vcn, speedClamped, pitchClamped);
            if (audio == null || audio.length == 0) {
                String err = client.getErrorMessage();
                throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                        StringUtils.hasText(err) ? "讯飞 TTS 失败: " + err : "讯飞 TTS 合成失败，请检查 IFLYTEK_APP_ID/API_KEY/API_SECRET 及讯飞控制台 IP 白名单");
            }
            Path workPath = Path.of(workDir);
            if (!Files.exists(workPath)) Files.createDirectories(workPath);
            Path out = workPath.resolve("tts_" + System.currentTimeMillis() + ".mp3");
            Files.write(out, audio);
            String audioUrl = out.toAbsolutePath().toString();
            long fileSize = audio.length;
            long durationMs = fileSize > 0 ? (long) (fileSize / 16.0) : 0; // 约 128kbps
            saveHistory(userId, audioUrl, request.text(), voice);
            return new AudioResult(audioUrl, request.text(), durationMs, fileSize);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("讯飞 TTS 失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "讯飞 TTS 失败: " + e.getMessage());
        }
    }

    private AudioResult textToSpeechHttp(TtsRequest request, Long userId) {
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("text", request.text());
            requestBody.put("voice", request.voice() != null ? request.voice() : "zh-CN-XiaoxiaoNeural");
            requestBody.put("language", request.language() != null ? request.language() : "zh-CN");
            requestBody.put("speed", request.speed() != null ? request.speed() : 1.0);
            requestBody.put("pitch", request.pitch() != null ? request.pitch() : 1.0);
            requestBody.put("format", request.format() != null ? request.format() : "mp3");

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(ttsUrl + "/api/tts"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toJSONString()))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "语音合成失败: " + response.body());
            }

            JSONObject responseJson = JSON.parseObject(response.body());
            String audioUrl = responseJson.getString("audio_url");
            Long duration = responseJson.getLong("duration");
            Long fileSize = responseJson.getLong("file_size");

            saveHistory(userId, audioUrl, request.text(), request.voice());

            return new AudioResult(audioUrl, request.text(), duration, fileSize);
        } catch (Exception e) {
            log.error("语音合成失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "语音合成失败: " + e.getMessage());
        }
    }

    @Override
    public List<VoiceInfo> getAvailableVoices() {
        List<VoiceInfo> voices = new ArrayList<>();
        if (isIflytekConfigured()) {
            voices.add(new VoiceInfo("xiaoyan", "小燕", "zh-CN", "female", "青年女声"));
            voices.add(new VoiceInfo("xiaoyu", "小宇", "zh-CN", "male", "青年男声"));
            voices.add(new VoiceInfo("xiaoxiao", "晓晓", "zh-CN", "female", "温柔女声"));
            voices.add(new VoiceInfo("xiaoqi", "小琪", "zh-CN", "female", "知性女声"));
        } else {
            voices.add(new VoiceInfo("zh-CN-XiaoxiaoNeural", "晓晓", "zh-CN", "female", "温柔女声"));
            voices.add(new VoiceInfo("zh-CN-YunxiNeural", "云希", "zh-CN", "male", "阳光男声"));
            voices.add(new VoiceInfo("zh-CN-YunyangNeural", "云扬", "zh-CN", "male", "成熟男声"));
            voices.add(new VoiceInfo("zh-CN-XiaoyiNeural", "晓伊", "zh-CN", "female", "知性女声"));
            voices.add(new VoiceInfo("en-US-JennyNeural", "Jenny", "en-US", "female", "美式女声"));
            voices.add(new VoiceInfo("en-US-GuyNeural", "Guy", "en-US", "male", "美式男声"));
        }
        return voices;
    }

    @Override
    public List<TtsHistory> getHistory(Long userId, int page, int size) {
        return ttsGenerationRepository
                .findByUserIdAndDeletedOrderByCreateTimeDesc(userId, 0, PageRequest.of(page, size))
                .stream()
                .map(entity -> new TtsHistory(
                        entity.getId(),
                        entity.getAudioUrl(),
                        entity.getText(),
                        entity.getVoice(),
                        entity.getCreateTime().getTime()
                ))
                .collect(Collectors.toList());
    }

    private void saveHistory(Long userId, String audioUrl, String text, String voice) {
        AiTtsGeneration entity = new AiTtsGeneration();
        entity.setUserId(userId);
        entity.setAudioUrl(audioUrl);
        entity.setText(text);
        entity.setVoice(voice);
        entity.setStatus(1);
        ttsGenerationRepository.save(entity);
    }
}
