package cn.gaifan.douyinOperations.module.benchmark.repository;

import cn.gaifan.douyinOperations.module.benchmark.entity.DouyinCookie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 抖音Cookie Repository
 */
@Repository
public interface DouyinCookieRepository extends JpaRepository<DouyinCookie, Long>, JpaSpecificationExecutor<DouyinCookie> {

    /**
     * 根据ownerId和有效性查找Cookie
     */
    List<DouyinCookie> findByOwnerIdAndIsValid(Long ownerId, Boolean isValid);

    /**
     * 根据ownerId和平台查找Cookie
     */
    List<DouyinCookie> findByOwnerIdAndPlatform(Long ownerId, String platform);

    /**
     * 根据ownerId、平台和有效性查找Cookie
     */
    List<DouyinCookie> findByOwnerIdAndPlatformAndIsValid(Long ownerId, String platform, Boolean isValid);

    /**
     * 根据cookieName查找
     */
    Optional<DouyinCookie> findByCookieName(String cookieName);
}
