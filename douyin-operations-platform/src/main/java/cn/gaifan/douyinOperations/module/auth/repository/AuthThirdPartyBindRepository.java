package cn.gaifan.douyinOperations.module.auth.repository;

import cn.gaifan.douyinOperations.module.auth.entity.AuthThirdPartyBind;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 第三方账号绑定表 Repository
 */
public interface AuthThirdPartyBindRepository extends JpaRepository<AuthThirdPartyBind, Long> {

    Optional<AuthThirdPartyBind> findByProviderAndOpenIdAndDeleted(String provider, String openId, Integer deleted);

    Optional<AuthThirdPartyBind> findByUserIdAndProviderAndDeleted(Long userId, String provider, Integer deleted);

    List<AuthThirdPartyBind> findByUserIdAndDeleted(Long userId, Integer deleted);

    boolean existsByUserIdAndProviderAndDeleted(Long userId, String provider, Integer deleted);
}
