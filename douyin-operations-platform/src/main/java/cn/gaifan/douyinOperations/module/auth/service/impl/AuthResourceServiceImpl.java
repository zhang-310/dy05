package cn.gaifan.douyinOperations.module.auth.service.impl;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.auth.entity.AuthResource;
import cn.gaifan.douyinOperations.module.auth.entity.AuthRole;
import cn.gaifan.douyinOperations.module.auth.entity.AuthUser;
import cn.gaifan.douyinOperations.module.auth.repository.AuthResourceRepository;
import cn.gaifan.douyinOperations.module.auth.repository.AuthRoleRepository;
import cn.gaifan.douyinOperations.module.auth.repository.AuthRoleResourceRepository;
import cn.gaifan.douyinOperations.module.auth.repository.AuthUserRepository;
import cn.gaifan.douyinOperations.module.auth.service.AuthResourceService;
import cn.gaifan.douyinOperations.module.auth.vo.AuthResourceSaveVO;
import cn.gaifan.douyinOperations.module.auth.vo.AuthResourceSearchVO;
import cn.gaifan.douyinOperations.module.auth.vo.AuthResourceVO;
import cn.gaifan.douyinOperations.module.auth.vo.MenuItemVO;
import cn.gaifan.douyinOperations.module.auth.vo.ResourceCodeVO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import jakarta.persistence.criteria.Predicate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 资源：当前用户资源编码；管理端资源 CRUD、菜单树
 */
@Service
public class AuthResourceServiceImpl implements AuthResourceService {

    @Resource
    private AuthUserRepository authUserRepository;
    @Resource
    private AuthRoleRepository authRoleRepository;
    @Resource
    private AuthRoleResourceRepository authRoleResourceRepository;
    @Resource
    private AuthResourceRepository authResourceRepository;

    @Override
    public ResourceCodeVO getResourceCodes(Long userId) {
        ResourceCodeVO vo = new ResourceCodeVO();
        vo.setMenus(new ArrayList<>());
        vo.setApis(new ArrayList<>());
        vo.setButtons(new ArrayList<>());
        Optional<AuthUser> userOpt = authUserRepository.findById(userId);
        if (!userOpt.isPresent()) return vo;
        Optional<AuthRole> roleOpt = authRoleRepository.findByRoleCodeAndDeleted(userOpt.get().getRoleCode(), 0);
        if (!roleOpt.isPresent()) return vo;
        List<Long> resourceIds = authRoleResourceRepository.findByRoleId(roleOpt.get().getId()).stream()
                .map(rr -> rr.getResourceId()).collect(Collectors.toList());
        if (resourceIds.isEmpty()) return vo;
        List<AuthResource> list = authResourceRepository.findAllById(resourceIds);
        for (AuthResource r : list) {
            if ("menu".equals(r.getResourceType())) vo.getMenus().add(r.getResourceCode());
            else if ("api".equals(r.getResourceType())) vo.getApis().add(r.getResourceCode());
            else if ("button".equals(r.getResourceType())) vo.getButtons().add(r.getResourceCode());
        }
        return vo;
    }

    @Override
    public PageResultVO<AuthResourceVO> search(AuthResourceSearchVO vo) {
        final AuthResourceSearchVO q = vo != null ? vo : new AuthResourceSearchVO();
        q.validateParams();
        Specification<AuthResource> spec = (root, query, cb) -> {
            List<Predicate> list = new ArrayList<>();
            list.add(cb.equal(root.get("deleted"), 0));
            if (q.getResourceType() != null && !q.getResourceType().trim().isEmpty()) {
                list.add(cb.equal(root.get("resourceType"), q.getResourceType().trim()));
            }
            if (q.getModule() != null && !q.getModule().trim().isEmpty()) {
                list.add(cb.like(root.get("module"), "%" + q.getModule().trim() + "%"));
            }
            if (q.getResourceCode() != null && !q.getResourceCode().trim().isEmpty()) {
                list.add(cb.like(root.get("resourceCode"), "%" + q.getResourceCode().trim() + "%"));
            }
            if (q.getResourceName() != null && !q.getResourceName().trim().isEmpty()) {
                list.add(cb.like(root.get("resourceName"), "%" + q.getResourceName().trim() + "%"));
            }
            if (q.getParentId() != null) {
                list.add(cb.equal(root.get("parentId"), q.getParentId()));
            }
            return cb.and(list.toArray(new Predicate[0]));
        };
        Sort sort = Sort.by("desc".equalsIgnoreCase(q.getSortOrder()) ? Sort.Direction.DESC : Sort.Direction.ASC,
                q.getSortName() != null ? q.getSortName() : "sortOrder");
        Page<AuthResource> page = authResourceRepository.findAll(spec,
                PageRequest.of(q.getPage(), q.getRows(), sort));
        List<AuthResourceVO> list = page.getContent().stream().map(this::toVO).collect(Collectors.toList());
        return PageResultVO.of(page.getTotalElements(), list, q.getPage(), q.getRows());
    }

