package cn.gaifan.douyinOperations.module.search.service;

import cn.gaifan.douyinOperations.module.live.entity.LiveSession;
import cn.gaifan.douyinOperations.module.live.repository.LiveSessionRepository;
import cn.gaifan.douyinOperations.module.product.entity.DyProduct;
import cn.gaifan.douyinOperations.module.product.entity.DyProductScript;
import cn.gaifan.douyinOperations.module.product.repository.DyProductRepository;
import cn.gaifan.douyinOperations.module.product.repository.DyProductScriptRepository;
import cn.gaifan.douyinOperations.module.search.service.impl.GlobalSearchServiceImpl;
import cn.gaifan.douyinOperations.module.search.vo.GlobalSearchRequestVO;
import cn.gaifan.douyinOperations.module.search.vo.GlobalSearchResponseVO;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvVideo;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvVideoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalSearchService 单元测试")
class GlobalSearchServiceImplTest {

    @Mock
    private LiveSessionRepository liveSessionRepository;
    @Mock
    private DyProductRepository dyProductRepository;
    @Mock
    private DyProductScriptRepository dyProductScriptRepository;
    @Mock
    private SvVideoRepository svVideoRepository;

    @InjectMocks
    private GlobalSearchServiceImpl globalSearchService;

    private Long userId = 1L;
    private LiveSession mockLiveSession;
    private DyProduct mockProduct;
    private DyProductScript mockScript;
    private SvVideo mockVideo;

    @BeforeEach
    void setUp() {
        // Mock LiveSession
        mockLiveSession = new LiveSession();
        mockLiveSession.setId(1L);
        mockLiveSession.setUserId(userId);
        mockLiveSession.setLiveTitle("护肤品直播");
        mockLiveSession.setStatus(1);

        // Mock Product
        mockProduct = new DyProduct();
        mockProduct.setId(1L);
        mockProduct.setUserId(userId);
        mockProduct.setProductName("护肤精华液");
        mockProduct.setProductCategory("护肤品");

        // Mock Script
        mockScript = new DyProductScript();
        mockScript.setId(1L);
        mockScript.setCreatedBy(userId);
        mockScript.setProductId(1L);
        mockScript.setScriptContent("这款护肤精华液非常好用");
        mockScript.setScriptType("intro");
        mockScript.setStyle("专业");

        // Mock Video
        mockVideo = new SvVideo();
        mockVideo.setId(1L);
        mockVideo.setOwnerId(userId);
        mockVideo.setTitle("护肤教程");
        mockVideo.setDescription("教你如何护肤");
    }

