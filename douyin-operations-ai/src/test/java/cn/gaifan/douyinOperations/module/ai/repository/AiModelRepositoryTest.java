package cn.gaifan.douyinOperations.module.ai.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AiModelRepository 契约测试")
class AiModelRepositoryTest {

    @Test
    @DisplayName("模型用量增量更新 - 更新查询必须自带事务")
    void incrementQuotaUsed_shouldDeclareTransactionalModifyingQuery() throws Exception {
        Method method = AiModelRepository.class.getMethod("incrementQuotaUsed", Long.class, Long.class);

        assertThat(method.getAnnotation(Modifying.class)).isNotNull();
        assertThat(method.getAnnotation(Transactional.class)).isNotNull();
    }
}
