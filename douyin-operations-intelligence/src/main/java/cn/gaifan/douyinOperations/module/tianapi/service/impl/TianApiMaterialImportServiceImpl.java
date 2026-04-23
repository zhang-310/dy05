package cn.gaifan.douyinOperations.module.tianapi.service.impl;

import cn.gaifan.douyinOperations.module.copy.entity.CopyLibrary;
import cn.gaifan.douyinOperations.module.copy.repository.CopyLibraryRepository;
import cn.gaifan.douyinOperations.module.tianapi.config.TianApiProperties;
import cn.gaifan.douyinOperations.module.tianapi.exception.TianApiQuotaExceededException;
import cn.gaifan.douyinOperations.module.tianapi.service.TianApiMaterialImportService;
import cn.gaifan.douyinOperations.module.tianapi.service.TianApiService;
import cn.gaifan.douyinOperations.module.ai.service.KnowledgeBaseService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TianAPI 文案素材自动入库实现
 * 支持同步入库到文案库（copy_library）和 AI 知识库（可选）
 * 配额说明：每类每日限制约 1 万次，类目总数 × 1万 = 账号当日总可用次数。某类触发 code 150 时，仅跳过该分类，继续采集其他类目。
 */
@Service
public class TianApiMaterialImportServiceImpl implements TianApiMaterialImportService {

    private static final Logger log = LoggerFactory.getLogger(TianApiMaterialImportServiceImpl.class);
    private static final int TITLE_MAX_LEN = 80;
    private static final String SOURCE_TYPE_TIANAPI = "tianapi";

    @Resource
    private TianApiService tianApiService;
    @Resource
    private CopyLibraryRepository copyLibraryRepository;
    @Resource
    private TianApiProperties properties;
    @Autowired(required = false)
    private KnowledgeBaseService knowledgeBaseService;

