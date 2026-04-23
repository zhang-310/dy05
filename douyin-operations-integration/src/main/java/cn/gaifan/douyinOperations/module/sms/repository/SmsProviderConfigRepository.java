package cn.gaifan.douyinOperations.module.sms.repository;

import cn.gaifan.douyinOperations.module.sms.entity.SmsProviderConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SmsProviderConfigRepository extends JpaRepository<SmsProviderConfig, Long>, JpaSpecificationExecutor<SmsProviderConfig> {

    Optional<SmsProviderConfig> findByIdAndDeleted(Long id, Integer deleted);

    List<SmsProviderConfig> findByOwnerIdAndDeleted(Long ownerId, Integer deleted);

    Optional<SmsProviderConfig> findByOwnerIdAndIsDefaultAndDeleted(Long ownerId, Integer isDefault, Integer deleted);

    List<SmsProviderConfig> findByOwnerIdAndProviderCodeAndDeleted(Long ownerId, String providerCode, Integer deleted);

    @Modifying
    @Query("UPDATE SmsProviderConfig c SET c.status = :status WHERE c.id = :id")
    void updateStatus(@Param("id") Long id, @Param("status") Integer status);

    @Modifying
    @Query("UPDATE SmsProviderConfig c SET c.isDefault = 0 WHERE c.ownerId = :ownerId AND c.deleted = 0")
    void clearDefaultForOwner(@Param("ownerId") Long ownerId);

    @Modifying
    @Query("UPDATE SmsProviderConfig c SET c.isDefault = 1 WHERE c.id = :id")
    void setAsDefault(@Param("id") Long id);
}
