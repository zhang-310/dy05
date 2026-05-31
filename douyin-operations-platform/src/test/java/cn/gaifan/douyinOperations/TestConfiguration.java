package cn.gaifan.douyinOperations;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.stereotype.Repository;

/**
 * Test configuration for platform module tests
 *
 * This configuration class enables Spring Boot test context without requiring
 * a dependency on the app module (which would create a circular dependency).
 *
 * Excludes DataSourceAutoConfiguration and related JPA/Hibernate configurations
 * since platform module tests don't need a real database connection (services are mocked).
 *
 * Component scanning excludes:
 * - @Repository classes to prevent Spring Data from trying to initialize JPA repositories
 * - Aspect classes that depend on repositories (AuditLogAspect, AdminOnlyAspect)
 */
@SpringBootConfiguration
@EnableAutoConfiguration(exclude = {
    DataSourceAutoConfiguration.class,
    org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
    org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration.class,
    org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration.class,
    org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration.class
})
@ComponentScan(
    basePackages = "cn.gaifan.douyinOperations",
    excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = Repository.class),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "cn\\.gaifan\\.douyinOperations\\.common\\.aspect\\..*"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "cn\\.gaifan\\.douyinOperations\\.common\\.filter\\..*")
    }
)
public class TestConfiguration {
}
