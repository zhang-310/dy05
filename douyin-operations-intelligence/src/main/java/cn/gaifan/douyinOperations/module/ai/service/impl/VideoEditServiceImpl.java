package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.ai.service.VideoEditService;

import static cn.gaifan.douyinOperations.module.ai.service.VideoEditService.TtsBgmSfxMixRequest;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class VideoEditServiceImpl implements VideoEditService {

    private static final Logger log = LoggerFactory.getLogger(VideoEditServiceImpl.class);

    @Value("${app.video-analysis.ffmpeg-path:ffmpeg}")
    private String ffmpegPath;

    @Value("${app.video-analysis.work-dir:/tmp/video-edit}")
    private String workDir;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    @Override
    public VideoResult trimVideo(TrimRequest request, Long userId) {
        try {
            String outputFile = workDir + "/trim_" + UUID.randomUUID() + ".mp4";

            List<String> command = new ArrayList<>();
            command.add(ffmpegPath);
            command.add("-i");
            command.add(request.videoUrl());
            command.add("-ss");
            command.add(String.valueOf(request.startTime()));
            command.add("-to");
            command.add(String.valueOf(request.endTime()));
            command.add("-c");
            command.add("copy");
            command.add(outputFile);

            executeFFmpeg(command);

            File output = new File(outputFile);
            return new VideoResult(
                    outputFile,
                    (long) ((request.endTime() - request.startTime()) * 1000),
                    output.length(),
                    "mp4"
            );
        } catch (Exception e) {
            log.error("视频剪辑失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "视频剪辑失败: " + e.getMessage());
        }
    }

    @Override
    public VideoResult mergeVideos(MergeRequest request, Long userId) {
        try {
            if (request.videoUrls() == null || request.videoUrls().isEmpty()) {
                throw new BusinessException(ErrorCode.VALIDATION_FAIL, "视频列表不能为空");
            }
            Path workPath = Path.of(workDir).toAbsolutePath().normalize();
            Files.createDirectories(workPath);
            if (!Files.isDirectory(workPath)) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "工作目录无效: " + workPath);
            }

            String runId = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
            List<String> localPaths = new ArrayList<>();
            for (int i = 0; i < request.videoUrls().size(); i++) {
                String url = request.videoUrls().get(i);
                String localName = "clip_" + runId + "_" + i + ".mp4";
                Path localPath = workPath.resolve(localName);
                downloadVideoToFile(url, localPath);
                localPaths.add(localName);
            }

            String listFileName = "list_" + runId + ".txt";
            Path listPath = workPath.resolve(listFileName);
            StringBuilder listContent = new StringBuilder();
            for (String name : localPaths) {
                listContent.append("file '").append(name).append("'\n");
            }
            Files.writeString(listPath, listContent.toString(), StandardCharsets.UTF_8);

            String outputFileName = "merge_" + runId + ".mp4";
            Path outputPath = workPath.resolve(outputFileName);

            boolean isWindows = System.getProperty("os.name", "").toLowerCase().contains("win");
            if (isWindows) {
                Path batPath = workPath.resolve("run_" + runId + ".bat");
                String ffmpegExe = ffmpegPath.contains(" ") ? "\"" + ffmpegPath.replace("\"", "\"\"") + "\"" : ffmpegPath;
                String batContent = "@echo off\r\ncd /d \"%~dp0\"\r\n"
                        + ffmpegExe + " -y -f concat -safe 0 -i \"" + listFileName
                        + "\" -c:v libx264 -preset fast -crf 23 -c:a aac -movflags +faststart \""
                        + outputFileName + "\"\r\n";
                Files.writeString(batPath, batContent, StandardCharsets.UTF_8);
                List<String> cmdCommand = List.of("cmd.exe", "/c", batPath.toString());
                executeFFmpeg(cmdCommand, null);
                Files.deleteIfExists(batPath);
            } else {
                List<String> command = new ArrayList<>();
                command.add(ffmpegPath);
                command.add("-y");
                command.add("-f");
                command.add("concat");
                command.add("-safe");
                command.add("0");
                command.add("-i");
                command.add(listFileName);
                command.add("-c:v");
                command.add("libx264");
                command.add("-preset");
                command.add("fast");
                command.add("-crf");
                command.add("23");
                command.add("-c:a");
                command.add("aac");
                command.add("-movflags");
                command.add("+faststart");
                command.add(outputFileName);
                executeFFmpeg(command, workPath.toFile());
            }

            for (String name : localPaths) {
                Files.deleteIfExists(workPath.resolve(name));
            }
            Files.deleteIfExists(listPath);

            File output = outputPath.toFile();
            return new VideoResult(outputPath.toString(), 0L, output.length(), "mp4");
        } catch (Exception e) {
            log.error("视频合并失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "视频合并失败: " + e.getMessage());
        }
    }

    private void downloadVideoToFile(String url, Path dest) throws Exception {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("视频 URL 为空");
        }
        if (url.startsWith("http://") || url.startsWith("https://")) {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(120))
                    .GET()
                    .build();
            HttpResponse<byte[]> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofByteArray());
            if (resp.statusCode() != 200) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "下载视频失败: HTTP " + resp.statusCode() + " " + url);
            }
            Files.write(dest, resp.body());
        } else {
            Path src = Path.of(url);
            if (!Files.exists(src)) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "视频文件不存在: " + url);
            }
            Files.copy(src, dest);
        }
    }

    @Override
    public VideoResult addSubtitles(SubtitleRequest request, Long userId) {
        try {
            // 生成 SRT 字幕文件
            String srtFile = workDir + "/subtitle_" + UUID.randomUUID() + ".srt";
            StringBuilder srtContent = new StringBuilder();

            for (int i = 0; i < request.subtitles().size(); i++) {
                SubtitleItem item = request.subtitles().get(i);
                srtContent.append(i + 1).append("\n");
                srtContent.append(formatTime(item.startTime())).append(" --> ")
                        .append(formatTime(item.endTime())).append("\n");
                srtContent.append(item.text()).append("\n\n");
            }

            java.nio.file.Files.writeString(
                    java.nio.file.Path.of(srtFile),
                    srtContent.toString()
            );

            String outputFile = workDir + "/subtitle_" + UUID.randomUUID() + ".mp4";

            List<String> command = new ArrayList<>();
            command.add(ffmpegPath);
            command.add("-i");
            command.add(request.videoUrl());
            command.add("-vf");
            command.add("subtitles=" + srtFile + ":force_style='FontName=" +
                    request.fontFamily() + ",FontSize=" + request.fontSize() +
                    ",PrimaryColour=" + request.fontColor() + "'");
            command.add(outputFile);

            executeFFmpeg(command);

            File output = new File(outputFile);
            return new VideoResult(outputFile, 0L, output.length(), "mp4");
        } catch (Exception e) {
            log.error("添加字幕失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "添加字幕失败: " + e.getMessage());
        }
    }

    @Override
    public VideoResult addBackgroundMusic(MusicRequest request, Long userId) {
        try {
            String outputFile = workDir + "/music_" + UUID.randomUUID() + ".mp4";

            List<String> command = new ArrayList<>();
            command.add(ffmpegPath);
            command.add("-i");
            command.add(request.videoUrl());
            command.add("-i");
            command.add(request.musicUrl());
            command.add("-filter_complex");

            String filter = "[1:a]volume=" + request.volume();
            if (request.fadeIn()) {
                filter += ",afade=t=in:st=0:d=2";
            }
            if (request.fadeOut()) {
                filter += ",afade=t=out:st=58:d=2";
            }
            filter += "[a1];[0:a][a1]amix=inputs=2:duration=first[aout]";

            command.add(filter);
            command.add("-map");
            command.add("0:v");
            command.add("-map");
            command.add("[aout]");
            command.add("-c:v");
            command.add("copy");
            command.add(outputFile);

            executeFFmpeg(command);

            File output = new File(outputFile);
            return new VideoResult(outputFile, 0L, output.length(), "mp4");
        } catch (Exception e) {
            log.error("添加背景音乐失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "添加背景音乐失败: " + e.getMessage());
        }
    }

    @Override
    public VideoResult transcodeVideo(TranscodeRequest request, Long userId) {
        try {
            String outputFile = workDir + "/transcode_" + UUID.randomUUID() + "." + request.format();

            List<String> command = new ArrayList<>();
            command.add(ffmpegPath);
            command.add("-i");
            command.add(request.videoUrl());

            if (request.resolution() != null) {
                command.add("-s");
                command.add(request.resolution());
            }

            if (request.bitrate() != null) {
                command.add("-b:v");
                command.add(request.bitrate() + "k");
            }

            command.add(outputFile);

            executeFFmpeg(command);

            File output = new File(outputFile);
            return new VideoResult(outputFile, 0L, output.length(), request.format());
        } catch (Exception e) {
            log.error("视频转码失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "视频转码失败: " + e.getMessage());
        }
    }

    @Override
    public VideoResult autoCompose(AutoComposeRequest request, Long userId) {
        try {
            // 1. 合并视频片段
            MergeRequest mergeRequest = new MergeRequest(request.videoClips(), "fade");
            VideoResult merged = mergeVideos(mergeRequest, userId);

            // 2. 音频处理：3轨混音 (TTS+BGM+SFX) 或 2轨 (配音+BGM)
            if ((request.voiceClipUrls() != null && !request.voiceClipUrls().isEmpty())
                    || (request.musicUrl() != null && !request.musicUrl().isBlank())
                    || (request.sfxUrls() != null && !request.sfxUrls().isEmpty())) {
                merged = addTtsBgmSfxMix(new TtsBgmSfxMixRequest(
                        merged.videoUrl(),
                        request.voiceClipUrls(),
                        request.musicUrl(),
                        request.sfxUrls(),
                        0.3, true, true
                ), userId);
            }

            // 3. 添加字幕
            List<SubtitleItem> subs = request.subtitles();
            if (subs == null && request.scriptText() != null && !request.scriptText().isBlank()) {
                subs = buildSubtitlesFromScript(request.scriptText());
            }
            if (subs != null && !subs.isEmpty()) {
                SubtitleRequest subReq = new SubtitleRequest(
                        merged.videoUrl(), subs, "Arial", 24, "&Hffffff");
                merged = addSubtitles(subReq, userId);
            }

            return merged;
        } catch (Exception e) {
            log.error("自动成片失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "自动成片失败: " + e.getMessage());
        }
    }

    private VideoResult replaceVideoAudioWithVoice(String videoUrl, List<String> voiceUrls, Long userId) throws Exception {
        Path workPath = Path.of(workDir).toAbsolutePath().normalize();
        Files.createDirectories(workPath);
        String runId = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        Path mergedVoicePath;
        if (voiceUrls.size() == 1) {
            String name = "voice_" + runId + "_0.mp3";
            Path localPath = workPath.resolve(name);
            downloadAudioToFile(voiceUrls.get(0), localPath);
            mergedVoicePath = localPath;
        } else {
            List<String> localPaths = new ArrayList<>();
            for (int i = 0; i < voiceUrls.size(); i++) {
                String name = "voice_" + runId + "_" + i + ".mp3";
                Path localPath = workPath.resolve(name);
                downloadAudioToFile(voiceUrls.get(i), localPath);
                localPaths.add(name);
            }
            String listFileName = "list_voice_" + runId + ".txt";
            Path listPath = workPath.resolve(listFileName);
            StringBuilder sb = new StringBuilder();
            for (String name : localPaths) {
                sb.append("file '").append(name).append("'\n");
            }
            Files.writeString(listPath, sb.toString(), StandardCharsets.UTF_8);
            String mergedVoiceFileName = "voice_merged_" + runId + ".mp3";
            mergedVoicePath = workPath.resolve(mergedVoiceFileName);
            boolean isWindows = System.getProperty("os.name", "").toLowerCase().contains("win");
            if (isWindows) {
                Path batPath = workPath.resolve("run_voice_" + runId + ".bat");
                String batContent = "@echo off\r\ncd /d \"%~dp0\"\r\n"
                        + ffmpegPath + " -y -f concat -safe 0 -i \"" + listFileName + "\" -c copy \"" + mergedVoiceFileName + "\"\r\n";
                Files.writeString(batPath, batContent, StandardCharsets.UTF_8);
                executeFFmpeg(List.of("cmd.exe", "/c", batPath.toString()), null);
                Files.deleteIfExists(batPath);
            } else {
                List<String> cmd = List.of(ffmpegPath, "-y", "-f", "concat", "-safe", "0", "-i", listFileName, "-c", "copy", mergedVoiceFileName);
                executeFFmpeg(cmd, workPath.toFile());
            }
            for (String name : localPaths) {
                Files.deleteIfExists(workPath.resolve(name));
            }
            Files.deleteIfExists(listPath);
        }

        String outputFile = workPath + "/compose_voice_" + runId + ".mp4";
        List<String> replaceCmd = new ArrayList<>();
        replaceCmd.add(ffmpegPath);
        replaceCmd.add("-y");
        replaceCmd.add("-i");
        replaceCmd.add(videoUrl);
        replaceCmd.add("-i");
        replaceCmd.add(mergedVoicePath.toString());
        replaceCmd.add("-c:v");
        replaceCmd.add("copy");
        replaceCmd.add("-map");
        replaceCmd.add("0:v");
        replaceCmd.add("-map");
        replaceCmd.add("1:a");
        replaceCmd.add("-shortest");
        replaceCmd.add(outputFile);
        executeFFmpeg(replaceCmd, null);
        Files.deleteIfExists(mergedVoicePath);
        File output = new File(outputFile);
        return new VideoResult(outputFile, 0L, output.length(), "mp4");
    }

    private void downloadAudioToFile(String url, Path dest) throws Exception {
        if (url.startsWith("http://") || url.startsWith("https://")) {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(60))
                    .GET()
                    .build();
            HttpResponse<byte[]> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofByteArray());
            if (resp.statusCode() != 200) throw new BusinessException(ErrorCode.INTERNAL_ERROR, "下载配音失败: HTTP " + resp.statusCode());
            Files.write(dest, resp.body());
        } else {
            Files.copy(Path.of(url), dest);
        }
    }

    private List<VideoEditService.SubtitleItem> buildSubtitlesFromScript(String scriptText) {
        List<VideoEditService.SubtitleItem> items = new ArrayList<>();
        String[] parts = scriptText.split("[，。！？、；\\s]+");
        double start = 0;
        for (String p : parts) {
            String t = p.trim();
            if (t.isEmpty()) continue;
            double duration = 1.5 + t.length() * 0.1;
            items.add(new VideoEditService.SubtitleItem(start, start + duration, t));
            start += duration;
        }
        return items;
    }

    private void executeFFmpeg(List<String> command) throws Exception {
        executeFFmpeg(command, null);
    }

    private void executeFFmpeg(List<String> command, File workDirFile) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        if (workDirFile != null) {
            pb.directory(workDirFile);
        }
        pb.redirectErrorStream(true);
        Process process;
        try {
            process = pb.start();
        } catch (java.io.IOException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            if (msg.contains("CreateProcess error=2") || msg.contains("系统找不到指定的文件") || msg.contains("No such file")) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                        "FFmpeg 未安装或未加入 PATH。请安装 FFmpeg 或配置 app.video-analysis.ffmpeg-path。");
            }
            throw e;
        }

        StringBuilder errBuf = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.debug("FFmpeg: {}", line);
                if (line.toLowerCase().contains("error") || line.contains("Invalid")) {
                    errBuf.append(line).append("; ");
                }
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            String errHint = errBuf.length() > 0 ? " " + errBuf : "";
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "FFmpeg 执行失败，退出码: " + exitCode + errHint);
        }
    }

    @Override
    public VideoResult addTtsBgmSfxMix(TtsBgmSfxMixRequest request, Long userId) {
        try {
            Path workPath = Path.of(workDir).toAbsolutePath().normalize();
            Files.createDirectories(workPath);
            String runId = UUID.randomUUID().toString().replace("-", "").substring(0, 12);

            List<String> inputs = new ArrayList<>();
            inputs.add("-i");
            inputs.add(request.videoUrl());
            int ttsIdx = 1, bgmIdx = -1, sfxIdx = -1;

            Path ttsPath = null;
            if (request.ttsUrls() != null && !request.ttsUrls().isEmpty()) {
                ttsPath = mergeAudioClips(request.ttsUrls(), workPath, runId + "_tts", userId);
                inputs.add("-i");
                inputs.add(ttsPath.toString());
                bgmIdx = 2;
            }
            Path bgmPath = null;
            if (request.bgmUrl() != null && !request.bgmUrl().isBlank()) {
                bgmPath = workPath.resolve("bgm_" + runId + ".mp3");
                downloadAudioToFile(request.bgmUrl(), bgmPath);
                inputs.add("-i");
                inputs.add(bgmPath.toString());
                sfxIdx = (ttsPath != null ? 2 : 1) + 1;
            }
            Path sfxPath = null;
            if (request.sfxUrls() != null && !request.sfxUrls().isEmpty()) {
                sfxPath = mergeAudioClips(request.sfxUrls(), workPath, runId + "_sfx", userId);
                inputs.add("-i");
                inputs.add(sfxPath.toString());
            }

            if (ttsPath == null && bgmPath == null && sfxPath == null) {
                return new VideoResult(request.videoUrl(), 0L, 0L, "mp4");
            }

            List<String> mixParts = new ArrayList<>();
            int ai = 1;
            if (ttsPath != null) {
                mixParts.add("[" + ai + ":a]");
                ai++;
            }
            if (bgmPath != null) {
                double vol = request.bgmVolume() != null ? request.bgmVolume() : 0.3;
                mixParts.add("[" + ai + ":a]volume=" + vol + "[a" + ai + "];[a" + ai + "]");
                ai++;
            }
            if (sfxPath != null) {
                mixParts.add("[" + ai + ":a]");
            }

            String filter = String.join("", mixParts).replaceAll("\\[a\\d+\\];\\[a\\d+\\]", "");
            if (mixParts.size() == 1) {
                if (!filter.contains("volume")) filter = filter + "volume=1";
                filter = filter + "[outa]";
            } else {
                filter += "amix=inputs=" + mixParts.size() + ":duration=longest:dropout_transition=2[outa]";
            }

            String outputFile = workPath + "/mix3_" + runId + ".mp4";
            List<String> cmd = new ArrayList<>();
            cmd.add(ffmpegPath);
            cmd.add("-y");
            cmd.addAll(inputs);
            cmd.add("-filter_complex");
            cmd.add(filter.toString());
            cmd.add("-map");
            cmd.add("0:v");
            cmd.add("-map");
            cmd.add("[outa]");
            cmd.add("-c:v");
            cmd.add("copy");
            cmd.add("-shortest");
            cmd.add(outputFile);

            executeFFmpeg(cmd, null);
            if (ttsPath != null) Files.deleteIfExists(ttsPath);
            if (bgmPath != null) Files.deleteIfExists(bgmPath);
            if (sfxPath != null) Files.deleteIfExists(sfxPath);

            File output = new File(outputFile);
            return new VideoResult(outputFile, 0L, output.length(), "mp4");
        } catch (Exception e) {
            log.error("3轨混音失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "3轨混音失败: " + e.getMessage());
        }
    }

    private Path mergeAudioClips(List<String> urls, Path workPath, String prefix, Long userId) throws Exception {
        if (urls == null || urls.isEmpty()) return null;
        if (urls.size() == 1) {
            Path p = workPath.resolve(prefix + ".mp3");
            downloadAudioToFile(urls.get(0), p);
            return p;
        }
        List<String> localPaths = new ArrayList<>();
        for (int i = 0; i < urls.size(); i++) {
            Path p = workPath.resolve(prefix + "_" + i + ".mp3");
            downloadAudioToFile(urls.get(i), p);
            localPaths.add(p.getFileName().toString());
        }
        Path listPath = workPath.resolve("list_" + prefix + ".txt");
        StringBuilder sb = new StringBuilder();
        for (String name : localPaths) {
            sb.append("file '").append(name).append("'\n");
        }
        Files.writeString(listPath, sb.toString(), StandardCharsets.UTF_8);
        Path outPath = workPath.resolve(prefix + "_merged.mp3");
        List<String> cmd = List.of(ffmpegPath, "-y", "-f", "concat", "-safe", "0", "-i", listPath.getFileName().toString(), "-c", "copy", outPath.getFileName().toString());
        executeFFmpeg(cmd, workPath.toFile());
        for (String name : localPaths) Files.deleteIfExists(workPath.resolve(name));
        Files.deleteIfExists(listPath);
        return outPath;
    }

    private String formatTime(double seconds) {
        int hours = (int) (seconds / 3600);
        int minutes = (int) ((seconds % 3600) / 60);
        int secs = (int) (seconds % 60);
        int millis = (int) ((seconds - (int) seconds) * 1000);

        return String.format("%02d:%02d:%02d,%03d", hours, minutes, secs, millis);
    }
}
