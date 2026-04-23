package cn.gaifan.douyinOperations.module.benchmark.repository;

import cn.gaifan.douyinOperations.module.benchmark.entity.DouyinCookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DouyinCookieRepository 数据层测试
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("DouyinCookieRepository 数据层测试")
class DouyinCookieRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private DouyinCookieRepository repository;

    private DouyinCookie testCookie;
    private static final Long TEST_USER_ID = 1L;

    @BeforeEach
    void setUp() {
        testCookie = new DouyinCookie();
        testCookie.setOwnerId(TEST_USER_ID);
        testCookie.setCookieName("测试Cookie");
        testCookie.setCookieValue("sessionid=test123; ttwid=test456");
        testCookie.setIsValid(true);
        testCookie.setDeleted(0);
    }

    @Test
    @DisplayName("保存 Cookie - 应成功保存")
    void save_shouldPersistCookie() {
        DouyinCookie saved = repository.save(testCookie);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCookieName()).isEqualTo("测试Cookie");
        assertThat(saved.getOwnerId()).isEqualTo(TEST_USER_ID);
    }

    @Test
    @DisplayName("按 ID 查询 - 应返回 Cookie")
    void findById_shouldReturnCookie() {
        DouyinCookie saved = entityManager.persistAndFlush(testCookie);

        Optional<DouyinCookie> found = repository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getCookieName()).isEqualTo("测试Cookie");
    }

    @Test
    @DisplayName("按 ownerId 查询 - 应返回用户的 Cookie 列表")
    void findByOwnerId_shouldReturnUserCookies() {
        entityManager.persistAndFlush(testCookie);

        DouyinCookie anotherCookie = new DouyinCookie();
        anotherCookie.setOwnerId(TEST_USER_ID);
        anotherCookie.setCookieName("另一个Cookie");
        anotherCookie.setCookieValue("sessionid=test789");
        anotherCookie.setIsValid(true);
        anotherCookie.setDeleted(0);
        entityManager.persistAndFlush(anotherCookie);

        List<DouyinCookie> cookies = repository.findByOwnerIdAndPlatform(TEST_USER_ID, "douyin");

        assertThat(cookies).hasSize(2);
        assertThat(cookies).extracting(DouyinCookie::getCookieName)
                .containsExactlyInAnyOrder("测试Cookie", "另一个Cookie");
    }

    @Test
    @DisplayName("按有效性查询 - 应返回有效的 Cookie")
    void findByIsValid_shouldReturnValidCookies() {
        entityManager.persistAndFlush(testCookie);

        DouyinCookie invalidCookie = new DouyinCookie();
        invalidCookie.setOwnerId(TEST_USER_ID);
        invalidCookie.setCookieName("失效Cookie");
        invalidCookie.setCookieValue("sessionid=invalid");
        invalidCookie.setIsValid(false);
        invalidCookie.setDeleted(0);
        entityManager.persistAndFlush(invalidCookie);

        List<DouyinCookie> validCookies = repository.findByOwnerIdAndIsValid(TEST_USER_ID, true);

        assertThat(validCookies).hasSize(1);
        assertThat(validCookies.get(0).getCookieName()).isEqualTo("测试Cookie");
    }

    @Test
    @DisplayName("更新有效性 - 应成功更新")
    void updateIsValid_shouldModifyValidity() {
        DouyinCookie saved = entityManager.persistAndFlush(testCookie);

        saved.setIsValid(false);
        repository.save(saved);
        entityManager.flush();
        entityManager.clear();

        Optional<DouyinCookie> updated = repository.findById(saved.getId());
        assertThat(updated).isPresent();
        assertThat(updated.get().getIsValid()).isEqualTo(false);
    }

    @Test
    @DisplayName("逻辑删除 - 应更新 deleted 字段")
    void logicalDelete_shouldUpdateDeletedFlag() {
        DouyinCookie saved = entityManager.persistAndFlush(testCookie);

        saved.setDeleted(1);
        repository.save(saved);
        entityManager.flush();
        entityManager.clear();

        Optional<DouyinCookie> found = repository.findById(saved.getId());
        assertThat(found).isEmpty();
    }
}
