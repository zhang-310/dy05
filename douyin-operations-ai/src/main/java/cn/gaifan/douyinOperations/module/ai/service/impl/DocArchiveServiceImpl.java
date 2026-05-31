package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.service.DocArchiveService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 文档本地归档：按日期分文件夹保存到 D:\docs（或配置目录）
 * 支持从老文档文件名（如 evolved_2026-02-15_0253）解析日期，按原日期归档
 */
@Service
public class DocArchiveServiceImpl implements DocArchiveService {

    private static final Logger log = LoggerFactory.getLogger(DocArchiveServiceImpl.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Value("${app.ai.kb.doc-archive-enabled:false}")
    private boolean enabled;

    @Value("${app.ai.kb.doc-archive-dir:D:/docs}")
    private String baseDir;

    @Override
    public void archive(String title, String content, String kbName, LocalDate sourceDate) {
        if (!enabled || baseDir == null || baseDir.isBlank()) return;
        if (title == null || title.isBlank() || content == null) return;

        try {
            LocalDate date = sourceDate != null ? sourceDate : LocalDate.now();
            String dateFolder = date.format(DATE_FMT);
            Path datePath = Path.of(baseDir.trim(), dateFolder);
            if (kbName != null && !kbName.isBlank()) {
                datePath = datePath.resolve(sanitizeDirName(kbName));
            }
            Files.createDirectories(datePath);

            String safeName = sanitizeFileName(title) + ".md";
            Path file = datePath.resolve(safeName);
            Files.writeString(file, content, StandardCharsets.UTF_8);
            log.debug("文档已归档: {}", file);
        } catch (Exception e) {
            log.warn("文档归档失败: {} - {}", title, e.getMessage());
        }
    }

    private String sanitizeFileName(String name) {
        if (name == null) return "untitled";
        return name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }

    private String sanitizeDirName(String name) {
        return sanitizeFileName(name);
    }
}
