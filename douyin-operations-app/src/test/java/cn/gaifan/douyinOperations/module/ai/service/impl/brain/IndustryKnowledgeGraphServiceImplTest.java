package cn.gaifan.douyinOperations.module.ai.service.impl.brain;

import cn.gaifan.douyinOperations.module.ai.entity.AiGraphEdge;
import cn.gaifan.douyinOperations.module.ai.entity.AiGraphNode;
import cn.gaifan.douyinOperations.module.ai.repository.AiGraphEdgeRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiGraphNodeRepository;
import cn.gaifan.douyinOperations.module.ai.service.KnowledgeBaseService;
import cn.gaifan.douyinOperations.module.ai.service.brain.IndustryKnowledgeGraphService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("IndustryKnowledgeGraphServiceImpl 知识图谱服务测试")
class IndustryKnowledgeGraphServiceImplTest {

    private IndustryKnowledgeGraphServiceImpl kgService;

    @Mock
    private Neo4jClient neo4jClient;

    @Mock
    private KnowledgeBaseService knowledgeBaseService;

    @Mock
    private AiGraphNodeRepository nodeRepository;

    @Mock
    private AiGraphEdgeRepository edgeRepository;

    @BeforeEach
    void setUp() {
        kgService = new IndustryKnowledgeGraphServiceImpl();
        ReflectionTestUtils.setField(kgService, "enabled", true);
        ReflectionTestUtils.setField(kgService, "maxHops", 2);
        ReflectionTestUtils.setField(kgService, "maxEdgesPerNode", 5);
        ReflectionTestUtils.setField(kgService, "freshnessHint", true);
        ReflectionTestUtils.setField(kgService, "allowSharedKbFallback", false);
        ReflectionTestUtils.setField(kgService, "sharedKbOwnerId", 0L);
    }

    @Nested
    @DisplayName("queryEntities")
    class QueryEntitiesTests {

        @Test
        void queryEntities_disabled_shouldReturnEmpty() {
            ReflectionTestUtils.setField(kgService, "enabled", false);
            assertThat(kgService.queryEntities("topic", "护肤", 20)).isEmpty();
        }

        @Test
        void queryEntities_noNeo4jNoKb_shouldReturnEmpty() {
            ReflectionTestUtils.setField(kgService, "neo4jClient", null);
            ReflectionTestUtils.setField(kgService, "knowledgeBaseService", null);
            assertThat(kgService.queryEntities("topic", "护肤", 20)).isEmpty();
        }

        @Test
        void queryEntities_knowledgeBaseOnly_shouldQueryOwnerScopedKb() {
            ReflectionTestUtils.setField(kgService, "neo4jClient", null);
            ReflectionTestUtils.setField(kgService, "knowledgeBaseService", knowledgeBaseService);

            var kb = new cn.gaifan.douyinOperations.module.ai.entity.AiKnowledgeBase();
            kb.setId(9L);
            when(knowledgeBaseService.listKnowledgeBases(9L)).thenReturn(List.of(kb));
            when(knowledgeBaseService.hybridSearch(9L, "护肤", 20, 9L))
                    .thenReturn(List.of(new KnowledgeBaseService.SearchResult(1L, "标题", "内容", 0.9, "hybrid")));

            List<Map<String, Object>> result = kgService.queryEntities("topic", "护肤", 20, 9L);
            assertThat(result).hasSize(1);
            assertThat(result.get(0)).containsKey("entityId");
            assertThat(result.get(0)).containsKey("name");
        }

        @Test
        void queryEntities_emptyKbList_shouldReturnEmpty() {
            ReflectionTestUtils.setField(kgService, "neo4jClient", null);
            ReflectionTestUtils.setField(kgService, "knowledgeBaseService", knowledgeBaseService);
            when(knowledgeBaseService.listKnowledgeBases(9L)).thenReturn(Collections.emptyList());

            assertThat(kgService.queryEntities("topic", "护肤", 20, 9L)).isEmpty();
        }

        @Test
        void queryEntities_ownerHasNoKb_andSharedFallbackEnabled_shouldQuerySharedKb() {
            ReflectionTestUtils.setField(kgService, "neo4jClient", null);
            ReflectionTestUtils.setField(kgService, "knowledgeBaseService", knowledgeBaseService);
            ReflectionTestUtils.setField(kgService, "allowSharedKbFallback", true);
            ReflectionTestUtils.setField(kgService, "sharedKbOwnerId", 7L);

            var kb = new cn.gaifan.douyinOperations.module.ai.entity.AiKnowledgeBase();
            kb.setId(7L);
            when(knowledgeBaseService.listKnowledgeBases(9L)).thenReturn(Collections.emptyList());
            when(knowledgeBaseService.listKnowledgeBases(7L)).thenReturn(List.of(kb));
            when(knowledgeBaseService.hybridSearch(7L, "护肤", 20, 7L))
                    .thenReturn(List.of(new KnowledgeBaseService.SearchResult(2L, "共享标题", "共享内容", 0.8, "hybrid")));

            List<Map<String, Object>> result = kgService.queryEntities("topic", "护肤", 20, 9L);
            assertThat(result).hasSize(1);
            assertThat(result.get(0)).containsEntry("name", "共享标题");
        }
    }

    @Nested
    @DisplayName("getEntityDetail")
    class GetEntityDetailTests {

