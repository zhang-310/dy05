package cn.gaifan.douyinOperations.module.abtest.service.impl;

import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.abtest.entity.AbEvent;
import cn.gaifan.douyinOperations.module.abtest.entity.AbExperiment;
import cn.gaifan.douyinOperations.module.abtest.entity.AbVariant;
import cn.gaifan.douyinOperations.module.abtest.repository.AbEventRepository;
import cn.gaifan.douyinOperations.module.abtest.repository.AbExperimentRepository;
import cn.gaifan.douyinOperations.module.abtest.repository.AbVariantRepository;
import cn.gaifan.douyinOperations.module.abtest.vo.*;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AbTestServiceImpl A/B测试服务测试")
class AbTestServiceImplTest {

    @InjectMocks
    private AbTestServiceImpl abTestService;

    @Mock
    private AbExperimentRepository experimentRepository;
    @Mock
    private AbVariantRepository variantRepository;
    @Mock
    private AbEventRepository eventRepository;

    private AbExperiment buildExperiment(Long id, String name, Integer status) {
        AbExperiment e = new AbExperiment();
        e.setId(id);
        e.setOwnerId(1L);
        e.setName(name);
        e.setExperimentType("copy");
        e.setStatus(status);
        e.setDeleted(0);
        e.setCreateTime(new Timestamp(System.currentTimeMillis()));
        e.setUpdateTime(new Timestamp(System.currentTimeMillis()));
        return e;
    }

    private AbVariant buildVariant(Long id, Long experimentId, String name) {
        AbVariant v = new AbVariant();
        v.setId(id);
        v.setExperimentId(experimentId);
        v.setVariantName(name);
        v.setVariantType("A");
        v.setViewCount(0L);
        v.setClickCount(0L);
        v.setConversionCount(0L);
        v.setDeleted(0);
        v.setCreateTime(new Timestamp(System.currentTimeMillis()));
        v.setUpdateTime(new Timestamp(System.currentTimeMillis()));
        return v;
    }

    @Nested
    @DisplayName("search 搜索实验")
    class SearchTests {

        @Test
        void search_shouldReturnPagedResults() {
            AbExperiment exp = buildExperiment(1L, "测试实验", 0);
            when(experimentRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(exp)));
            when(variantRepository.findByExperimentIdAndDeleted(1L, 0))
                    .thenReturn(Collections.emptyList());

            AbExperimentSearchVO vo = new AbExperimentSearchVO();
            PageResultVO<AbExperimentVO> result = abTestService.search(vo);

            assertThat(result.getTotal()).isEqualTo(1L);
            assertThat(result.getList()).hasSize(1);
            assertThat(result.getList().get(0).getName()).isEqualTo("测试实验");
        }

        @Test
        void search_withVariants_shouldIncludeVariants() {
            AbExperiment exp = buildExperiment(1L, "实验A", 1);
            AbVariant v = buildVariant(10L, 1L, "变体A");
            when(experimentRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(exp)));
            when(variantRepository.findByExperimentIdAndDeleted(1L, 0))
                    .thenReturn(List.of(v));

            PageResultVO<AbExperimentVO> result = abTestService.search(new AbExperimentSearchVO());

