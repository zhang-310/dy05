package cn.gaifan.douyinOperations.module.slangdict.repository;

import cn.gaifan.douyinOperations.module.slangdict.entity.SdEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface SdEntryRepository extends JpaRepository<SdEntry, Long>, JpaSpecificationExecutor<SdEntry> {

    Optional<SdEntry> findByIdAndDeleted(Long id, Integer deleted);
}
