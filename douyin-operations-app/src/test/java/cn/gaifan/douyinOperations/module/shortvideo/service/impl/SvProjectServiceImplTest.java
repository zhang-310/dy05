package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvProject;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvProjectRepository;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvProjectSaveVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvProjectSearchVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvProjectVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * SvProjectServiceImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SvProjectServiceImpl 单元测试")
class SvProjectServiceImplTest {

    @Mock
    private SvProjectRepository projectRepository;

    @InjectMocks
    private SvProjectServiceImpl svProjectService;

    private SvProject sampleProject;
    private static final Long TEST_OWNER_ID = 1L;
    private static final Long TEST_PROJECT_ID = 100L;

    @BeforeEach
    void setUp() {
        sampleProject = new SvProject();
        sampleProject.setId(TEST_PROJECT_ID);
        sampleProject.setOwnerId(TEST_OWNER_ID);
        sampleProject.setTitle("测试项目");
        sampleProject.setStatus("draft");
        sampleProject.setProjectType("short_video");
        sampleProject.setDeleted(0);
    }

    @Nested
    @DisplayName("search")
    class SearchTests {

        @Test
        @DisplayName("search_valid_shouldReturnPage")
        void search_valid_shouldReturnPage() {
            SvProjectSearchVO vo = new SvProjectSearchVO();
            vo.setPage(0);
            vo.setRows(10);
            List<Long> visibleIds = List.of(TEST_OWNER_ID);

            when(projectRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(sampleProject), org.springframework.data.domain.PageRequest.of(0, 10), 1));

            PageResultVO<SvProjectVO> result = svProjectService.search(vo, TEST_OWNER_ID, visibleIds);

            assertThat(result.getTotal()).isEqualTo(1);
            assertThat(result.getList()).hasSize(1);
            assertThat(result.getList().get(0).getTitle()).isEqualTo("测试项目");
        }

        @Test
        @DisplayName("search_nullOwner_shouldThrow")
        void search_nullOwner_shouldThrow() {
            SvProjectSearchVO vo = new SvProjectSearchVO();
            vo.setPage(0);
            vo.setRows(10);

            assertThrows(BusinessException.class, () -> svProjectService.search(vo, null, List.of(1L)));
        }
    }

    @Nested
    @DisplayName("get")
    class GetTests {

        @Test
        @DisplayName("get_valid_shouldReturn")
        void get_valid_shouldReturn() {
            when(projectRepository.findById(TEST_PROJECT_ID)).thenReturn(Optional.of(sampleProject));

            SvProjectVO result = svProjectService.get(TEST_PROJECT_ID, TEST_OWNER_ID, List.of(TEST_OWNER_ID));

            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo("测试项目");
        }

        @Test
        @DisplayName("get_noAccess_shouldThrow")
        void get_noAccess_shouldThrow() {
            when(projectRepository.findById(TEST_PROJECT_ID)).thenReturn(Optional.of(sampleProject));

            assertThrows(BusinessException.class, () ->
                    svProjectService.get(TEST_PROJECT_ID, TEST_OWNER_ID, Collections.emptyList()));
        }
    }

    @Nested
    @DisplayName("save")
    class SaveTests {

        @Test
        @DisplayName("save_new_shouldCreate")
        void save_new_shouldCreate() {
            SvProjectSaveVO vo = new SvProjectSaveVO();
            vo.setTitle("新项目");
            vo.setProjectType("short_video");

            when(projectRepository.save(any(SvProject.class))).thenAnswer(inv -> {
                SvProject p = inv.getArgument(0);
                p.setId(200L);
                return p;
            });

            Long id = svProjectService.save(vo, TEST_OWNER_ID);

            assertThat(id).isEqualTo(200L);
            verify(projectRepository).save(argThat(p -> "新项目".equals(p.getTitle())));
        }

        @Test
        @DisplayName("save_existingPartial_shouldPreserveWorkflowLinks")
        void save_existingPartial_shouldPreserveWorkflowLinks() {
            sampleProject.setScriptId(11L);
            sampleProject.setShotListId(22L);
            sampleProject.setFinalVideoUrl("https://cdn.example.com/old.mp4");
            sampleProject.setThumbnailUrl("https://cdn.example.com/cover.jpg");
            sampleProject.setDuration(30);
            sampleProject.setRelatedProductIds(List.of(7L));

            SvProjectSaveVO vo = new SvProjectSaveVO();
            vo.setId(TEST_PROJECT_ID);
            vo.setTitle("测试项目");
            vo.setProjectType("short_video");
            vo.setFinalVideoUrl("https://cdn.example.com/final.mp4");

            when(projectRepository.findById(TEST_PROJECT_ID)).thenReturn(Optional.of(sampleProject));
            when(projectRepository.save(any(SvProject.class))).thenAnswer(inv -> inv.getArgument(0));

            Long id = svProjectService.save(vo, TEST_OWNER_ID);

            assertThat(id).isEqualTo(TEST_PROJECT_ID);
            verify(projectRepository).save(argThat(p ->
                    p.getScriptId().equals(11L)
                            && p.getShotListId().equals(22L)
                            && p.getFinalVideoUrl().equals("https://cdn.example.com/final.mp4")
                            && p.getThumbnailUrl().equals("https://cdn.example.com/cover.jpg")
                            && p.getDuration().equals(30)
                            && p.getRelatedProductIds().equals(List.of(7L))
            ));
        }

        @Test
        @DisplayName("save_newWithRelatedProducts_shouldPersistProductIds")
        void save_newWithRelatedProducts_shouldPersistProductIds() {
            SvProjectSaveVO vo = new SvProjectSaveVO();
            vo.setTitle("商品短视频");
            vo.setProjectType("soft_ad");
            vo.setRelatedProductIds(List.of(7L, 8L));

            when(projectRepository.save(any(SvProject.class))).thenAnswer(inv -> {
                SvProject p = inv.getArgument(0);
                p.setId(201L);
                return p;
            });

            Long id = svProjectService.save(vo, TEST_OWNER_ID);

            assertThat(id).isEqualTo(201L);
            verify(projectRepository).save(argThat(p -> p.getRelatedProductIds().equals(List.of(7L, 8L))));
        }
    }
}
