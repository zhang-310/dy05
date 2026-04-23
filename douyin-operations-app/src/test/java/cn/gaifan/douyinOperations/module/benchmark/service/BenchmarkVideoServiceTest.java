package cn.gaifan.douyinOperations.module.benchmark.service;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.benchmark.entity.BenchmarkAccount;
import cn.gaifan.douyinOperations.module.benchmark.entity.BenchmarkVideo;
import cn.gaifan.douyinOperations.module.benchmark.metrics.BenchmarkMetrics;
import cn.gaifan.douyinOperations.module.benchmark.repository.BenchmarkAccountRepository;
import cn.gaifan.douyinOperations.module.benchmark.repository.BenchmarkVideoRepository;
import cn.gaifan.douyinOperations.module.benchmark.service.impl.BenchmarkVideoServiceImpl;
import cn.gaifan.douyinOperations.module.benchmark.vo.*;
import cn.gaifan.douyinOperations.module.shortvideo.service.impl.AccountVideoScraper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * BenchmarkVideoService 单元测试
 */
@DisplayName("BenchmarkVideoService 单元测试")
class BenchmarkVideoServiceTest {

    @Mock
    private BenchmarkVideoRepository videoRepository;

    @Mock
    private BenchmarkAccountRepository accountRepository;

    @Mock
    private DouyinCookieService cookieService;

    @Mock
    private AccountVideoScraper accountVideoScraper;

    @Mock
    private BenchmarkMetrics metrics;

    @InjectMocks
    private BenchmarkVideoServiceImpl service;

    private static final Long TEST_USER_ID = 1L;
    private BenchmarkVideo testVideo;
    private BenchmarkAccount testAccount;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        testAccount = new BenchmarkAccount();
        testAccount.setId(1L);
        testAccount.setOwnerId(TEST_USER_ID);
        testAccount.setAccountName("测试账号");
        testAccount.setAccountUrl("https://www.douyin.com/user/MS4wLjABAAAAtest");
        testAccount.setPlatform("douyin");

        testVideo = new BenchmarkVideo();
        testVideo.setId(1L);
        testVideo.setBenchmarkAccountId(1L);
        testVideo.setVideoId("test_video_123");
        testVideo.setTitle("测试视频");
        testVideo.setLikeCount(5000);
        testVideo.setIsQualified(true);
        testVideo.setAnalysisStatus("pending");
        testVideo.setDeleted(0);
        testVideo.setCreateTime(LocalDateTime.now());
        testVideo.setUpdateTime(LocalDateTime.now());

        when(accountVideoScraper.isAvailable()).thenReturn(true);
    }

    @Test
    @DisplayName("查询视频列表 - 应返回分页结果")
    void search_shouldReturnPagedResult() {
        BenchmarkVideoSearchVO searchVO = new BenchmarkVideoSearchVO();
        searchVO.setPage(0);
        searchVO.setRows(10);
        searchVO.setBenchmarkAccountId(1L);

        Page<BenchmarkVideo> page = new PageImpl<>(List.of(testVideo), PageRequest.of(0, 10), 1);
        when(videoRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(page);

        PageResultVO<BenchmarkVideoVO> result = service.search(searchVO, TEST_USER_ID);

        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getList()).hasSize(1);
        assertThat(result.getList().get(0).getTitle()).isEqualTo("测试视频");
    }

    @Test
    @DisplayName("按ID查询视频 - 应返回视频")
    void getById_shouldReturnVideo() {
        when(videoRepository.findById(1L)).thenReturn(Optional.of(testVideo));
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));

        BenchmarkVideoVO result = service.getById(1L, TEST_USER_ID);

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("测试视频");
    }

    @Test
    @DisplayName("删除视频 - 应逻辑删除")
    void delete_shouldLogicallyDelete() {
        when(videoRepository.findById(1L)).thenReturn(Optional.of(testVideo));
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));

        service.delete(1L, TEST_USER_ID);

        verify(videoRepository, times(1)).save(testVideo);
        assertThat(testVideo.getDeleted()).isEqualTo(1);
    }

    @Test
    @DisplayName("采集视频 - 应保存符合条件的视频")
    void collectVideos_shouldSaveQualifiedVideos() {
        CollectAccountVideosVO collectVO = new CollectAccountVideosVO();
        collectVO.setBenchmarkAccountId(1L);
        collectVO.setMinLikeCount(1000);
        collectVO.setMaxVideos(50);

        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(cookieService.getAvailableCookie(TEST_USER_ID, "douyin")).thenReturn("sessionid=test123");

        AccountVideoScraper.ScrapeResult scrapeResult = new AccountVideoScraper.ScrapeResult();
        scrapeResult.setAccountName("测试账号");
        scrapeResult.setSecUid("MS4wLjABAAAAtest");

        AccountVideoScraper.ScrapedVideo scrapedVideo = new AccountVideoScraper.ScrapedVideo();
        scrapedVideo.setVideoId("video_123");
        scrapedVideo.setVideoUrl("https://www.douyin.com/video/123");
        scrapedVideo.setTitle("采集的视频");
        scrapedVideo.setLikeCount(5000L);
        scrapeResult.setVideos(List.of(scrapedVideo));

        when(accountVideoScraper.scrapeAccountVideos(anyString())).thenReturn(scrapeResult);
        when(videoRepository.findByVideoIdAndBenchmarkAccountId(anyString(), eq(1L))).thenReturn(Optional.empty());
        when(videoRepository.save(any(BenchmarkVideo.class))).thenReturn(testVideo);
        when(videoRepository.countByBenchmarkAccountId(1L)).thenReturn(1);

        List<BenchmarkVideoVO> result = service.collectAccountVideos(collectVO, TEST_USER_ID);

        assertThat(result).isNotEmpty();
        verify(videoRepository, times(1)).save(any(BenchmarkVideo.class));
        verify(metrics, times(1)).recordVideoCollected(eq(1L), anyInt());
    }

    @Test
    @DisplayName("更新分析状态 - 应更新成功")
    void updateAnalysisStatus_shouldUpdate() {
        when(videoRepository.findById(1L)).thenReturn(Optional.of(testVideo));

        service.updateAnalysisStatus(1L, "completed");

        verify(videoRepository, times(1)).save(testVideo);
        assertThat(testVideo.getAnalysisStatus()).isEqualTo("completed");
    }
}