    @Override
    public Map<String, Object> runImport() {
        Map<String, Object> result = new HashMap<>();
        result.put("totalImported", 0);
        result.put("totalSkipped", 0);
        result.put("totalCalls", 0);
        result.put("totalKbImported", 0);
        result.put("byCategory", new HashMap<String, Map<String, Integer>>());

        if (!properties.isConfigured() || !properties.isMaterialImportEnabled()
                || properties.getMaterialImportUserId() == null || properties.getMaterialImportUserId() <= 0) {
            log.debug("TianAPI 素材入库未配置或已禁用，跳过");
            return result;
        }

        Long userId = properties.getMaterialImportUserId();
        int delayMs = Math.max(50, Math.min(2000, properties.getMaterialImportDelayMs()));
        Integer apiCap = properties.getMaterialImportApiCallsPerCategory();
        final int batchHttpLoops;
        final int singleHttpLoops;
        if (apiCap != null && apiCap > 0) {
            int v = Math.max(1, Math.min(10_000, apiCap));
            batchHttpLoops = v;
            singleHttpLoops = v;
            log.info("TianAPI 素材入库使用「按类 HTTP 次数」模式: 每类最多 {} 次请求（批量/单条一致）", v);
        } else {
            int c = Math.max(1, Math.min(2000, properties.getMaterialImportCallsPerCategory()));
            batchHttpLoops = Math.max(1, (c + 9) / 10);
            singleHttpLoops = Math.max(1, Math.min(200, c / 5));
        }

        AtomicInteger totalImported = new AtomicInteger(0);
        AtomicInteger totalSkipped = new AtomicInteger(0);
        AtomicInteger totalCalls = new AtomicInteger(0);
        AtomicInteger totalKbImported = new AtomicInteger(0);

        // 先跑批量类（每次 10 条，API 利用率高），再跑单条类，避免单条类先耗尽配额导致批量类无机会
        Map<String, Integer> godreplyStat = runGodReplyCategory(userId, batchHttpLoops, delayMs, totalImported, totalSkipped, totalCalls, totalKbImported);
        ((Map<String, Map<String, Integer>>) result.get("byCategory")).put("tianapi_godreply", godreplyStat);

        Map<String, Integer> hotwordStat = runHotWordCategory(userId, batchHttpLoops, delayMs, totalImported, totalSkipped, totalCalls, totalKbImported);
        ((Map<String, Map<String, Integer>>) result.get("byCategory")).put("tianapi_hotword", hotwordStat);

        Map<String, Integer> dictumStat = runDictumCategory(userId, batchHttpLoops, delayMs, totalImported, totalSkipped, totalCalls, totalKbImported);
        ((Map<String, Map<String, Integer>>) result.get("byCategory")).put("tianapi_dictum", dictumStat);

        Map<String, Integer> mingyanStat = runMingyanCategory(userId, batchHttpLoops, delayMs, totalImported, totalSkipped, totalCalls, totalKbImported);
        ((Map<String, Map<String, Integer>>) result.get("byCategory")).put("tianapi_mingyan", mingyanStat);

        Map<String, Integer> jokeStat = runJokeCategory(userId, batchHttpLoops, delayMs, totalImported, totalSkipped, totalCalls, totalKbImported);
        ((Map<String, Map<String, Integer>>) result.get("byCategory")).put("tianapi_joke", jokeStat);

        Map<String, Integer> xiehouStat = runXiehouyuCategory(userId, batchHttpLoops, delayMs, totalImported, totalSkipped, totalCalls, totalKbImported);
        ((Map<String, Map<String, Integer>>) result.get("byCategory")).put("tianapi_xiehouyu", xiehouStat);

        Map<String, Integer> msdlStat = runMsDuilianCategory(userId, batchHttpLoops, delayMs, totalImported, totalSkipped, totalCalls, totalKbImported);
        ((Map<String, Map<String, Integer>>) result.get("byCategory")).put("tianapi_msdl", msdlStat);

        Map<String, Integer> flmjStat = runFlMingjuCategory(userId, batchHttpLoops, delayMs, totalImported, totalSkipped, totalCalls, totalKbImported);
        ((Map<String, Map<String, Integer>>) result.get("byCategory")).put("tianapi_flmj", flmjStat);

        // 单条类：每次调用 1 条，限制次数避免耗尽配额
        List<ContentFetcher> singleFetchers = List.of(
                new ContentFetcher("tianapi_pyqwenan", "朋友圈文案", () -> tianApiService.pyqWenan(), 1),
                new ContentFetcher("tianapi_dgryl", "打工人语录", () -> tianApiService.dagongrenYulu(), 1),
                new ContentFetcher("tianapi_saylor", "土味情话", () -> tianApiService.tuweiQinghua(), 1),
                new ContentFetcher("tianapi_dujitang", "毒鸡汤", () -> tianApiService.duJitang(), 1),
                new ContentFetcher("tianapi_caihongpi", "彩虹屁", () -> tianApiService.caihongPi(), 1),
                new ContentFetcher("tianapi_zhanan", "渣男语录", () -> tianApiService.zhananYulu(), 1),
                new ContentFetcher("tianapi_zaoan", "早安心语", () -> tianApiService.zaoAnXinyu(), 1),
                new ContentFetcher("tianapi_wanan", "晚安心语", () -> tianApiService.wanAnXinyu(), 1),
                new ContentFetcher("tianapi_tiangou", "舔狗日记", () -> tianApiService.tiangouRiji(), 1),
                new ContentFetcher("tianapi_moodpoetry", "情绪诗句", () -> tianApiService.moodPoetry(), 1),
                new ContentFetcher("tianapi_zmsc", "最美宋词", () -> tianApiService.zuiMeiSongci(), 1),
                new ContentFetcher("tianapi_gjmj", "古籍名句", () -> tianApiService.guJiMingju(), 1),
                new ContentFetcher("tianapi_lzmy", "励志古言", () -> tianApiService.liZhiGuyan(), 1),
                new ContentFetcher("tianapi_hotreview", "云音乐热评", () -> tianApiService.hotReview(), 1),
                new ContentFetcher("tianapi_mnpara", "小段子", () -> tianApiService.xiaoDuanzi(), 1),
                new ContentFetcher("tianapi_skl", "顺口溜", () -> tianApiService.shunKouliu(), 1),
                new ContentFetcher("tianapi_sentence", "精美句子", () -> tianApiService.jingMeiJuzi(), 1),
                new ContentFetcher("tianapi_qingshi", "古代情诗", () -> tianApiService.guDaiQingshi(), 1),
                new ContentFetcher("tianapi_hsjz", "失恋分手", () -> tianApiService.shiLianFenshou(), 1),
                new ContentFetcher("tianapi_raokouling", "绕口令", () -> tianApiService.raoKouling(), 1)
        );

        for (ContentFetcher f : singleFetchers) {
            Map<String, Integer> stat = runSingleCategory(userId, f, singleHttpLoops, delayMs, totalImported, totalSkipped, totalCalls, totalKbImported);
            ((Map<String, Map<String, Integer>>) result.get("byCategory")).put(f.category, stat);
        }

        // 经典台词：每次 1 条，同单条类
        Map<String, Integer> dialogueStat = runDialogueCategory(userId, singleHttpLoops, delayMs, totalImported, totalSkipped, totalCalls, totalKbImported);
        ((Map<String, Map<String, Integer>>) result.get("byCategory")).put("tianapi_dialogue", dialogueStat);

        result.put("totalImported", totalImported.get());
        result.put("totalSkipped", totalSkipped.get());
        result.put("totalCalls", totalCalls.get());
        result.put("totalKbImported", totalKbImported.get());

        // 当 API 全部失败（code160 未申请接口等）导致 0 条入库时，用内置示例演示流程
        if (totalImported.get() == 0 && totalCalls.get() == 0 && properties.isDemoFallbackOnEmpty()) {
            Map<String, Integer> demoStat = runDemoFallback(userId, totalKbImported);
            int demoImported = demoStat.get("imported");
            int demoKb = demoStat.get("kbImported");
            result.put("totalImported", totalImported.get() + demoImported);
            result.put("totalKbImported", totalKbImported.get() + demoKb);
            result.put("demoFallbackUsed", true);
            log.info("TianAPI API 不可用，已用演示数据入库: imported={}, kbImported={}（请在天聚数行控制台申请接口后获取真实数据）", demoImported, demoKb);
        } else {
            log.info("TianAPI 素材入库完成: imported={}, skipped={}, calls={}, kbImported={}", totalImported.get(), totalSkipped.get(), totalCalls.get(), totalKbImported.get());
        }
        return result;
    }