        @Test
        void getEntityDetail_disabled_shouldReturnEmptyMap() {
            ReflectionTestUtils.setField(kgService, "enabled", false);
            assertThat(kgService.getEntityDetail("e1", "topic")).isEmpty();
        }

        @Test
        void getEntityDetail_enabled_shouldReturnBasicStructure() {
            ReflectionTestUtils.setField(kgService, "neo4jClient", null);
            Map<String, Object> result = kgService.getEntityDetail("e1", "topic");
            assertThat(result).containsEntry("entityId", "e1");
            assertThat(result).containsEntry("entityType", "topic");
            assertThat(result).containsKey("relations");
        }
    }

    @Nested
    @DisplayName("getGraphContextForQuery")
    class GetGraphContextForQueryTests {

        @Test
        void getGraphContext_twoHops_shouldIncludeChainedPath() {
            ReflectionTestUtils.setField(kgService, "nodeRepository", nodeRepository);
            ReflectionTestUtils.setField(kgService, "edgeRepository", edgeRepository);

            AiGraphNode a = new AiGraphNode();
            a.setId(1L);
            a.setEntityName("玻尿酸");
            a.setOwnerId(0L);
            AiGraphNode b = new AiGraphNode();
            b.setId(2L);
            b.setEntityName("保湿");
            b.setOwnerId(0L);
            AiGraphNode c = new AiGraphNode();
            c.setId(3L);
            c.setEntityName("干性肌");
            c.setOwnerId(0L);

            AiGraphEdge e1 = new AiGraphEdge();
            e1.setSourceNodeId(1L);
            e1.setTargetNodeId(2L);
            e1.setRelationType("relates");
            AiGraphEdge e2 = new AiGraphEdge();
            e2.setSourceNodeId(2L);
            e2.setTargetNodeId(3L);
            e2.setRelationType("suits");

            when(nodeRepository.findByEntityNameContainingIgnoreCaseAndDeleted(anyString(), anyInt()))
                    .thenReturn(List.of(a));
            when(edgeRepository.findBySourceNodeIdAndDeleted(1L, 0)).thenReturn(List.of(e1));
            when(edgeRepository.findBySourceNodeIdAndDeleted(2L, 0)).thenReturn(List.of(e2));
            when(nodeRepository.findById(2L)).thenReturn(java.util.Optional.of(b));
            when(nodeRepository.findById(3L)).thenReturn(java.util.Optional.of(c));

            a.setUpdateTime(LocalDateTime.now().minusDays(1));
            b.setUpdateTime(LocalDateTime.now().minusDays(5));
            c.setUpdateTime(LocalDateTime.now().minusDays(20));

            String ctx = kgService.getGraphContextForQuery("玻尿酸怎么用", 1L, 10);
            assertThat(ctx).contains("玻尿酸-relates-保湿");
            assertThat(ctx).contains("玻尿酸-relates-保湿-suits-干性肌");
            assertThat(ctx).contains("图谱新鲜度");
            assertThat(ctx).contains("天前更新");
        }

        @Test
        void getGraphContext_oneHop_onlyDirectNeighbors() {
            ReflectionTestUtils.setField(kgService, "maxHops", 1);
            ReflectionTestUtils.setField(kgService, "nodeRepository", nodeRepository);
            ReflectionTestUtils.setField(kgService, "edgeRepository", edgeRepository);

            AiGraphNode a = new AiGraphNode();
            a.setId(1L);
            a.setEntityName("玻尿酸");
            a.setOwnerId(0L);
            AiGraphNode b = new AiGraphNode();
            b.setId(2L);
            b.setEntityName("保湿");
            b.setOwnerId(0L);

            AiGraphEdge e1 = new AiGraphEdge();
            e1.setSourceNodeId(1L);
            e1.setTargetNodeId(2L);
            e1.setRelationType("relates");
            when(nodeRepository.findByEntityNameContainingIgnoreCaseAndDeleted(anyString(), anyInt()))
                    .thenReturn(List.of(a));
            when(edgeRepository.findBySourceNodeIdAndDeleted(1L, 0)).thenReturn(List.of(e1));
            when(nodeRepository.findById(2L)).thenReturn(java.util.Optional.of(b));

            String ctx = kgService.getGraphContextForQuery("玻尿酸", 1L, 10);
            assertThat(ctx).contains("玻尿酸-relates-保湿");
            assertThat(ctx).doesNotContain("suits-干性肌");
        }
    }

    @Nested
    @DisplayName("isAvailable")
    class IsAvailableTests {

        @Test
        void isAvailable_noBackend_shouldReturnFalse() {
            ReflectionTestUtils.setField(kgService, "neo4jClient", null);
            ReflectionTestUtils.setField(kgService, "knowledgeBaseService", null);
            assertThat(kgService.isAvailable()).isFalse();
        }

        @Test
        void isAvailable_withKb_shouldReturnTrue() {
            ReflectionTestUtils.setField(kgService, "neo4jClient", null);
            ReflectionTestUtils.setField(kgService, "knowledgeBaseService", knowledgeBaseService);
            assertThat(kgService.isAvailable()).isTrue();
        }

        @Test
        void isAvailable_disabled_shouldReturnFalse() {
            ReflectionTestUtils.setField(kgService, "enabled", false);
            ReflectionTestUtils.setField(kgService, "knowledgeBaseService", knowledgeBaseService);
            assertThat(kgService.isAvailable()).isFalse();
        }
    }
}
