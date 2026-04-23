package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.module.ai.service.KnowledgeBaseService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LiveKnowledgeBaseAccessResolver 测试")
class LiveKnowledgeBaseAccessResolverTest {

    @Mock
    private KnowledgeBaseService knowledgeBaseService;

    @InjectMocks
    private LiveKnowledgeBaseAccessResolver resolver;

    @Test
    @DisplayName("优先返回当前用户自己的知识库")
    void resolveHuashu_shouldPreferUserOwnedKnowledgeBase() {
        ReflectionTestUtils.setField(resolver, "allowSharedKbFallback", true);
        ReflectionTestUtils.setField(resolver, "sharedKbOwnerId", 99L);
        when(knowledgeBaseService.resolveKbIdByName(12L, "huashu")).thenReturn(1001L);

        LiveKnowledgeBaseAccessResolver.ResolvedKnowledgeBase result = resolver.resolveHuashu(12L);

        assertThat(result).isNotNull();
        assertThat(result.kbId()).isEqualTo(1001L);
        assertThat(result.accessUserId()).isEqualTo(12L);
        assertThat(result.sharedFallback()).isFalse();
    }

    @Test
    @DisplayName("用户没有知识库时可显式回退到共享知识库")
    void resolveHuashu_shouldUseSharedKnowledgeBaseWhenEnabled() {
        ReflectionTestUtils.setField(resolver, "allowSharedKbFallback", true);
        ReflectionTestUtils.setField(resolver, "sharedKbOwnerId", 0L);
        when(knowledgeBaseService.resolveKbIdByName(12L, "huashu")).thenReturn(null);
        when(knowledgeBaseService.resolveKbIdByName(0L, "huashu")).thenReturn(2002L);

        LiveKnowledgeBaseAccessResolver.ResolvedKnowledgeBase result = resolver.resolveHuashu(12L);

        assertThat(result).isNotNull();
        assertThat(result.kbId()).isEqualTo(2002L);
        assertThat(result.accessUserId()).isEqualTo(0L);
        assertThat(result.sharedFallback()).isTrue();
    }

    @Test
    @DisplayName("未开启共享回退时不返回共享知识库")
    void resolveHuashu_shouldReturnNullWhenSharedFallbackDisabled() {
        ReflectionTestUtils.setField(resolver, "allowSharedKbFallback", false);
        ReflectionTestUtils.setField(resolver, "sharedKbOwnerId", 99L);
        when(knowledgeBaseService.resolveKbIdByName(12L, "huashu")).thenReturn(null);

        LiveKnowledgeBaseAccessResolver.ResolvedKnowledgeBase result = resolver.resolveHuashu(12L);

        assertThat(result).isNull();
    }
}
