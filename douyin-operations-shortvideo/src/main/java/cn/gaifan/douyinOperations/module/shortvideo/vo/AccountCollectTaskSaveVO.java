package cn.gaifan.douyinOperations.module.shortvideo.vo;

import lombok.Data;

/**
 * 发起账号采集任务请求。
 * input 字段支持三种输入：视频链接 / 账号主页链接 / 抖音号，系统自动识别。
 */
@Data
public class AccountCollectTaskSaveVO {

    /** 关联 douyin_account.id（可选） */
    private Long accountId;

    /** 智能输入：粘贴视频链接 / 账号主页链接 / 抖音号均可；collectMode=keyword_video 时为搜索关键词 */
    private String input;

    /**
     * 采集模式：空或 auto 表示智能识别；
     * keyword_video 表示将 input 作为关键词，打开抖音「视频」搜索结果页并滚动采集（需 Playwright +有效 Cookie 时成功率更高）。
     */
    private String collectMode;

    /** 兼容旧字段（等同 input） */
    private String accountUrl;

    /** 目标知识库 ID（可选，不传则自动选择用户首个话术知识库） */
    private Long targetKbId;

    /** 本次最多采集多少条视频，避免一次任务无限滚动 */
    private Integer maxCount;

    /** 获取有效输入（优先 input，回退 accountUrl） */
    public String getEffectiveInput() {
        if (input != null && !input.isBlank()) return input.trim();
        if (accountUrl != null && !accountUrl.isBlank()) return accountUrl.trim();
        return null;
    }
}