    /** 内置示例数据（API 未申请时演示入库流程） */
    private static final List<DemoItem> DEMO_SAMPLES = List.of(
            new DemoItem("tianapi_pyqwenan", "朋友圈文案", "今日份开心：早起发现闹钟没响，但自然醒了。"),
            new DemoItem("tianapi_pyqwenan", "朋友圈文案", "生活需要仪式感，比如周末睡到自然醒。"),
            new DemoItem("tianapi_caihongpi", "彩虹屁", "你的存在就像 WiFi，走到哪信号都是满格。"),
            new DemoItem("tianapi_caihongpi", "彩虹屁", "你一笑，我的整个世界都亮了。"),
            new DemoItem("tianapi_dujitang", "毒鸡汤", "万事开头难，然后中间难，最后结尾难。"),
            new DemoItem("tianapi_dujitang", "毒鸡汤", "只要我够努力，老板就能过上他想要的生活。"),
            new DemoItem("tianapi_dgryl", "打工人语录", "上班摸鱼，下班学习，卷死他们。"),
            new DemoItem("tianapi_dialogue", "经典台词", "生活就像一盒巧克力", "《阿甘正传》\n生活就像一盒巧克力，你永远不知道下一颗是什么味道。"),
            new DemoItem("tianapi_godreply", "神回复", "如何优雅地拒绝", "如何优雅地拒绝加班？\n答：我下班后手机没电。"),
            new DemoItem("tianapi_dictum", "名言警句", "鲁迅", "鲁迅：时间就像海绵里的水，只要愿挤，总还是有的。"),
            new DemoItem("tianapi_mingyan", "名人名言", "爱因斯坦", "爱因斯坦：想象力比知识更重要。")
    );

    private record DemoItem(String category, String displayName, String title, String content) {
        DemoItem(String category, String displayName, String content) {
            this(category, displayName, content, content);
        }
    }

    private Map<String, Integer> runDemoFallback(Long userId, AtomicInteger totalKbImported) {
        AtomicInteger imported = new AtomicInteger(0);
        int kbBefore = totalKbImported.get();
        for (DemoItem item : DEMO_SAMPLES) {
            if (copyLibraryRepository.countByUserIdAndCategoryAndContentAndDeleted(userId, item.category, item.content) > 0) continue;
            saveCopy(userId, item.category, item.displayName, item.title, item.content, totalKbImported);
            imported.incrementAndGet();
        }
        return Map.of("imported", imported.get(), "kbImported", totalKbImported.get() - kbBefore);
    }

