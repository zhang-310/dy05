package cn.gaifan.douyinOperations.module.benchmark.service.impl;

import cn.gaifan.douyinOperations.module.benchmark.service.OcrService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * OCR文字识别服务实现
 * 使用 FFmpeg 提取视频帧 + Tesseract OCR 识别文字
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OcrServiceImpl implements OcrService {

    private static final int FRAME_INTERVAL_SECONDS = 2; // 每2秒提取一帧
    private static final String TEMP_FRAMES_DIR = "temp/ocr_frames";

    @Override
    public String recognizeTextFromVideo(String videoPath) {
        log.info("开始从视频提取文字: {}", videoPath);

        try {
            // 1. 创建临时目录
            Path framesDir = Paths.get(TEMP_FRAMES_DIR, String.valueOf(System.currentTimeMillis()));
            Files.createDirectories(framesDir);

            // 2. 使用FFmpeg提取视频帧
            List<String> framePaths = extractFrames(videoPath, framesDir.toString());
            log.info("提取了 {} 帧图片", framePaths.size());

            // 3. 批量OCR识别
            List<OcrResult> results = batchRecognize(framePaths);

            // 4. 合并结果（按时间轴）
            StringBuilder mergedText = new StringBuilder();
            for (OcrResult result : results) {
                if (result.getText() != null && !result.getText().trim().isEmpty()) {
                    mergedText.append(String.format("[%ds] %s\n",
                            result.getTimestamp() / 1000, result.getText()));
                }
            }

            // 5. 清理临时文件
            cleanupTempFiles(framesDir);

            String finalText = mergedText.toString();
            log.info("OCR识别完成，提取了 {} 个字符", finalText.length());
            return finalText;

        } catch (Exception e) {
            log.error("视频OCR识别失败: {}", e.getMessage(), e);
            return "";
        }
    }

    @Override
    public String recognizeTextFromImage(String imagePath) {
        try {
            // 使用Tesseract OCR识别
            ProcessBuilder pb = new ProcessBuilder(
                    "tesseract",
                    imagePath,
                    "stdout",
                    "-l", "chi_sim+eng", // 中文简体+英文
                    "--psm", "6" // 假设单个文本块
            );

            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"));

            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line).append("\n");
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.warn("Tesseract OCR 返回非零退出码: {}", exitCode);
            }

            return result.toString().trim();

        } catch (Exception e) {
            log.error("图片OCR识别失败: {}", e.getMessage(), e);
            return "";
        }
    }

    @Override
    public List<OcrResult> batchRecognize(List<String> imagePaths) {
        List<OcrResult> results = new ArrayList<>();

        for (String imagePath : imagePaths) {
            try {
                String text = recognizeTextFromImage(imagePath);
                Long timestamp = extractTimestampFromFilename(imagePath);

                OcrResult result = new OcrResult(imagePath, text, 1.0, timestamp);
                results.add(result);

            } catch (Exception e) {
                log.error("识别图片失败: {}", imagePath, e);
            }
        }

        return results;
    }

    /**
     * 使用FFmpeg提取视频帧
     */
    private List<String> extractFrames(String videoPath, String outputDir) throws Exception {
        // FFmpeg命令：每N秒提取一帧
        // ffmpeg -i video.mp4 -vf "fps=1/2" output_%04d.png
        ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg",
                "-i", videoPath,
                "-vf", String.format("fps=1/%d", FRAME_INTERVAL_SECONDS),
                Paths.get(outputDir, "frame_%04d.png").toString()
        );

        pb.redirectErrorStream(true);
        Process process = pb.start();

        // 读取输出（避免缓冲区满）
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            // 可以解析FFmpeg输出获取进度
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("FFmpeg提取帧失败，退出码: " + exitCode);
        }

        // 收集生成的帧文件路径
        List<String> framePaths = new ArrayList<>();
        File dir = new File(outputDir);
        File[] files = dir.listFiles((d, name) -> name.startsWith("frame_") && name.endsWith(".png"));

        if (files != null) {
            for (File file : files) {
                framePaths.add(file.getAbsolutePath());
            }
        }

        framePaths.sort(String::compareTo); // 按文件名排序
        return framePaths;
    }

    /**
     * 从文件名提取时间戳
     * 例如：frame_0001.png -> 0秒，frame_0002.png -> 2秒
     */
    private Long extractTimestampFromFilename(String filename) {
        Pattern pattern = Pattern.compile("frame_(\\d+)\\.png");
        Matcher matcher = pattern.matcher(filename);

        if (matcher.find()) {
            int frameNumber = Integer.parseInt(matcher.group(1));
            return (long) (frameNumber - 1) * FRAME_INTERVAL_SECONDS * 1000; // 转换为毫秒
        }

        return 0L;
    }

    /**
     * 清理临时文件
     */
    private void cleanupTempFiles(Path framesDir) {
        try {
            File dir = framesDir.toFile();
            if (dir.exists()) {
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        file.delete();
                    }
                }
                dir.delete();
            }
        } catch (Exception e) {
            log.warn("清理临时文件失败: {}", e.getMessage());
        }
    }
}
