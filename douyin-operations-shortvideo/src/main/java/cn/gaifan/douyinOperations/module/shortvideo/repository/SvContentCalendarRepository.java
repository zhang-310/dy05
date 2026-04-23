package cn.gaifan.douyinOperations.module.shortvideo.repository;

import cn.gaifan.douyinOperations.module.shortvideo.entity.SvContentCalendar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.List;

public interface SvContentCalendarRepository extends JpaRepository<SvContentCalendar, Long>,
        JpaSpecificationExecutor<SvContentCalendar> {

    List<SvContentCalendar> findByOwnerIdAndPlanDateBetweenAndDeletedOrderByPlanDateAscPriorityDesc(
            Long ownerId, LocalDate from, LocalDate to, Integer deleted);
}
