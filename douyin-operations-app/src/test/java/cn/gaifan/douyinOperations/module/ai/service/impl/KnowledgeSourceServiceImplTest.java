package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.ai.entity.AiKnowledgeSource;
import cn.gaifan.douyinOperations.module.ai.repository.AiKnowledgeSourceRepository;
import cn.gaifan.douyinOperations.module.ai.vo.KnowledgeSourceSearchVO;
import cn.gaifan.douyinOperations.module.ai.vo.KnowledgeSourceSaveVO;
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
 * KnowledgeSourceServiceImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("KnowledgeSourceServiceImpl 单元测试")
class KnowledgeSourceServiceImplTest {

    @Mock
    private AiKnowledgeSourceRepository repository;

    @InjectMocks
    private KnowledgeSourceServiceImpl knowledgeSourceService;

    private AiKnowledgeSource sampleSource;
    private static final Long TEST_ID = 1L;

    @BeforeEach
    void setUp() {
        sampleSource = new AiKnowledgeSource();
        sampleSource.setId(TEST_ID);
        sampleSource.setSourceName("测试知识源");
        sampleSource.setSourcePath("/path/to/source");
        sampleSource.setSourceType("local");
        sampleSource.setDeleted(0);
    }

    @Nested
    @DisplayName("search")
    class SearchTests {

        @Test
        @DisplayName("search_shouldReturnPage")
        void search_shouldReturnPage() {
            KnowledgeSourceSearchVO vo = new KnowledgeSourceSearchVO();
            vo.setPage(0);
            vo.setRows(10);

            when(repository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(sampleSource), org.springframework.data.domain.PageRequest.of(0, 10), 1));

            PageResultVO<AiKnowledgeSource> result = knowledgeSourceService.search(vo);

            assertThat(result.getTotal()).isEqualTo(1);
            assertThat(result.getList()).hasSize(1);
            assertThat(result.getList().get(0).getSourceName()).isEqualTo("测试知识源");
        }
    }

    @Nested
    @DisplayName("getById")
    class GetByIdTests {

        @Test
        @DisplayName("getById_existing_shouldReturn")
        void getById_existing_shouldReturn() {
            when(repository.findById(TEST_ID)).thenReturn(Optional.of(sampleSource));

            AiKnowledgeSource result = knowledgeSourceService.getById(TEST_ID);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(TEST_ID);
        }

        @Test
        @DisplayName("getById_notFound_shouldThrow")
        void getById_notFound_shouldThrow() {
            when(repository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(BusinessException.class, () -> knowledgeSourceService.getById(999L));
        }
    }

    @Nested
    @DisplayName("save")
    class SaveTests {

        @Test
        @DisplayName("save_new_shouldCreate")
        void save_new_shouldCreate() {
            KnowledgeSourceSaveVO vo = new KnowledgeSourceSaveVO();
            vo.setSourceName("新知识源");
            vo.setSourcePath("/new/path");
            vo.setSourceType("local");

            when(repository.existsBySourcePathAndDeleted("/new/path", 0)).thenReturn(false);
            when(repository.save(any(AiKnowledgeSource.class))).thenAnswer(inv -> {
                AiKnowledgeSource e = inv.getArgument(0);
                e.setId(99L);
                return e;
            });

            Long id = knowledgeSourceService.save(vo);

            assertThat(id).isEqualTo(99L);
            verify(repository).save(argThat(e -> "新知识源".equals(e.getSourceName())));
        }

        @Test
        @DisplayName("save_duplicatePath_shouldThrow")
        void save_duplicatePath_shouldThrow() {
            KnowledgeSourceSaveVO vo = new KnowledgeSourceSaveVO();
            vo.setSourceName("重复");
            vo.setSourcePath("/existing/path");

            when(repository.existsBySourcePathAndDeleted("/existing/path", 0)).thenReturn(true);

            BusinessException ex = assertThrows(BusinessException.class, () -> knowledgeSourceService.save(vo));
            assertThat(ex.getCode()).isEqualTo(ErrorCode.DATA_ALREADY_EXISTS);
        }
    }

    @Nested
    @DisplayName("delete")
    class DeleteTests {

        @Test
        @DisplayName("delete_existing_shouldSoftDelete")
        void delete_existing_shouldSoftDelete() {
            when(repository.findById(TEST_ID)).thenReturn(Optional.of(sampleSource));
            when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

            knowledgeSourceService.delete(TEST_ID);

            verify(repository).save(argThat(e -> e.getDeleted() == 1));
        }
    }
}
