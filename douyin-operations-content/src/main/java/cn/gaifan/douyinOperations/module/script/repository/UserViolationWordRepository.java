package cn.gaifan.douyinOperations.module.script.repository;

import cn.gaifan.douyinOperations.module.script.entity.UserViolationWord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserViolationWordRepository extends JpaRepository<UserViolationWord, Long>, JpaSpecificationExecutor<UserViolationWord> {

    Optional<UserViolationWord> findByIdAndDeleted(Long id, Integer deleted);

    List<UserViolationWord> findByUserIdAndStatusAndDeleted(Long userId, Integer status, Integer deleted);

    /** 按 scope 过滤 */
    List<UserViolationWord> findByUserIdAndStatusAndDeletedAndScopeIn(Long userId, Integer status, Integer deleted, Collection<String> scopes);

    boolean existsByUserIdAndWordAndDeleted(Long userId, String word, Integer deleted);
}
