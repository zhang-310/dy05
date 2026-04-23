package cn.gaifan.douyinOperations.module.auth.service.impl;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.auth.entity.AuthRole;
import cn.gaifan.douyinOperations.module.auth.entity.AuthRoleResource;
import cn.gaifan.douyinOperations.module.auth.repository.AuthRoleRepository;
import cn.gaifan.douyinOperations.module.auth.repository.AuthRoleResourceRepository;
import cn.gaifan.douyinOperations.module.auth.service.AuthRoleService;
import cn.gaifan.douyinOperations.module.auth.vo.AuthRoleSaveVO;
import cn.gaifan.douyinOperations.module.auth.vo.AuthRoleSearchVO;
import cn.gaifan.douyinOperations.module.auth.vo.AuthRoleVO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 角色管理实现
 */
@Service
public class AuthRoleServiceImpl implements AuthRoleService {

    @Resource
    private AuthRoleRepository authRoleRepository;
    @Resource
    private AuthRoleResourceRepository authRoleResourceRepository;

    @Override
    public PageResultVO<AuthRoleVO> search(AuthRoleSearchVO vo) {
        final AuthRoleSearchVO q = vo != null ? vo : new AuthRoleSearchVO();
        q.validateParams();
        Specification<AuthRole> spec = (root, query, cb) -> {
            List<Predicate> list = new ArrayList<>();
            list.add(cb.equal(root.get("deleted"), 0));
            if (q.getRoleCode() != null && !q.getRoleCode().trim().isEmpty()) {
                list.add(cb.like(root.get("roleCode"), "%" + q.getRoleCode().trim() + "%"));
            }
            if (q.getRoleName() != null && !q.getRoleName().trim().isEmpty()) {
                list.add(cb.like(root.get("roleName"), "%" + q.getRoleName().trim() + "%"));
            }
            if (q.getStatus() != null) {
                list.add(cb.equal(root.get("status"), q.getStatus()));
            }
            return cb.and(list.toArray(new Predicate[0]));
        };
        Sort sort = Sort.by("desc".equalsIgnoreCase(q.getSortOrder()) ? Sort.Direction.DESC : Sort.Direction.ASC,
                q.getSortName() != null ? q.getSortName() : "id");
        Page<AuthRole> page = authRoleRepository.findAll(spec,
                PageRequest.of(q.getPage(), q.getRows(), sort));
        List<AuthRoleVO> list = page.getContent().stream().map(this::toVO).collect(Collectors.toList());
        return PageResultVO.of(page.getTotalElements(), list, q.getPage(), q.getRows());
    }

    @Override
    public List<AuthRoleVO> listAll() {
        return authRoleRepository.findByDeletedOrderBySortOrderAsc(0).stream()
                .map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public AuthRoleVO getById(Long id) {
        if (id == null) return null;
        return authRoleRepository.findById(id).map(this::toVO).orElse(null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long save(AuthRoleSaveVO vo) {
        if (vo == null) return 0;
        AuthRole entity;
        if (vo.getId() != null && vo.getId() > 0) {
            entity = authRoleRepository.findById(vo.getId()).orElse(null);
            if (entity == null) return 0;
        } else {
            entity = new AuthRole();
            if (authRoleRepository.findByRoleCodeAndDeleted(vo.getRoleCode(), 0).isPresent()) {
                throw new IllegalArgumentException("角色编码已存在");
            }
        }
        entity.setRoleCode(vo.getRoleCode());
        entity.setRoleName(vo.getRoleName());
        entity.setSortOrder(vo.getSortOrder() != null ? vo.getSortOrder() : 0);
        entity.setStatus(vo.getStatus() != null ? vo.getStatus() : 1);
        entity = authRoleRepository.save(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long id) {
        if (id == null) return;
        Optional<AuthRole> opt = authRoleRepository.findById(id);
        if (!opt.isPresent()) return;
        AuthRole r = opt.get();
        r.setDeleted(1);
        authRoleRepository.save(r);
    }

    @Override
    @Cacheable(value = "auth:roleResources", key = "#roleId", unless = "#result == null || #result.isEmpty()")
    public List<Long> getResourceIdsByRoleId(Long roleId) {
        if (roleId == null) return new ArrayList<>();
        return authRoleResourceRepository.findByRoleId(roleId).stream()
                .map(AuthRoleResource::getResourceId).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "auth:roleResources", key = "#roleId")
    public void saveRoleResources(Long roleId, List<Long> resourceIds) {
        if (roleId == null) return;
        authRoleResourceRepository.deleteByRoleId(roleId);
        if (resourceIds != null && !resourceIds.isEmpty()) {
            for (Long resId : resourceIds) {
                if (resId == null) continue;
                AuthRoleResource rr = new AuthRoleResource();
                rr.setRoleId(roleId);
                rr.setResourceId(resId);
                authRoleResourceRepository.save(rr);
            }
        }
    }

    private AuthRoleVO toVO(AuthRole r) {
        AuthRoleVO vo = new AuthRoleVO();
        vo.setId(r.getId());
        vo.setRoleCode(r.getRoleCode());
        vo.setRoleName(r.getRoleName());
        vo.setSortOrder(r.getSortOrder());
        vo.setStatus(r.getStatus());
        vo.setCreateTime(r.getCreateTime());
        vo.setUpdateTime(r.getUpdateTime());
        return vo;
    }
}