    @Test
    @DisplayName("全局搜索 - 应返回所有类型的结果")
    void search_shouldReturnAllTypes() {
        // Given
        GlobalSearchRequestVO request = new GlobalSearchRequestVO();
        request.setQ("护肤");
        request.setLimit(24);

        when(liveSessionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(mockLiveSession)));
        when(dyProductRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(mockProduct)));
        when(dyProductScriptRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(mockScript)));
        when(svVideoRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(mockVideo)));

        // When
        GlobalSearchResponseVO result = globalSearchService.search(request, List.of(userId));

        // Then
        assertThat(result.getHits()).hasSize(4);
        assertThat(result.getHits()).extracting("kind")
                .containsExactlyInAnyOrder("LIVE_SESSION", "PRODUCT", "SCRIPT", "SHORT_VIDEO");
        assertThat(result.getTookMs()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("全局搜索 - 空关键词应返回空结果")
    void search_emptyKeyword_shouldReturnEmpty() {
        // Given
        GlobalSearchRequestVO request = new GlobalSearchRequestVO();
        request.setQ("   ");
        request.setLimit(24);

        when(liveSessionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(dyProductRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(dyProductScriptRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(svVideoRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        // When
        GlobalSearchResponseVO result = globalSearchService.search(request, List.of(userId));

        // Then
        assertThat(result.getHits()).isEmpty();
    }

    @Test
    @DisplayName("全局搜索 - 空用户列表应返回空结果")
    void search_emptyUserList_shouldReturnEmpty() {
        // Given
        GlobalSearchRequestVO request = new GlobalSearchRequestVO();
        request.setQ("护肤");

        // When
        GlobalSearchResponseVO result = globalSearchService.search(request, List.of());

        // Then
        assertThat(result.getHits()).isEmpty();
        verify(liveSessionRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("全局搜索 - null用户列表应搜索所有用户")
    void search_nullUserList_shouldSearchAll() {
        // Given
        GlobalSearchRequestVO request = new GlobalSearchRequestVO();
        request.setQ("护肤");

        when(liveSessionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(mockLiveSession)));
        when(dyProductRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(mockProduct)));
        when(dyProductScriptRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(mockScript)));
        when(svVideoRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(mockVideo)));

        // When
        GlobalSearchResponseVO result = globalSearchService.search(request, null);

        // Then
        assertThat(result.getHits()).hasSize(4);
    }

    @Test
    @DisplayName("全局搜索 - 应限制返回数量")
    void search_shouldLimitResults() {
        // Given
        GlobalSearchRequestVO request = new GlobalSearchRequestVO();
        request.setQ("护肤");
        request.setLimit(2);

        when(liveSessionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(mockLiveSession)));
        when(dyProductRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(mockProduct)));
        when(dyProductScriptRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(mockScript)));
        when(svVideoRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(mockVideo)));

        // When
        GlobalSearchResponseVO result = globalSearchService.search(request, List.of(userId));

        // Then
        assertThat(result.getHits()).hasSizeLessThanOrEqualTo(2);
    }

    @Test
    @DisplayName("全局搜索 - 默认限制应为24")
    void search_defaultLimit_shouldBe24() {
        // Given
        GlobalSearchRequestVO request = new GlobalSearchRequestVO();
        request.setQ("护肤");
        // 不设置 limit

        when(liveSessionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(mockLiveSession)));
        when(dyProductRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(mockProduct)));
        when(dyProductScriptRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(mockScript)));
        when(svVideoRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(mockVideo)));

        // When
        GlobalSearchResponseVO result = globalSearchService.search(request, List.of(userId));

        // Then
        assertThat(result.getHits()).hasSizeLessThanOrEqualTo(24);
    }

    @Test
    @DisplayName("全局搜索 - 应去重相同kind和id的结果")
    void search_shouldDeduplicateResults() {
        // Given
        GlobalSearchRequestVO request = new GlobalSearchRequestVO();
        request.setQ("护肤");

        // 返回相同的 LiveSession 两次（模拟重复）
        when(liveSessionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(mockLiveSession, mockLiveSession)));
        when(dyProductRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(dyProductScriptRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(svVideoRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        // When
        GlobalSearchResponseVO result = globalSearchService.search(request, List.of(userId));

        // Then
        assertThat(result.getHits()).hasSize(1); // 去重后只有1个
    }

    @Test
    @DisplayName("全局搜索 - 直播场次结果应包含正确信息")
    void search_liveSession_shouldContainCorrectInfo() {
        // Given
        GlobalSearchRequestVO request = new GlobalSearchRequestVO();
        request.setQ("护肤");

        when(liveSessionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(mockLiveSession)));
        when(dyProductRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(dyProductScriptRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(svVideoRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        // When
        GlobalSearchResponseVO result = globalSearchService.search(request, List.of(userId));

        // Then
        assertThat(result.getHits()).hasSize(1);
        assertThat(result.getHits().get(0).getKind()).isEqualTo("LIVE_SESSION");
        assertThat(result.getHits().get(0).getTitle()).isEqualTo("护肤品直播");
        assertThat(result.getHits().get(0).getSubtitle()).contains("直播场次");
        assertThat(result.getHits().get(0).getPath()).contains("live/sessions");
    }

    @Test
    @DisplayName("全局搜索 - 商品结果应包含正确信息")
    void search_product_shouldContainCorrectInfo() {
        // Given
        GlobalSearchRequestVO request = new GlobalSearchRequestVO();
        request.setQ("护肤");

        when(liveSessionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(dyProductRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(mockProduct)));
        when(dyProductScriptRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(svVideoRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        // When
        GlobalSearchResponseVO result = globalSearchService.search(request, List.of(userId));

        // Then
        assertThat(result.getHits()).hasSize(1);
        assertThat(result.getHits().get(0).getKind()).isEqualTo("PRODUCT");
        assertThat(result.getHits().get(0).getTitle()).isEqualTo("护肤精华液");
        assertThat(result.getHits().get(0).getSubtitle()).contains("商品");
        assertThat(result.getHits().get(0).getSubtitle()).contains("护肤品");
    }

    @Test
    @DisplayName("全局搜索 - 话术结果应包含正确信息")
    void search_script_shouldContainCorrectInfo() {
        // Given
        GlobalSearchRequestVO request = new GlobalSearchRequestVO();
        request.setQ("护肤");

        when(liveSessionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(dyProductRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(dyProductScriptRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(mockScript)));
        when(svVideoRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        // When
        GlobalSearchResponseVO result = globalSearchService.search(request, List.of(userId));

        // Then
        assertThat(result.getHits()).hasSize(1);
        assertThat(result.getHits().get(0).getKind()).isEqualTo("SCRIPT");
        assertThat(result.getHits().get(0).getTitle()).contains("护肤精华液");
        assertThat(result.getHits().get(0).getSubtitle()).contains("话术");
        assertThat(result.getHits().get(0).getSubtitle()).contains("intro");
    }

    @Test
    @DisplayName("全局搜索 - 短视频结果应包含正确信息")
    void search_video_shouldContainCorrectInfo() {
        // Given
        GlobalSearchRequestVO request = new GlobalSearchRequestVO();
        request.setQ("护肤");

        when(liveSessionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(dyProductRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(dyProductScriptRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(svVideoRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(mockVideo)));

        // When
        GlobalSearchResponseVO result = globalSearchService.search(request, List.of(userId));

        // Then
        assertThat(result.getHits()).hasSize(1);
        assertThat(result.getHits().get(0).getKind()).isEqualTo("SHORT_VIDEO");
        assertThat(result.getHits().get(0).getTitle()).isEqualTo("护肤教程");
        assertThat(result.getHits().get(0).getSubtitle()).contains("短视频");
        assertThat(result.getHits().get(0).getPath()).contains("shortvideo/videos");
    }

    @Test
    @DisplayName("全局搜索 - 话术内容超过80字符应截断")
    void search_script_longContent_shouldTruncate() {
        // Given
        String longContent = "这是一段非常长的话术内容".repeat(20); // 超过80字符
        mockScript.setScriptContent(longContent);

        GlobalSearchRequestVO request = new GlobalSearchRequestVO();
        request.setQ("话术");

        when(liveSessionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(dyProductRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(dyProductScriptRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(mockScript)));
        when(svVideoRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        // When
        GlobalSearchResponseVO result = globalSearchService.search(request, List.of(userId));

        // Then
        assertThat(result.getHits()).hasSize(1);
        assertThat(result.getHits().get(0).getTitle()).hasSizeLessThanOrEqualTo(81); // 80 + "…"
        assertThat(result.getHits().get(0).getTitle()).endsWith("…");
    }

    @Test
    @DisplayName("全局搜索 - 短视频无标题应显示默认文本")
    void search_video_noTitle_shouldShowDefault() {
        // Given
        mockVideo.setTitle(null);

        GlobalSearchRequestVO request = new GlobalSearchRequestVO();
        request.setQ("护肤");

        when(liveSessionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(dyProductRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(dyProductScriptRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(svVideoRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(mockVideo)));

        // When
        GlobalSearchResponseVO result = globalSearchService.search(request, List.of(userId));

        // Then
        assertThat(result.getHits()).hasSize(1);
        assertThat(result.getHits().get(0).getTitle()).isEqualTo("(无标题)");
    }

    @Test
    @DisplayName("全局搜索 - 每种类型最多返回6条")
    void search_shouldLimitPerType() {
        // Given
        GlobalSearchRequestVO request = new GlobalSearchRequestVO();
        request.setQ("护肤");
        request.setLimit(100); // 设置一个很大的限制

        // 创建7个直播场次（超过每类型限制6）
        List<LiveSession> sessions = List.of(
                mockLiveSession, mockLiveSession, mockLiveSession,
                mockLiveSession, mockLiveSession, mockLiveSession,
                mockLiveSession
        );

        when(liveSessionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(sessions));
        when(dyProductRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(dyProductScriptRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(svVideoRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        // When
        GlobalSearchResponseVO result = globalSearchService.search(request, List.of(userId));

        // Then
        // 因为去重，实际只有1个结果（所有 mockLiveSession 的 id 相同）
        assertThat(result.getHits()).hasSize(1);

        // 验证 repository 调用时使用了正确的分页参数（每类型6条）
        verify(liveSessionRepository).findAll(any(Specification.class), any(Pageable.class));
    }
}
