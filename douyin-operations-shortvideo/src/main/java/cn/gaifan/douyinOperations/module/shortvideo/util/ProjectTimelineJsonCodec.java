package cn.gaifan.douyinOperations.module.shortvideo.util;

import cn.gaifan.douyinOperations.module.ai.service.VideoEditService;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * V-1 / V-3 / V-6：工程成片与分镜共用的 {@code timelineJson} 扩展（version 2）。
 * <p>
 * 约定根字段示例：
 * <pre>
 * {
 *   "version": 2,
 *   "videoTracks": [ { "id": "main", "zIndex": 0, "clipIndicesOrdered": [0,1,2] } ],
 *   "subtitleTrack": { "segments": [ { "inSec": 0, "outSec": 1.5, "text": "…" } ] },
 *   "audio": { "bgmUrl": "…", "voiceClipUrls": ["…"] }
 * }
 * </pre>
 */
public final class ProjectTimelineJsonCodec {

    private ProjectTimelineJsonCodec() {
    }

    public static List<VideoEditService.SubtitleItem> extractSubtitleSegments(String timelineJson) {
        if (!StringUtils.hasText(timelineJson)) {
            return List.of();
        }
        try {
            JSONObject root = JSON.parseObject(timelineJson.trim());
            if (root == null) {
                return List.of();
            }
            JSONObject st = root.getJSONObject("subtitleTrack");
            if (st == null) {
                return List.of();
            }
            JSONArray segs = st.getJSONArray("segments");
            if (segs == null || segs.isEmpty()) {
                return List.of();
            }
            List<VideoEditService.SubtitleItem> out = new ArrayList<>();
            for (int i = 0; i < segs.size(); i++) {
                JSONObject seg = segs.getJSONObject(i);
                if (seg == null) {
                    continue;
                }
                Double inSec = firstDouble(seg, "inSec", "startTime", "startSec");
                Double outSec = firstDouble(seg, "outSec", "endTime", "endSec");
                String text = seg.getString("text");
                if (text == null || text.isBlank()) {
                    continue;
                }
                if (inSec == null) {
                    inSec = 0.0;
                }
                if (outSec == null || outSec <= inSec) {
                    outSec = inSec + 0.5;
                }
                out.add(new VideoEditService.SubtitleItem(inSec, outSec, text.trim()));
            }
            return out;
        } catch (Exception ignored) {
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    public static List<String> extractVoiceClipUrls(String timelineJson) {
        if (!StringUtils.hasText(timelineJson)) {
            return List.of();
        }
        try {
            JSONObject root = JSON.parseObject(timelineJson.trim());
            if (root == null) {
                return List.of();
            }
            JSONObject audio = root.getJSONObject("audio");
            if (audio == null) {
                return List.of();
            }
            Object raw = audio.get("voiceClipUrls");
            if (raw instanceof List<?> list) {
                List<String> urls = new ArrayList<>();
                for (Object o : list) {
                    if (o instanceof String s && StringUtils.hasText(s)) {
                        urls.add(s.trim());
                    }
                }
                return urls;
            }
            if (raw instanceof String s && StringUtils.hasText(s)) {
                return List.of(s.trim());
            }
            return List.of();
        } catch (Exception ignored) {
            return List.of();
        }
    }

    /**
     * 若 {@code clipIndicesOrdered} 为合法排列，则按排列重排元素；否则返回原列表引用。
     */
    public static <T> List<T> reorderAlignedListIfTimelineSaysSo(List<T> items, String timelineJson) {
        if (items == null || items.isEmpty() || !StringUtils.hasText(timelineJson)) {
            return items;
        }
        int[] perm = parseClipIndicesPermutation(timelineJson, items.size());
        if (perm == null) {
            return items;
        }
        List<T> reordered = new ArrayList<>(items.size());
        for (int idx : perm) {
            reordered.add(items.get(idx));
        }
        return reordered;
    }

    /**
     * @return 合法排列下标；非法时 null
     */
    public static int[] parseClipIndicesPermutation(String timelineJson, int n) {
        if (n <= 0 || !StringUtils.hasText(timelineJson)) {
            return null;
        }
        try {
            JSONObject root = JSON.parseObject(timelineJson.trim());
            if (root == null || root.getIntValue("version") < 2) {
                return null;
            }
            JSONArray tracks = root.getJSONArray("videoTracks");
            if (tracks == null || tracks.isEmpty()) {
                return null;
            }
            JSONObject main = tracks.getJSONObject(0);
            if (main == null) {
                return null;
            }
            JSONArray ord = main.getJSONArray("clipIndicesOrdered");
            if (ord == null || ord.size() != n) {
                return null;
            }
            boolean[] seen = new boolean[n];
            int[] out = new int[n];
            for (int i = 0; i < ord.size(); i++) {
                int idx = ord.getIntValue(i);
                if (idx < 0 || idx >= n || seen[idx]) {
                    return null;
                }
                seen[idx] = true;
                out[i] = idx;
            }
            return out;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Double firstDouble(JSONObject o, String... keys) {
        for (String k : keys) {
            Double d = o.getDouble(k);
            if (d != null) {
                return d;
            }
        }
        return null;
    }

}