    private Map<String, Integer> runSingleCategory(Long userId, ContentFetcher fetcher, int numHttpCalls,
                                                   int delayMs, AtomicInteger totalImported, AtomicInteger totalSkipped, AtomicInteger totalCalls,
                                                   AtomicInteger totalKbImported) {
        AtomicInteger imported = new AtomicInteger(0);
        AtomicInteger skipped = new AtomicInteger(0);
        AtomicInteger calls = new AtomicInteger(0);

        for (int i = 0; i < numHttpCalls; i++) {
            sleep(delayMs);
            try {
                String content = fetcher.fetch().get();
                if (!StringUtils.hasText(content)) continue;
                calls.incrementAndGet();

                if (copyLibraryRepository.countByUserIdAndCategoryAndContentAndDeleted(userId, fetcher.category, content) > 0) {
                    skipped.incrementAndGet();
                    continue;
                }
                saveCopy(userId, fetcher.category, fetcher.displayName, content, content, totalKbImported);
                imported.incrementAndGet();
            } catch (TianApiQuotaExceededException e) {
                log.info("TianAPI 分类 {} 今日配额已用尽（每类约1万次/天），已跳过该分类，继续其他类目", fetcher.displayName);
                break;
            } catch (Exception e) {
                log.warn("TianAPI 素材拉取失败: category={}, err={}", fetcher.category, e.getMessage());
            }
        }

        totalImported.addAndGet(imported.get());
        totalSkipped.addAndGet(skipped.get());
        totalCalls.addAndGet(calls.get());
        return Map.of("imported", imported.get(), "skipped", skipped.get(), "calls", calls.get());
    }

    private Map<String, Integer> runDialogueCategory(Long userId, int numHttpCalls, int delayMs,
                                                     AtomicInteger totalImported, AtomicInteger totalSkipped, AtomicInteger totalCalls,
                                                     AtomicInteger totalKbImported) {
        AtomicInteger imported = new AtomicInteger(0);
        AtomicInteger skipped = new AtomicInteger(0);
        AtomicInteger calls = new AtomicInteger(0);

        for (int i = 0; i < numHttpCalls; i++) {
            sleep(delayMs);
            try {
                Map<String, String> m = tianApiService.classicDialogue();
                if (m == null || m.isEmpty()) continue;
                String dialogue = m.getOrDefault("dialogue", "");
                String source = m.getOrDefault("source", "");
                if (!StringUtils.hasText(dialogue)) continue;
                calls.incrementAndGet();

                String content = dialogue;
                if (StringUtils.hasText(m.get("english"))) {
                    content = dialogue + "\n" + m.get("english");
                }
                if (copyLibraryRepository.countByUserIdAndCategoryAndContentAndDeleted(userId, "tianapi_dialogue", content) > 0) {
                    skipped.incrementAndGet();
                    continue;
                }
                String title = StringUtils.hasText(source) ? source : truncate(dialogue, TITLE_MAX_LEN);
                saveCopy(userId, "tianapi_dialogue", "经典台词", title, content, totalKbImported);
                imported.incrementAndGet();
            } catch (TianApiQuotaExceededException e) {
                log.info("TianAPI 分类 经典台词 今日配额已用尽，已跳过该分类");
                break;
            } catch (Exception e) {
                log.warn("TianAPI 经典台词拉取失败: err={}", e.getMessage());
            }
        }

        totalImported.addAndGet(imported.get());
        totalSkipped.addAndGet(skipped.get());
        totalCalls.addAndGet(calls.get());
        return Map.of("imported", imported.get(), "skipped", skipped.get(), "calls", calls.get());
    }

    private Map<String, Integer> runGodReplyCategory(Long userId, int numHttpCalls, int delayMs,
                                                     AtomicInteger totalImported, AtomicInteger totalSkipped, AtomicInteger totalCalls,
                                                     AtomicInteger totalKbImported) {
        AtomicInteger imported = new AtomicInteger(0);
        AtomicInteger skipped = new AtomicInteger(0);
        AtomicInteger calls = new AtomicInteger(0);
        int batchSize = 10;

        for (int b = 0; b < numHttpCalls; b++) {
            sleep(delayMs);
            try {
                List<Map<String, String>> list = tianApiService.godReply(batchSize);
                if (list == null || list.isEmpty()) continue;
                calls.incrementAndGet();

                for (Map<String, String> item : list) {
                    String title = item.getOrDefault("title", "");
                    String content = item.getOrDefault("content", "");
                    if (!StringUtils.hasText(content)) continue;
                    String fullContent = StringUtils.hasText(title) ? title + "\n" + content : content;
                    if (copyLibraryRepository.countByUserIdAndCategoryAndContentAndDeleted(userId, "tianapi_godreply", fullContent) > 0) {
                        skipped.incrementAndGet();
                        continue;
                    }
                    saveCopy(userId, "tianapi_godreply", "神回复", truncate(title, TITLE_MAX_LEN), fullContent, totalKbImported);
                    imported.incrementAndGet();
                }
            } catch (TianApiQuotaExceededException e) {
                log.info("TianAPI 分类 神回复 今日配额已用尽，已跳过该分类");
                break;
            } catch (Exception e) {
                log.warn("TianAPI 神回复拉取失败: err={}", e.getMessage());
            }
        }

        totalImported.addAndGet(imported.get());
        totalSkipped.addAndGet(skipped.get());
        totalCalls.addAndGet(calls.get());
        return Map.of("imported", imported.get(), "skipped", skipped.get(), "calls", calls.get());
    }

