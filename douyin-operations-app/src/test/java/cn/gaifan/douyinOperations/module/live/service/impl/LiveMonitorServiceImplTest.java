package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.live.entity.LiveMonitor;
import cn.gaifan.douyinOperations.module.live.repository.LiveMonitorRepository;
import cn.gaifan.douyinOperations.module.live.vo.LiveMonitorSearchVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveMonitorVO;
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

import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * LiveMonitorServiceImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LiveMonitorServiceImpl 单元测试")
class LiveMonitorServiceImplTest {

    @Mock
    private LiveMonitorRepository liveMonitorRepository;

    @InjectMocks
    private LiveMonitorServiceImpl liveMonitorService;

    private LiveMonitor sampleMonitor;
    private static final Long TEST_SESSION_ID = 100L;

    @BeforeEach
    void setUp() {
        sampleMonitor = new LiveMonitor();
        sampleMonitor.setId(1L);
        sampleMonitor.setSessionId(TEST_SESSION_ID);
        sampleMonitor.setTimestamp(new Timestamp(System.currentTimeMillis()));
        sampleMonitor.setViewers(1000);
        sampleMonitor.setLikes(500L);
        sampleMonitor.setComments(50);
        sampleMonitor.setShares(10);
        sampleMonitor.setProductImpressions(200);
        sampleMonitor.setCreateTime(new Timestamp(System.currentTimeMillis()));
    }

    @Nested
    @DisplayName("search 搜索")
    class SearchTests {

        @Test
        @DisplayName("search_bySessionId_shouldReturnPage")
        void search_bySessionId_shouldReturnPage() {
            LiveMonitorSearchVO vo = new LiveMonitorSearchVO();
            vo.setPage(0);
            vo.setRows(10);
            vo.setSessionId(TEST_SESSION_ID);

            when(liveMonitorRepository.findBySessionId(eq(TEST_SESSION_ID), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(sampleMonitor), org.springframework.data.domain.PageRequest.of(0, 10), 1));

            PageResultVO<LiveMonitorVO> result = liveMonitorService.search(vo);

            assertThat(result.getTotal()).isEqualTo(1);
            assertThat(result.getList()).hasSize(1);
            assertThat(result.getList().get(0).getSessionId()).isEqualTo(TEST_SESSION_ID);
            assertThat(result.getList().get(0).getViewers()).isEqualTo(1000);
        }

        @Test
        @DisplayName("search_emptyResult_shouldReturnEmpty")
        void search_emptyResult_shouldReturnEmpty() {
            LiveMonitorSearchVO vo = new LiveMonitorSearchVO();
            vo.setPage(0);
            vo.setRows(10);
            vo.setSessionId(TEST_SESSION_ID);

            when(liveMonitorRepository.findBySessionId(eq(TEST_SESSION_ID), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(Collections.emptyList(), org.springframework.data.domain.PageRequest.of(0, 10), 0));

            PageResultVO<LiveMonitorVO> result = liveMonitorService.search(vo);

            assertThat(result.getTotal()).isEqualTo(0);
            assertThat(result.getList()).isEmpty();
        }
    }

    @Nested
    @DisplayName("save 保存")
    class SaveTests {

        @Test
        @DisplayName("save_validVo_shouldReturnId")
        void save_validVo_shouldReturnId() {
            LiveMonitorVO vo = new LiveMonitorVO();
            vo.setSessionId(TEST_SESSION_ID);
            vo.setTimestamp(new Timestamp(System.currentTimeMillis()));
            vo.setViewers(800);
            vo.setLikes(400L);

            when(liveMonitorRepository.save(any(LiveMonitor.class))).thenAnswer(inv -> {
                LiveMonitor m = inv.getArgument(0);
                m.setId(99L);
                return m;
            });

            long id = liveMonitorService.save(vo);

            assertThat(id).isEqualTo(99L);
            verify(liveMonitorRepository).save(argThat(m ->
                    m.getSessionId().equals(TEST_SESSION_ID) && m.getViewers() == 800));
        }

        @Test
        @DisplayName("save_nullSessionId_shouldThrow")
        void save_nullSessionId_shouldThrow() {
            LiveMonitorVO vo = new LiveMonitorVO();
            vo.setSessionId(null);

            BusinessException ex = assertThrows(BusinessException.class, () -> liveMonitorService.save(vo));
            assertThat(ex.getCode()).isEqualTo(ErrorCode.VALIDATION_FAIL);
        }
    }

    @Nested
    @DisplayName("getBySessionId")
    class GetBySessionIdTests {

        @Test
        @DisplayName("getBySessionId_valid_shouldReturnList")
        void getBySessionId_valid_shouldReturnList() {
            when(liveMonitorRepository.findBySessionId(TEST_SESSION_ID)).thenReturn(List.of(sampleMonitor));

            List<LiveMonitorVO> result = liveMonitorService.getBySessionId(TEST_SESSION_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getSessionId()).isEqualTo(TEST_SESSION_ID);
        }

        @Test
        @DisplayName("getBySessionId_invalid_shouldThrow")
        void getBySessionId_invalid_shouldThrow() {
            assertThrows(BusinessException.class, () -> liveMonitorService.getBySessionId(null));
            assertThrows(BusinessException.class, () -> liveMonitorService.getBySessionId(0L));
        }
    }

    @Nested
    @DisplayName("deleteBySessionId")
    class DeleteBySessionIdTests {

        @Test
        @DisplayName("deleteBySessionId_valid_shouldCallRepository")
        void deleteBySessionId_valid_shouldCallRepository() {
            when(liveMonitorRepository.deleteBySessionId(TEST_SESSION_ID)).thenReturn(3L);

            liveMonitorService.deleteBySessionId(TEST_SESSION_ID);

            verify(liveMonitorRepository).deleteBySessionId(TEST_SESSION_ID);
        }

        @Test
        @DisplayName("deleteBySessionId_invalid_shouldThrow")
        void deleteBySessionId_invalid_shouldThrow() {
            assertThrows(BusinessException.class, () -> liveMonitorService.deleteBySessionId(null));
        }
    }
}
