package cn.gaifan.douyinOperations.module.douyinapi.repository;

import cn.gaifan.douyinOperations.module.douyinapi.entity.OAuthToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.sql.Timestamp;
import java.util.Optional;

public interface OAuthTokenRepository extends JpaRepository<OAuthToken, Long> {

    /**
     * 根据用户ID和提供商查找token
     */
    Optional<OAuthToken> findByUserIdAndProviderAndDeleted(Long userId, String provider, Integer deleted);

    /**
     * 根据openId和提供商查找token
     */
    Optional<OAuthToken> findByOpenIdAndProviderAndDeleted(String openId, String provider, Integer deleted);

    /**
     * 更新token（保留 scope）
     */
    @Modifying
    @Query("UPDATE OAuthToken t SET t.accessToken = :accessToken, t.refreshToken = :refreshToken, " +
           "t.expiresAt = :expiresAt, t.updateTime = CURRENT_TIMESTAMP " +
           "WHERE t.userId = :userId AND t.provider = :provider AND t.deleted = 0")
    int updateToken(@Param("userId") Long userId,
                    @Param("provider") String provider,
                    @Param("accessToken") String accessToken,
                    @Param("refreshToken") String refreshToken,
                    @Param("expiresAt") Timestamp expiresAt);

    /**
     * 删除用户的token（软删除）
     */
    @Modifying
    @Query("UPDATE OAuthToken t SET t.deleted = 1, t.updateTime = CURRENT_TIMESTAMP " +
           "WHERE t.userId = :userId AND t.provider = :provider")
    int deleteByUserIdAndProvider(@Param("userId") Long userId, @Param("provider") String provider);

    /** 按 provider + deleted 查询所有 token（token 健康探针用） */
    java.util.List<OAuthToken> findAllByProviderAndDeleted(String provider, Integer deleted);
}
