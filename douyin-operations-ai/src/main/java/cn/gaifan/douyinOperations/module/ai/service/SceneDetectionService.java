package cn.gaifan.douyinOperations.module.ai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ffmpeg scenecut 智能场景切分服务。
 * <p>
 * 使用 ffmpeg 内置 {@code select='gt(scene,THRESHOLD)'} 检测场景变化边界，
 * 替代固定 FPS 抽帧，获得更精准的关键帧。
 */
@Service
public class SceneDetectionService {

    private static final Logger log = LoggerFactory.getLogger(SceneDetectionService.class);

    /** showinfo 输出中提取 pts_time 的正则 */
    private static final Pattern PTS_TIME_PATTERN = Pattern.compile("pts_time:([\\d.]+)");

    @Value("${app.video-analysis.ffmpeg-path:ffmpeg}")
    private String ffmpegPath;

    @Value("${app.video-analysis.scene-detection-threshold:0.3}")
    private double defaultThreshold;

    @Value("${app.video-analysis.scene-max-count:20}")
    private int maxSceneCount;

    @Value("${app.video-analysis.work-dir:/tmp/video-analysis}")
    private String workDir;

    /**
     * 场景片段信息
     *
     * @param startTimeSec  场景开始时间（秒）
     * @param endTimeSec    场景结束时间（秒）
     * @param keyframePath  该场景关键帧图片路径
     */
    public record SceneSegment(double startTimeSec, double endTimeSec, String keyframePath) {}

    /**
     * 检测视频中的场景切换边界，输出关键帧。
     *
     * @param videoPath 视频文件路径
     * @param threshold 场景变化阈值（0-1），越小越敏感，推荐 0.3
     * @return 场景片段列表（含关键帧路径和时间戳）
     */
    public List<SceneSegment> detectScenes(String videoPath, double threshold) throws Exception {
        log.info("[SceneDetection] 开始场景检测: videoPath={}, threshold={}", videoPath, threshold);

        String videoId = new File(videoPath).getName().replaceAll("\\.[^.]+$", "");
        Path scenesDir = Paths.get(workDir, "scenes", videoId);
        Files.createDirectories(scenesDir);

        // Step 1: 用 ffmpeg 检测场景变化帧并输出关键帧 + showinfo 日志
        String outputPattern = scenesDir.resolve("scene_%04d.jpg").toString();

        ProcessBuilder pb = new ProcessBuilder(
                ffmpegPath,
                "-i", videoPath,
                "-vf", "select='gt(scene," + threshold + ")',showinfo",
                "-vsync", "vfr",
                "-q:v", "2",
                outputPattern
        );
        pb.redirectErrorStream(true);

        Process process = pb.start();
        // 需要读取 stderr/stdout 获取 showinfo 输出中的 pts_time
        StringBuilder output = new StringBuilder();
        Thread reader = new Thread(() -> appendStreamCapped(process.getInputStream(), output, 512_000),
                "scene-detect-output");
        reader.setDaemon(true);
        reader.start();

        boolean finished = process.waitFor(5, TimeUnit.MINUTES);
        try {
            reader.join(10_000);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }

        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("场景检测超时（5分钟）");
        }

        // ffmpeg select filter 即使成功也可能 exit != 0（如视频太短没有场景切换），
        // 但只要产生了帧文件就算成功
        File[] frameFiles = scenesDir.toFile().listFiles((dir, name) -> name.endsWith(".jpg"));
        if (frameFiles == null || frameFiles.length == 0) {
            log.info("[SceneDetection] 未检测到场景切换（threshold={}），回退到首帧", threshold);
            // 至少提取首帧
            return extractFirstFrame(videoPath, scenesDir);
        }

        // Step 2: 解析 showinfo 输出获取每帧 pts_time
        List<Double> timestamps = parsePtsTimestamps(output.toString());

        // Step 3: 获取视频总时长
        double duration = getVideoDuration(videoPath);

        // Step 4: 将帧文件与时间戳配对，构建 SceneSegment 列表
        // 按文件名排序
        java.util.Arrays.sort(frameFiles, Comparator.comparing(File::getName));
        List<SceneSegment> segments = new ArrayList<>();

        for (int i = 0; i < frameFiles.length; i++) {
            double startTime = (i < timestamps.size()) ? timestamps.get(i) : 0;
            double endTime;
            if (i + 1 < timestamps.size()) {
                endTime = timestamps.get(i + 1);
            } else {
                endTime = duration > 0 ? duration : startTime + 5;
            }
            segments.add(new SceneSegment(startTime, endTime, frameFiles[i].getAbsolutePath()));
        }

