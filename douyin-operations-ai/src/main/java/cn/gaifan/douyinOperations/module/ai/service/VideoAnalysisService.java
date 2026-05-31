package cn.gaifan.douyinOperations.module.ai.service;

import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.repository.AiModelRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.annotation.Resource;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;
import java.util.concurrent.TimeUnit;

/**
 * 视频分析服务
 * 支持视频下载、抽帧、ASR 识别、多模态分析
 */
@Service
@ConditionalOnProperty(name = "app.video-analysis.enabled", havingValue = "true", matchIfMissing = false)
public class VideoAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(VideoAnalysisService.class);

    @Value("${app.video-analysis.work-dir:/tmp/video-analysis}")
    private String workDir;

    @Value("${app.video-analysis.ffmpeg-path:ffmpeg}")
    private String ffmpegPath;

    @Value("${app.video-analysis.yt-dlp-path:yt-dlp}")
    private String ytDlpPath;

    /** Netscape 格式 cookies.txt 绝对路径；抖音等站点常需登录态，见 yt-dlp 文档 --cookies */
    @Value("${app.video-analysis.yt-dlp-cookies-file:}")
    private String ytDlpCookiesFile;

    /** 与 ViralMetadataExtractor 一致：如 chrome、edge:Default；无 cookies 文件时可试此项（需本机有对应浏览器） */
    @Value("${app.video-analysis.yt-dlp-cookies-from-browser:}")
    private String ytDlpCookiesFromBrowser;

    @Value("${app.video-analysis.yt-dlp-force-direct-for-douyin:false}")
    private boolean ytDlpForceDirectForDouyin;

    @Value("${app.video-analysis.yt-dlp-user-agent:}")
    private String ytDlpUserAgent;

    @Value("${app.video-analysis.yt-dlp-impersonate-douyin:}")
    private String ytDlpImpersonateDouyin;

    private static final String DEFAULT_DOUYIN_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36";

    @Value("${app.video-analysis.whisper-enabled:false}")
    private boolean whisperEnabled;

    @Value("${app.video-analysis.whisper-model:base}")
    private String whisperModel;

    @Resource
    private LlmClient llmClient;

    @Resource
    private AiModelRepository aiModelRepository;

    /** 抖音：yt-dlp 失败时尝试 Playwright 拦截 CDN（需 app.video-analysis.playwright-enabled=true 且已安装 Chromium） */
    @Value("${app.video-analysis.douyin-yt-dlp-fallback-playwright:true}")
    private boolean douyinYtDlpFallbackPlaywright;

    @Autowired(required = false)
    private PlaywrightDouyinDownloader playwrightDouyinDownloader;

    /**
     * 下载视频
     * @param videoUrl 视频 URL（支持抖音、YouTube 等）
     * @return 本地视频文件路径
     */
    public String downloadVideo(String videoUrl) throws Exception {
        log.info("开始下载视频: {}", videoUrl);

        Path videosDir = Paths.get(workDir, "videos");
        Files.createDirectories(videosDir);

        boolean douyin = videoUrl.toLowerCase().contains("douyin");
        try {
            return downloadWithYtDlp(videoUrl, videosDir);
        } catch (Exception ex) {
            if (douyin && douyinYtDlpFallbackPlaywright
                    && playwrightDouyinDownloader != null
                    && playwrightDouyinDownloader.isAvailable()) {
                log.warn("抖音 yt-dlp 未成功，尝试 Playwright 拦截 CDN 下载（不依赖 cookies.txt 是否被 extractor 接受）: {}",
                        ex.getMessage() != null ? ex.getMessage().split("\n", 2)[0] : ex);
                String path = playwrightDouyinDownloader.download(videoUrl, videosDir);
                log.info("视频下载完成（Playwright）: {}", path);
                return path;
            }
            throw ex;
        }
    }

    /**
     * 使用 yt-dlp 下载；抖音参数与 ViralMetadataExtractor.buildDumpJsonCommand 对齐。
     */
    private String downloadWithYtDlp(String videoUrl, Path videosDir) throws Exception {
        String outputTemplate = videosDir.resolve("%(id)s.%(ext)s").toString();
        List<String> cmd = new ArrayList<>();
        cmd.add(ytDlpPath);

        boolean douyin = videoUrl.toLowerCase().contains("douyin");
        boolean cookiesApplied = false;

        if (StringUtils.hasText(ytDlpCookiesFile)) {
            Path cookiePath = Paths.get(ytDlpCookiesFile.trim());
            if (Files.isRegularFile(cookiePath)) {
                cmd.add("--cookies");
                cmd.add(cookiePath.toAbsolutePath().toString());
                log.info("yt-dlp 使用 cookies 文件: {}", cookiePath.toAbsolutePath());
                cookiesApplied = true;
            } else {
                log.warn("yt-dlp cookies 文件不存在或不是文件，已忽略: {}", ytDlpCookiesFile);
            }
        } else if (StringUtils.hasText(ytDlpCookiesFromBrowser)) {
            cmd.add("--cookies-from-browser");
            cmd.add(ytDlpCookiesFromBrowser.trim());
            log.info("yt-dlp 使用 cookies-from-browser: {}", ytDlpCookiesFromBrowser.trim());
            cookiesApplied = true;
        }

        if (douyin && !cookiesApplied) {
            log.warn("抖音下载未传入任何 cookies：进程内未配置有效的 app.video-analysis.yt-dlp-cookies-file / YT_DLP_COOKIES_FILE，"
                    + "或未设置 yt-dlp-cookies-from-browser。请在运行配置或 application*.yml 中配置并重启（仅写 C:\\secrets\\... 文件而不设变量不会生效）。");
        }

        if (douyin && ytDlpForceDirectForDouyin) {
            cmd.add("--proxy");
            cmd.add("");
        }

        if (douyin) {
            cmd.add("--add-header");
            cmd.add("Referer:https://www.douyin.com/");
            cmd.add("--add-header");
            cmd.add("Accept-Language:zh-CN,zh;q=0.9,en;q=0.8");
        }

        String ua = StringUtils.hasText(ytDlpUserAgent) ? ytDlpUserAgent.trim() : (douyin ? DEFAULT_DOUYIN_UA : null);
        if (ua != null) {
            cmd.add("--user-agent");
            cmd.add(ua);
        }

        if (douyin && StringUtils.hasText(ytDlpImpersonateDouyin)) {
            cmd.add("--impersonate");
            cmd.add(ytDlpImpersonateDouyin.trim());
        }

        cmd.add("-o");
        cmd.add(outputTemplate);
        cmd.add("--no-playlist");
        cmd.add(videoUrl);
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);

        Process process = pb.start();
        boolean finished = process.waitFor(5, TimeUnit.MINUTES);

        if (!finished) {
            process.destroy();
            throw new RuntimeException("视频下载超时（5 分钟）");
        }

        int exit = process.exitValue();
        String combinedLog = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        if (exit != 0) {
            String tail = combinedLog.length() > 1500
                    ? combinedLog.substring(combinedLog.length() - 1500)
                    : combinedLog;
            if (tail.isEmpty()) {
                tail = "（无输出；常见：yt-dlp 未安装/不在 PATH、抖音需有效 cookies、网络阻断。可升级 yt-dlp 或配置 --cookies）";
            }
            log.warn("yt-dlp 下载失败 exit={} url={}\n{}", exit, videoUrl, tail);
            throw new RuntimeException("视频下载失败 (exit=" + exit + "): " + tail);
        }

        File[] files = videosDir.toFile().listFiles((dir, name) -> name.endsWith(".mp4") || name.endsWith(".webm"));
        if (files == null || files.length == 0) {
            File[] any = videosDir.toFile().listFiles();
            String hint = "";
            if (any != null && any.length > 0) {
                hint = " 目录内现有: " + Arrays.stream(any).map(File::getName).collect(Collectors.joining(", "));
            }
            throw new RuntimeException("未找到下载的 mp4/webm 文件。" + hint);
        }

        String videoPath = files[0].getAbsolutePath();
        log.info("视频下载完成: {}", videoPath);
        return videoPath;
    }

    /**
     * 抽帧提取
     * @param videoPath 视频文件路径
     * @param fps 每秒提取帧数（默认1帧/秒）
     * @return 帧图片路径列表
     */
    public List<String> extractFrames(String videoPath, int fps) throws Exception {
        log.info("开始抽帧: videoPath={}, fps={}", videoPath, fps);

        // 创建帧目录
        String videoId = new File(videoPath).getName().replaceAll("\\.[^.]+$", "");
        Path framesDir = Paths.get(workDir, "frames", videoId);
        Files.createDirectories(framesDir);

        // 使用 FFmpeg 抽帧
        String outputPattern = framesDir.resolve("frame_%04d.jpg").toString();
        ProcessBuilder pb = new ProcessBuilder(
                ffmpegPath,
                "-i", videoPath,
                "-vf", "fps=" + fps,
                "-q:v", "2",
                outputPattern
        );

        Process process = pb.start();
        boolean finished = process.waitFor(3, TimeUnit.MINUTES);

        if (!finished) {
            process.destroy();
            throw new RuntimeException("抽帧超时");
        }

        if (process.exitValue() != 0) {
            String error = new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))
                    .lines().reduce("", (a, b) -> a + "\n" + b);
            throw new RuntimeException("抽帧失败: " + error);
        }

        // 收集帧文件路径
        List<String> framePaths = new ArrayList<>();
        File[] files = framesDir.toFile().listFiles((dir, name) -> name.endsWith(".jpg"));
        if (files != null) {
            for (File file : files) {
                framePaths.add(file.getAbsolutePath());
            }
        }

        log.info("抽帧完成: 共 {} 帧", framePaths.size());
        return framePaths;
    }

    /**
     * 图像分析（使用多模态 LLM）
     * @param framePaths 帧图片路径列表
     * @param prompt 分析提示词
     * @return 分析结果列表
     */
    public List<String> analyzeFrames(List<String> framePaths, String prompt) throws Exception {
        log.info("开始图像分析: 共 {} 帧", framePaths.size());

        AiModel model = findVisionModel();
        if (model == null) {
            log.warn("未找到支持视觉的 AI 模型，跳过图像分析");
            return List.of("未配置视觉模型");
        }

        List<String> analyses = new ArrayList<>();
        int maxFrames = Math.min(framePaths.size(), 10); // 最多分析10帧

        for (int i = 0; i < maxFrames; i++) {
            String framePath = framePaths.get(i);
            try {
                // 读取图片并转为 Base64
                byte[] imageBytes = Files.readAllBytes(Paths.get(framePath));
                String base64Image = Base64.getEncoder().encodeToString(imageBytes);

                // 调用多模态 LLM（这里简化处理，实际需要支持图片输入的 LLM 客户端）
                String analysis = analyzeImageWithLlm(model, base64Image, prompt);
                analyses.add(String.format("帧 %d: %s", i + 1, analysis));

            } catch (Exception e) {
                log.error("分析帧失败: {}", framePath, e);
                analyses.add(String.format("帧 %d: 分析失败", i + 1));
            }
        }

        log.info("图像分析完成");
        return analyses;
    }

    /**
     * 提取音频
     * @param videoPath 视频文件路径
     * @return 音频文件路径
     */
    public String extractAudio(String videoPath) throws Exception {
        log.info("开始提取音频: {}", videoPath);

        // 创建音频目录
        String videoId = new File(videoPath).getName().replaceAll("\\.[^.]+$", "");
        Path audioDir = Paths.get(workDir, "audio");
        Files.createDirectories(audioDir);

        String audioPath = audioDir.resolve(videoId + ".wav").toString();

        // 使用 FFmpeg 提取音频
        ProcessBuilder pb = new ProcessBuilder(
                ffmpegPath,
                "-i", videoPath,
                "-vn",
                "-acodec", "pcm_s16le",
                "-ar", "16000",
                "-ac", "1",
                audioPath
        );

        Process process = pb.start();
        boolean finished = process.waitFor(2, TimeUnit.MINUTES);

        if (!finished) {
            process.destroy();
            throw new RuntimeException("音频提取超时");
        }

        if (process.exitValue() != 0) {
            String error = new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))
                    .lines().reduce("", (a, b) -> a + "\n" + b);
            throw new RuntimeException("音频提取失败: " + error);
        }

        log.info("音频提取完成: {}", audioPath);
        return audioPath;
    }

    /**
     * ASR 语音识别
     * @param audioPath 音频文件路径
     * @return 文字稿
     */
    public String transcribeAudio(String audioPath) throws Exception {
        log.info("开始 ASR 识别: {}", audioPath);

        if (!whisperEnabled) {
            log.warn("Whisper 未启用，跳过 ASR 识别");
            return "[ASR 未启用]";
        }

        // 使用 Whisper 进行 ASR
        ProcessBuilder pb = new ProcessBuilder(
                "whisper",
                audioPath,
                "--model", whisperModel,
                "--language", "zh",
                "--output_format", "txt",
                "--output_dir", Paths.get(workDir, "transcripts").toString()
        );

        Process process = pb.start();
        boolean finished = process.waitFor(5, TimeUnit.MINUTES);

        if (!finished) {
            process.destroy();
            throw new RuntimeException("ASR 识别超时");
        }

        if (process.exitValue() != 0) {
            String error = new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))
                    .lines().reduce("", (a, b) -> a + "\n" + b);
            throw new RuntimeException("ASR 识别失败: " + error);
        }

        // 读取转录文本
        String transcriptPath = Paths.get(workDir, "transcripts",
                new File(audioPath).getName().replaceAll("\\.[^.]+$", ".txt")).toString();
        String transcript = Files.readString(Paths.get(transcriptPath));

        log.info("ASR 识别完成: {} 字", transcript.length());
        return transcript;
    }

    /**
     * 清理临时文件
     */
    public void cleanup(String videoPath) {
        try {
            String videoId = new File(videoPath).getName().replaceAll("\\.[^.]+$", "");

            // 删除视频文件
            Files.deleteIfExists(Paths.get(videoPath));

            // 删除帧目录
            Path framesDir = Paths.get(workDir, "frames", videoId);
            if (Files.exists(framesDir)) {
                Files.walk(framesDir).sorted((a, b) -> -a.compareTo(b)).forEach(path -> {
                    try { Files.delete(path); } catch (Exception ignored) {
                        // 文件删除失败，忽略（可能已被删除或无权限）
                    }
                });
            }

            // 删除音频文件
            Files.deleteIfExists(Paths.get(workDir, "audio", videoId + ".wav"));

            log.info("临时文件清理完成: {}", videoId);
        } catch (Exception e) {
            log.error("清理临时文件失败", e);
        }
    }

    // ─── 工具方法 ──────────────────────────────────────

    private AiModel findVisionModel() {
        List<AiModel> models = aiModelRepository.findByStatusAndDeleted(1, 0);
        // 优先选择支持视觉的模型（GPT-4V, Claude Opus 等）
        return models.stream()
                .filter(m -> m.getModelVersion() != null &&
                        (m.getModelVersion().contains("gpt-4") || m.getModelVersion().contains("claude")))
                .findFirst()
                .orElse(null);
    }

    private String analyzeImageWithLlm(AiModel model, String base64Image, String prompt) {
        // 简化实现：实际需要支持图片输入的 LLM 客户端
        // 这里返回占位符，实际应调用 GPT-4V 或 Claude Vision API
        return "图像分析功能需要支持多模态的 LLM 客户端（如 GPT-4V 或 Claude Vision）";
    }
}
