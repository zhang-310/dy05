package cn.gaifan.douyinOperations.common.util;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.shortvideo.util.DouyinSharePasteParser;
import cn.gaifan.douyinOperations.module.shortvideo.vo.ViralCollectVO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DouyinSharePasteParserTest {

    @Test
    void firstDouyinVideoUrl_extractsShortLinkFromShareText() {
        String paste = "8.25 复制打开抖音，看看【某某的作品】https://v.douyin.com/iAbCdEf/ 07/28 ";
        assertEquals("https://v.douyin.com/iAbCdEf/", DouyinSharePasteParser.firstDouyinVideoUrl(paste).orElseThrow());
    }

    @Test
    void firstDouyinVideoUrl_prefersEarlierMatchBetweenShortAndWww() {
        String paste = "先看 https://www.douyin.com/video/7123456789012345678 再看 https://v.douyin.com/iXxXxX/";
        assertEquals("https://www.douyin.com/video/7123456789012345678",
                DouyinSharePasteParser.firstDouyinVideoUrl(paste).orElseThrow());
    }

    @Test
    void firstDouyinVideoUrl_trimsTrailingPunctuation() {
        assertEquals("https://v.douyin.com/iTest/", DouyinSharePasteParser.firstDouyinVideoUrl("链接 https://v.douyin.com/iTest/。").orElseThrow());
    }

    @Test
    void resolveCollectVideoUrl_singleLineNonDouyinHttp() {
        String wechat = "https://channels.weixin.qq.com/xxx";
        assertEquals(wechat, DouyinSharePasteParser.resolveCollectVideoUrl(wechat).orElseThrow());
    }

    @Test
    void titleFromBrackets_firstPair() {
        assertEquals("某某的作品", DouyinSharePasteParser.titleFromBrackets("看看【某某的作品】https://v.douyin.com/x/").orElseThrow());
    }

    @Test
    void applyToCollectVo_setsUrlAndTitleFromPaste() {
        ViralCollectVO vo = new ViralCollectVO();
        vo.setVideoUrl("前缀【标题测试】后缀\nhttps://v.douyin.com/iZzZzZ/\n");
        vo.setDouyinVideoId("url_1");
        DouyinSharePasteParser.applyToCollectVo(vo);
        assertEquals("https://v.douyin.com/iZzZzZ/", vo.getVideoUrl());
        assertEquals("标题测试", vo.getTitle());
    }

    @Test
    void applyToCollectVo_doesNotOverrideExistingTitle() {
        ViralCollectVO vo = new ViralCollectVO();
        vo.setVideoUrl("https://v.douyin.com/iAa/");
        vo.setTitle("已有标题");
        DouyinSharePasteParser.applyToCollectVo(vo);
        assertEquals("已有标题", vo.getTitle());
    }

    @Test
    void applyToCollectVo_throwsWhenUnparseableMultiline() {
        ViralCollectVO vo = new ViralCollectVO();
        vo.setVideoUrl("只有文案\n没有链接\n");
        BusinessException ex = assertThrows(BusinessException.class, () -> DouyinSharePasteParser.applyToCollectVo(vo));
        assertEquals(ErrorCode.VALIDATION_FAIL, ex.getCode());
        assertTrue(ex.getMessage().contains("未识别"));
    }

    @Test
    void applyToCollectVo_skipsWhenVideoUrlBlank() {
        ViralCollectVO vo = new ViralCollectVO();
        vo.setVideoUrl("  ");
        DouyinSharePasteParser.applyToCollectVo(vo);
        assertEquals("  ", vo.getVideoUrl());
    }

    @Test
    void primaryTitleFromDouyinSharePaste_extractsHookAfterAuthorWorksBracket() {
        String paste = "5.69 复制打开抖音，看看【尔滨黛玉的作品】老公说吃胖了，就不要我了...# 夫妻日常 # 婆... https://v.douyin.com/i-lLj07dtdQ/ 03/14 teo:/ G@i.PX";
        assertEquals("老公说吃胖了，就不要我了",
                DouyinSharePasteParser.primaryTitleFromDouyinSharePaste(paste).orElseThrow());
        assertEquals("https://v.douyin.com/i-lLj07dtdQ/", DouyinSharePasteParser.firstDouyinVideoUrl(paste).orElseThrow());
    }

    @Test
    void applyToCollectVo_overridesPlaceholderTitleFromAnalyzeViral() {
        ViralCollectVO vo = new ViralCollectVO();
        vo.setVideoUrl("看看【尔滨黛玉的作品】老公说吃胖了，就不要我了#日常 https://v.douyin.com/i-lLj07dtdQ/");
        vo.setTitle("爆款分析");
        DouyinSharePasteParser.applyToCollectVo(vo);
        assertEquals("老公说吃胖了，就不要我了", vo.getTitle());
        assertEquals("https://v.douyin.com/i-lLj07dtdQ/", vo.getVideoUrl());
    }
}
