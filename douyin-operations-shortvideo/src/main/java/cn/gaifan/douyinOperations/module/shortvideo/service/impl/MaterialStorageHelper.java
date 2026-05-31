package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.module.shortvideo.entity.SvMaterial;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvMaterialRepository;
import cn.gaifan.douyinOperations.module.shortvideo.util.ImageDHashUtil;
import cn.gaifan.douyinOperations.module.shortvideo.util.MaterialRefreshFailureClassifier;
import cn.gaifan.douyinOperations.module.storage.service.BosStorageService;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Helper for material metadata enrichment, storage operations, and media probing.
 * Extracted from ShortVideoMaterialServiceImpl to reduce class size.
 */
@Component
public class MaterialStorageHelper {

    private static final Logger log = LoggerFactory.getLogger(MaterialStorageHelper.class);
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Autowired
    private SvMaterialRepository materialRepository;

    @Autowired(required = false)
    private BosStorageService bosStorageService;

    @Value("${app.video-analysis.ffmpeg-path:ffmpeg}")
    private String ffmpegPath;
    @Value("${app.video-analysis.ffprobe-path:ffprobe}")
    private String ffprobePath;

    @Value("${app.shortvideo.material.enrich-async:true}")
    private boolean materialEnrichAsync;

    @Value("${app.shortvideo.material.enrich-retry-max-attempts:3}")
    private int materialEnrichRetryMax;

    @Value("${app.shortvideo.material.enrich-retry-delay-ms:400}")
    private long materialEnrichRetryDelayMs;

    /** source_video | none | placeholder */
    @Value("${app.shortvideo.material.thumbnail-fallback:source_video}")
    private String materialThumbnailFallback;

    @Value("${app.shortvideo.material.thumbnail-placeholder-url:}")
    private String materialThumbnailPlaceholderUrl;

    @Autowired(required = false)
    private MeterRegistry meterRegistry;

    @Autowired
    @Qualifier("mediaTaskExecutor")
    private Executor mediaTaskExecutor;

    // ──────────────────── Enrichment Scheduling ────────────────────

    /**
     * A-5: schedule async enrichment (configurable) to avoid ffprobe/ffmpeg blocking the critical path;
     * reloads by ID to detach from caller's transaction.
     */
    public void scheduleEnrichAfterSave(Long materialId, Long ownerId, String materialType, String url) {
        if (materialId == null || !StringUtils.hasText(url)) {
            return;
        }
        if (!materialEnrichAsync) {
            materialRepository.findById(materialId)
                    .ifPresent(mm -> enrichSavedMaterial(mm, ownerId, materialType, url));
            return;
        }
        CompletableFuture.runAsync(() -> materialRepository.findById(materialId)
                .ifPresent(mm -> enrichSavedMaterial(mm, ownerId, materialType, url)), mediaTaskExecutor)
                .exceptionally(ex -> {
                    log.warn("素材元数据异步提取失败 materialId={}", materialId, ex);
                    recordMaterialEnrichMetric(false, "async_error");
                    recordMaterialEnrichDlq("async_error");
                    return null;
                });
    }

    // ──────────────────── Material Enrichment ────────────────────

