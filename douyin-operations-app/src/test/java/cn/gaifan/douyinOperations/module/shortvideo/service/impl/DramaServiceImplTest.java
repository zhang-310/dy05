package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvDrama;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvDramaCharacter;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvDramaEpisode;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvDramaCharacterRepository;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvDramaEpisodeRepository;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvDramaRepository;
import cn.gaifan.douyinOperations.module.shortvideo.service.SvProjectService;
import cn.gaifan.douyinOperations.module.shortvideo.service.SvScriptService;
import cn.gaifan.douyinOperations.module.shortvideo.service.SvShotListService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * DramaServiceImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DramaServiceImpl 单元测试")
class DramaServiceImplTest {

    @Mock
    private SvDramaRepository dramaRepository;
    @Mock
    private SvDramaEpisodeRepository episodeRepository;
    @Mock
    private SvDramaCharacterRepository characterRepository;
    @Mock
    private SvScriptService scriptService;
    @Mock
    private SvShotListService shotListService;
    @Mock
    private SvProjectService projectService;

    @InjectMocks
    private DramaServiceImpl dramaService;

    private SvDrama sampleDrama;
    private static final Long TEST_OWNER_ID = 1L;

    @BeforeEach
    void setUp() {
        sampleDrama = new SvDrama();
        sampleDrama.setId(100L);
        sampleDrama.setOwnerId(TEST_OWNER_ID);
        sampleDrama.setTitle("测试短剧");
        sampleDrama.setStatus("draft");
        sampleDrama.setTotalEpisodes(3);
    }

    @Nested
    @DisplayName("getDrama")
    class GetDramaTests {

        @Test
        @DisplayName("getDrama_valid_shouldReturn")
        void getDrama_valid_shouldReturn() {
            when(dramaRepository.findById(100L)).thenReturn(Optional.of(sampleDrama));

            SvDrama result = dramaService.getDrama(100L, TEST_OWNER_ID);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(100L);
            assertThat(result.getTitle()).isEqualTo("测试短剧");
        }

        @Test
        @DisplayName("getDrama_wrongOwner_shouldThrow")
        void getDrama_wrongOwner_shouldThrow() {
            when(dramaRepository.findById(100L)).thenReturn(Optional.of(sampleDrama));

            BusinessException ex = assertThrows(BusinessException.class, () -> dramaService.getDrama(100L, 999L));
            assertThat(ex.getCode()).isEqualTo(ErrorCode.FORBIDDEN);
        }

        @Test
        @DisplayName("getDrama_notFound_shouldThrow")
        void getDrama_notFound_shouldThrow() {
            when(dramaRepository.findById(999L)).thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class, () -> dramaService.getDrama(999L, TEST_OWNER_ID));
            assertThat(ex.getCode()).isEqualTo(ErrorCode.DATA_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("listDramas")
    class ListDramasTests {

        @Test
        @DisplayName("listDramas_valid_shouldReturnList")
        void listDramas_valid_shouldReturnList() {
            when(dramaRepository.findByOwnerIdOrderByUpdateTimeDesc(TEST_OWNER_ID)).thenReturn(List.of(sampleDrama));
            when(episodeRepository.findByDramaIdOrderByEpisodeNumberAsc(100L)).thenReturn(Collections.emptyList());

            List<SvDrama> result = dramaService.listDramas(TEST_OWNER_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTitle()).isEqualTo("测试短剧");
        }

        @Test
        @DisplayName("listDramas_nullOwner_shouldThrow")
        void listDramas_nullOwner_shouldThrow() {
            assertThrows(BusinessException.class, () -> dramaService.listDramas(null));
        }
    }

    @Nested
    @DisplayName("createDrama")
    class CreateDramaTests {

        @Test
        @DisplayName("createDrama_valid_shouldCreate")
        void createDrama_valid_shouldCreate() {
            SvDrama savedDrama = new SvDrama();
            savedDrama.setId(200L);
            savedDrama.setOwnerId(TEST_OWNER_ID);
            savedDrama.setTitle("新短剧");
            when(dramaRepository.save(any(SvDrama.class))).thenAnswer(inv -> {
                SvDrama d = inv.getArgument(0);
                d.setId(200L);
                return d;
            });
            when(dramaRepository.findById(200L)).thenReturn(Optional.of(savedDrama));
            when(episodeRepository.save(any(SvDramaEpisode.class))).thenAnswer(inv -> inv.getArgument(0));

            SvDrama result = dramaService.createDrama(TEST_OWNER_ID, "新短剧", "描述", "爱情", 2);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(200L);
            assertThat(result.getTitle()).isEqualTo("新短剧");
            verify(episodeRepository, times(2)).save(any(SvDramaEpisode.class));
        }

        @Test
        @DisplayName("createDrama_emptyTitle_shouldThrow")
        void createDrama_emptyTitle_shouldThrow() {
            assertThrows(BusinessException.class, () -> dramaService.createDrama(TEST_OWNER_ID, "", null, null, 1));
        }
    }
}
