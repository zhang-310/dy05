package cn.gaifan.douyinOperations.module.ai.config;

import cn.gaifan.douyinOperations.module.ai.entity.AiKnowledgeBase;
import cn.gaifan.douyinOperations.module.ai.repository.AiKnowledgeBaseRepository;
import cn.gaifan.douyinOperations.module.ai.service.KnowledgeBaseService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("KnowledgeBaseInitializer 测试")
class KnowledgeBaseInitializerTest {

    @Mock
    private AiKnowledgeBaseRepository knowledgeBaseRepository;

    @Mock
    private KnowledgeBaseService knowledgeBaseService;

    @InjectMocks
    private KnowledgeBaseInitializer initializer;

    @Test
    @DisplayName("应按共享 owner 初始化默认知识库")
    void init_shouldCreateDefaultKnowledgeBasesForSharedOwner() {
        ReflectionTestUtils.setField(initializer, "initEnabled", true);
        ReflectionTestUtils.setField(initializer, "sharedOwnerId", 0L);
        when(knowledgeBaseRepository.findByUserIdAndKbNameAndDeleted(0L, "douyin", 0)).thenReturn(Optional.empty());
        when(knowledgeBaseRepository.findByUserIdAndKbNameAndDeleted(0L, "zhishi", 0)).thenReturn(Optional.empty());
        when(knowledgeBaseRepository.findByUserIdAndKbNameAndDeleted(0L, "huashu", 0)).thenReturn(Optional.empty());

        AiKnowledgeBase kb = new AiKnowledgeBase();
        kb.setId(1L);
        when(knowledgeBaseService.createKnowledgeBase(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.eq(0L)))
                .thenReturn(kb);

        initializer.init();

        verify(knowledgeBaseService, times(3)).createKnowledgeBase(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.eq(0L));
    }

    @Test
    @DisplayName("初始化关闭时应跳过")
    void init_shouldSkipWhenDisabled() {
        ReflectionTestUtils.setField(initializer, "initEnabled", false);
        ReflectionTestUtils.setField(initializer, "sharedOwnerId", 0L);

        initializer.init();

        verify(knowledgeBaseRepository, never()).findByUserIdAndKbNameAndDeleted(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyInt());
        verify(knowledgeBaseService, never()).createKnowledgeBase(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyLong());
    }
}
