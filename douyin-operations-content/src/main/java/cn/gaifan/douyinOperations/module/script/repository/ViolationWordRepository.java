package cn.gaifan.douyinOperations.module.script.repository;

import cn.gaifan.douyinOperations.module.script.entity.ViolationWord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ViolationWordRepository extends JpaRepository<ViolationWord, Long>, JpaSpecificationExecutor<ViolationWord> {

    Optional<ViolationWord> findByIdAndDeleted(Long id, Integer deleted);

    List<ViolationWord> findByStatusAndDeleted(Integer status, Integer deleted);

    /** 按 scope 过滤：scope 为 all 时匹配 all；live 时匹配 all+live_only；video 时匹配 all+video_only */
    List<ViolationWord> findByStatusAndDeletedAndScopeIn(Integer status, Integer deleted, Collection<String> scopes);

    boolean existsByWordAndDeleted(String word, Integer deleted);
}
