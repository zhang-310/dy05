package cn.gaifan.douyinOperations.module.douyin.service.impl;

import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.douyin.entity.DouyinVideo;
import cn.gaifan.douyinOperations.module.douyin.repository.DouyinVideoRepository;
import cn.gaifan.douyinOperations.module.douyin.vo.DouyinVideoSaveVO;
import cn.gaifan.douyinOperations.module.douyin.vo.DouyinVideoSearchVO;
import cn.gaifan.douyinOperations.module.douyin.vo.DouyinVideoVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DouyinVideoServiceImpl 视频服务测试")
class DouyinVideoServiceImplTest {

    @InjectMocks
    private DouyinVideoServiceImpl videoService;

    @Mock
    private DouyinVideoRepository douyinVideoRepository;

    private DouyinVideo buildVideo(Long id, Long accountId, String videoId, String title) {
        DouyinVideo v = new DouyinVideo();
        v.setId(id);
        v.setAccountId(accountId);
        v.setVideoId(videoId);
        v.setTitle(title);
        v.setViewCount(100L);
        v.setLikeCount(10L);
        v.setShareCount(5L);
        v.setCommentCount(3L);
        v.setDownloadCount(1L);
        v.setDeleted(0);
        v.setCreateTime(new Timestamp(System.currentTimeMillis()));
        v.setUpdateTime(new Timestamp(System.currentTimeMillis()));
        return v;
    }

    @Nested
    @DisplayName("search 分页搜索")
    class SearchTests {

        @Test
        void search_withData_shouldReturnPageResult() {
            DouyinVideo v = buildVideo(1L, 100L, "vid001", "测试视频");
            when(douyinVideoRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(v)));

            DouyinVideoSearchVO vo = new DouyinVideoSearchVO();
            PageResultVO<DouyinVideoVO> result = videoService.search(vo);

            assertThat(result.getTotal()).isEqualTo(1L);
            assertThat(result.getList()).hasSize(1);
            assertThat(result.getList().get(0).getTitle()).isEqualTo("测试视频");
        }

        @Test
        void search_empty_shouldReturnEmptyList() {
            when(douyinVideoRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            DouyinVideoSearchVO vo = new DouyinVideoSearchVO();
            PageResultVO<DouyinVideoVO> result = videoService.search(vo);

            assertThat(result.getTotal()).isEqualTo(0L);
            assertThat(result.getList()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getVideo 获取视频详情")
    class GetVideoTests {

        @Test
        void getVideo_exists_shouldReturnVO() {
            DouyinVideo v = buildVideo(1L, 100L, "vid001", "测试视频");
            when(douyinVideoRepository.findByIdAndDeleted(1L, 0)).thenReturn(Optional.of(v));

            DouyinVideoVO result = videoService.getVideo(1L);
            assertThat(result).isNotNull();
            assertThat(result.getVideoId()).isEqualTo("vid001");
        }

        @Test
        void getVideo_notExists_shouldThrow() {
            when(douyinVideoRepository.findByIdAndDeleted(999L, 0)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> videoService.getVideo(999L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("视频不存在");
        }

        @Test
        void getVideo_nullId_shouldThrow() {
            assertThatThrownBy(() -> videoService.getVideo(null))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        void getVideo_negativeId_shouldThrow() {
            assertThatThrownBy(() -> videoService.getVideo(-1L))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("saveVideo 保存视频")
    class SaveVideoTests {

        @Test
        void saveVideo_new_shouldCreate() {
            when(douyinVideoRepository.existsByVideoIdAndDeleted("vid_new", 0)).thenReturn(false);
            when(douyinVideoRepository.save(any(DouyinVideo.class))).thenAnswer(inv -> {
                DouyinVideo arg = inv.getArgument(0);
                arg.setId(10L);
                return arg;
            });

            DouyinVideoSaveVO vo = new DouyinVideoSaveVO();
            vo.setAccountId(1L);
            vo.setVideoId("vid_new");
            vo.setTitle("新视频");

            long id = videoService.saveVideo(vo);
            assertThat(id).isEqualTo(10L);
        }

        @Test
        void saveVideo_duplicateVideoId_shouldThrow() {
            when(douyinVideoRepository.existsByVideoIdAndDeleted("dup_vid", 0)).thenReturn(true);

            DouyinVideoSaveVO vo = new DouyinVideoSaveVO();
            vo.setAccountId(1L);
            vo.setVideoId("dup_vid");
            vo.setTitle("重复视频");

            assertThatThrownBy(() -> videoService.saveVideo(vo))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("视频 ID 已存在");
        }

        @Test
        void saveVideo_update_shouldModify() {
            DouyinVideo existing = buildVideo(5L, 1L, "vid005", "旧标题");
            when(douyinVideoRepository.findByIdAndDeleted(5L, 0)).thenReturn(Optional.of(existing));
            when(douyinVideoRepository.save(any(DouyinVideo.class))).thenReturn(existing);

            DouyinVideoSaveVO vo = new DouyinVideoSaveVO();
            vo.setId(5L);
            vo.setTitle("新标题");

            long id = videoService.saveVideo(vo);
            assertThat(id).isEqualTo(5L);
            assertThat(existing.getTitle()).isEqualTo("新标题");
        }
    }

    @Nested
    @DisplayName("syncVideos 同步视频")
    class SyncTests {

        @Test
        void syncVideos_nullAccountId_shouldThrow() {
            assertThatThrownBy(() -> videoService.syncVideos(null))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        void syncVideos_negativeAccountId_shouldThrow() {
            assertThatThrownBy(() -> videoService.syncVideos(-1L))
                    .isInstanceOf(BusinessException.class);
        }
    }
}
