package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.ai.entity.AiImageGeneration;
import cn.gaifan.douyinOperations.module.ai.repository.AiImageGenerationRepository;
import cn.gaifan.douyinOperations.module.ai.service.ImageGenerationService;
import cn.gaifan.douyinOperations.module.config.service.ConfigService;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ImageGenerationServiceImpl implements ImageGenerationService {

    private static final Logger log = LoggerFactory.getLogger(ImageGenerationServiceImpl.class);
    private static final String KLING_IMAGE_BASE = "https://api.klingai.com/v1/images";
    private static final int KLING_POLL_INTERVAL_MS = 3000;
    private static final int KLING_POLL_MAX_ATTEMPTS = 60; // 3 分钟超时

    @Value("${app.ai.kling.api-url:}")
    private String klingApiUrlFromConfig;

    @Value("${app.ai.stable-diffusion-url:http://localhost:7860}")
    private String sdUrl;

    @Value("${app.ai.kling.api-key:}")
    private String klingApiKeyFromConfig;

    @Value("${app.ai.kling.api-secret:}")
    private String klingApiSecretFromConfig;

    @Value("${app.ai.image.local-upload-dir:uploads/images}")
    private String localImageUploadDir;

    @Value("${app.ai.image.public-url-prefix:/uploads/images}")
    private String imagePublicUrlPrefix;

    @Resource
    private AiImageGenerationRepository imageGenerationRepository;

    @Resource
    private ConfigService configService;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    private String getKlingApiKey() {
        String key = configService != null ? configService.getRawValueByKey("ai.kling.api-key") : null;
        if (key == null || key.isBlank()) key = klingApiKeyFromConfig;
        if (key == null || key.isBlank()) key = System.getenv("KLING_API_KEY");
        return StringUtils.hasText(key) ? key.trim() : null;
    }

    private String getKlingApiSecret() {
        String secret = configService != null ? configService.getRawValueByKey("ai.kling.api-secret") : null;
        if (secret == null || secret.isBlank()) secret = klingApiSecretFromConfig;
        if (secret == null || secret.isBlank()) secret = System.getenv("KLING_API_SECRET");
        return StringUtils.hasText(secret) ? secret.trim() : null;
    }

    private boolean isKlingConfigured() {
        return getKlingApiKey() != null;
    }

    private String getKlingImageBase() {
        String url = configService != null ? configService.getRawValueByKey("ai.kling.api-url") : null;
        if (url == null || url.isBlank()) url = klingApiUrlFromConfig;
        if (url == null || url.isBlank()) url = System.getenv("KLING_API_URL");
        if (!StringUtils.hasText(url)) return KLING_IMAGE_BASE;
        url = url.trim().replaceAll("/$", "");
        // 若用户配置了完整路径 .../generations，需截断为 base，否则会变成 .../generations/generations
        if (url.endsWith("/generations")) url = url.substring(0, url.length() - "/generations".length());
        return url;
    }

    /** 获取可灵 Bearer Token：有 Secret 时用 JWT（官方平台 Key+Secret），否则用 API Key */
    private String getKlingBearerToken() {
        String key = getKlingApiKey();
        String secret = getKlingApiSecret();
        if (StringUtils.hasText(secret)) {
            try {
                long nowSec = System.currentTimeMillis() / 1000;
                SecretKey sk = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
                return Jwts.builder()
                        .issuer(key)
                        .issuedAt(Date.from(java.time.Instant.ofEpochSecond(nowSec - 5)))
                        .notBefore(Date.from(java.time.Instant.ofEpochSecond(nowSec - 5)))
                        .expiration(Date.from(java.time.Instant.ofEpochSecond(nowSec + 1800)))
                        .signWith(sk)
                        .compact();
            } catch (Exception e) {
                log.warn("可灵 JWT 生成失败，回退到 API Key: {}", e.getMessage());
            }
        }
        return key;
    }

    @Override
    public ImageResult textToImage(TextToImageRequest request, Long userId) {
        long startTime = System.currentTimeMillis();

        // 默认使用可灵（配置了 KLING_API_KEY 时，不再回退本地 SD）
        if (isKlingConfigured()) {
            try {
                return textToImageViaKling(request, userId, startTime);
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                log.error("可灵文生图失败", e);
                String msg = e.getMessage() != null && !e.getMessage().isBlank() ? e.getMessage() : e.getClass().getSimpleName();
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "可灵文生图失败: " + msg);
            }
        }

        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("prompt", request.prompt());
            requestBody.put("negative_prompt", request.negativePrompt() != null ? request.negativePrompt() : "");
            requestBody.put("width", request.width() != null ? request.width() : 512);
            requestBody.put("height", request.height() != null ? request.height() : 512);
            requestBody.put("steps", request.steps() != null ? request.steps() : 20);
            requestBody.put("cfg_scale", request.cfgScale() != null ? request.cfgScale() : 7.0);

            if (request.seed() != null) {
                requestBody.put("seed", request.seed());
            }

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(sdUrl + "/sdapi/v1/txt2img"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toJSONString()))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                String body = response.body();
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "图像生成失败: " + (body != null ? body : "HTTP " + response.statusCode()));
            }

            JSONObject responseJson = JSON.parseObject(response.body());
            var images = responseJson != null ? responseJson.getJSONArray("images") : null;
            if (images == null || images.isEmpty()) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "图像生成失败: Stable Diffusion 返回空结果，请检查服务 " + sdUrl);
            }
            String base64Image = images.getString(0);

            // 保存到文件系统或对象存储
            String imageUrl = saveImage(base64Image, userId);

            long generationTime = System.currentTimeMillis() - startTime;

            // 保存生成历史
            saveHistory(userId, imageUrl, request.prompt(), "text2img", new HashMap<>(requestBody));

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("width", request.width());
            parameters.put("height", request.height());
            parameters.put("steps", request.steps());
            parameters.put("cfg_scale", request.cfgScale());

            return new ImageResult(imageUrl, request.prompt(), parameters, generationTime);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("文生图失败", e);
            String msg = e.getMessage();
            if (msg == null || msg.isBlank()) {
                msg = e.getClass().getSimpleName() + "，请确认 Stable Diffusion 已启动 (" + sdUrl + ") 或 ComfyUI 已配置";
            } else {
                msg = "图像生成失败: " + msg;
            }
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, msg);
        }
    }

    @Override
    public ImageResult imageToImage(ImageToImageRequest request, Long userId) {
        long startTime = System.currentTimeMillis();
        if (request == null || request.imageUrl() == null || request.imageUrl().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "参考图 URL 不能为空");
        }

        // 可灵支持图生图（参考图 + 提示词）：优先使用可灵
        if (isKlingConfigured()) {
            try {
                return imageToImageViaKling(request, userId, startTime);
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                log.error("可灵图生图失败", e);
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "可灵图生图失败: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
            }
        }

        try {
            // 降级：SD img2img
            String base64Image = downloadImageAsBase64(request.imageUrl());
            if (base64Image == null || base64Image.isBlank()) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "无法下载参考图");
            }

            JSONObject requestBody = new JSONObject();
            requestBody.put("init_images", Collections.singletonList(base64Image));
            requestBody.put("prompt", request.prompt());
            requestBody.put("negative_prompt", request.negativePrompt() != null ? request.negativePrompt() : "");
            requestBody.put("denoising_strength", request.strength() != null ? request.strength() : 0.75);
            requestBody.put("steps", request.steps() != null ? request.steps() : 20);
            requestBody.put("cfg_scale", request.cfgScale() != null ? request.cfgScale() : 7.0);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(sdUrl + "/sdapi/v1/img2img"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toJSONString()))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "图像生成失败: " + response.body());
            }

            JSONObject responseJson = JSON.parseObject(response.body());
            String resultBase64 = responseJson.getJSONArray("images").getString(0);

            String imageUrl = saveImage(resultBase64, userId);
            long generationTime = System.currentTimeMillis() - startTime;

            saveHistory(userId, imageUrl, request.prompt(), "img2img", new HashMap<>(requestBody));

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("strength", request.strength());
            parameters.put("steps", request.steps());

            return new ImageResult(imageUrl, request.prompt(), parameters, generationTime);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("图生图失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "图像生成失败: " + e.getMessage());
        }
    }

    /** 可灵图生图：参考图 URL + 提示词（可灵 API 支持 image 参数时生效） */
    private ImageResult imageToImageViaKling(ImageToImageRequest request, Long userId, long startTime) throws Exception {
        String prompt = request.prompt() != null && !request.prompt().isBlank() ? request.prompt() : "根据参考图风格生成高质量图像";
        JSONObject body = new JSONObject();
        body.put("model", "kling-v1");
        body.put("prompt", prompt);
        if (StringUtils.hasText(request.negativePrompt())) body.put("negative_prompt", request.negativePrompt());
        body.put("aspect_ratio", "16:9");
        body.put("n", 1);
        // 可灵图生图：image 支持 URL，部分接口用 image_url。不支持的 API 会忽略未知参数，等效文生图
        body.put("image", request.imageUrl());

        String base = getKlingImageBase();
        String createUrl = base + "/generations";
        String bearer = getKlingBearerToken();
        log.debug("可灵图生图: POST {} image={}", createUrl, request.imageUrl());
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(createUrl))
                .header("Authorization", "Bearer " + bearer)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toJSONString(), StandardCharsets.UTF_8))
                .timeout(java.time.Duration.ofSeconds(30))
                .build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() != 200 && resp.statusCode() != 201) {
            String bodyStr = resp.body();
            // 可灵若不支持 image 参数，降级为文生图
            if (resp.statusCode() == 400 && bodyStr != null && (bodyStr.contains("image") || bodyStr.contains("invalid"))) {
                log.info("可灵图生图 image 参数不支持，降级文生图");
                return textToImageViaKling(new TextToImageRequest(prompt, request.negativePrompt(), null, 1024, 576, 20, 7.0, null), userId, startTime);
            }
            log.warn("可灵图生图提交失败: status={} body={}", resp.statusCode(), bodyStr != null && bodyStr.length() > 300 ? bodyStr.substring(0, 300) + "..." : bodyStr);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "可灵图生图失败: HTTP " + resp.statusCode());
        }
        JSONObject root = JSON.parseObject(resp.body());
        String taskId = root.getString("task_id");
        if (taskId == null) {
            JSONObject data = root.getJSONObject("data");
            if (data != null) taskId = data.getString("task_id");
        }
        if (taskId == null || taskId.isBlank()) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "可灵未返回 task_id");
        }

        String imageUrl = pollKlingTask(bearer, taskId);
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "可灵返回空图片");
        }

        long generationTime = System.currentTimeMillis() - startTime;
        Map<String, Object> params = new HashMap<>();
        params.put("provider", "kling");
        params.put("img2img", true);
        saveHistory(userId, imageUrl, prompt, "img2img-kling", params);
        return new ImageResult(imageUrl, prompt, params, generationTime);
    }

    @Override
    public ImageResult editImage(ImageEditRequest request, Long userId) {
        long startTime = System.currentTimeMillis();

        try {
            String base64Image = downloadImageAsBase64(request.imageUrl());
            String base64Mask = downloadImageAsBase64(request.maskUrl());

            JSONObject requestBody = new JSONObject();
            requestBody.put("init_images", Collections.singletonList(base64Image));
            requestBody.put("mask", base64Mask);
            requestBody.put("prompt", request.prompt());
            requestBody.put("steps", request.steps() != null ? request.steps() : 20);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(sdUrl + "/sdapi/v1/img2img"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toJSONString()))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "图像编辑失败: " + response.body());
            }

            JSONObject responseJson = JSON.parseObject(response.body());
            String resultBase64 = responseJson.getJSONArray("images").getString(0);

            String imageUrl = saveImage(resultBase64, userId);
            long generationTime = System.currentTimeMillis() - startTime;

            saveHistory(userId, imageUrl, request.prompt(), "edit", new HashMap<>(requestBody));

            return new ImageResult(imageUrl, request.prompt(), new HashMap<>(), generationTime);
        } catch (Exception e) {
            log.error("图像编辑失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "图像编辑失败: " + e.getMessage());
        }
    }

    @Override
    public List<ImageGenerationHistory> getHistory(Long userId, int page, int size) {
        return imageGenerationRepository
                .findByUserIdAndDeletedOrderByCreateTimeDesc(userId, 0, PageRequest.of(page, size))
                .stream()
                .map(entity -> new ImageGenerationHistory(
                        entity.getId(),
                        entity.getImageUrl(),
                        entity.getPrompt(),
                        entity.getGenerationType(),
                        JSON.parseObject(entity.getParameters()),
                        entity.getCreateTime().getTime()
                ))
                .collect(Collectors.toList());
    }

    /**
     * 通过可灵 API 文生图（异步任务，提交后轮询结果）
     */
    private ImageResult textToImageViaKling(TextToImageRequest request, Long userId, long startTime) throws Exception {
        String apiKey = getKlingApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "可灵 API Key 未配置");
        }
        String prompt = request.prompt() != null ? request.prompt() : "高质量场景图";
        String negativePrompt = request.negativePrompt() != null ? request.negativePrompt() : "";
        int w = request.width() != null ? request.width() : 512;
        int h = request.height() != null ? request.height() : 512;
        String aspectRatio = (w == h) ? "1:1" : (w > h ? "16:9" : "9:16");

        JSONObject body = new JSONObject();
        body.put("model", "kling-v1");
        body.put("prompt", prompt);
        if (StringUtils.hasText(negativePrompt)) body.put("negative_prompt", negativePrompt);
        body.put("aspect_ratio", aspectRatio);
        body.put("n", 1);

        String base = getKlingImageBase();
        String createUrl = base + "/generations";
        String bearer = getKlingBearerToken();
        log.debug("可灵文生图: POST {} model=kling-v1", createUrl);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(createUrl))
                .header("Authorization", "Bearer " + bearer)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toJSONString(), StandardCharsets.UTF_8))
                .timeout(java.time.Duration.ofSeconds(30))
                .build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() != 200 && resp.statusCode() != 201) {
            String bodyStr = resp.body();
            log.warn("可灵提交失败: status={} body={}", resp.statusCode(), bodyStr);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "可灵提交失败: " + resp.statusCode() + " " + (bodyStr != null && bodyStr.length() > 200 ? bodyStr.substring(0, 200) + "..." : bodyStr));
        }
        JSONObject root = JSON.parseObject(resp.body());
        String taskId = root.getString("task_id");
        if (taskId == null) {
            JSONObject data = root.getJSONObject("data");
            if (data != null) taskId = data.getString("task_id");
        }
        if (taskId == null || taskId.isBlank()) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "可灵未返回 task_id: " + resp.body());
        }

        String imageUrl = pollKlingTask(bearer, taskId);
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "可灵返回空图片");
        }

        long generationTime = System.currentTimeMillis() - startTime;
        Map<String, Object> params = new HashMap<>();
        params.put("width", w);
        params.put("height", h);
        params.put("provider", "kling");
        saveHistory(userId, imageUrl, prompt, "text2img-kling", params);
        return new ImageResult(imageUrl, prompt, params, generationTime);
    }

    private String pollKlingTask(String bearerToken, String taskId) throws Exception {
        String base = getKlingImageBase();
        // 可灵图片 API：GET /generations?pageSize=500 返回任务列表，需按 task_id 过滤（无 /tasks/{id} 端点）
        String url = base + "/generations?pageSize=500";
        for (int i = 0; i < KLING_POLL_MAX_ATTEMPTS; i++) {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + bearerToken)
                    .GET()
                    .timeout(java.time.Duration.ofSeconds(30))
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() != 200) {
                String bodyStr = resp.body();
                log.warn("可灵查询状态失败: status={} body={}", resp.statusCode(), bodyStr);
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "可灵查询状态失败: " + resp.statusCode());
            }
            JSONObject root = JSON.parseObject(resp.body());
            Object dataObj = root.get("data");
            JSONObject taskData = findTaskById(dataObj, taskId);
            if (taskData == null) {
                Thread.sleep(KLING_POLL_INTERVAL_MS);
                continue;
            }
            String status = taskData.getString("task_status");
            if (status == null) status = taskData.getString("status");

            if ("succeed".equals(status) || "succeeded".equals(status) || "completed".equals(status) || "done".equals(status)) {
                String u = extractKlingImageUrl(root, taskData);
                if (u != null && !u.isBlank()) return u;
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "可灵返回成功但无图片 URL");
            }
            if ("failed".equals(status) || "error".equals(status)) {
                String err = taskData.getString("task_status_msg");
                if (err == null || err.isBlank()) err = taskData.getString("fail_reason");
                if (err == null || err.isBlank()) err = taskData.getString("message");
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "可灵任务失败: " + (err != null ? err : "未知"));
            }
            Thread.sleep(KLING_POLL_INTERVAL_MS);
        }
        throw new BusinessException(ErrorCode.INTERNAL_ERROR, "可灵任务超时");
    }

    private static JSONObject findTaskById(Object dataObj, String taskId) {
        if (dataObj instanceof JSONArray arr && !arr.isEmpty()) {
            for (int j = 0; j < arr.size(); j++) {
                Object item = arr.get(j);
                if (item instanceof JSONObject jo && taskId.equals(jo.getString("task_id"))) {
                    return jo;
                }
            }
        }
        if (dataObj instanceof JSONObject jo && taskId.equals(jo.getString("task_id"))) {
            return jo;
        }
        return null;
    }

    private static String extractKlingImageUrl(JSONObject root, Object dataObj) {
        if (dataObj instanceof JSONArray arr && !arr.isEmpty()) {
            Object first = arr.get(0);
            if (first instanceof JSONObject jo) {
                String u = jo.getString("url");
                if (u != null && !u.isBlank()) return u;
            }
        }
        if (dataObj instanceof JSONObject data) {
            JSONObject taskResult = data.getJSONObject("task_result");
            if (taskResult != null) {
                JSONArray images = taskResult.getJSONArray("images");
                if (images != null && !images.isEmpty()) {
                    Object first = images.get(0);
                    if (first instanceof String) return (String) first;
                    if (first instanceof JSONObject jo) {
                        String u = jo.getString("url");
                        if (u != null && !u.isBlank()) return u;
                    }
                }
                String u = taskResult.getString("url");
                if (u != null && !u.isBlank()) return u;
            }
            String u = data.getString("image_url");
            if (u != null && !u.isBlank()) return u;
        }
        String u = root.getString("image_url");
        if (u != null && !u.isBlank()) return u;
        return root.getString("url");
    }

    private String saveImage(String base64Image, Long userId) {
        if (!StringUtils.hasText(base64Image)) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "图像生成结果为空，无法保存");
        }
        try {
            String payload = base64Image;
            int comma = payload.indexOf(',');
            if (payload.startsWith("data:") && comma >= 0) {
                payload = payload.substring(comma + 1);
            }
            byte[] bytes = Base64.getDecoder().decode(payload);
            if (bytes.length == 0) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "图像生成结果为空，无法保存");
            }

            java.nio.file.Path dir = java.nio.file.Path.of(localImageUploadDir).toAbsolutePath().normalize();
            java.nio.file.Files.createDirectories(dir);
            String filename = "img_" + (userId != null ? userId : 0L) + "_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + ".png";
            java.nio.file.Path target = dir.resolve(filename).normalize();
            if (!target.startsWith(dir)) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "图像保存路径非法");
            }
            java.nio.file.Files.write(target, bytes);

            String prefix = StringUtils.hasText(imagePublicUrlPrefix) ? imagePublicUrlPrefix.trim() : "/uploads/images";
            prefix = prefix.replaceAll("/$", "");
            return prefix + "/" + filename;
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "图像 Base64 解码失败");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("保存生成图片失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "保存生成图片失败: " + e.getMessage());
        }
    }

    private String downloadImageAsBase64(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) return null;
        try {
            if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(imageUrl))
                        .timeout(java.time.Duration.ofSeconds(30))
                        .GET()
                        .build();
                HttpResponse<byte[]> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofByteArray());
                if (resp.statusCode() != 200) return null;
                byte[] bytes = resp.body();
                return bytes != null && bytes.length > 0 ? Base64.getEncoder().encodeToString(bytes) : null;
            }
            java.nio.file.Path path = java.nio.file.Path.of(imageUrl);
            if (java.nio.file.Files.exists(path)) {
                byte[] bytes = java.nio.file.Files.readAllBytes(path);
                return Base64.getEncoder().encodeToString(bytes);
            }
        } catch (Exception e) {
            log.debug("下载图片失败: {}", e.getMessage());
        }
        return null;
    }

    private void saveHistory(Long userId, String imageUrl, String prompt, String type, Map<String, Object> parameters) {
        AiImageGeneration entity = new AiImageGeneration();
        entity.setUserId(userId);
        entity.setImageUrl(imageUrl);
        entity.setPrompt(prompt);
        entity.setGenerationType(type);
        entity.setParameters(JSON.toJSONString(parameters));
        entity.setStatus(1);
        imageGenerationRepository.save(entity);
    }
}