    private static final String[] HOT_WORDS = {"一哥", "YYDS", "内卷", "摆烂", "绝绝子", "栓Q", "芭比Q", "emo", "天花板", "破防", "卷", "躺平", "拿捏", "整活", "上头"};

    private Map<String, Integer> runHotWordCategory(Long userId, int numHttpCalls, int delayMs,
                                                    AtomicInteger totalImported, AtomicInteger totalSkipped, AtomicInteger totalCalls,
                                                    AtomicInteger totalKbImported) {
        AtomicInteger imported = new AtomicInteger(0);
        AtomicInteger skipped = new AtomicInteger(0);
        AtomicInteger calls = new AtomicInteger(0);
        int batchSize = 10;

        for (int b = 0; b < numHttpCalls; b++) {
            sleep(delayMs);
            try {
                String word = HOT_WORDS[b % HOT_WORDS.length];
                List<Map<String, String>> list = tianApiService.hotWord(word, batchSize);
                if (list == null || list.isEmpty()) continue;
                calls.incrementAndGet();

                for (Map<String, String> item : list) {
                    String title = item.getOrDefault("title", "");
                    String content = item.getOrDefault("content", "");
                    if (!StringUtils.hasText(content)) continue;
                    String fullContent = StringUtils.hasText(title) ? title + "\n" + content : content;
                    if (copyLibraryRepository.countByUserIdAndCategoryAndContentAndDeleted(userId, "tianapi_hotword", fullContent) > 0) {
                        skipped.incrementAndGet();
                        continue;
                    }
                    saveCopy(userId, "tianapi_hotword", "网络流行语", truncate(title, TITLE_MAX_LEN), fullContent, totalKbImported);
                    imported.incrementAndGet();
                }
            } catch (TianApiQuotaExceededException e) {
                log.info("TianAPI 分类 网络流行语 今日配额已用尽，已跳过该分类");
                break;
            } catch (Exception e) {
                log.warn("TianAPI 网络流行语拉取失败: err={}", e.getMessage());
            }
        }

        totalImported.addAndGet(imported.get());
        totalSkipped.addAndGet(skipped.get());
        totalCalls.addAndGet(calls.get());
        return Map.of("imported", imported.get(), "skipped", skipped.get(), "calls", calls.get());
    }

    private Map<String, Integer> runDictumCategory(Long userId, int numHttpCalls, int delayMs,
                                                   AtomicInteger totalImported, AtomicInteger totalSkipped, AtomicInteger totalCalls,
                                                   AtomicInteger totalKbImported) {
        AtomicInteger imported = new AtomicInteger(0);
        AtomicInteger skipped = new AtomicInteger(0);
        AtomicInteger calls = new AtomicInteger(0);
        int batchSize = 10;

        for (int b = 0; b < numHttpCalls; b++) {
            sleep(delayMs);
            try {
                List<Map<String, String>> list = tianApiService.dictum(batchSize);
                if (list == null || list.isEmpty()) continue;
                calls.incrementAndGet();

                for (Map<String, String> item : list) {
                    String mrname = item.getOrDefault("mrname", "");
                    String content = item.getOrDefault("content", "");
                    if (!StringUtils.hasText(content)) continue;
                    String fullContent = StringUtils.hasText(mrname) ? mrname + "：" + content : content;
                    if (copyLibraryRepository.countByUserIdAndCategoryAndContentAndDeleted(userId, "tianapi_dictum", fullContent) > 0) {
                        skipped.incrementAndGet();
                        continue;
                    }
                    saveCopy(userId, "tianapi_dictum", "名言警句", truncate(mrname, TITLE_MAX_LEN), fullContent, totalKbImported);
                    imported.incrementAndGet();
                }
            } catch (TianApiQuotaExceededException e) {
                log.info("TianAPI 分类 名言警句 今日配额已用尽，已跳过该分类");
                break;
            } catch (Exception e) {
                log.warn("TianAPI 名言警句拉取失败: err={}", e.getMessage());
            }
        }

        totalImported.addAndGet(imported.get());
        totalSkipped.addAndGet(skipped.get());
        totalCalls.addAndGet(calls.get());
        return Map.of("imported", imported.get(), "skipped", skipped.get(), "calls", calls.get());
    }

