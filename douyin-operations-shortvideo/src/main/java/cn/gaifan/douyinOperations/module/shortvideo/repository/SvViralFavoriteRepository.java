package cn.gaifan.douyinOperations.module.shortvideo.repository;

import cn.gaifan.douyinOperations.module.shortvideo.entity.SvViralFavorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;
import java.util.Optional;

/**
 * 爆款收藏 Repository
 */
public interface SvViralFavoriteRepository extends JpaRepository<SvViralFavorite, Long> {

    Optional<SvViralFavorite> findByUserIdAndViralVideoId(Long userId, Long viralVideoId);

    List<SvViralFavorite> findByUserIdOrderByCreateTimeDesc(Long userId);

    boolean existsByUserIdAndViralVideoId(Long userId, Long viralVideoId);

    @Modifying
    void deleteByUserIdAndViralVideoId(Long userId, Long viralVideoId);
}
