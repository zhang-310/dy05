package cn.gaifan.douyinOperations.module.benchmark.repository;

import cn.gaifan.douyinOperations.module.benchmark.entity.BenchmarkVideo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BenchmarkVideoRepository 数据层测试
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("BenchmarkVideoRepository 数据层测试")
class BenchmarkVideoRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private BenchmarkVideoRepository repository;

    private BenchmarkVideo testVideo;
    private static final Long TEST_ACCOUNT_ID = 100L;

    @BeforeEach
    void setUp() {
        testVideo = new BenchmarkVideo();
        testVideo.setBenchmarkAccountId(TEST_ACCOUNT_ID);
        testVideo.setVideoId("7123456789012345678");
        testVideo.setVideoUrl("https://www.douyin.com/video/7123456789012345678");
        testVideo.setTitle("测试视频标题");
        testVideo.setDescription("测试视频描述");
        testVideo.setCoverUrl("https://example.com/cover.jpg");
        testVideo.setDuration(60);
        testVideo.setLikeCount(5000);
        testVideo.setCommentCount(200);
        testVideo.setShareCount(100);
        testVideo.setPublishTime(LocalDateTime.now());
        testVideo.setAnalysisStatus("pending");
        testVideo.setDeleted(0);
    }

    @Test
    @DisplayName("保存视频 - 应成功保存")
    void save_shouldPersistVideo() {
        BenchmarkVideo saved = repository.save(testVideo);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getVideoId()).isEqualTo("7123456789012345678");
    }

    @Test
    @DisplayName("按 ID 查询 - 应返回视频")
    void findById_shouldReturnVideo() {
        BenchmarkVideo saved = entityManager.persistAndFlush(testVideo);

        Optional<BenchmarkVideo> found = repository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("测试视频标题");
    }

    @Test
    @DisplayName("按账号 ID 查询 - 应返回账号的视频列表")
    void findByAccountId_shouldReturnAccountVideos() {
        entityManager.persistAndFlush(testVideo);

        BenchmarkVideo anotherVideo = new BenchmarkVideo();
        anotherVideo.setBenchmarkAccountId(TEST_ACCOUNT_ID);
        anotherVideo.setVideoId("7123456789012345679");
        anotherVideo.setVideoUrl("https://www.douyin.com/video/7123456789012345679");
        anotherVideo.setTitle("另一个视频");
        anotherVideo.setAnalysisStatus("pending");
        anotherVideo.setDeleted(0);
        entityManager.persistAndFlush(anotherVideo);

        List<BenchmarkVideo> videos = repository.findByBenchmarkAccountId(TEST_ACCOUNT_ID);

        assertThat(videos).hasSize(2);
        assertThat(videos).extracting(BenchmarkVideo::getTitle)
                .containsExactlyInAnyOrder("测试视频标题", "另一个视频");
    }

    @Test
    @DisplayName("按 videoId 查询 - 应返回视频")
    void findByVideoId_shouldReturnVideo() {
        entityManager.persistAndFlush(testVideo);

        Optional<BenchmarkVideo> found = repository.findByVideoId("7123456789012345678");

        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("测试视频标题");
    }

    @Test
    @DisplayName("按账号ID和分析状态查询 - 应返回对应状态的视频")
    void findByAccountIdAndAnalysisStatus_shouldReturnVideos() {
        entityManager.persistAndFlush(testVideo);

        BenchmarkVideo completedVideo = new BenchmarkVideo();
        completedVideo.setBenchmarkAccountId(TEST_ACCOUNT_ID);
        completedVideo.setVideoId("7123456789012345680");
        completedVideo.setVideoUrl("https://www.douyin.com/video/7123456789012345680");
        completedVideo.setTitle("已完成视频");
        completedVideo.setAnalysisStatus("completed");
        completedVideo.setDeleted(0);
        entityManager.persistAndFlush(completedVideo);

        List<BenchmarkVideo> pendingVideos = repository.findByBenchmarkAccountIdAndAnalysisStatus(TEST_ACCOUNT_ID, "pending");
        List<BenchmarkVideo> completedVideos = repository.findByBenchmarkAccountIdAndAnalysisStatus(TEST_ACCOUNT_ID, "completed");

        assertThat(pendingVideos).hasSize(1);
        assertThat(pendingVideos.get(0).getTitle()).isEqualTo("测试视频标题");
        assertThat(completedVideos).hasSize(1);
        assertThat(completedVideos.get(0).getTitle()).isEqualTo("已完成视频");
    }

    @Test
    @DisplayName("更新分析状态 - 应成功更新")
    void updateAnalysisStatus_shouldModifyStatus() {
        BenchmarkVideo saved = entityManager.persistAndFlush(testVideo);

        saved.setAnalysisStatus("processing");
        repository.save(saved);
        entityManager.flush();
        entityManager.clear();

        Optional<BenchmarkVideo> updated = repository.findById(saved.getId());
        assertThat(updated).isPresent();
        assertThat(updated.get().getAnalysisStatus()).isEqualTo("processing");
    }

    @Test
    @DisplayName("逻辑删除 - 应更新 deleted 字段")
    void logicalDelete_shouldUpdateDeletedFlag() {
        BenchmarkVideo saved = entityManager.persistAndFlush(testVideo);

        saved.setDeleted(1);
        repository.save(saved);
        entityManager.flush();
        entityManager.clear();

        Optional<BenchmarkVideo> found = repository.findById(saved.getId());
        assertThat(found).isEmpty();
    }
}
