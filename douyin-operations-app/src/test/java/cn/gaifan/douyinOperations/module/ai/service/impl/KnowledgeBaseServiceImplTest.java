package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.ai.entity.AiKnowledgeBase;
import cn.gaifan.douyinOperations.module.ai.repository.AiKbDocumentRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiKnowledgeBaseRepository;
import cn.gaifan.douyinOperations.module.ai.service.KnowledgeBaseService;
import cn.gaifan.douyinOperations.module.ai.service.QueryRewriteService;
import cn.gaifan.douyinOperations.module.ai.service.SearchService;
import cn.gaifan.douyinOperations.module.ai.service.VectorService;
import cn.gaifan.douyinOperations.module.ai.util.DocumentTypeDetector;
import cn.gaifan.douyinOperations.module.ai.util.MixedDocumentProcessor;
import cn.gaifan.douyinOperations.module.ai.vo.DedupPreviewVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 知识库服务单元测试（Phase 2 任务 24：去重预览行为；Phase 3 任务 35：skipCache 不读缓存）
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("KnowledgeBaseServiceImpl 知识库服务测试")
class KnowledgeBaseServiceImplTest {

    private static final Long KB_ID = 1L;
    private static final Long USER_ID = 1L;

    @InjectMocks
    private KnowledgeBaseServiceImpl knowledgeBaseService;

    @Mock
    private AiKnowledgeBaseRepository knowledgeBaseRepository;

    @Mock
    private AiKbDocumentRepository documentRepository;

    @Mock
    private VectorService vectorService;

    @Mock
    private DocumentTypeDetector documentTypeDetector;

    @Mock
    private MixedDocumentProcessor mixedDocumentProcessor;

    @Mock
    private SearchService searchService;

    @Mock
    private QueryRewriteService queryRewriteService;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private AiKnowledgeBase kb;

    /** 1024 维占位向量，满足 embedding 维度 */
    private static final List<Float> PLACEHOLDER_EMBEDDING = Collections.nCopies(1024, 0.1f);

    @BeforeEach
    void setUp() {
        kb = new AiKnowledgeBase();
        kb.setId(KB_ID);
        kb.setUserId(USER_ID);
        kb.setKbName("test");
        kb.setStatus(1);
        kb.setDeleted(0);
        ReflectionTestUtils.setField(knowledgeBaseService, "dedupSkipThreshold", 0.92);
        ReflectionTestUtils.setField(knowledgeBaseService, "dedupDownweightThreshold", 0.80);
        ReflectionTestUtils.setField(knowledgeBaseService, "embeddingDimension", 1024);
    }

    @Nested
    @DisplayName("dedupPreview 去重预览（任务 24：重复/近似文档导入行为）")
    class DedupPreviewTests {

        @Test
        void dedupPreview_highScore_shouldMarkSkip() {
            when(knowledgeBaseRepository.findById(KB_ID)).thenReturn(Optional.of(kb));
            when(vectorService.generateEmbedding(anyString())).thenReturn(PLACEHOLDER_EMBEDDING);
            when(vectorService.search(eq("kb_1"), any(), eq(1), isNull()))
                    .thenReturn(List.of(new VectorService.VectorSearchResult(100L, 0.95f,
                            Map.of("text", "已有相似内容", "doc_id", 10L, "chunk_index", 0))));

            DedupPreviewVO result = knowledgeBaseService.dedupPreview(KB_ID, "这是一段测试话术内容，用于去重预览。", "general", USER_ID);

            assertThat(result).isNotNull();
            assertThat(result.getTotalChunks()).isGreaterThanOrEqualTo(1);
            assertThat(result.getSkip()).isGreaterThanOrEqualTo(1);
            assertThat(result.getDetails()).isNotEmpty();
            assertThat(result.getDetails().get(0).getAction()).isEqualTo("skip");
            assertThat(result.getDetails().get(0).getScore()).isEqualTo(0.95f);
        }