            assertThat(result.getList().get(0).getVariants()).hasSize(1);
            assertThat(result.getList().get(0).getVariants().get(0).getVariantName()).isEqualTo("变体A");
        }
    }

    @Nested
    @DisplayName("getById 获取实验")
    class GetByIdTests {

        @Test
        void getById_exists_shouldReturnVO() {
            AbExperiment exp = buildExperiment(1L, "实验", 0);
            when(experimentRepository.findByIdAndDeleted(1L, 0)).thenReturn(Optional.of(exp));
            when(variantRepository.findByExperimentIdAndDeleted(1L, 0)).thenReturn(Collections.emptyList());

            AbExperimentVO vo = abTestService.getById(1L);
            assertThat(vo.getName()).isEqualTo("实验");
        }

        @Test
        void getById_notExists_shouldThrow() {
            when(experimentRepository.findByIdAndDeleted(999L, 0)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> abTestService.getById(999L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("实验不存在");
        }
    }

    @Nested
    @DisplayName("save 保存实验")
    class SaveTests {

        @Test
        void save_newExperiment_shouldCreate() {
            AbExperiment saved = buildExperiment(1L, "新实验", 0);
            when(experimentRepository.save(any(AbExperiment.class))).thenReturn(saved);

            AbExperimentSaveVO vo = new AbExperimentSaveVO();
            vo.setName("新实验");
            vo.setExperimentType("copy");

            long id = abTestService.save(vo, 1L);
            assertThat(id).isEqualTo(1L);
            verify(experimentRepository).save(argThat(exp -> exp.getOwnerId().equals(1L)));
        }

        @Test
        void save_existingExperiment_shouldUpdate() {
            AbExperiment existing = buildExperiment(5L, "旧名", 0);
            when(experimentRepository.findByIdAndDeleted(5L, 0)).thenReturn(Optional.of(existing));
            when(experimentRepository.save(any(AbExperiment.class))).thenReturn(existing);

            AbExperimentSaveVO vo = new AbExperimentSaveVO();
            vo.setId(5L);
            vo.setName("新名");
            vo.setExperimentType("copy");

            long id = abTestService.save(vo, 1L);
            assertThat(id).isEqualTo(5L);
            assertThat(existing.getName()).isEqualTo("新名");
        }

        @Test
        void save_notFoundById_shouldThrow() {
            when(experimentRepository.findByIdAndDeleted(999L, 0)).thenReturn(Optional.empty());

            AbExperimentSaveVO vo = new AbExperimentSaveVO();
            vo.setId(999L);
            vo.setName("test");
            vo.setExperimentType("copy");

            assertThatThrownBy(() -> abTestService.save(vo))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        void save_existingExperimentWrongOwner_shouldThrow() {
            AbExperiment existing = buildExperiment(5L, "旧名", 0);
            when(experimentRepository.findByIdAndDeleted(5L, 0)).thenReturn(Optional.of(existing));

            AbExperimentSaveVO vo = new AbExperimentSaveVO();
            vo.setId(5L);
            vo.setName("新名");
            vo.setExperimentType("copy");

            assertThatThrownBy(() -> abTestService.save(vo, 2L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("无权修改此实验");
        }
    }

    @Nested
    @DisplayName("delete 删除实验")
    class DeleteTests {

        @Test
        void delete_shouldSoftDeleteExperimentAndVariants() {
            AbExperiment exp = buildExperiment(1L, "实验", 0);
            AbVariant v = buildVariant(10L, 1L, "变体");
            when(experimentRepository.findByIdAndDeleted(1L, 0)).thenReturn(Optional.of(exp));
            when(variantRepository.findByExperimentIdAndDeleted(1L, 0)).thenReturn(List.of(v));

            abTestService.delete(1L);

            assertThat(exp.getDeleted()).isEqualTo(1);
            assertThat(v.getDeleted()).isEqualTo(1);
            verify(experimentRepository).save(exp);
            verify(variantRepository).save(v);
        }

        @Test
        void delete_notExists_shouldThrow() {
            when(experimentRepository.findByIdAndDeleted(999L, 0)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> abTestService.delete(999L))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("recordEvent 事件记录")
    class RecordEventTests {

        @Test
        void recordEvent_newEvent_shouldSaveAndIncrementView() {
            when(eventRepository.existsByVariantIdAndEventTypeAndUserFingerprint(1L, "view", "fp1"))
                    .thenReturn(false);

            AbEventSaveVO vo = new AbEventSaveVO();
            vo.setExperimentId(1L);
            vo.setVariantId(1L);
            vo.setEventType("view");
            vo.setUserFingerprint("fp1");
            vo.setSessionId("s1");

            abTestService.recordEvent(vo);

            verify(eventRepository).save(any(AbEvent.class));
            verify(variantRepository).incrementViewCount(1L);
        }

        @Test
        void recordEvent_clickEvent_shouldIncrementClick() {
            when(eventRepository.existsByVariantIdAndEventTypeAndUserFingerprint(1L, "click", "fp1"))
                    .thenReturn(false);

            AbEventSaveVO vo = new AbEventSaveVO();
            vo.setExperimentId(1L);
            vo.setVariantId(1L);
            vo.setEventType("click");
            vo.setUserFingerprint("fp1");

            abTestService.recordEvent(vo);
            verify(variantRepository).incrementClickCount(1L);
        }

        @Test
        void recordEvent_conversionEvent_shouldIncrementConversion() {
            when(eventRepository.existsByVariantIdAndEventTypeAndUserFingerprint(1L, "conversion", "fp1"))
                    .thenReturn(false);

            AbEventSaveVO vo = new AbEventSaveVO();
            vo.setExperimentId(1L);
            vo.setVariantId(1L);
            vo.setEventType("conversion");
            vo.setUserFingerprint("fp1");

            abTestService.recordEvent(vo);
            verify(variantRepository).incrementConversionCount(1L);
        }

        @Test
        void recordEvent_duplicate_shouldSkip() {
            when(eventRepository.existsByVariantIdAndEventTypeAndUserFingerprint(1L, "view", "fp1"))
                    .thenReturn(true);

            AbEventSaveVO vo = new AbEventSaveVO();
            vo.setExperimentId(1L);
            vo.setVariantId(1L);
            vo.setEventType("view");
            vo.setUserFingerprint("fp1");

            abTestService.recordEvent(vo);

            verify(eventRepository, never()).save(any());
            verify(variantRepository, never()).incrementViewCount(any());
        }
    }

    @Nested
    @DisplayName("setWinner 设置赢家")
    class SetWinnerTests {

        @Test
        void setWinner_shouldUpdateExperimentAndVariant() {
            when(experimentRepository.findByIdAndDeleted(1L, 0))
                    .thenReturn(Optional.of(buildExperiment(1L, "exp", 1)));
            when(variantRepository.findByIdAndDeleted(10L, 0))
                    .thenReturn(Optional.of(buildVariant(10L, 1L, "winner")));

            AbSetWinnerVO vo = new AbSetWinnerVO();
            vo.setExperimentId(1L);
            vo.setVariantId(10L);
            vo.setConclusion("变体A胜出");

            abTestService.setWinner(vo);

            verify(experimentRepository).setWinner(eq(1L), eq(10L), eq("变体A胜出"), any(Timestamp.class));
            verify(variantRepository).updateIsWinner(10L, 1);
        }

        @Test
        void setWinner_experimentNotFound_shouldThrow() {
            when(experimentRepository.findByIdAndDeleted(999L, 0)).thenReturn(Optional.empty());

            AbSetWinnerVO vo = new AbSetWinnerVO();
            vo.setExperimentId(999L);
            vo.setVariantId(1L);

            assertThatThrownBy(() -> abTestService.setWinner(vo))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        void setWinner_variantNotFound_shouldThrow() {
            when(experimentRepository.findByIdAndDeleted(1L, 0))
                    .thenReturn(Optional.of(buildExperiment(1L, "exp", 1)));
            when(variantRepository.findByIdAndDeleted(999L, 0)).thenReturn(Optional.empty());

            AbSetWinnerVO vo = new AbSetWinnerVO();
            vo.setExperimentId(1L);
            vo.setVariantId(999L);

            assertThatThrownBy(() -> abTestService.setWinner(vo))
                    .isInstanceOf(BusinessException.class);
        }
    }
}
