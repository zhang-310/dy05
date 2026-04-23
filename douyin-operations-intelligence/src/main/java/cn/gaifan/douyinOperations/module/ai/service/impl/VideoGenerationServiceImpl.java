package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.ai.service.VideoEditService;
import cn.gaifan.douyinOperations.module.ai.service.VideoGenerationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 视频生成服务实现：首尾帧图片 → 过渡视频
 * 使用 ffmpeg 将两张图片合成为带过渡效果的短视频
 */
@Service
public class VideoGenerationServiceImpl implements VideoGenerationService {

    private static final Logger log = LoggerFactory.getLogger(VideoGenerationServiceImpl.class);

    @Value("${app.video-analysis.ffmpeg-path:ffmpeg}")
    private String ffmpegPath;

    @Value("${app.video-analysis.work-dir:/tmp/video-edit}")
    private String workDir;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    @Override
    public VideoEditService.VideoResult generateFromFrames(
            String startFrameUrl,
            String endFrameUrl,
            int durationSec,
            Long userId
    ) {
        if (startFrameUrl == null || startFrameUrl.isBlank() || endFrameUrl == null || endFrameUrl.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "首尾帧 URL 不能为空");
        }
        if (durationSec < 1 || durationSec > 60) {
            durationSec = 5;
        }

        try {
            Path workPath = Path.of(workDir);
            if (!Files.exists(workPath)) {
                Files.createDirectories(workPath);
            }

            String prefix = "gen_" + UUID.randomUUID().toString().substring(0, 8);
            String img1 = workPath.resolve(prefix + "_1.png").toString();
            String img2 = workPath.resolve(prefix + "_2.png").toString();
            String outputFile = workPath.resolve(prefix + ".mp4").toString();

            downloadImage(startFrameUrl, img1);
            downloadImage(endFrameUrl, img2);

            boolean sameImage = startFrameUrl.trim().equals(endFrameUrl.trim());
            if (sameImage) {
                // 首尾同一张图：用 zoompan 实现 Ken Burns 运镜（缓推镜头），避免静止画面
                // zoompan=z='min(zoom+0.0015,1.5)':d=帧数:s=1280x720，5秒=150帧(30fps)
                int fps = 30;
                int totalFrames = durationSec * fps;
                String zoompan = "scale=1280:720:force_original_aspect_ratio=decrease,pad=1280:720:(ow-iw)/2:(oh-ih)/2," +
                        "zoompan=z='min(zoom+0.0012,1.4)':d=" + totalFrames + ":s=1280x720:fps=" + fps;
                List<String> command = new ArrayList<>();
                command.add(ffmpegPath);
                command.add("-y");
                command.add("-loop");
                command.add("1");
                command.add("-i");
                command.add(img1);
                command.add("-t");
                command.add(String.valueOf(durationSec));
                command.add("-vf");
                command.add(zoompan);
                command.add("-c:v");
                command.add("libx264");
                command.add("-pix_fmt");
                command.add("yuv420p");
                command.add("-movflags");
                command.add("+faststart");
                command.add(outputFile);
                executeFFmpeg(command);
            } else {
                // 两张不同图：各占 half 秒，xfade 过渡
                int halfSec = Math.max(1, durationSec / 2);
                List<String> command = new ArrayList<>();
                command.add(ffmpegPath);
                command.add("-y");
                command.add("-loop");
                command.add("1");
                command.add("-i");
                command.add(img1);
                command.add("-t");
                command.add(String.valueOf(halfSec));
                command.add("-vf");
                command.add("scale=1280:720:force_original_aspect_ratio=decrease,pad=1280:720:(ow-iw)/2:(oh-ih)/2");
                command.add("-c:v");
                command.add("libx264");
                command.add("-pix_fmt");
                command.add("yuv420p");
                command.add(workPath.resolve(prefix + "_v1.mp4").toString());
                executeFFmpeg(command);

                command = new ArrayList<>();
                command.add(ffmpegPath);
                command.add("-y");
                command.add("-loop");
                command.add("1");
                command.add("-i");
                command.add(img2);
                command.add("-t");
                command.add(String.valueOf(halfSec));
                command.add("-vf");
                command.add("scale=1280:720:force_original_aspect_ratio=decrease,pad=1280:720:(ow-iw)/2:(oh-ih)/2");
                command.add("-c:v");
                command.add("libx264");
                command.add("-pix_fmt");
                command.add("yuv420p");
                command.add(workPath.resolve(prefix + "_v2.mp4").toString());
                executeFFmpeg(command);

                command = new ArrayList<>();
                command.add(ffmpegPath);
                command.add("-y");
                command.add("-i");
                command.add(workPath.resolve(prefix + "_v1.mp4").toString());
                command.add("-i");
                command.add(workPath.resolve(prefix + "_v2.mp4").toString());
                command.add("-filter_complex");
                command.add("xfade=transition=fade:duration=1:offset=" + (halfSec - 1));
                command.add("-c:v");
                command.add("libx264");
                command.add(outputFile);
                executeFFmpeg(command);

                Files.deleteIfExists(workPath.resolve(prefix + "_v1.mp4"));
                Files.deleteIfExists(workPath.resolve(prefix + "_v2.mp4"));
            }

            Files.deleteIfExists(Path.of(img1));
            Files.deleteIfExists(Path.of(img2));

            File output = new File(outputFile);
            return new VideoEditService.VideoResult(
                    outputFile,
                    (long) durationSec * 1000,
                    output.length(),
                    "mp4"
            );
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("首尾帧视频生成失败", e);
            throw new BusinessException(ErrorCode.AI_MEDIA_GENERATE_FAIL, "视频生成失败: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
        }
    }

    private void downloadImage(String url, String destPath) throws Exception {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("图片 URL 为空");
        }
        if (url.startsWith("http://") || url.startsWith("https://")) {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(60))
                    .GET()
                    .build();
            HttpResponse<byte[]> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofByteArray());
            if (resp.statusCode() != 200) {
                throw new RuntimeException("下载图片失败: HTTP " + resp.statusCode() + " " + url);
            }
            Files.write(Path.of(destPath), resp.body());
        } else {
            // 相对路径或本地文件
            Path src = Path.of(url);
            if (!Files.exists(src)) {
                throw new RuntimeException("图片文件不存在: " + url);
            }
            Files.copy(src, Path.of(destPath));
        }
    }

    private void executeFFmpeg(List<String> command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process;
        try {
            process = pb.start();
        } catch (java.io.IOException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            if (msg.contains("CreateProcess error=2") || msg.contains("系统找不到指定的文件") || msg.contains("No such file")) {
                throw new BusinessException(ErrorCode.AI_MEDIA_GENERATE_FAIL,
                        "FFmpeg 未安装或未加入 PATH。请安装 FFmpeg（如 winget install ffmpeg）或配置 app.video-analysis.ffmpeg-path 指向可执行文件路径。");
            }
            throw e;
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            log.debug("FFmpeg: {}", line);
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("FFmpeg 执行失败，退出码: " + exitCode);
        }
    }
}
