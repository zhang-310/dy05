package cn.gaifan.douyinOperations.module.tianapi.service;

import cn.gaifan.douyinOperations.module.tianapi.vo.*;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

/**
 * TianAPI 统一服务入口
 */
public interface TianApiService {

    // ─── 热搜榜 ──────────────────────────────────────

    /** 抖音热搜榜（约 3 分钟更新，建议配合缓存） */
    List<HotItemVO> douyinHot();

    /** 头条热搜榜 */
    List<HotItemVO> toutiaoHot();

    /** 微博热搜榜 */
    List<HotItemVO> weiboHot();

    /** 全网热搜榜 */
    List<HotItemVO> networkHot();

    /** 百度热搜榜 */
    List<HotItemVO> baiduHot();

    /** 腾讯/微信热搜榜 */
    List<HotItemVO> tencentHot();

    // ─── 文案/话术素材 ──────────────────────────────────────

    /** 朋友圈文案（随机一条） */
    String pyqWenan();

    /** 打工人语录 */
    String dagongrenYulu();

    /** 土味情话 */
    String tuweiQinghua();

    /** 毒鸡汤 */
    String duJitang();

    /** 彩虹屁 */
    String caihongPi();

    /** 渣男语录 */
    String zhananYulu();

    /** 早安心语 */
    String zaoAnXinyu();

    /** 晚安心语 */
    String wanAnXinyu();

    /** 经典台词（含中英对照、来源） */
    Map<String, String> classicDialogue();

    /** 神回复（1–10 条，可选 num） */
    List<Map<String, String>> godReply(int num);

    /** 藏头诗生成，word 2–8 字，len 0五言 1七言 */
    String cangtoushi(String word, int len);

    /** 网络流行语，按关键词查询，word 必填 */
    List<Map<String, String>> hotWord(String word, int num);

    /** 名言警句，随机返回 num 条 */
    List<Map<String, String>> dictum(int num);

    /** 名人名言，typeid 1–24 可选，num 返回数量 */
    List<Map<String, String>> mingyan(int num, Integer typeid);

    /** 舔狗日记（单条） */
    String tiangouRiji();

    /** 雷人笑话，num 1–10 */
    List<Map<String, String>> joke(int num);

    /** 歇后语，num 1–10 */
    List<Map<String, String>> xiehouyu(int num);

    /** 情绪诗句（单条） */
    String moodPoetry();

    /** 民俗对联，num 1–10，fenlei 可选如春联 */
    List<Map<String, String>> msDuilian(int num, String fenlei);

    /** 分类名句，type 必填如春天/中秋节，num 1–20 */
    List<Map<String, String>> flMingju(String type, int num);

    /** 最美宋词（单条） */
    String zuiMeiSongci();

    /** 古籍名句（单条） */
    String guJiMingju();

    /** 励志古言（单条） */
    String liZhiGuyan();

    /** 雷人笑话（单条，兼容） */
    String jokeOne();

    /** 云音乐热评（单条） */
    String hotReview();

    /** 小段子（单条） */
    String xiaoDuanzi();

    /** 顺口溜（单条） */
    String shunKouliu();

    /** 精美句子（单条） */
    String jingMeiJuzi();

    /** 古代情诗（单条） */
    String guDaiQingshi();

    /** 失恋分手句子（单条） */
    String shiLianFenshou();

    /** 绕口令（单条） */
    String raoKouling();

    // ─── 合规/审核（按次计费） ──────────────────────────────────────

    /** 广告法违禁词检测，content 最大 1000 字 */
    AdReviewResultVO adReview(String content);

    /** 文本审核（暴恐/色情/涉政/低俗），content 最大 1000 字 */
    TextAuditResultVO textAudit(String content);

    // ─── 智能文案（按次计费） ──────────────────────────────────────

    /** 智能文案生成，text 主题关键词最多 60 字 */
    String aiTextGenerate(String text);

    // ─── 节假日/简报 ──────────────────────────────────────

    /** 节假日查询，date 格式 2025-01-01 或 2025-01-01,2025-01-02，type 0批量 1年 2月 3范围 */
    JsonNode jiejiari(String date, int type);

    /** 每日简报 */
    List<BulletinItemVO> bulletin();

    /** 是否已配置可用 */
    boolean isEnabled();
}