    /** A-5: image dimensions / video duration+resolution / video thumbnail (when BOS available); metrics + retry + thumbnail fallback */
    public void enrichSavedMaterial(SvMaterial m, Long ownerId, String materialType, String url) {
        if (m == null || m.getId() == null || !StringUtils.hasText(url)) return;
        boolean changed = false;
        String detail = "ok";
        boolean thrown = false;
        try {
            if ("image".equalsIgnoreCase(materialType)) {
                byte[] b = downloadToBytes(url);
                if (b == null && isHttpUrl(url)) {
                    detail = "http_miss";
                    recordMaterialStepFailed("http");
                }
                if (b != null) {
                    try (ByteArrayInputStream in = new ByteArrayInputStream(b)) {
                        BufferedImage img = ImageIO.read(in);
                        if (img != null) {
                            m.setWidth(img.getWidth());
                            m.setHeight(img.getHeight());
                            changed = true;
                            if (!StringUtils.hasText(m.getPerceptualHash())) {
                                String ph = ImageDHashUtil.dHashHex(img);
                                if (StringUtils.hasText(ph)) {
                                    m.setPerceptualHash(ph);
                                    changed = true;
                                }
                            }
                        }
                    }
                    if (b.length > 0 && (m.getFileSize() == null || m.getFileSize() <= 0)) {
                        m.setFileSize((long) b.length);
                        changed = true;
                    }
                    if (!StringUtils.hasText(m.getThumbnailUrl()) && StringUtils.hasText(url)) {
                        m.setThumbnailUrl(url);
                        changed = true;
                    }
                    if (!StringUtils.hasText(m.getContentSha256())) {
                        String h = sha256Hex(b);
                        if (StringUtils.hasText(h)) {
                            m.setContentSha256(h);
                            changed = true;
                        }
                    }
                }
            } else if ("video".equalsIgnoreCase(materialType)) {
                VideoProbe probe = probeVideoWithFfprobe(url);
                if (probe == null) {
                    detail = "ffprobe_miss";
                    recordMaterialStepFailed("ffprobe");
                }
                if (probe != null) {
                    if (probe.durationSec != null && (m.getDuration() == null || m.getDuration() <= 0)) {
                        m.setDuration(probe.durationSec);
                        changed = true;
                    }
                    if (probe.width != null) {
                        m.setWidth(probe.width);
                        changed = true;
                    }
                    if (probe.height != null) {
                        m.setHeight(probe.height);
                        changed = true;
                    }
                }
                boolean thumbFromBos = false;
                if (bosStorageService != null && bosStorageService.isConfigured()) {
                    byte[] frame = extractVideoFrameJpeg(url, 1);
                    if (frame == null || frame.length == 0) {
                        recordMaterialStepFailed("ffmpeg");
                    }
                    if (frame != null && frame.length > 0) {
                        try {
                            String key = ownerId + "/shortvideo/material-thumb/" + m.getId() + ".jpg";
                            int attempts = Math.max(1, materialEnrichRetryMax);
                            Exception last = null;
                            for (int i = 0; i < attempts; i++) {
                                try {
                                    String thumbUrl = bosStorageService.uploadBytes(key, frame, "image/jpeg");
                                    if (StringUtils.hasText(thumbUrl)) {
                                        m.setThumbnailUrl(thumbUrl);
                                        changed = true;
                                        thumbFromBos = true;
                                        last = null;
                                        break;
                                    }
                                } catch (Exception e) {
                                    last = e;
                                    if (i < attempts - 1 && isTransientFailure(e)) {
                                        enrichBackoffSleep(i);
                                    } else {
                                        break;
                                    }
                                }
                            }
                            if (!thumbFromBos && last != null) {
                                log.debug("素材缩略图上传跳过: {}", last.getMessage());
                                recordMaterialStepFailed("bos");
                            }
                        } catch (Exception e) {
                            log.debug("素材缩略图上传跳过: {}", e.getMessage());
                            recordMaterialStepFailed("bos");
                        }
                    }
                }
                if (!StringUtils.hasText(m.getThumbnailUrl())) {
                    applyVideoThumbnailFallback(m, url);
                    if (StringUtils.hasText(m.getThumbnailUrl())) {
                        changed = true;
                        if (!thumbFromBos && !"ok".equals(detail)) {
                            detail = "partial_thumb_fallback";
                        }
                    }
                }
                if ((m.getFileSize() == null || m.getFileSize() <= 0)) {
                    Long cl = httpHeadContentLength(url);
                    if (cl == null || cl <= 0) {
                        cl = httpRangeContentLength(url);
                    }
                    if (cl != null && cl > 0) {
                        m.setFileSize(cl);
                        changed = true;
                    } else if (isHttpUrl(url)) {
                        recordMaterialStepFailed("http");
                        if ("ok".equals(detail)) {
                            detail = "http_size_miss";
                        }
                    }
                }
            } else if ("audio".equalsIgnoreCase(materialType)) {
                VideoProbe probe = probeVideoWithFfprobe(url);
                if (probe == null) {
                    detail = "ffprobe_miss";
                    recordMaterialStepFailed("ffprobe");
                }
                if (probe != null && probe.durationSec != null && (m.getDuration() == null || m.getDuration() <= 0)) {
                    m.setDuration(probe.durationSec);
                    changed = true;
                }
                if ((m.getFileSize() == null || m.getFileSize() <= 0)) {
                    Long cl = httpHeadContentLength(url);
                    if (cl == null || cl <= 0) {
                        cl = httpRangeContentLength(url);
                    }
                    if (cl != null && cl > 0) {
                        m.setFileSize(cl);
                        changed = true;
                    } else if (isHttpUrl(url)) {
                        recordMaterialStepFailed("http");
                        if ("ok".equals(detail)) {
                            detail = "http_size_miss";
                        }
                    }
                }
            }
            if (changed) {
                materialRepository.save(m);
            }
        } catch (Exception e) {
            thrown = true;
            detail = MaterialRefreshFailureClassifier.failureReasonBucket(e);
            log.warn("素材元数据提取失败 materialId={} ownerId={} materialType={} detail={}",
                    m.getId(), ownerId, materialType, detail, e);
        } finally {
            recordMaterialEnrichMetric(!thrown, detail);
            if (thrown) {
                recordMaterialEnrichDlq(detail);
            }
        }
    }

