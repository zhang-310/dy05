package cn.gaifan.douyinOperations.module.ai.service.impl;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * V-2：非法 xfade 名回退 fade（与 {@code shortvideo-transitions.ts} 白名单一致）。
 */
class VideoEditServiceImplXfadeSanitizeTest {

    /** 与 frontend-react/src/constants/shortvideo-transitions.ts 中 SHORTVIDEO_XFADE_MENU 的 value 集合一致（漂移时三处同步）。 */
    private static final Set<String> EXPECTED_XFADE_VALUES = Set.of(
            "fade", "slideleft", "slideright", "slideup", "slidedown",
            "wipeleft", "wiperight", "wipeup", "wipedown",
            "circleopen", "circleclose", "radial",
            "diagtl", "diagtr", "diagbl", "diagbr");

    @Test
    void sanitizeXfadeTransition_invalidBecomesFade() {
        assertEquals("fade", VideoComposeXfadeSupport.sanitize("not_in_whitelist", EXPECTED_XFADE_VALUES, null));
        assertEquals("fade", VideoComposeXfadeSupport.sanitize("", EXPECTED_XFADE_VALUES, null));
        assertEquals("fade", VideoComposeXfadeSupport.sanitize("   ", EXPECTED_XFADE_VALUES, null));
    }

    @Test
    void sanitizeXfadeTransition_whitelistPreserved() {
        assertEquals("slideleft", VideoComposeXfadeSupport.sanitize("  SLIDELEFT ", EXPECTED_XFADE_VALUES, null));
        assertEquals("diagbr", VideoComposeXfadeSupport.sanitize("diagbr", EXPECTED_XFADE_VALUES, null));
    }

    @Test
    void xfadeWhitelist_matchesShortvideoTransitionsTsCatalog() {
        assertEquals(new TreeSet<>(EXPECTED_XFADE_VALUES),
                new TreeSet<>(VideoComposeXfadeSupport.DEFAULT_WHITELIST));
    }

    @Test
    void parseWhitelist_emptyUsesDefault() {
        assertEquals(VideoComposeXfadeSupport.DEFAULT_WHITELIST, VideoComposeXfadeSupport.parseWhitelist(null));
        assertEquals(VideoComposeXfadeSupport.DEFAULT_WHITELIST, VideoComposeXfadeSupport.parseWhitelist("  "));
    }

    @Test
    void parseWhitelist_customSubset() {
        Set<String> s = VideoComposeXfadeSupport.parseWhitelist("fade, wipeleft,Wiperight");
        assertEquals(Set.of("fade", "wipeleft", "wiperight"), s);
    }
}
