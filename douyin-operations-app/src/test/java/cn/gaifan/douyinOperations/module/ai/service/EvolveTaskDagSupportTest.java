package cn.gaifan.douyinOperations.module.ai.service;

import cn.gaifan.douyinOperations.module.ai.entity.AiEvolveTask;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EvolveTaskDagSupportTest {

    @Test
    void parseDepends_trimsAndDedupes() {
        assertEquals(List.of("a", "b"), EvolveTaskDagSupport.parseDependsOnTaskNos(" a , b , a "));
        assertTrue(EvolveTaskDagSupport.parseDependsOnTaskNos(null).isEmpty());
    }

    @Test
    void wouldCreateCycle_detectsTwoTaskCycle() {
        AiEvolveTask t1 = new AiEvolveTask();
        t1.setTaskNo("T1");
        t1.setDependsOnTaskNos("T2");
        AiEvolveTask t2 = new AiEvolveTask();
        t2.setTaskNo("T2");
        t2.setDependsOnTaskNos("T1");
        assertTrue(EvolveTaskDagSupport.wouldCreateCycle(List.of(t1, t2), "T3", List.of()));
    }

    @Test
    void wouldCreateCycle_linearChainOk() {
        AiEvolveTask t1 = new AiEvolveTask();
        t1.setTaskNo("T1");
        t1.setDependsOnTaskNos(null);
        AiEvolveTask t2 = new AiEvolveTask();
        t2.setTaskNo("T2");
        t2.setDependsOnTaskNos("T1");
        assertFalse(EvolveTaskDagSupport.wouldCreateCycle(List.of(t1, t2), "T3", List.of("T2")));
    }

    @Test
    void wouldCreateCycle_parallelForkOk() {
        AiEvolveTask t1 = new AiEvolveTask();
        t1.setTaskNo("T1");
        AiEvolveTask t2 = new AiEvolveTask();
        t2.setTaskNo("T2");
        assertFalse(EvolveTaskDagSupport.wouldCreateCycle(List.of(t1, t2), "T3", List.of("T1", "T2")));
    }

    @Test
    void allDependenciesCompleted_requiresCompletedStatus() {
        assertTrue(EvolveTaskDagSupport.allDependenciesCompleted(
                java.util.Map.of("A", "completed", "B", "completed"), List.of("A", "B")));
        assertFalse(EvolveTaskDagSupport.allDependenciesCompleted(
                java.util.Map.of("A", "completed", "B", "blocked"), List.of("A", "B")));
    }

    @Test
    void parseTopicIdList_handlesBracketForm() {
        assertEquals(List.of(1L, 2L), EvolveTaskDagSupport.parseTopicIdList("[1,2]"));
    }
}
