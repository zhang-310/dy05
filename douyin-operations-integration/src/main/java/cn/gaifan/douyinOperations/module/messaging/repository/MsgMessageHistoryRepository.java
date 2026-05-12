package cn.gaifan.douyinOperations.module.messaging.repository;

import cn.gaifan.douyinOperations.module.messaging.entity.MsgMessageHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * P1-5: 消息历史记录 Repository
 */
@Repository
public interface MsgMessageHistoryRepository extends JpaRepository<MsgMessageHistory, Long>, JpaSpecificationExecutor<MsgMessageHistory> {

    List<MsgMessageHistory> findByConfigIdAndDeletedOrderByCreateTimeDesc(Long configId, Integer deleted);

    List<MsgMessageHistory> findBySenderIdAndDeletedOrderByCreateTimeDesc(String senderId, Integer deleted);

    List<MsgMessageHistory> findByAgentIdAndDeletedOrderByCreateTimeDesc(Long agentId, Integer deleted);
}
