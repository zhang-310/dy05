package cn.gaifan.douyinOperations.module.ai.repository;

import cn.gaifan.douyinOperations.module.ai.entity.AiEvolveTask;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import static org.assertj.core.api.Assertions.assertThat;

class EvolveTaskSpecificationsTest {

    @Test
    void listFilter_returnsNonNullSpecification() {
        Specification<AiEvolveTask> spec = EvolveTaskSpecifications.listFilter(1L, "gap", null, 2);
        assertThat(spec).isNotNull();
    }

    @Test
    void kbIdOptional_null_isConjunction() {
        assertThat(EvolveTaskSpecifications.kbIdOptional(null)).isNotNull();
    }
}
