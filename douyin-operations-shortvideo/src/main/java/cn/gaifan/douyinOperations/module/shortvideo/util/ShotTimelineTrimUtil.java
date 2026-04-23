package cn.gaifan.douyinOperations.module.shortvideo.util;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Phase 1：从 {@code sv_shot.timeline_json} 解析裁剪区间（列字段为空时回退）。
 * 约定：根对象 {@code trimStartSec}/{@code trimEndSec}，或 {@code clips[]} 中 {@code order} 最小的一条（V-1 MVP2）。
 */
public final class ShotTimelineTrimUtil {

    private ShotTimelineTrimUtil() {
    }

    /**
     * @return length 2：{@code [trimStart, trimEnd]}，可能为 null 元素表示未指定
     */
    public static Double[] resolveTrimSeconds(String timelineJson, Double columnTrimStart, Double columnTrimEnd) {
        if (columnTrimStart != null || columnTrimEnd != null) {
            return new Double[] { columnTrimStart, columnTrimEnd };
        }
        if (!StringUtils.hasText(timelineJson)) {
            return new Double[] { null, null };
        }
        try {
            JSONObject o = JSON.parseObject(timelineJson.trim());
            if (o == null) {
                return new Double[] { null, null };
            }
            Double ts = o.getDouble("trimStartSec");
            Double te = o.getDouble("trimEndSec");
            if (ts == null && te == null) {
                JSONObject c = pickPrimaryClip(o.getJSONArray("clips"));
                if (c != null) {
                    ts = c.getDouble("trimStartSec");
                    te = c.getDouble("trimEndSec");
                }
            }
            return new Double[] { ts, te };
        } catch (Exception ignored) {
            return new Double[] { null, null };
        }
    }

    private static JSONObject pickPrimaryClip(JSONArray clips) {
        if (clips == null || clips.isEmpty()) {
            return null;
        }
        List<JSONObject> list = new ArrayList<>();
        for (int i = 0; i < clips.size(); i++) {
            JSONObject c = clips.getJSONObject(i);
            if (c != null) {
                list.add(c);
            }
        }
        if (list.isEmpty()) {
            return null;
        }
        list.sort(Comparator.comparingInt(c -> c.containsKey("order") ? c.getIntValue("order") : Integer.MAX_VALUE));
        return list.get(0);
    }
}
