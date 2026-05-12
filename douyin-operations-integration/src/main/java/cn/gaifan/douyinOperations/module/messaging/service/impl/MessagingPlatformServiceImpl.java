package cn.gaifan.douyinOperations.module.messaging.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.messaging.entity.MsgPlatformConfig;
import cn.gaifan.douyinOperations.module.messaging.repository.MsgPlatformConfigRepository;
import cn.gaifan.douyinOperations.module.messaging.service.MessagingPlatformService;
import cn.gaifan.douyinOperations.module.messaging.vo.MsgPlatformConfigSearchVO;
import cn.gaifan.douyinOperations.module.messaging.vo.MsgPlatformConfigSaveVO;
import cn.gaifan.douyinOperations.module.messaging.vo.MsgPlatformConfigVO;
import jakarta.annotation.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

@Service
public class MessagingPlatformServiceImpl implements MessagingPlatformService {

    @Resource
    private MsgPlatformConfigRepository repository;

    @Override
    public PageResultVO<MsgPlatformConfigVO> search(MsgPlatformConfigSearchVO vo) {
        MsgPlatformConfigSearchVO searchVO = (vo == null) ? new MsgPlatformConfigSearchVO() : vo;
        searchVO.validateParams();
        Specification<MsgPlatformConfig> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("deleted"), 0));
            if (searchVO.getOwnerId() != null) predicates.add(cb.equal(root.get("ownerId"), searchVO.getOwnerId()));
            if (searchVO.getPlatform() != null && !searchVO.getPlatform().isBlank()) {
                predicates.add(cb.equal(root.get("platform"), searchVO.getPlatform().trim()));
            }
            if (searchVO.getStatus() != null) predicates.add(cb.equal(root.get("status"), searchVO.getStatus()));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        String sortName = "id";
        PageRequest pageable = PageRequest.of(searchVO.getPage(), searchVO.getRows(),
                Sort.by("desc".equalsIgnoreCase(searchVO.getSortOrder()) ? Sort.Direction.DESC : Sort.Direction.ASC, sortName));
        Page<MsgPlatformConfig> page = repository.findAll(spec, pageable);
        return PageResultVO.of(page.getTotalElements(),
                page.getContent().stream().map(this::toVO).toList(),
                searchVO.getPage(), searchVO.getRows());
    }

    @Override
    public MsgPlatformConfigVO getById(Long id, Long userId) {
        MsgPlatformConfig entity = repository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "配置不存在"));
        // P1-1: IDOR 防护 - 校验所有权
        if (!entity.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问他人的配置");
        }
        return toVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long save(MsgPlatformConfigSaveVO vo, Long userId) {
        MsgPlatformConfig entity;
        if (vo.getId() != null && vo.getId() > 0) {
            entity = repository.findByIdAndDeleted(vo.getId(), 0)
                    .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "配置不存在"));
            // P1-1: IDOR 防护 - 校验所有权
            if (!entity.getOwnerId().equals(userId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权修改他人的配置");
            }
        } else {
            entity = new MsgPlatformConfig();
            entity.setOwnerId(vo.getOwnerId());
        }
        entity.setPlatform(vo.getPlatform().trim());
        entity.setAppId(vo.getAppId());
        entity.setCorpId(vo.getCorpId());
        entity.setSecret(vo.getSecret());
        entity.setCallbackToken(vo.getCallbackToken());
        entity.setCallbackEncodingAesKey(vo.getCallbackEncodingAesKey());
        entity.setAgentId(vo.getAgentId());
        if (vo.getStatus() != null) entity.setStatus(vo.getStatus());
        return repository.save(entity).getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, Long userId) {
        MsgPlatformConfig entity = repository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "配置不存在"));
        // P1-1: IDOR 防护 - 校验所有权
        if (!entity.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权删除他人的配置");
        }
        entity.setDeleted(1);
        repository.save(entity);
    }

    @Override
    public MsgPlatformConfigVO getByPlatformAndToken(String platform, String callbackToken) {
        MsgPlatformConfig e = getConfigEntityByPlatformAndToken(platform, callbackToken);
        return e != null ? toVO(e) : null;
    }

    @Override
    public MsgPlatformConfig getConfigEntityByPlatformAndToken(String platform, String callbackToken) {
        if (platform == null || callbackToken == null || callbackToken.isBlank()) return null;
        return repository.findByPlatformAndCallbackTokenAndDeleted(platform, callbackToken, 0).orElse(null);
    }

    private MsgPlatformConfigVO toVO(MsgPlatformConfig e) {
        MsgPlatformConfigVO vo = new MsgPlatformConfigVO();
        vo.setId(e.getId());
        vo.setOwnerId(e.getOwnerId());
        vo.setPlatform(e.getPlatform());
        vo.setAppId(e.getAppId());
        vo.setCorpId(e.getCorpId());
        vo.setAgentId(e.getAgentId());
        vo.setStatus(e.getStatus());
        vo.setCreateTime(e.getCreateTime());
        vo.setUpdateTime(e.getUpdateTime());
        return vo;
    }
}