        @Test
        void dedupPreview_mediumScore_shouldMarkDownweight() {
            when(knowledgeBaseRepository.findById(KB_ID)).thenReturn(Optional.of(kb));
            when(vectorService.generateEmbedding(anyString())).thenReturn(PLACEHOLDER_EMBEDDING);
            when(vectorService.search(eq("kb_1"), any(), eq(1), isNull()))
                    .thenReturn(List.of(new VectorService.VectorSearchResult(100L, 0.85f,
                            Map.of("text", "部分相似", "doc_id", 10L, "chunk_index", 0))));

            DedupPreviewVO result = knowledgeBaseService.dedupPreview(KB_ID, "测试内容。", "general", USER_ID);

            assertThat(result).isNotNull();
            assertThat(result.getDownweight()).isGreaterThanOrEqualTo(1);
            assertThat(result.getDetails().get(0).getAction()).isEqualTo("downweight");
            assertThat(result.getDetails().get(0).getScore()).isEqualTo(0.85f);
        }

        @Test
        void dedupPreview_lowScore_shouldMarkKeep() {
            when(knowledgeBaseRepository.findById(KB_ID)).thenReturn(Optional.of(kb));
            when(vectorService.generateEmbedding(anyString())).thenReturn(PLACEHOLDER_EMBEDDING);
            when(vectorService.search(eq("kb_1"), any(), eq(1), isNull()))
                    .thenReturn(List.of(new VectorService.VectorSearchResult(100L, 0.50f,
                            Map.of("text", "不太相似", "doc_id", 10L, "chunk_index", 0))));

            DedupPreviewVO result = knowledgeBaseService.dedupPreview(KB_ID, "全新内容。", "general", USER_ID);

            assertThat(result).isNotNull();
            assertThat(result.getKeep()).isGreaterThanOrEqualTo(1);
            assertThat(result.getDetails().get(0).getAction()).isEqualTo("keep");
        }

        @Test
        void dedupPreview_emptyVectorSearch_shouldMarkKeep() {
            when(knowledgeBaseRepository.findById(KB_ID)).thenReturn(Optional.of(kb));
            when(vectorService.generateEmbedding(anyString())).thenReturn(PLACEHOLDER_EMBEDDING);
            when(vectorService.search(eq("kb_1"), any(), eq(1), isNull())).thenReturn(List.of());

            DedupPreviewVO result = knowledgeBaseService.dedupPreview(KB_ID, "无相似文档。", "general", USER_ID);

            assertThat(result).isNotNull();
            assertThat(result.getKeep()).isGreaterThanOrEqualTo(1);
            assertThat(result.getDetails().get(0).getAction()).isEqualTo("keep");
        }

        @Test
        void dedupPreview_forbidden_whenUserNotOwner() {
            when(knowledgeBaseRepository.findById(KB_ID)).thenReturn(Optional.of(kb));

            assertThatThrownBy(() ->
                    knowledgeBaseService.dedupPreview(KB_ID, "内容", "general", 999L))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        void dedupPreview_notFound_whenKbMissing() {
            when(knowledgeBaseRepository.findById(KB_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    knowledgeBaseService.dedupPreview(KB_ID, "内容", "general", USER_ID))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("hybridSearch skipCache（任务 35：新导入文档可被检索，RAG 使用 skipCache 不读缓存）")
    class HybridSearchSkipCacheTests {

        @Test
        void hybridSearch_skipCacheTrue_shouldNotReadCache() {
            when(knowledgeBaseRepository.findById(KB_ID)).thenReturn(Optional.of(kb));
            when(queryRewriteService.rewrite(eq(USER_ID), anyString())).thenReturn(List.of("测试查询"));
            when(vectorService.generateEmbedding(anyString())).thenReturn(PLACEHOLDER_EMBEDDING);
            when(vectorService.search(anyString(), anyList(), anyInt(), any()))
                    .thenReturn(List.of(new VectorService.VectorSearchResult(10001L, 0.9f,
                            Map.of("doc_id", 1L, "chunk_index", 0, "title", "参考话术", "text", "案例内容"))));
            when(searchService.search(anyString(), anyString(), eq(0), anyInt(), isNull())).thenReturn(Collections.emptyList());
            when(documentRepository.findByIdIn(anyList())).thenReturn(Collections.emptyList());
            when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

            ReflectionTestUtils.setField(knowledgeBaseService, "cacheEnabled", true);

            List<KnowledgeBaseService.SearchResult> result =
                    knowledgeBaseService.hybridSearch(KB_ID, "测试查询", 5, USER_ID, null, true);

            assertThat(result).isNotNull();
            assertThat(result).isNotEmpty();
            verify(valueOperations, never()).get(anyString());
        }
    }
}
