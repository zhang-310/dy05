package cn.gaifan.douyinOperations.module.sms.repository;

import cn.gaifan.douyinOperations.module.sms.entity.SmsTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SmsTemplateRepository extends JpaRepository<SmsTemplate, Long>, JpaSpecificationExecutor<SmsTemplate> {

    Optional<SmsTemplate> findByIdAndDeleted(Long id, Integer deleted);

    List<SmsTemplate> findByOwnerIdAndDeleted(Long ownerId, Integer deleted);

    Optional<SmsTemplate> findByOwnerIdAndTemplateCodeAndDeleted(Long ownerId, String templateCode, Integer deleted);

    List<SmsTemplate> findByOwnerIdAndTemplateTypeAndDeleted(Long ownerId, String templateType, Integer deleted);

    List<SmsTemplate> findByOwnerIdAndStatusAndDeleted(Long ownerId, Integer status, Integer deleted);

    @Modifying
    @Query("UPDATE SmsTemplate t SET t.status = :status WHERE t.id = :id")
    void updateStatus(@Param("id") Long id, @Param("status") Integer status);
}
