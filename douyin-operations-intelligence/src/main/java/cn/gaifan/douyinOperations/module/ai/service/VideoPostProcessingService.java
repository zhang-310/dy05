package cn.gaifan.douyinOperations.module.ai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 视频后期处理服务
 *
 * 设计原则: 所有滤镜合并为单条 FFmpeg filter_complex，只编码一次
 *
 * 处理流水线 (单通道):
 *   输入 → [去噪 → 锐化 → 色彩调整 → LUT调色 → 黑边] → H.264 编码 → 输出
 */
@Service
public class VideoPostProcessingService {

    private static final Logger log = LoggerFactory.getLogger(VideoPostProcessingService.class);

    @Value("${app.video-analysis.ffmpeg-path:ffmpeg}")
    private String ffmpegPath;

    @Value("${app.video-analysis.work-dir:/tmp/video-analysis}")
    private String workDir;

    @Value("${app.video-analysis.post-processing.enabled:true}")
    private boolean enabled;

    @Value("${app.video-analysis.post-processing.default-lut:}")
    private String defaultLut;

    /**
     * 后期处理参数
     */
    public record PostProcessConfig(
            String lutFile,           // LUT 文件路径 (null = 不调色)
            boolean denoise,          // 降噪
            boolean sharpen,          // 锐化
            boolean addLetterbox,     // 添加电影黑边
            float brightness,         // 亮度调整 (-1.0 ~ 1.0, 0=不调)
            float contrast,           // 对比度 (0.0 ~ 2.0, 1=不调)
            float saturation         // 饱和度 (0.0 ~ 3.0, 1=不调)
    ) {
        public static PostProcessConfig defaults() {
            return new PostProcessConfig(null, true, true, false, 0f, 1f, 1f);
        }
    }

    /**
     * 处理视频 - 单通道 FFmpeg
     *
     * @param inputPath 输入视频路径 (本地文件)
     * @param config    后期配置
     * @return 处理后的视频路径，失败或禁用时返回原路径
     */
    public String processVideo(String inputPath, PostProcessConfig config) {
        if (!enabled) return inputPath;
        if (inputPath == null || inputPath.isBlank()) return inputPath;
        File inputFile = new File(inputPath);
        if (!inputFile.exists() || !inputFile.isFile()) return inputPath;

        try {
            Path workPath = Path.of(workDir);
            if (!Files.exists(workPath)) Files.createDirectories(workPath);

            String outputName = "post_" + UUID.randomUUID().toString().substring(0, 8) + ".mp4";
            String outputPath = workPath.resolve(outputName).toString();

            List<String> filters = new ArrayList<>();

            // 1. 降噪 (hqdn3d: 轻度降噪，保留细节)
            if (config.denoise()) {
                filters.add("hqdn3d=3:3:4:4");
            }

            // 2. 锐化 (unsharp: 适度锐化)
            if (config.sharpen()) {
                filters.add("unsharp=3:3:0.5:3:3:0.5");
            }

            // 3. 色彩调整 (eq: 亮度/对比度/饱和度)
            if (config.brightness() != 0f || config.contrast() != 1f || config.saturation() != 1f) {
                filters.add(String.format("eq=brightness=%.2f:contrast=%.2f:saturation=%.2f",
                        config.brightness(), config.contrast(), config.saturation()));
            }

            // 4. LUT 调色 (支持 .cube 等格式)
            String lut = StringUtils.hasText(config.lutFile()) ? config.lutFile() : defaultLut;
            if (StringUtils.hasText(lut)) {
                File lutFile = new File(lut);
                if (lutFile.exists()) {
                    String lutPath = lutFile.getAbsolutePath().replace("\\", "/");
                    filters.add("lut3d=file='" + lutPath + "'");
                }
            }

            // 5. 电影黑边 (letterbox 2.39:1，竖屏添加上下黑边)
            if (config.addLetterbox()) {
                filters.add("pad=iw:ih*16/9:0:(oh-ih)/2:black");
            }

            if (filters.isEmpty()) {
                return inputPath;
            }

            String filterChain = String.join(",", filters);

            List<String> cmd = new ArrayList<>();
            cmd.add(ffmpegPath);
            cmd.add("-i");
            cmd.add(inputPath);
            cmd.add("-vf");
            cmd.add(filterChain);
            cmd.add("-c:v");
            cmd.add("libx264");
            cmd.add("-preset");
            cmd.add("slow");
            cmd.add("-crf");
            cmd.add("18");
            cmd.add("-c:a");
            cmd.add("copy");
            cmd.add("-movflags");
            cmd.add("+faststart");
            cmd.add("-y");
            cmd.add(outputPath);

            log.debug("FFmpeg 后期处理: {}", String.join(" ", cmd));

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                log.warn("FFmpeg 后期处理失败, exitCode={}", exitCode);
                return inputPath;
            }

            try {
                Files.deleteIfExists(Path.of(inputPath));
            } catch (Exception ignored) {
            }

            return outputPath;

        } catch (Exception e) {
            log.warn("后期处理异常: {}", e.getMessage());
            return inputPath;
        }
    }
}
