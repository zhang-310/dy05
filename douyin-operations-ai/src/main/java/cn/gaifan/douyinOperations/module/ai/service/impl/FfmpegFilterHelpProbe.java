package cn.gaifan.douyinOperations.module.ai.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * V-4：通过 {@code ffmpeg -h filter=&lt;name&gt;} 判断本机构建是否包含指定滤镜（零依赖集成测友好）。
 */
public final class FfmpegFilterHelpProbe {

    private static final Logger log = LoggerFactory.getLogger(FfmpegFilterHelpProbe.class);

    private FfmpegFilterHelpProbe() {
    }

    /**
     * @return true 当进程退出码为 0（滤镜帮助可用）
     */
    public static boolean filterHelpSucceeds(String ffmpegPath, String filterName) {
        if (ffmpegPath == null || ffmpegPath.isBlank() || filterName == null || filterName.isBlank()) {
            return false;
        }
        String safeName = filterName.trim().replaceAll("[^a-zA-Z0-9_]", "");
        if (safeName.isEmpty()) {
            return false;
        }
        ProcessBuilder pb = new ProcessBuilder(ffmpegPath, "-hide_banner", "-h", "filter=" + safeName);
        pb.redirectErrorStream(true);
        try {
            Process p = pb.start();
            // 消耗输出，避免管道阻塞
            try (var in = p.getInputStream()) {
                in.transferTo(java.io.OutputStream.nullOutputStream());
            }
            boolean finished = p.waitFor(25, TimeUnit.SECONDS);
            if (!finished) {
                p.destroyForcibly();
                log.debug("ffmpeg filter probe 超时: {}", safeName);
                return false;
            }
            return p.exitValue() == 0;
        } catch (Exception e) {
            log.debug("ffmpeg filter probe 失败 {}: {}", safeName, e.getMessage());
            return false;
        }
    }

}
