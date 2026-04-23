package cn.gaifan.douyinOperations.module.benchmark.service.impl;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.benchmark.entity.DouyinCookie;
import cn.gaifan.douyinOperations.module.benchmark.repository.DouyinCookieRepository;
import cn.gaifan.douyinOperations.module.benchmark.service.DouyinCookieService;
import cn.gaifan.douyinOperations.module.benchmark.vo.DouyinCookieSearchVO;
import cn.gaifan.douyinOperations.module.benchmark.vo.DouyinCookieSaveVO;
import cn.gaifan.douyinOperations.module.benchmark.vo.DouyinCookieVO;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 抖音Cookie管理服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DouyinCookieServiceImpl implements DouyinCookieService {

    private final DouyinCookieRepository cookieRepository;

    // AES加密密钥（生产环境应从配置文件或密钥管理服务获取）
    // 必须是16/24/32字节
    private static final String AES_KEY = "DY02_COOKIE_2026_SECURE_KEY!"; // 28字节，补齐到32
    private static final String ALGORITHM = "AES";

    /**
     * 获取32字节的AES密钥
     */
    private byte[] getAesKey() {
        byte[] keyBytes = AES_KEY.getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[32]; // AES-256需要32字节
        System.arraycopy(keyBytes, 0, result, 0, Math.min(keyBytes.length, 32));
        return result;
    }

    @Override
    public PageResultVO<DouyinCookieVO> search(DouyinCookieSearchVO searchVO, Long ownerId) {
        searchVO.validateParams();

        Specification<DouyinCookie> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 数据隔离
            predicates.add(cb.equal(root.get("ownerId"), ownerId));

            // 关键词搜索
            if (StringUtils.hasText(searchVO.getKeyword())) {
                String keyword = "%" + searchVO.getKeyword() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("cookieName"), keyword),
                        cb.like(root.get("accountName"), keyword)
                ));
            }

            // 平台过滤
            if (StringUtils.hasText(searchVO.getPlatform())) {
                predicates.add(cb.equal(root.get("platform"), searchVO.getPlatform()));
            }

            // 有效性过滤
            if (searchVO.getIsValid() != null) {
                predicates.add(cb.equal(root.get("isValid"), searchVO.getIsValid()));
            }

            // 验证状态过滤
            if (StringUtils.hasText(searchVO.getCheckStatus())) {
                predicates.add(cb.equal(root.get("checkStatus"), searchVO.getCheckStatus()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Sort sort = Sort.by(Sort.Direction.DESC, "createTime");
        Pageable pageable = PageRequest.of(searchVO.getPage(), searchVO.getRows(), sort);
        Page<DouyinCookie> page = cookieRepository.findAll(spec, pageable);

        List<DouyinCookieVO> voList = page.getContent().stream()
                .map(this::entityToVO)
                .toList();

        return new PageResultVO<>(page.getTotalElements(), voList, searchVO.getPage(), searchVO.getRows());
    }

    @Override
    public DouyinCookieVO getById(Long id, Long ownerId) {
        DouyinCookie cookie = cookieRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cookie不存在"));

        if (!cookie.getOwnerId().equals(ownerId)) {
            throw new RuntimeException("无权访问该Cookie");
        }

        return entityToVO(cookie);
    }

    @Override
    @Transactional
    public DouyinCookieVO save(DouyinCookieSaveVO saveVO, Long ownerId) {
        DouyinCookie cookie;

        if (saveVO.getId() != null) {
            // 更新
            cookie = cookieRepository.findById(saveVO.getId())
                    .orElseThrow(() -> new RuntimeException("Cookie不存在"));

            if (!cookie.getOwnerId().equals(ownerId)) {
                throw new RuntimeException("无权修改该Cookie");
            }
        } else {
            // 新增
            cookie = new DouyinCookie();
            cookie.setOwnerId(ownerId);
        }

        BeanUtils.copyProperties(saveVO, cookie, "id", "ownerId", "cookieValue");

        // 加密存储Cookie值
        if (StringUtils.hasText(saveVO.getCookieValue())) {
            try {
                String encryptedValue = encrypt(saveVO.getCookieValue());
                cookie.setCookieValue(encryptedValue);
            } catch (Exception e) {
                log.error("Cookie加密失败: {}", e.getMessage(), e);
                throw new RuntimeException("Cookie加密失败");
            }
        }

        cookie = cookieRepository.save(cookie);

        return entityToVO(cookie);
    }

    @Override
    @Transactional
    public void delete(Long id, Long ownerId) {
        DouyinCookie cookie = cookieRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cookie不存在"));

        if (!cookie.getOwnerId().equals(ownerId)) {
            throw new RuntimeException("无权删除该Cookie");
        }

        cookie.setDeleted(1);
        cookieRepository.save(cookie);
    }

    @Override
    @Transactional
    public Boolean validate(Long id, Long ownerId) {
        DouyinCookie cookie = cookieRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cookie不存在"));

        if (!cookie.getOwnerId().equals(ownerId)) {
            throw new RuntimeException("无权验证该Cookie");
        }

        // TODO: 实际验证逻辑 - 使用Playwright访问抖音验证Cookie有效性
        boolean isValid = validateCookieWithPlaywright(cookie.getCookieValue());

        cookie.setIsValid(isValid);
        cookie.setLastCheckTime(LocalDateTime.now());
        cookie.setCheckStatus(isValid ? "valid" : "invalid");
        cookieRepository.save(cookie);

        return isValid;
    }

    @Override
    public String getAvailableCookie(Long ownerId, String platform) {
        List<DouyinCookie> cookies = cookieRepository.findByOwnerIdAndPlatformAndIsValid(
                ownerId, platform, true
        );
        // 无「已标记有效」记录时，仍允许用该平台任意一条（含弱校验保存后未过验证的），否则 Playwright 采集永远拿不到库内 Cookie
        if (cookies.isEmpty()) {
            cookies = cookieRepository.findByOwnerIdAndPlatform(ownerId, platform);
        }
        if (cookies.isEmpty()) {
            return null;
        }

        DouyinCookie selectedCookie = cookies.stream()
                .min((c1, c2) -> Integer.compare(c1.getUsageCount(), c2.getUsageCount()))
                .orElse(cookies.get(0));

        selectedCookie.setUsageCount(selectedCookie.getUsageCount() + 1);
        selectedCookie.setLastUsedTime(LocalDateTime.now());
        cookieRepository.save(selectedCookie);

        try {
            return decrypt(selectedCookie.getCookieValue());
        } catch (Exception e) {
            log.error("Cookie解密失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * AES加密
     */
    private String encrypt(String content) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(getAesKey(), ALGORITHM);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);
        byte[] encrypted = cipher.doFinal(content.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    /**
     * AES解密
     */
    private String decrypt(String encryptedContent) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(getAesKey(), ALGORITHM);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, keySpec);
        byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedContent));
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    @Override
    @Transactional
    public void recordUsage(Long id) {
        cookieRepository.findById(id).ifPresent(cookie -> {
            cookie.setUsageCount(cookie.getUsageCount() + 1);
            cookie.setLastUsedTime(LocalDateTime.now());
            cookieRepository.save(cookie);
        });
    }

    /**
     * 校验 Cookie 串是否含抖音网页登录常见「强」会话键（与扫码弱校验区分，避免仅游客/SSO 态误判为可用）。
     */
    private boolean validateCookieWithPlaywright(String encryptedCookieValue) {
        try {
            String header = decrypt(encryptedCookieValue);
            if (!StringUtils.hasText(header)) {
                return false;
            }
            String preview = header.length() > 24 ? header.substring(0, 24) + "…" : header;
            boolean ok = hasStrongSessionInHeader(header);
            log.info("验证Cookie（键名规则）: 强会话特征={}, 预览={}", ok, preview);
            return ok;
        } catch (Exception e) {
            log.warn("验证Cookie失败: {}", e.getMessage());
            return false;
        }
    }

    /** 与扫码轮询强规则对齐：需出现典型会话类键且值非空 */
    private static boolean hasStrongSessionInHeader(String cookieHeader) {
        Set<String> names = new HashSet<>();
        for (String part : cookieHeader.split(";")) {
            String p = part.trim();
            if (p.isEmpty()) {
                continue;
            }
            int eq = p.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String name = p.substring(0, eq).trim().toLowerCase(Locale.ROOT);
            String val = eq + 1 < p.length() ? p.substring(eq + 1).trim() : "";
            if (name.isEmpty() || !StringUtils.hasText(val)) {
                continue;
            }
            names.add(name);
        }
        if (names.contains("sessionid") || names.contains("sessionid_ss")) {
            return true;
        }
        if (names.contains("sid_tt") || names.contains("sid_guard")) {
            return true;
        }
        return false;
    }

    private DouyinCookieVO entityToVO(DouyinCookie entity) {
        DouyinCookieVO vo = new DouyinCookieVO();
        BeanUtils.copyProperties(entity, vo);
        // 不返回完整Cookie值，只返回前10个字符（脱敏）
        if (StringUtils.hasText(entity.getCookieValue())) {
            try {
                String decrypted = decrypt(entity.getCookieValue());
                vo.setCookieValue(decrypted.length() > 10 ? decrypted.substring(0, 10) + "..." : decrypted);
            } catch (Exception e) {
                vo.setCookieValue("***");
            }
        }
        return vo;
    }
}
