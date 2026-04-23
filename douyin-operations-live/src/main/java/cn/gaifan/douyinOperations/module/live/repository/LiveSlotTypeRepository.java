package cn.gaifan.douyinOperations.module.live.repository;

import cn.gaifan.douyinOperations.module.live.entity.LiveSlotType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LiveSlotTypeRepository extends JpaRepository<LiveSlotType, Long> {

    List<LiveSlotType> findByIsEnabledTrueOrderBySortOrderAsc();

    Optional<LiveSlotType> findBySlotCodeAndDeleted(String slotCode, int deleted);
}
