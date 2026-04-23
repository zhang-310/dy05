package cn.gaifan.douyinOperations.module.live.util;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 场次槽位模板 JSON 解析：与 {@code LiveScript.script_type} 一致。
 */
public final class LiveSessionSlotBlueprintParser {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "opening", "product", "transition", "closing", "chat", "custom");

    public record Blueprint(String scriptType, String requirement, Integer durationLimitSec) {}

    private LiveSessionSlotBlueprintParser() {}

    public static List<Blueprint> parse(String json) {
        if (json == null || json.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "槽位结构 JSON 不能为空");
        }
        JSONArray arr;
        try {
            arr = JSON.parseArray(json);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "槽位结构不是合法 JSON 数组: " + e.getMessage());
        }
        if (arr == null || arr.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "槽位结构至少包含 1 个对象");
        }
        List<Blueprint> out = new ArrayList<>();
        for (int i = 0; i < arr.size(); i++) {
            JSONObject o = arr.getJSONObject(i);
            if (o == null) {
                throw new BusinessException(ErrorCode.VALIDATION_FAIL, "槽位[" + i + "] 不是 JSON 对象");
            }
            String st = o.getString("scriptType");
            if (st == null || st.isBlank()) {
                throw new BusinessException(ErrorCode.VALIDATION_FAIL, "槽位[" + i + "] 缺少 scriptType");
            }
            st = st.trim();
            if (!ALLOWED_TYPES.contains(st)) {
                throw new BusinessException(ErrorCode.VALIDATION_FAIL,
                        "槽位[" + i + "] scriptType 非法，允许: " + ALLOWED_TYPES);
            }
            String req = o.getString("requirement");
            if (req != null && req.isBlank()) {
                req = null;
            }
            Integer dur = o.getInteger("durationLimitSec");
            out.add(new Blueprint(st, req, dur));
        }
        return out;
    }
}
