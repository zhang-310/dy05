package cn.gaifan.douyinOperations.module.ai.config.official;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 抖音电商学习中心官方资料采集配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.douyin-school.collector")
public class DouyinSchoolCollectorProperties {

    private boolean enabled = true;

    private String cron = "0 40 2 * * ?";

    private boolean importOnStartup = false;

    private Long userId = 1L;

    private String generalKbName = "douyin";

    private String violationKbName = "douyin_weigui";

    private String baseUrl = "https://school.jinritemai.com";

    private String searchPath = "/api/eschool/v3/library/vector/search";

    private int pageSize = 50;

    private int maxPagesPerKeyword = 20;

    private int maxItems = 5000;

    private int minContentLength = 80;

    private int requestDelayMs = 250;

    private int httpTimeoutSeconds = 12;

    private int progressLogEvery = 100;

    private boolean extractImages = true;

    private boolean extractVideos = true;

    /**
     * 大规模首轮学习默认只采官方正文、结构化字段、图片/视频 URL 和已有字幕字段。
     * OCR/ASR 属于重媒体补采，开启后会显著拉长单条处理时间，适合专题补采而不是铺底座。
     */
    private boolean deepMediaExtractionEnabled = false;

    private int maxImagesPerItem = 8;

    private int maxVideosPerItem = 3;

    private int mediaTextMaxLength = 6000;

    private int maxImageBytes = 5 * 1024 * 1024;

    private int maxVideoBytes = 80 * 1024 * 1024;

    private boolean imageOcrEnabled = false;

    private String imageOcrCommand = "tesseract {input} stdout -l chi_sim+eng --psm 6";

    private int maxImageOcrPerItem = 1;

    private int imageOcrTimeoutSeconds = 8;

    private boolean videoAsrEnabled = false;

    private String videoAsrCommand = "";

    private String videoAsrApiUrl = "";

    private String videoAsrApiKey = "";

    private String videoAsrModel = "whisper-1";

    private int maxVideoAsrPerItem = 1;

    private int videoAsrTimeoutSeconds = 120;

    private List<String> seedUrls = new ArrayList<>(List.of(
            "https://school.jinritemai.com/doudian/web/home",
            "https://school.jinritemai.com/doudian/web/rules",
            "https://school.jinritemai.com/doudian/web/funcs",
            "https://school.jinritemai.com/doudian/web/ecomcase",
            "https://school.jinritemai.com/doudian/web/help/25",
            "https://school.jinritemai.com/doudian/web/topic",
            "https://school.jinritemai.com/doudian/web/article",
            "https://school.jinritemai.com/doudian/web/video-article",
            "https://school.jinritemai.com/doudian/web/course-series"
    ));

    private List<String> seedKeywords = new ArrayList<>(List.of(
            "违规",
            "直播违规",
            "短视频违规",
            "素材违规",
            "千川 素材 违规",
            "商品违规发布",
            "虚假宣传",
            "违规营销",
            "站外引流",
            "诱导互动",
            "低俗",
            "侵权",
            "禁售",
            "处罚",
            "规则",
            "新规",
            "案例",
            "白皮书",
            "高频违规",
            "直播运营",
            "直播规则",
            "直播规范",
            "直播间违规",
            "直播带货",
            "直播话术",
            "直播转化",
            "直播流量",
            "直播商品讲解",
            "主播",
            "助播",
            "场控",
            "中控",
            "福袋",
            "连麦",
            "憋单",
            "短视频",
            "短视频运营",
            "短视频规则",
            "短视频规范",
            "短视频创作",
            "短视频脚本",
            "短视频文案",
            "短视频标题",
            "短视频封面",
            "短视频剪辑",
            "短视频带货",
            "短视频挂车",
            "短视频违规案例",
            "短视频素材违规",
            "千川",
            "巨量千川",
            "素材",
            "千川素材",
            "千川素材审核",
            "广告素材",
            "素材审核",
            "素材规范",
            "图文素材",
            "视频素材",
            "店铺运营",
            "商品发布",
            "商品标题",
            "商品主图",
            "商品详情",
            "商品宣传",
            "价格宣传",
            "功效宣传",
            "售后",
            "达人",
            "达人带货",
            "达人规则",
            "达人橱窗",
            "精选联盟",
            "体验分",
            "商家体验分",
            "创作者信用分",
            "内容营销",
            "流量获取",
            "内容安全",
            "平台治理",
            "治理动态",
            "规则解读",
            "处罚申诉",
            "信用分",
            "营销活动",
            "搜索运营",
            "商城运营",
            "货架电商",
            "内容电商",
            "罗盘",
            "数据诊断",
            "转化率",
            "复购",
            "客单价",
            "选品",
            "排品",
            "测品",
            "爆品",
            "利润品",
            "引流品",
            "平价品"
    ));
}