    @Override
    public List<MenuItemVO> listMenuTree() {
        List<AuthResource> menus = authResourceRepository.findByResourceTypeAndDeletedOrderBySortOrderAsc("menu", 0);
        return buildMenuTree(menus, 0L);
    }

    @Override
    public List<MenuItemVO> listResourceTree() {
        List<AuthResource> all = authResourceRepository.findByDeletedOrderBySortOrderAsc(0);
        return buildResourceTree(all, 0L);
    }

    @Override
    public AuthResourceVO getById(Long id) {
        if (id == null) return null;
        return authResourceRepository.findById(id).map(this::toVO).orElse(null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long save(AuthResourceSaveVO vo) {
        if (vo == null) return 0;
        AuthResource entity;
        if (vo.getId() != null && vo.getId() > 0) {
            entity = authResourceRepository.findById(vo.getId()).orElse(null);
            if (entity == null) return 0;
        } else {
            entity = new AuthResource();
        }
        entity.setResourceType(vo.getResourceType());
        entity.setResourceCode(vo.getResourceCode());
        entity.setRequestMethod(vo.getRequestMethod());
        entity.setModule(vo.getModule());
        entity.setResourceName(vo.getResourceName());
        entity.setParentId(vo.getParentId() != null ? vo.getParentId() : 0L);
        entity.setSortOrder(vo.getSortOrder() != null ? vo.getSortOrder() : 0);
        entity = authResourceRepository.save(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long id) {
        if (id == null) return;
        Optional<AuthResource> opt = authResourceRepository.findById(id);
        if (!opt.isPresent()) return;
        AuthResource r = opt.get();
        r.setDeleted(1);
        authResourceRepository.save(r);
    }

    private List<MenuItemVO> buildMenuTree(List<AuthResource> menus, Long parentId) {
        List<MenuItemVO> result = new ArrayList<>();
        for (AuthResource r : menus) {
            if (!Objects.equals(r.getParentId() != null ? r.getParentId() : 0L, parentId)) continue;
            MenuItemVO item = new MenuItemVO();
            item.setId(r.getId());
            item.setResourceCode(r.getResourceCode());
            item.setResourceName(r.getResourceName());
            item.setParentId(r.getParentId());
            item.setSortOrder(r.getSortOrder());
            item.setChildren(buildMenuTree(menus, r.getId()));
            result.add(item);
        }
        result.sort(Comparator.comparing(MenuItemVO::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder())));
        return result;
    }

    private List<MenuItemVO> buildResourceTree(List<AuthResource> resources, Long parentId) {
        List<MenuItemVO> result = new ArrayList<>();
        for (AuthResource r : resources) {
            if (!Objects.equals(r.getParentId() != null ? r.getParentId() : 0L, parentId)) continue;
            MenuItemVO item = new MenuItemVO();
            item.setId(r.getId());
            item.setResourceCode(r.getResourceCode());
            item.setResourceName(r.getResourceName());
            item.setResourceType(r.getResourceType());
            item.setModule(r.getModule());
            item.setParentId(r.getParentId());
            item.setSortOrder(r.getSortOrder());
            item.setChildren(buildResourceTree(resources, r.getId()));
            result.add(item);
        }
        result.sort(Comparator
            .comparing(MenuItemVO::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(m -> "menu".equals(m.getResourceType()) ? 0 : "api".equals(m.getResourceType()) ? 1 : 2)
            .thenComparing(MenuItemVO::getId, Comparator.nullsLast(Comparator.naturalOrder())));
        return result;
    }

    private AuthResourceVO toVO(AuthResource r) {
        AuthResourceVO vo = new AuthResourceVO();
        vo.setId(r.getId());
        vo.setResourceType(r.getResourceType());
        vo.setResourceCode(r.getResourceCode());
        vo.setRequestMethod(r.getRequestMethod());
        vo.setModule(r.getModule());
        vo.setResourceName(r.getResourceName());
        vo.setParentId(r.getParentId());
        vo.setSortOrder(r.getSortOrder());
        vo.setCreateTime(r.getCreateTime());
        vo.setUpdateTime(r.getUpdateTime());
        return vo;
    }
}
