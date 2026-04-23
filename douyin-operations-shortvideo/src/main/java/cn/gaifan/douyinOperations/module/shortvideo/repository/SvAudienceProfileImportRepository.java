package cn.gaifan.douyinOperations.module.shortvideo.repository;

import cn.gaifan.douyinOperations.module.shortvideo.entity.SvAudienceProfileImport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SvAudienceProfileImportRepository extends JpaRepository<SvAudienceProfileImport, Long> {

    Optional<SvAudienceProfileImport> findFirstByOwnerIdAndDeletedOrderByCreateTimeDesc(Long ownerId, int deleted);
}