        // Step 5: 如果超过上限，按场景时长排序取 top N（较长的场景更重要）
        if (segments.size() > maxSceneCount) {
            log.info("[SceneDetection] 检测到 {} 个场景，取 top {}", segments.size(), maxSceneCount);
            segments.sort(Comparator.comparingDouble(
                    (SceneSegment s) -> s.endTimeSec() - s.startTimeSec()).reversed());
            segments = new ArrayList<>(segments.subList(0, maxSceneCount));
            // 按时间排序回去
            segments.sort(Comparator.comparingDouble(SceneSegment::startTimeSec));
        }

        log.info("[SceneDetection] 完成：{} 个场景片段", segments.size());
        return segments;
    }

    /**
     * 使用默认阈值检测场景
     */
    public List<SceneSegment> detectScenes(String videoPath) throws Exception {
        return detectScenes(videoPath, defaultThreshold);
    }

    /**
     * 从 showinfo 输出中解析 pts_time 时间戳列表
     */
    private List<Double> parsePtsTimestamps(String ffmpegOutput) {
        List<Double> times = new ArrayList<>();
        Matcher m = PTS_TIME_PATTERN.matcher(ffmpegOutput);
        while (m.find()) {
            try {
                times.add(Double.parseDouble(m.group(1)));
            } catch (NumberFormatException e) {
                log.debug("[SceneDetection] 解析 pts_time 失败: {}", m.group(1));
            }
        }
        return times;
    }

    /**
     * 获取视频时长（秒）
     */
    private double getVideoDuration(String videoPath) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    ffmpegPath,
                    "-i", videoPath,
                    "-f", "null",
                    "-"
            );
            pb.redirectErrorStream(true);
            Process p = pb.start();
            StringBuilder out = new StringBuilder();
            appendStreamCapped(p.getInputStream(), out, 64_000);
            p.waitFor(30, TimeUnit.SECONDS);

            // 解析 Duration: HH:MM:SS.xx
            Pattern durPat = Pattern.compile("Duration:\\s*(\\d{2}):(\\d{2}):(\\d{2}\\.\\d+)");
            Matcher m = durPat.matcher(out.toString());
            if (m.find()) {
                return Integer.parseInt(m.group(1)) * 3600
                        + Integer.parseInt(m.group(2)) * 60
                        + Double.parseDouble(m.group(3));
            }
        } catch (Exception e) {
            log.debug("[SceneDetection] 获取视频时长失败: {}", e.getMessage());
        }
        return 0;
    }

    /**
     * 场景检测无结果时的回退：提取首帧
     */
    private List<SceneSegment> extractFirstFrame(String videoPath, Path scenesDir) throws Exception {
        String outputPath = scenesDir.resolve("scene_first.jpg").toString();
        ProcessBuilder pb = new ProcessBuilder(
                ffmpegPath,
                "-i", videoPath,
                "-vframes", "1",
                "-q:v", "2",
                outputPath
        );
        pb.redirectErrorStream(true);
        Process p = pb.start();
        drainStreamAsync(p.getInputStream());
        p.waitFor(30, TimeUnit.SECONDS);

        if (Files.exists(Path.of(outputPath))) {
            double duration = getVideoDuration(videoPath);
            return List.of(new SceneSegment(0, duration > 0 ? duration : 30, outputPath));
        }
        return List.of();
    }

    private static void appendStreamCapped(InputStream in, StringBuilder sb, int maxChars) {
        try (in) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) >= 0) {
                sb.append(new String(buf, 0, n, StandardCharsets.UTF_8));
                if (sb.length() > maxChars) {
                    sb.delete(0, sb.length() - maxChars);
                }
            }
        } catch (IOException ignored) {
        }
    }

    private static void drainStreamAsync(InputStream stream) {
        Thread t = new Thread(() -> {
            try (stream) {
                stream.transferTo(OutputStream.nullOutputStream());
            } catch (IOException ignored) {
            }
        }, "scene-detect-drain");
        t.setDaemon(true);
        t.start();
    }

    /**
     * 清理场景检测临时文件
     */
    public void cleanup(String videoPath) {
        try {
            String videoId = new File(videoPath).getName().replaceAll("\\.[^.]+$", "");
            Path scenesDir = Paths.get(workDir, "scenes", videoId);
            if (Files.exists(scenesDir)) {
                Files.walk(scenesDir).sorted(Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (Exception ignored) {
                    }
                });
            }
        } catch (Exception e) {
            log.debug("[SceneDetection] 清理失败: {}", e.getMessage());
        }
    }

}
