package cn.gaifan.douyinOperations.module.payment.repository;

import cn.gaifan.douyinOperations.module.payment.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    Optional<Subscription> findByUserIdAndDeletedAndStatus(Long userId, int deleted, String status);
    Optional<Subscription> findByOrgIdAndDeletedAndStatus(Long orgId, int deleted, String status);
}
