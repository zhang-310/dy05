package cn.gaifan.douyinOperations.module.ai.service.impl;

import org.springframework.lang.Nullable;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * V-2：xfade 转场白名单与归一化（与前端 {@code constants/shortvideo-transitions} 对齐）。
 * 可通过 {@code app.shortvideo.compose.xfade-transition-whitelist} 覆写。
 */
public final class VideoComposeXfadeSupport {

    /** 与 frontend-react/src/constants/shortvideo-transitions.ts 中 SHORTVIDEO_XFADE_MENU 的 value 集合一致。 */
    public static final Set<String> DEFAULT_WHITELIST = Set.of(
            "fade",
            "slideleft",
            "slideright",
            "slideup",
            "slidedown",
            "wipeleft",
            "wiperight",
            "wipeup",
            "wipedown",
            "circleopen",
            "circleclose",
            "radial",
            "diagtl",
            "diagtr",
            "diagbl",
            "diagbr");

    private VideoComposeXfadeSupport() {
    }

    /**
     * @param csv 逗号分隔；null/空白则返回 {@link #DEFAULT_WHITELIST}
     */
    public static Set<String> parseWhitelist(@Nullable String csv) {
        if (csv == null || csv.isBlank()) {
            return DEFAULT_WHITELIST;
        }
        Set<String> s = Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(t -> !t.isEmpty())
                .map(t -> t.toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(TreeSet::new));
        return s.isEmpty() ? DEFAULT_WHITELIST : Set.copyOf(s);
    }

    public static boolean supportsMerge(@Nullable String trans, Set<String> whitelist) {
        if (trans == null || trans.isBlank() || whitelist == null || whitelist.isEmpty()) {
            return false;
        }
        return whitelist.contains(trans.trim().toLowerCase(Locale.ROOT));
    }

    public static String sanitize(@Nullable String trans, Set<String> whitelist,
            @Nullable Consumer<String> onInvalidTransition) {
        if (trans == null || trans.isBlank()) {
            return "fade";
        }
        String raw = trans.trim();
        String t = raw.toLowerCase(Locale.ROOT);
        if (whitelist != null && whitelist.contains(t)) {
            return t;
        }
        if (onInvalidTransition != null) {
            onInvalidTransition.accept(raw);
        }
        return "fade";
    }
}
