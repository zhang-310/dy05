package cn.gaifan.douyinOperations.module.product.repository;

import cn.gaifan.douyinOperations.module.product.entity.StylePreset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StylePresetRepository extends JpaRepository<StylePreset, Long> {

    List<StylePreset> findByIsEnabledTrueOrderBySortOrderAsc();

    Optional<StylePreset> findByPresetCodeAndDeleted(String presetCode, int deleted);

    List<StylePreset> findAllByOrderBySortOrderAsc();
}
