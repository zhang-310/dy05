package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.service.ComfyUIService;
import cn.gaifan.douyinOperations.module.config.service.ConfigService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * ComfyUI 本地服务实现
 * 默认 http://localhost:8188，可通过 ai.comfyui.url 或 COMFYUI_URL 配置
 */
@Service
public class ComfyUIServiceImpl implements ComfyUIService {

    private static final Logger log = LoggerFactory.getLogger(ComfyUIServiceImpl.class);
    private static final int POLL_INTERVAL_MS = 2000;
    private static final int POLL_MAX_ATTEMPTS = 120; // 4 分钟超时

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.video-analysis.work-dir:/tmp/video-edit}")
    private String workDir;

    @Resource
    private ConfigService configService;

    @Override
    public boolean isAvailable() {
        String baseUrl = getBaseUrl();
        if (!StringUtils.hasText(baseUrl)) return false;
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/queue"))
                    .GET()
                    .timeout(Duration.ofSeconds(5))
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return resp.statusCode() == 200;
        } catch (Exception e) {
            log.debug("ComfyUI 不可用: {}", e.getMessage());
            return false;
        }
    }

    private String getBaseUrl() {
        String url = configService != null ? configService.getRawValueByKey("ai.comfyui.url") : null;
        if (url == null || url.isBlank()) {
            url = System.getenv("COMFYUI_URL");
        }
        if (url == null || url.isBlank()) {
            url = "http://localhost:8188";
        }
        return url.trim().replaceAll("/$", "");
    }

    private String getCheckpoint() {
        String ckpt = configService != null ? configService.getRawValueByKey("ai.comfyui.checkpoint") : null;
        if (ckpt == null || ckpt.isBlank()) {
            ckpt = System.getenv("COMFYUI_CHECKPOINT");
        }
        return StringUtils.hasText(ckpt) ? ckpt.trim() : "v1-5-pruned-ema.safetensors";
    }

    @Override
    public String generateKeyframe(String prompt, String characterReferenceUrl, String sceneReferenceUrl,
                                   int width, int height, int steps, double cfg) {
        String baseUrl = getBaseUrl();
        String refUrl = StringUtils.hasText(characterReferenceUrl) ? characterReferenceUrl : sceneReferenceUrl;
        boolean useImg2Img = StringUtils.hasText(refUrl);

        try {
            String inputImageName = null;
            if (useImg2Img) {
                inputImageName = uploadImage(baseUrl, refUrl);
                if (inputImageName == null) {
                    log.warn("参考图上传失败，降级为纯文生图");
                    useImg2Img = false;
                }
            }

            Map<String, Object> workflow = buildWorkflow(prompt, width, height, steps, cfg, useImg2Img, inputImageName);
            String promptId = submitPrompt(baseUrl, workflow);
            String imagePath = pollAndDownload(baseUrl, promptId);
            if (imagePath != null) return imagePath;
            throw new RuntimeException("ComfyUI 生成超时或失败");
        } catch (Exception e) {
            log.error("ComfyUI 生成失败", e);
            throw new RuntimeException("ComfyUI 生成失败: " + e.getMessage(), e);
        }
    }

    private String uploadImage(String baseUrl, String imageUrl) {
        try {
            byte[] bytes = downloadImage(imageUrl);
            if (bytes == null || bytes.length == 0) return null;

            String boundary = "----ComfyUI" + UUID.randomUUID();
            String filename = "ref_" + UUID.randomUUID().toString().substring(0, 8) + ".png";
            byte[] header = ("--" + boundary + "\r\nContent-Disposition: form-data; name=\"image\"; filename=\"" + filename + "\"\r\nContent-Type: image/png\r\n\r\n").getBytes(StandardCharsets.UTF_8);
            byte[] footer = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);
            byte[] body = new byte[header.length + bytes.length + footer.length];
            System.arraycopy(header, 0, body, 0, header.length);
            System.arraycopy(bytes, 0, body, header.length, bytes.length);
            System.arraycopy(footer, 0, body, header.length + bytes.length, footer.length);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/upload/image"))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .timeout(Duration.ofSeconds(30))
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() != 200) {
                log.warn("ComfyUI 上传图片失败: {}", resp.body());
                return null;
            }
            JsonNode node = objectMapper.readTree(resp.body());
            String name = node.path("name").asText(null);
            String subfolder = node.path("subfolder").asText("");
            return StringUtils.hasText(subfolder) ? subfolder + "/" + name : name;
        } catch (Exception e) {
            log.warn("上传参考图失败: {}", e.getMessage());
            return null;
        }
    }

    private byte[] downloadImage(String url) {
        if (!StringUtils.hasText(url)) return null;
        try {
            if (url.startsWith("/") && !url.startsWith("//")) {
                Path p = java.nio.file.Paths.get(url);
                if (Files.exists(p)) return Files.readAllBytes(p);
                return null;
            }
            if (url.startsWith("http://") || url.startsWith("https://")) {
                HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).GET().timeout(Duration.ofSeconds(30)).build();
                HttpResponse<byte[]> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofByteArray());
                if (resp.statusCode() == 200) return resp.body();
            }
        } catch (Exception e) {
            log.warn("下载图片失败: {}", e.getMessage());
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildWorkflow(String prompt, int width, int height, int steps, double cfg,
                                              boolean img2img, String inputImageName) {
        String ckpt = getCheckpoint();
        long seed = System.currentTimeMillis() % 1_000_000_000;
        String negPrompt = "blurry, low quality, distorted, ugly";

        ObjectNode n4 = objectMapper.createObjectNode();
        n4.put("class_type", "CheckpointLoaderSimple");
        ObjectNode i4 = objectMapper.createObjectNode();
        i4.put("ckpt_name", ckpt);
        n4.set("inputs", i4);

        ObjectNode n5 = objectMapper.createObjectNode();
        n5.put("class_type", "EmptyLatentImage");
        ObjectNode i5 = objectMapper.createObjectNode();
        i5.put("width", width);
        i5.put("height", height);
        i5.put("batch_size", 1);
        n5.set("inputs", i5);

        ObjectNode n6 = objectMapper.createObjectNode();
        n6.put("class_type", "CLIPTextEncode");
        ObjectNode i6 = objectMapper.createObjectNode();
        i6.put("text", prompt);
        i6.putArray("clip").add("4").add(1);
        n6.set("inputs", i6);

        ObjectNode n7 = objectMapper.createObjectNode();
        n7.put("class_type", "CLIPTextEncode");
        ObjectNode i7 = objectMapper.createObjectNode();
        i7.put("text", negPrompt);
        i7.putArray("clip").add("4").add(1);
        n7.set("inputs", i7);

        ObjectNode n3 = objectMapper.createObjectNode();
        n3.put("class_type", "KSampler");
        ObjectNode i3 = objectMapper.createObjectNode();
        i3.put("seed", seed);
        i3.put("steps", steps);
        i3.put("cfg", cfg);
        i3.put("sampler_name", "euler");
        i3.put("scheduler", "normal");
        i3.put("denoise", img2img ? 0.75 : 1.0);
        i3.putArray("model").add("4").add(0);
        i3.putArray("positive").add("6").add(0);
        i3.putArray("negative").add("7").add(0);
        i3.putArray("latent_image").add(img2img ? "11" : "5").add(0);
        n3.set("inputs", i3);

        ObjectNode n8 = objectMapper.createObjectNode();
        n8.put("class_type", "VAEDecode");
        ObjectNode i8 = objectMapper.createObjectNode();
        i8.putArray("samples").add("3").add(0);
        i8.putArray("vae").add("4").add(2);
        n8.set("inputs", i8);

        ObjectNode n9 = objectMapper.createObjectNode();
        n9.put("class_type", "SaveImage");
        ObjectNode i9 = objectMapper.createObjectNode();
        i9.put("filename_prefix", "keyframe");
        i9.putArray("images").add("8").add(0);
        n9.set("inputs", i9);

        Map<String, Object> workflow = new java.util.LinkedHashMap<>();
        workflow.put("4", objectMapper.convertValue(n4, Map.class));
        workflow.put("5", objectMapper.convertValue(n5, Map.class));
        workflow.put("6", objectMapper.convertValue(n6, Map.class));
        workflow.put("7", objectMapper.convertValue(n7, Map.class));
        workflow.put("3", objectMapper.convertValue(n3, Map.class));
        workflow.put("8", objectMapper.convertValue(n8, Map.class));
        workflow.put("9", objectMapper.convertValue(n9, Map.class));

        if (img2img && StringUtils.hasText(inputImageName)) {
            ObjectNode n10 = objectMapper.createObjectNode();
            n10.put("class_type", "LoadImage");
            ObjectNode i10 = objectMapper.createObjectNode();
            i10.put("image", inputImageName);
            n10.set("inputs", i10);

            ObjectNode n11 = objectMapper.createObjectNode();
            n11.put("class_type", "VAEEncode");
            ObjectNode i11 = objectMapper.createObjectNode();
            i11.putArray("pixels").add("10").add(0);
            i11.putArray("vae").add("4").add(2);
            n11.set("inputs", i11);

            workflow.put("10", objectMapper.convertValue(n10, Map.class));
            workflow.put("11", objectMapper.convertValue(n11, Map.class));
        }

        return workflow;
    }

    private String submitPrompt(String baseUrl, Map<String, Object> workflow) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("prompt", workflow));
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/prompt"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .timeout(Duration.ofSeconds(30))
                .build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() != 200) {
            JsonNode err = objectMapper.readTree(resp.body());
            throw new RuntimeException("ComfyUI 提交失败: " + err.path("error").path("message").asText(resp.body()));
        }
        JsonNode root = objectMapper.readTree(resp.body());
        return root.path("prompt_id").asText(null);
    }

    private String pollAndDownload(String baseUrl, String promptId) throws Exception {
        for (int i = 0; i < POLL_MAX_ATTEMPTS; i++) {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/history/" + promptId))
                    .GET()
                    .timeout(Duration.ofSeconds(30))
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() != 200) continue;

            JsonNode root = objectMapper.readTree(resp.body());
            JsonNode promptData = root.path(promptId);
            if (promptData.isMissingNode()) {
                Thread.sleep(POLL_INTERVAL_MS);
                continue;
            }
            JsonNode outputs = promptData.path("outputs");
            Iterator<String> nodeIds = outputs.fieldNames();
            while (nodeIds.hasNext()) {
                JsonNode node = outputs.path(nodeIds.next());
                JsonNode images = node.path("images");
                if (images.isMissingNode() || !images.isArray() || images.size() == 0) continue;
                JsonNode img = images.get(0);
                    String filename = img.path("filename").asText(null);
                    String subfolder = img.path("subfolder").asText("");
                    String type = img.path("type").asText("output");
                if (filename != null) {
                    String viewUrl = baseUrl + "/view?filename=" + java.net.URLEncoder.encode(filename, StandardCharsets.UTF_8) + "&type=" + type;
                    if (StringUtils.hasText(subfolder)) {
                        viewUrl += "&subfolder=" + java.net.URLEncoder.encode(subfolder, StandardCharsets.UTF_8);
                    }
                    return downloadToTemp(viewUrl);
                }
            }
            Thread.sleep(POLL_INTERVAL_MS);
        }
        return null;
    }

    private String downloadToTemp(String viewUrl) throws Exception {
        Path workPath = Path.of(workDir);
        if (!Files.exists(workPath)) Files.createDirectories(workPath);
        String name = "comfy_" + UUID.randomUUID().toString().substring(0, 8) + ".png";
        Path dest = workPath.resolve(name);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(viewUrl))
                .GET()
                .timeout(Duration.ofSeconds(60))
                .build();
        HttpResponse<byte[]> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofByteArray());
        if (resp.statusCode() != 200) throw new RuntimeException("下载 ComfyUI 输出失败: " + resp.statusCode());
        Files.write(dest, resp.body());
        return dest.toAbsolutePath().toString();
    }
}
