package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.module.live.config.LiveDanmakuSentimentProperties;
import cn.gaifan.douyinOperations.module.live.vo.DanmakuSentimentSnapshotVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
class DanmakuSentimentServiceImplTest {

    private DanmakuSentimentServiceImpl service;
    private LiveDanmakuSentimentProperties props;

    @BeforeEach
    void setUp() {
        props = new LiveDanmakuSentimentProperties();
        props.setWindowSeconds(30);
        props.setEnabled(true);
        service = new DanmakuSentimentServiceImpl();
        try {
            var f = DanmakuSentimentServiceImpl.class.getDeclaredField("properties");
            f.setAccessible(true);
            f.set(service, props);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void ingest_classifiesAndAggregatesInWindow() {
        long sid = 9001L;
        service.ingest(sid, "太好用了回购");
        service.ingest(sid, "假货别买");
        service.ingest(sid, "哈哈哈哈");

        DanmakuSentimentSnapshotVO snap = service.getSnapshot(sid);
        assertEquals(1, snap.getPositiveCount());
        assertEquals(1, snap.getNegativeCount());
        assertEquals(1, snap.getNeutralCount());
        assertEquals(3, snap.getTotalInWindow());
    }

    @Test
    void negativeDominatesTie() {
        long sid = 9002L;
        service.ingest(sid, "好评");
        service.ingest(sid, "差评");
        DanmakuSentimentSnapshotVO snap = service.getSnapshot(sid);
        assertEquals(1, snap.getPositiveCount());
        assertEquals(1, snap.getNegativeCount());
        assertEquals("negative", snap.getDominant());
    }
}
