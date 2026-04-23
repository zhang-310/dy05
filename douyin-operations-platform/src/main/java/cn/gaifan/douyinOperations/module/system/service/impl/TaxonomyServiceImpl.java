package cn.gaifan.douyinOperations.module.system.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.system.entity.SysTaxonomyNode;
import cn.gaifan.douyinOperations.module.system.repository.SysTaxonomyNodeRepository;
import cn.gaifan.douyinOperations.module.system.service.TaxonomyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.annotation.Resource;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TaxonomyServiceImpl implements TaxonomyService {

    @Resource
    private SysTaxonomyNodeRepository taxonomyNodeRepository;

    @Override
    public List<Map<String, Object>> list(Long userOwnerId, String moduleScope, Long parentId) {
        if (!StringUtils.hasText(moduleScope)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "moduleScope 必填");
        }
        List<Long> owners = new ArrayList<>();
        owners.add(0L);
        if (userOwnerId != null && userOwnerId > 0) {
            owners.add(userOwnerId);
        }
        List<SysTaxonomyNode> rows = taxonomyNodeRepository.listByScopeOwnersAndParent(
                moduleScope.trim(), owners, parentId);
        List<Map<String, Object>> out = new ArrayList<>();
        for (SysTaxonomyNode n : rows) {
            out.add(toRow(n));
        }
        return out;
    }

    @Override
    @Transactional
    public Long save(Long operatorUserId, boolean admin, Map<String, Object> body) {
        if (operatorUserId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (!admin) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅管理员可维护全站分类");
        }
        if (body == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "body 不能为空");
        }
        Long id = body.get("id") instanceof Number n ? n.longValue() : null;
        String moduleScope = body.get("moduleScope") instanceof String s ? s.trim() : null;
        String code = body.get("code") instanceof String s ? s.trim() : null;
        String name = body.get("name") instanceof String s ? s.trim() : null;
        Long parentId = body.get("parentId") instanceof Number n ? n.longValue() : null;
        if (body.get("parentId") == null) {
            parentId = null;
        }
        int sortOrder = body.get("sortOrder") instanceof Number n ? n.intValue() : 0;
        int enabled = body.get("enabled") instanceof Number n && n.intValue() == 0 ? 0 : 1;
        if (!StringUtils.hasText(moduleScope) || !StringUtils.hasText(code) || !StringUtils.hasText(name)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "moduleScope、code、name 必填");
        }
        if (code.length() > 64 || name.length() > 128) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "code/name 超长");
        }
        if (parentId != null) {
            SysTaxonomyNode p = taxonomyNodeRepository.findById(parentId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_FAIL, "父节点不存在"));
            if (p.getDeleted() != null && p.getDeleted() != 0) {
                throw new BusinessException(ErrorCode.VALIDATION_FAIL, "父节点已删除");
            }
            if (!moduleScope.equals(p.getModuleScope())) {
                throw new BusinessException(ErrorCode.VALIDATION_FAIL, "parent 与 moduleScope 不一致");
            }
        }
        long platformOwner = 0L;
        if (id == null) {
            SysTaxonomyNode n = new SysTaxonomyNode();
            n.setOwnerId(platformOwner);
            n.setModuleScope(moduleScope);
            n.setParentId(parentId);
            n.setCode(code);
            n.setName(name);
            n.setSortOrder(sortOrder);
            n.setEnabled(enabled);
            n.setDeleted(0);
            return taxonomyNodeRepository.save(n).getId();
        }
        SysTaxonomyNode n = taxonomyNodeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "节点不存在"));
        if (n.getDeleted() != null && n.getDeleted() != 0) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "节点已删除");
        }
        if (n.getOwnerId() == null || n.getOwnerId() != platformOwner) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅可编辑平台预置节点");
        }
        n.setModuleScope(moduleScope);
        n.setParentId(parentId);
        n.setCode(code);
        n.setName(name);
        n.setSortOrder(sortOrder);
        n.setEnabled(enabled);
        n.setUpdateTime(new Timestamp(System.currentTimeMillis()));
        taxonomyNodeRepository.save(n);
        return n.getId();
    }

    @Override
    @Transactional
    public void delete(Long operatorUserId, boolean admin, Long id) {
        if (operatorUserId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (!admin) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅管理员可删除");
        }
        if (id == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "id 必填");
        }
        SysTaxonomyNode n = taxonomyNodeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "节点不存在"));
        if (n.getOwnerId() == null || n.getOwnerId() != 0L) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅可删除平台节点");
        }
        n.setDeleted(1);
        n.setUpdateTime(new Timestamp(System.currentTimeMillis()));
        taxonomyNodeRepository.save(n);
    }

    @Override
    public List<String> resolveMatchTokens(Long userOwnerId, String moduleScope, List<String> taxonomyCodes) {
        if (!StringUtils.hasText(moduleScope) || taxonomyCodes == null || taxonomyCodes.isEmpty()) {
            return List.of();
        }
        List<String> lower = taxonomyCodes.stream()
                .filter(StringUtils::hasText)
                .map(s -> s.trim().toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
        if (lower.isEmpty()) {
            return List.of();
        }
        List<Long> ownerList = new ArrayList<>();
        ownerList.add(0L);
        if (userOwnerId != null && userOwnerId > 0) {
            ownerList.add(userOwnerId);
        }
        List<SysTaxonomyNode> nodes = taxonomyNodeRepository.findActiveByScopeAndOwnersAndCodesLower(
                moduleScope.trim(), ownerList, lower);
        return expandTaxonomyTokens(nodes);
    }

    private static Map<String, Object> toRow(SysTaxonomyNode n) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", n.getId());
        m.put("ownerId", n.getOwnerId());
        m.put("moduleScope", n.getModuleScope());
        m.put("parentId", n.getParentId());
        m.put("code", n.getCode());
        m.put("name", n.getName());
        m.put("sortOrder", n.getSortOrder());
        m.put("enabled", n.getEnabled());
        return m;
    }

    /** code + name 转小写去重，用于与 tags 逗号分词后 contains 匹配（MVP） */
    static List<String> expandTaxonomyTokens(List<SysTaxonomyNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return List.of();
        }
        return nodes.stream()
                .flatMap(n -> {
                    List<String> t = new ArrayList<>();
                    if (StringUtils.hasText(n.getCode())) {
                        t.add(n.getCode().trim());
                    }
                    if (StringUtils.hasText(n.getName())) {
                        t.add(n.getName().trim());
                    }
                    return t.stream();
                })
                .map(s -> s.toLowerCase(Locale.ROOT))
                .distinct()
                .collect(Collectors.toList());
    }
}
