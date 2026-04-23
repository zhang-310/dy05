package cn.gaifan.douyinOperations.module.benchmark.repository;

import cn.gaifan.douyinOperations.module.benchmark.entity.BenchmarkAccount;
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
 * BenchmarkAccountRepository 数据层测试
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("BenchmarkAccountRepository 数据层测试")
class BenchmarkAccountRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private BenchmarkAccountRepository repository;

    private BenchmarkAccount testAccount;
    private static final Long TEST_USER_ID = 1L;

    @BeforeEach
    void setUp() {
        testAccount = new BenchmarkAccount();
        testAccount.setOwnerId(TEST_USER_ID);
        testAccount.setAccountName("测试账号");
        testAccount.setAccountUrl("https://www.douyin.com/user/MS4wLjABAAAAtest");
        testAccount.setSecUid("MS4wLjABAAAAtest");
        testAccount.setFanCount(100000L);
        testAccount.setVideoCount(500);
        testAccount.setDeleted(0);
        testAccount.setCreateTime(LocalDateTime.now());
        testAccount.setUpdateTime(LocalDateTime.now());
    }

    @Test
    @DisplayName("保存账号 - 应成功保存")
    void save_shouldPersistAccount() {
        BenchmarkAccount saved = repository.save(testAccount);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getAccountName()).isEqualTo("测试账号");
        assertThat(saved.getOwnerId()).isEqualTo(TEST_USER_ID);
    }

    @Test
    @DisplayName("按 ID 查询 - 应返回账号")
    void findById_shouldReturnAccount() {
        BenchmarkAccount saved = entityManager.persistAndFlush(testAccount);

        Optional<BenchmarkAccount> found = repository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getAccountName()).isEqualTo("测试账号");
    }

    @Test
    @DisplayName("按 ownerId 查询 - 应返回用户的账号列表")
    void findByOwnerId_shouldReturnUserAccounts() {
        entityManager.persistAndFlush(testAccount);

        BenchmarkAccount anotherAccount = new BenchmarkAccount();
        anotherAccount.setOwnerId(TEST_USER_ID);
        anotherAccount.setAccountName("另一个账号");
        anotherAccount.setAccountUrl("https://www.douyin.com/user/MS4wLjABAAAAtest2");
        anotherAccount.setSecUid("MS4wLjABAAAAtest2");
        anotherAccount.setDeleted(0);
        anotherAccount.setCreateTime(LocalDateTime.now());
        anotherAccount.setUpdateTime(LocalDateTime.now());
        entityManager.persistAndFlush(anotherAccount);

        List<BenchmarkAccount> accounts = repository.findByOwnerIdAndPlatform(TEST_USER_ID, "douyin");

        assertThat(accounts).hasSize(2);
        assertThat(accounts).extracting(BenchmarkAccount::getAccountName)
                .containsExactlyInAnyOrder("测试账号", "另一个账号");
    }

    @Test
    @DisplayName("按 secUid 查询 - 应返回账号")
    void findBySecUid_shouldReturnAccount() {
        entityManager.persistAndFlush(testAccount);

        BenchmarkAccount found = repository.findBySecUidAndOwnerId("MS4wLjABAAAAtest", TEST_USER_ID);

        assertThat(found).isNotNull();
        assertThat(found.getAccountName()).isEqualTo("测试账号");
    }

    @Test
    @DisplayName("按 secUid 查询 - 不同用户应返回空")
    void findBySecUid_differentUser_shouldReturnNull() {
        entityManager.persistAndFlush(testAccount);

        BenchmarkAccount found = repository.findBySecUidAndOwnerId("MS4wLjABAAAAtest", 999L);

        assertThat(found).isNull();
    }

    @Test
    @DisplayName("逻辑删除 - 应更新 deleted 字段")
    void logicalDelete_shouldUpdateDeletedFlag() {
        BenchmarkAccount saved = entityManager.persistAndFlush(testAccount);

        saved.setDeleted(1);
        repository.save(saved);
        entityManager.flush();
        entityManager.clear();

        // 由于 @SQLRestriction("deleted = 0")，逻辑删除的记录不会被查询到
        Optional<BenchmarkAccount> found = repository.findById(saved.getId());
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("更新账号信息 - 应成功更新")
    void update_shouldModifyAccount() {
        BenchmarkAccount saved = entityManager.persistAndFlush(testAccount);

        saved.setFanCount(200000L);
        saved.setVideoCount(600);
        repository.save(saved);
        entityManager.flush();
        entityManager.clear();

        Optional<BenchmarkAccount> updated = repository.findById(saved.getId());
        assertThat(updated).isPresent();
        assertThat(updated.get().getFanCount()).isEqualTo(200000L);
        assertThat(updated.get().getVideoCount()).isEqualTo(600);
    }
}
