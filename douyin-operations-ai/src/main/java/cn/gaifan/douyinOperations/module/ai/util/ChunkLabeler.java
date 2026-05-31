package cn.gaifan.douyinOperations.module.ai.util;

import java.util.HashSet;
import java.util.Set;

/**
 * Chunk 业务标签自动打标：二级 type:（内容类型）+ 三级 cat:（品类），与 classifyByContent 对齐。
 */
public final class ChunkLabeler {

    private ChunkLabeler() {
    }

    /** 为 chunk 自动打标签（返回二级 + 三级标签集合） */
    public static Set<String> label(String chunkText) {
        Set<String> labels = new HashSet<>();
        if (chunkText == null || chunkText.isBlank()) return labels;

        if (matchAny(chunkText, "种草", "安利", "推荐理由", "真的好用")) labels.add("type:种草");
        if (matchAny(chunkText, "秒杀", "限时", "倒计时", "抢购", "下单", "福利价")) labels.add("type:促销");
        if (matchAny(chunkText, "成分", "功效", "配方", "技术", "参数", "规格")) labels.add("type:产品介绍");
        if (matchAny(chunkText, "姐妹们", "宝子们", "家人们", "直播间")) labels.add("type:直播话术");
        if (matchAny(chunkText, "情绪", "共鸣", "故事", "经历", "感动")) labels.add("type:情绪价值");
        if (matchAny(chunkText, "过渡", "衔接", "接下来", "下一个")) labels.add("type:过渡话术");
        if (matchAny(chunkText, "感谢", "关注", "点赞", "粉丝", "评论")) labels.add("type:互动话术");
        if (matchAny(chunkText, "Q:", "A:", "问:", "答:", "常见问题")) labels.add("type:FAQ");

        if (matchAny(chunkText, "护肤", "面膜", "精华", "水乳", "防晒", "美白")) labels.add("cat:美妆护肤");
        if (matchAny(chunkText, "零食", "美食", "好吃", "口感", "配料")) labels.add("cat:食品");
        if (matchAny(chunkText, "衣服", "穿搭", "面料", "款式", "尺码")) labels.add("cat:服装");
        if (matchAny(chunkText, "家电", "智能", "厨房", "清洁", "收纳")) labels.add("cat:家居");
        if (matchAny(chunkText, "手机", "电脑", "芯片", "续航", "像素")) labels.add("cat:数码");

        return labels;
    }

    private static boolean matchAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }
}