    private Map<String, Integer> runMingyanCategory(Long userId, int numHttpCalls, int delayMs,
                                                    AtomicInteger totalImported, AtomicInteger totalSkipped, AtomicInteger totalCalls,
                                                    AtomicInteger totalKbImported) {
        AtomicInteger imported = new AtomicInteger(0);
        AtomicInteger skipped = new AtomicInteger(0);
        AtomicInteger calls = new AtomicInteger(0);
        int batchSize = 10;

        for (int b = 0; b < numHttpCalls; b++) {
            sleep(delayMs);
            try {
                int typeid = (b % 24) + 1;
                List<Map<String, String>> list = tianApiService.mingyan(batchSize, typeid);
                if (list == null || list.isEmpty()) continue;
                calls.incrementAndGet();

                for (Map<String, String> item : list) {
                    String author = item.getOrDefault("author", "");
                    String content = item.getOrDefault("content", "");
                    if (!StringUtils.hasText(content)) continue;
                    String fullContent = StringUtils.hasText(author) ? author + "：" + content : content;
                    if (copyLibraryRepository.countByUserIdAndCategoryAndContentAndDeleted(userId, "tianapi_mingyan", fullContent) > 0) {
                        skipped.incrementAndGet();
                        continue;
                    }
                    saveCopy(userId, "tianapi_mingyan", "名人名言", truncate(author, TITLE_MAX_LEN), fullContent, totalKbImported);
                    imported.incrementAndGet();
                }
            } catch (TianApiQuotaExceededException e) {
                log.info("TianAPI 分类 名人名言 今日配额已用尽，已跳过该分类");
                break;
            } catch (Exception e) {
                log.warn("TianAPI 名人名言拉取失败: err={}", e.getMessage());
            }
        }

        totalImported.addAndGet(imported.get());
        totalSkipped.addAndGet(skipped.get());
        totalCalls.addAndGet(calls.get());
        return Map.of("imported", imported.get(), "skipped", skipped.get(), "calls", calls.get());
    }

    /** 雷人笑话：每次 10 条 */
    private Map<String, Integer> runJokeCategory(Long userId, int numHttpCalls, int delayMs,
                                                  AtomicInteger totalImported, AtomicInteger totalSkipped, AtomicInteger totalCalls,
                                                  AtomicInteger totalKbImported) {
        return runBatchCategory(userId, "tianapi_joke", "雷人笑话", numHttpCalls, delayMs,
                () -> tianApiService.joke(10), "title", "content",
                totalImported, totalSkipped, totalCalls, totalKbImported);
    }

    /** 歇后语：quest + result 组合为 content */
    private Map<String, Integer> runXiehouyuCategory(Long userId, int numHttpCalls, int delayMs,
                                                    AtomicInteger totalImported, AtomicInteger totalSkipped, AtomicInteger totalCalls,
                                                    AtomicInteger totalKbImported) {
        AtomicInteger imported = new AtomicInteger(0);
        AtomicInteger skipped = new AtomicInteger(0);
        AtomicInteger calls = new AtomicInteger(0);
        int batchSize = 10;

        for (int b = 0; b < numHttpCalls; b++) {
            sleep(delayMs);
            try {
                List<Map<String, String>> list = tianApiService.xiehouyu(batchSize);
                if (list == null || list.isEmpty()) continue;
                calls.incrementAndGet();

                for (Map<String, String> item : list) {
                    String content = item.getOrDefault("content", "");
                    if (!StringUtils.hasText(content)) continue;
                    if (copyLibraryRepository.countByUserIdAndCategoryAndContentAndDeleted(userId, "tianapi_xiehouyu", content) > 0) {
                        skipped.incrementAndGet();
                        continue;
                    }
                    String title = truncate(item.getOrDefault("quest", ""), TITLE_MAX_LEN);
                    saveCopy(userId, "tianapi_xiehouyu", "歇后语", title, content, totalKbImported);
                    imported.incrementAndGet();
                }
            } catch (TianApiQuotaExceededException e) {
                log.info("TianAPI 分类 歇后语 今日配额已用尽，已跳过该分类");
                break;
            } catch (Exception e) {
                log.warn("TianAPI 歇后语拉取失败: err={}", e.getMessage());
            }
        }

        totalImported.addAndGet(imported.get());
        totalSkipped.addAndGet(skipped.get());
        totalCalls.addAndGet(calls.get());
        return Map.of("imported", imported.get(), "skipped", skipped.get(), "calls", calls.get());
    }

