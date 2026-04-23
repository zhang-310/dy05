package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.live.entity.LiveSession;
import cn.gaifan.douyinOperations.module.live.repository.LiveSessionRepository;
import cn.gaifan.douyinOperations.module.live.vo.*;
import org.junit.jupiter.api.BeforeEach;
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
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * LiveSessionServiceImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LiveSessionServiceImpl 单元测试")
class LiveSessionServiceImplTest {

    @Mock
    private LiveSessionRepository liveSessionRepository;

    @InjectMocks
    private LiveSessionServiceImpl liveSessionService;

    private LiveSession sampleSession;
    private static final Long TEST_USER_ID = 1L;
    private static final Long TEST_SESSION_ID = 100L;

    @BeforeEach
    void setUp() {
        sampleSession = new LiveSession();
        sampleSession.setId(TEST_SESSION_ID);
        sampleSession.setUserId(TEST_USER_ID);
        sampleSession.setAccountId(10L);
        sampleSession.setLiveTitle("测试直播");
        sampleSession.setStatus(1);
        sampleSession.setDeleted(0);
        sampleSession.setCreateTime(new Timestamp(System.currentTimeMillis()));
        sampleSession.setUpdateTime(new Timestamp(System.currentTimeMillis()));
    }

    @Nested
    @DisplayName("search 搜索")
    class SearchTests {

        @Test
        @DisplayName("search_withKeyword_shouldFilter")
        void search_withKeyword_shouldFilter() {
            LiveSessionSearchVO vo = new LiveSessionSearchVO();
            vo.setPage(0);
            vo.setRows(10);
            vo.setUserId(TEST_USER_ID);
            vo.setKeyword("测试");

            when(liveSessionRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(sampleSession), org.springframework.data.domain.PageRequest.of(0, 10), 1));

            PageResultVO<LiveSessionVO> result = liveSessionService.search(vo);

            assertThat(result.getTotal()).isEqualTo(1);
            assertThat(result.getList()).hasSize(1);
            assertThat(result.getList().get(0).getLiveTitle()).isEqualTo("测试直播");
        }

        @Test
        @DisplayName("search_emptyResult_shouldReturnEmpty")
        void search_emptyResult_shouldReturnEmpty() {
            LiveSessionSearchVO vo = new LiveSessionSearchVO();
            vo.setPage(0);
            vo.setRows(10);

            when(liveSessionRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(Collections.emptyList(), org.springframework.data.domain.PageRequest.of(0, 10), 0));

            PageResultVO<LiveSessionVO> result = liveSessionService.search(vo);

            assertThat(result.getTotal()).isEqualTo(0);
            assertThat(result.getList()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getById 获取详情")
    class GetByIdTests {

        @Test
        @DisplayName("getById_existing_shouldReturn")
        void getById_existing_shouldReturn() {
            when(liveSessionRepository.findByIdAndDeleted(TEST_SESSION_ID, 0))
                    .thenReturn(Optional.of(sampleSession));

            LiveSessionVO result = liveSessionService.getById(TEST_SESSION_ID);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(TEST_SESSION_ID);
            assertThat(result.getLiveTitle()).isEqualTo("测试直播");
        }

        @Test
        @DisplayName("getById_notFound_shouldThrow")
        void getById_notFound_shouldThrow() {
            when(liveSessionRepository.findByIdAndDeleted(999L, 0)).thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> liveSessionService.getById(999L));

            assertThat(ex.getCode()).isEqualTo(ErrorCode.DATA_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("delete 删除")
    class DeleteTests {

        @Test
        @DisplayName("delete_existing_shouldSoftDelete")
        void delete_existing_shouldSoftDelete() {
            when(liveSessionRepository.findByIdAndDeleted(TEST_SESSION_ID, 0))
                    .thenReturn(Optional.of(sampleSession));
            when(liveSessionRepository.save(any(LiveSession.class))).thenAnswer(i -> i.getArgument(0));

            assertDoesNotThrow(() -> liveSessionService.delete(TEST_SESSION_ID));
            verify(liveSessionRepository).save(argThat(s -> s.getDeleted() == 1));
        }

        @Test
        @DisplayName("delete_notFound_shouldThrow")
        void delete_notFound_shouldThrow() {
            when(liveSessionRepository.findByIdAndDeleted(999L, 0)).thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> liveSessionService.delete(999L));

            assertThat(ex.getCode()).isEqualTo(ErrorCode.DATA_NOT_FOUND);
        }
    }
}