    // ──────────────────── Thumbnail Fallback ────────────────────

    private void applyVideoThumbnailFallback(SvMaterial m, String videoUrl) {
        if (m == null || !StringUtils.hasText(videoUrl)) {
            return;
        }
        String mode = materialThumbnailFallback != null ? materialThumbnailFallback.trim().toLowerCase() : "source_video";
        switch (mode) {
            case "none" -> { /* keep empty */ }
            case "placeholder" -> {
                if (StringUtils.hasText(materialThumbnailPlaceholderUrl)) {
                    m.setThumbnailUrl(materialThumbnailPlaceholderUrl.trim());
                }
            }
            default -> m.setThumbnailUrl(videoUrl);
        }
    }

    // ──────────────────── HTTP Utilities ────────────────────

    public byte[] downloadToBytes(String url) {
        if (!StringUtils.hasText(url)) return null;
        try {
            if (url.startsWith("/") && !url.startsWith("//")) {
                Path p = Path.of(url);
                if (Files.exists(p)) return Files.readAllBytes(p);
                p = Path.of(System.getProperty("user.dir", "."), "uploads", url.replaceFirst("^/uploads/", ""));
                if (Files.exists(p)) return Files.readAllBytes(p);
                return null;
            }
            if (url.startsWith("http://") || url.startsWith("https://")) {
                int max = Math.max(1, materialEnrichRetryMax);
                for (int attempt = 0; attempt < max; attempt++) {
                    try {
                        HttpRequest req = HttpRequest.newBuilder()
                                .uri(URI.create(url))
                                .GET()
                                .timeout(java.time.Duration.ofSeconds(60))
                                .build();
                        HttpResponse<byte[]> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofByteArray());
                        if (resp.statusCode() == 200) return resp.body();
                        return null;
                    } catch (Exception e) {
                        if (attempt == max - 1 || !isTransientFailure(e)) {
                            return null;
                        }
                        enrichBackoffSleep(attempt);
                    }
                }
                return null;
            }
            Path p = Path.of(url);
            if (Files.exists(p)) return Files.readAllBytes(p);
        } catch (Exception ignored) {
            // 文件读取失败，返回null
        }
        return null;
    }

    Long httpHeadContentLength(String url) {
        if (!StringUtils.hasText(url) || (!url.startsWith("http://") && !url.startsWith("https://"))) {
            return null;
        }
        int max = Math.max(1, materialEnrichRetryMax);
        for (int attempt = 0; attempt < max; attempt++) {
            try {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .method("HEAD", HttpRequest.BodyPublishers.noBody())
                        .timeout(java.time.Duration.ofSeconds(15))
                        .build();
                HttpResponse<Void> resp = httpClient.send(req, HttpResponse.BodyHandlers.discarding());
                if (resp.statusCode() < 200 || resp.statusCode() >= 400) return null;
                var clOpt = resp.headers().firstValue("Content-Length");
                if (clOpt.isEmpty()) return null;
                try {
                    long v = Long.parseLong(clOpt.get().trim());
                    return v > 0 ? v : null;
                } catch (NumberFormatException e) {
                    return null;
                }
            } catch (Exception e) {
                if (attempt == max - 1 || !isTransientFailure(e)) {
                    log.debug("HEAD Content-Length 跳过: {}", e.getMessage());
                    return null;
                }
                enrichBackoffSleep(attempt);
            }
        }
        return null;
    }

    /**
     * Some object storage / CDN have unreliable HEAD support; use Range probe as fallback:
     * request bytes=0-0, prefer Content-Range total length, then Content-Length.
     */
    Long httpRangeContentLength(String url) {
        if (!StringUtils.hasText(url) || (!url.startsWith("http://") && !url.startsWith("https://"))) {
            return null;
        }
        int max = Math.max(1, materialEnrichRetryMax);
        for (int attempt = 0; attempt < max; attempt++) {
            try {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Range", "bytes=0-0")
                        .GET()
                        .timeout(java.time.Duration.ofSeconds(20))
                        .build();
                HttpResponse<byte[]> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofByteArray());
                int status = resp.statusCode();
                if (status != 200 && status != 206) {
                    return null;
                }
                Optional<String> crOpt = resp.headers().firstValue("Content-Range");
                if (crOpt.isPresent()) {
                    String cr = crOpt.get();
                    int slash = cr.lastIndexOf('/');
                    if (slash > -1 && slash < cr.length() - 1) {
                        String total = cr.substring(slash + 1).trim();
                        if (!"*".equals(total)) {
                            try {
                                long v = Long.parseLong(total);
                                if (v > 0) return v;
                            } catch (NumberFormatException ignored) {
                            }
                        }
                    }
                }
                Optional<String> clOpt = resp.headers().firstValue("Content-Length");
                if (clOpt.isPresent()) {
                    try {
                        long v = Long.parseLong(clOpt.get().trim());
                        if (status == 200 && v > 0) return v;
                    } catch (NumberFormatException ignored) {
                    }
                }
                return null;
            } catch (Exception e) {
                if (attempt == max - 1 || !isTransientFailure(e)) {
                    log.debug("Range Content-Length 跳过: {}", e.getMessage());
                    return null;
                }
                enrichBackoffSleep(attempt);
            }
        }
        return null;
    }

    // ──────────────────── FFprobe / FFmpeg ────────────────────

    record VideoProbe(Integer durationSec, Integer width, Integer height) {}

    VideoProbe probeVideoWithFfprobe(String url) {
        int max = Math.max(1, materialEnrichRetryMax);
        for (int i = 0; i < max; i++) {
            VideoProbe p = probeVideoWithFfprobeOnce(url);
            if (p != null) {
                return p;
            }
            if (i < max - 1) {
                enrichBackoffSleep(i);
            }
        }
        return null;
    }

    private VideoProbe probeVideoWithFfprobeOnce(String url) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    ffprobePath, "-v", "quiet", "-print_format", "json", "-show_format", "-show_streams", url);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            boolean done = p.waitFor(90, TimeUnit.SECONDS);
            if (!done) {
                p.destroyForcibly();
                return null;
            }
            if (p.exitValue() != 0) return null;
            String json = new BufferedReader(new InputStreamReader(p.getInputStream())).lines()
                    .reduce("", (a, b) -> a + b);
            JSONObject root = JSON.parseObject(json);
            Integer w = null, h = null, dur = null;
            JSONArray streams = root.getJSONArray("streams");
            if (streams != null) {
                for (int i = 0; i < streams.size(); i++) {
                    JSONObject st = streams.getJSONObject(i);
                    if (st == null) continue;
                    if (!"video".equalsIgnoreCase(st.getString("codec_type"))) continue;
                    w = st.getInteger("width");
                    h = st.getInteger("height");
                    break;
                }
            }
            JSONObject fmt = root.getJSONObject("format");
            if (fmt != null) {
                String ds = fmt.getString("duration");
                if (ds != null) {
                    try {
                        double d = Double.parseDouble(ds);
                        dur = Math.max(1, (int) Math.round(d));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            if (w == null && h == null && dur == null) return null;
            return new VideoProbe(dur, w, h);
        } catch (Exception e) {
            log.debug("ffprobe 跳过: {}", e.getMessage());
            return null;
        }
    }

    byte[] extractVideoFrameJpeg(String url, int startSec) {
        int max = Math.max(1, materialEnrichRetryMax);
        for (int i = 0; i < max; i++) {
            byte[] b = extractVideoFrameJpegOnce(url, startSec);
            if (b != null && b.length > 0) {
                return b;
            }
            if (i < max - 1) {
                enrichBackoffSleep(i);
            }
        }
        return null;
    }

    private byte[] extractVideoFrameJpegOnce(String url, int startSec) {
        Path tmp = null;
        try {
            tmp = Files.createTempFile("sv-mat-thumb-", ".jpg");
            Path out = tmp;
            int ss = Math.max(0, startSec);
            ProcessBuilder pb = new ProcessBuilder(
                    ffmpegPath, "-y", "-ss", String.valueOf(ss), "-i", url,
                    "-frames:v", "1", "-q:v", "2", out.toString());
            pb.redirectErrorStream(true);
            Process p = pb.start();
            boolean done = p.waitFor(120, TimeUnit.SECONDS);
            if (!done) {
                p.destroyForcibly();
                return null;
            }
            if (p.exitValue() != 0) return null;
            return Files.readAllBytes(out);
        } catch (Exception e) {
            log.debug("ffmpeg 抽帧跳过: {}", e.getMessage());
            return null;
        } finally {
            if (tmp != null) {
                try {
                    Files.deleteIfExists(tmp);
                } catch (Exception ignored) {
                }
            }
        }
    }

    // ──────────────────── Utility Methods ────────────────────

    static boolean isHttpUrl(String url) {
        return StringUtils.hasText(url) && (url.startsWith("http://") || url.startsWith("https://"));
    }

    static String sha256Hex(byte[] data) {
        if (data == null || data.length == 0) {
            return null;
        }
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(data);
            StringBuilder sb = new StringBuilder(d.length * 2);
            for (byte b : d) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    boolean isTransientFailure(Throwable t) {
        if (t == null) {
            return false;
        }
        if (t instanceof java.net.http.HttpTimeoutException || t instanceof java.io.InterruptedIOException) {
            return true;
        }
        if (t instanceof java.io.IOException) {
            return true;
        }
        String m = String.valueOf(t.getMessage()).toLowerCase();
        return m.contains("timeout") || m.contains("connection reset") || m.contains("temporarily unavailable");
    }

    void enrichBackoffSleep(int attemptIndex) {
        long base = Math.max(50L, materialEnrichRetryDelayMs);
        long jitter = ThreadLocalRandom.current().nextLong(0, Math.min(200L, base) + 1);
        try {
            Thread.sleep(base * (1L << Math.min(attemptIndex, 4)) + jitter);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    // ──────────────────── Metrics ────────────────────

    void recordMaterialEnrichMetric(boolean success, String reason) {
        if (meterRegistry == null) {
            return;
        }
        String r = reason != null ? reason : "unknown";
        meterRegistry.counter("shortvideo.material.enrich",
                "outcome", success ? "success" : "failure",
                "reason", r)
                .increment();
    }

    void recordMaterialStepFailed(String step) {
        if (meterRegistry == null) {
            return;
        }
        meterRegistry.counter("shortvideo.material.enrich.step_failed", "step", step).increment();
    }

    /** A-5: async/non-recoverable enrichment failure approximate DLQ (Prometheus: shortvideo_material_enrich_dlq_total) */
    void recordMaterialEnrichDlq(String reason) {
        if (meterRegistry == null) {
            return;
        }
        String r = reason != null && !reason.isBlank() ? reason : "unknown";
        meterRegistry.counter("shortvideo.material.enrich.dlq", "reason", r).increment();
    }
}