    /** 民俗对联：shanglian+xialian+hengpi 组合 */
    private Map<String, Integer> runMsDuilianCategory(Long userId, int numHttpCalls, int delayMs,
                                                      AtomicInteger totalImported, AtomicInteger totalSkipped, AtomicInteger totalCalls,
                                                      AtomicInteger totalKbImported) {
        AtomicInteger imported = new AtomicInteger(0);
        AtomicInteger skipped = new AtomicInteger(0);
        AtomicInteger calls = new AtomicInteger(0);
        int batchSize = 10;
        String[] fenleiArr = {"春联", "婚联", "寿联", "挽联", ""};

        for (int b = 0; b < numHttpCalls; b++) {
            sleep(delayMs);
            try {
                String fenlei = fenleiArr[b % fenleiArr.length];
                List<Map<String, String>> list = tianApiService.msDuilian(batchSize, fenlei);
                if (list == null || list.isEmpty()) continue;
                calls.incrementAndGet();

                for (Map<String, String> item : list) {
                    String content = item.getOrDefault("content", "");
                    if (!StringUtils.hasText(content)) continue;
                    if (copyLibraryRepository.countByUserIdAndCategoryAndContentAndDeleted(userId, "tianapi_msdl", content) > 0) {
                        skipped.incrementAndGet();
                        continue;
                    }
                    String title = truncate(item.getOrDefault("fenlei", "对联"), TITLE_MAX_LEN);
                    saveCopy(userId, "tianapi_msdl", "民俗对联", title, content, totalKbImported);
                    imported.incrementAndGet();
                }
            } catch (TianApiQuotaExceededException e) {
                log.info("TianAPI 分类 民俗对联 今日配额已用尽，已跳过该分类");
                break;
            } catch (Exception e) {
                log.warn("TianAPI 民俗对联拉取失败: err={}", e.getMessage());
            }
        }

        totalImported.addAndGet(imported.get());
        totalSkipped.addAndGet(skipped.get());
        totalCalls.addAndGet(calls.get());
        return Map.of("imported", imported.get(), "skipped", skipped.get(), "calls", calls.get());
    }

    private static final String[] FL_MINGJU_TYPES = {"春天", "秋天", "冬天", "夏天", "写雨", "中秋节", "春节", "爱情", "友情", "励志"};

    /** 分类名句：按 type 轮询 */
    private Map<String, Integer> runFlMingjuCategory(Long userId, int numHttpCalls, int delayMs,
                                                     AtomicInteger totalImported, AtomicInteger totalSkipped, AtomicInteger totalCalls,
                                                     AtomicInteger totalKbImported) {
        return runBatchCategoryWithType(userId, "tianapi_flmj", "分类名句", numHttpCalls, delayMs,
                FL_MINGJU_TYPES, 10, tianApiService::flMingju, "source", "content",
                totalImported, totalSkipped, totalCalls, totalKbImported);
    }

    /** 通用批量采集：title+content 结构 */
    private Map<String, Integer> runBatchCategory(Long userId, String category, String displayName, int numHttpCalls, int delayMs,
                                                   java.util.function.Supplier<List<Map<String, String>>> fetcher,
                                                   String titleKey, String contentKey,
                                                   AtomicInteger totalImported, AtomicInteger totalSkipped, AtomicInteger totalCalls,
                                                   AtomicInteger totalKbImported) {
        AtomicInteger imported = new AtomicInteger(0);
        AtomicInteger skipped = new AtomicInteger(0);
        AtomicInteger calls = new AtomicInteger(0);

        for (int b = 0; b < numHttpCalls; b++) {
            sleep(delayMs);
            try {
                List<Map<String, String>> list = fetcher.get();
                if (list == null || list.isEmpty()) continue;
                calls.incrementAndGet();

                for (Map<String, String> item : list) {
                    String title = item.getOrDefault(titleKey, "");
                    String content = item.getOrDefault(contentKey, "");
                    if (!StringUtils.hasText(content)) continue;
                    String fullContent = StringUtils.hasText(title) ? title + "\n" + content : content;
                    if (copyLibraryRepository.countByUserIdAndCategoryAndContentAndDeleted(userId, category, fullContent) > 0) {
                        skipped.incrementAndGet();
                        continue;
                    }
                    saveCopy(userId, category, displayName, truncate(title, TITLE_MAX_LEN), fullContent, totalKbImported);
                    imported.incrementAndGet();
                }
            } catch (TianApiQuotaExceededException e) {
                log.info("TianAPI 分类 {} 今日配额已用尽，已跳过该分类", displayName);
                break;
            } catch (Exception e) {
                log.warn("TianAPI {} 拉取失败: err={}", displayName, e.getMessage());
            }
        }

        totalImported.addAndGet(imported.get());
        totalSkipped.addAndGet(skipped.get());
        totalCalls.addAndGet(calls.get());
        return Map.of("imported", imported.get(), "skipped", skipped.get(), "calls", calls.get());
    }

