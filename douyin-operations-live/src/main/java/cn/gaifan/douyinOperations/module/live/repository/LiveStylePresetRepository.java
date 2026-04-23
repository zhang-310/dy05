package cn.gaifan.douyinOperations.module.live.repository;

import cn.gaifan.douyinOperations.module.live.entity.LiveStylePreset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LiveStylePresetRepository extends JpaRepository<LiveStylePreset, Long> {

    List<LiveStylePreset> findByActiveOrderBySortOrderAsc(Integer active);

    Optional<LiveStylePreset> findByStyleKeyAndActive(String styleKey, Integer active);
}
