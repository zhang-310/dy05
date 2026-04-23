package cn.gaifan.douyinOperations.module.storage.repository;

import cn.gaifan.douyinOperations.module.storage.entity.SysFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface SysFileRepository extends JpaRepository<SysFile, Long>, JpaSpecificationExecutor<SysFile> {

    Optional<SysFile> findByIdAndDeleted(Long id, Integer deleted);

    List<SysFile> findByOwnerIdAndDeleted(Long ownerId, Integer deleted);

    List<SysFile> findByOwnerIdAndModuleAndDeleted(Long ownerId, String module, Integer deleted);

    Page<SysFile> findByOwnerIdAndDeleted(Long ownerId, Integer deleted, Pageable pageable);

    Optional<SysFile> findByStoragePathAndDeleted(String storagePath, Integer deleted);
}