    /** 通用批量采集：按 type 轮询（如分类名句） */
    private Map<String, Integer> runBatchCategoryWithType(Long userId, String category, String displayName, int numHttpCalls, int delayMs,
                                                          String[] types, int batchSize,
                                                          java.util.function.BiFunction<String, Integer, List<Map<String, String>>> fetcher,
                                                          String titleKey, String contentKey,
                                                          AtomicInteger totalImported, AtomicInteger totalSkipped, AtomicInteger totalCalls,
                                                          AtomicInteger totalKbImported) {
        AtomicInteger imported = new AtomicInteger(0);
        AtomicInteger skipped = new AtomicInteger(0);
        AtomicInteger calls = new AtomicInteger(0);

        for (int b = 0; b < numHttpCalls; b++) {
            sleep(delayMs);
            try {
                String type = types[b % types.length];
                List<Map<String, String>> list = fetcher.apply(type, batchSize);
                if (list == null || list.isEmpty()) continue;
                calls.incrementAndGet();

                for (Map<String, String> item : list) {
                    String title = item.getOrDefault(titleKey, "");
                    String content = item.getOrDefault(contentKey, "");
                    if (!StringUtils.hasText(content)) continue;
                    String fullContent = StringUtils.hasText(title) ? title + "：" + content : content;
                    if (copyLibraryRepository.countByUserIdAndCategoryAndContentAndDeleted(userId, category, fullContent) > 0) {
                        skipped.incrementAndGet();
                        continue;
                    }
                    saveCopy(userId, category, displayName, truncate(title, TITLE_MAX_LEN), fullContent, totalKbImported);
                    imported.incrementAndGet();
                }
            } catch (TianApiQuotaExceededException e) {
                log.info("TianAPI 分类 {} 今日配额已用尽，已跳过该分类", displayName);
                break;
            } catch (Exception e) {
                log.warn("TianAPI {} 拉取失败: err={}", displayName, e.getMessage());
            }
        }

        totalImported.addAndGet(imported.get());
        totalSkipped.addAndGet(skipped.get());
        totalCalls.addAndGet(calls.get());
        return Map.of("imported", imported.get(), "skipped", skipped.get(), "calls", calls.get());
    }

    private void saveCopy(Long userId, String category, String displayName, String title, String content,
                          AtomicInteger totalKbImported) {
        String finalTitle = StringUtils.hasText(title) ? truncate(title, TITLE_MAX_LEN) : truncate(content, TITLE_MAX_LEN);
        CopyLibrary e = new CopyLibrary();
        e.setUserId(userId);
        e.setTitle(finalTitle);
        e.setContent(content);
        e.setCategory(category);
        e.setTags("TianAPI," + displayName);
        e.setWordCount(content != null ? content.length() : 0);
        e.setUseCount(0);
        e.setStatus(1);
        copyLibraryRepository.save(e);

        // 同步入库到 AI 知识库（文案类适合进 huashu 话术库）
        if (properties.isKbImportEnabled() && knowledgeBaseService != null && totalKbImported != null) {
            try {
                Long kbId = knowledgeBaseService.resolveKbIdByName(userId, properties.getKbName());
                if (kbId == null) return;
                String kbTitle = displayName + ": " + finalTitle;
                String kbContent = "# " + kbTitle + "\n\n" + (content != null ? content : "");
                var doc = knowledgeBaseService.uploadDocument(kbId, kbTitle, kbContent, "md", userId, SOURCE_TYPE_TIANAPI, category, null);
                if (doc != null) {
                    totalKbImported.incrementAndGet();
                }
            } catch (Exception ex) {
                log.warn("TianAPI 知识库入库失败: category={}, err={}", category, ex.getMessage());
            }
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private record ContentFetcher(String category, String displayName, ContentSupplier fetch, int itemsPerCall) {}

    @FunctionalInterface
    private interface ContentSupplier {
        String get();
    }
}
