package cn.gaifan.douyinOperations.module.shortvideo.repository;

import cn.gaifan.douyinOperations.module.shortvideo.entity.SvWebhookDlq;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SvWebhookDlqRepository extends JpaRepository<SvWebhookDlq, Long> {

    Page<SvWebhookDlq> findByOwnerIdOrderByIdDesc(Long ownerId, Pageable pageable);
}
