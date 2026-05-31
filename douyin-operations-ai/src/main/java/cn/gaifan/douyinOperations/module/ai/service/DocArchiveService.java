package cn.gaifan.douyinOperations.module.ai.service;

import java.time.LocalDate;

/**
 * 文档本地归档服务（可选）
 * 将入库文档另存到本地按日期分目录，便于备份与审计
 */
public interface DocArchiveService {

    /**
     * 归档文档到本地，目录结构：{baseDir}/{yyyy-MM-dd}/{kbName}/{title}.md
     *
     * @param title    文档标题（用于生成文件名）
     * @param content  文档内容
     * @param kbName   知识库名称（用于子目录，可选）
     * @param sourceDate 源文件日期（可选，从文件名解析；null 则用当天）
     */
    void archive(String title, String content, String kbName, LocalDate sourceDate);
}
