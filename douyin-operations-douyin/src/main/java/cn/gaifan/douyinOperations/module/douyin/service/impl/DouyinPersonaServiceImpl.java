package cn.gaifan.douyinOperations.module.douyin.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.douyin.entity.DyPersona;
import cn.gaifan.douyinOperations.module.douyin.repository.DyPersonaRepository;
import cn.gaifan.douyinOperations.module.douyin.service.DouyinPersonaService;
import cn.gaifan.douyinOperations.module.douyin.vo.PersonaSaveVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DouyinPersonaServiceImpl implements DouyinPersonaService {

    @Resource
    private DyPersonaRepository personaRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long savePersona(PersonaSaveVO vo, Long userId) {
        DyPersona persona;

        if (vo.getId() != null) {
            // 更新
            persona = personaRepository.findById(vo.getId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "人设不存在"));
            if (!persona.getOwnerId().equals(userId)) {
                throw new BusinessException(ErrorCode.PERMISSION_DENIED, "无权限修改此人设");
            }
        } else {
            // 新增
            persona = new DyPersona();
            persona.setOwnerId(userId);
        }

        persona.setAccountId(vo.getAccountId());
        persona.setPersonaName(vo.getPersonaName());
        persona.setPersonaType(vo.getPersonaType());
        persona.setDescription(vo.getDescription());
        persona.setTone(vo.getTone());
        persona.setTargetAudience(vo.getTargetAudience());
        persona.setContentStyle(vo.getContentStyle());
        persona.setKeywords(vo.getKeywords());

        // 如果设置为默认，先取消其他默认人设
        if (vo.getIsDefault() != null && vo.getIsDefault() == 1) {
            personaRepository.clearDefaultByOwnerId(userId);
            persona.setIsDefault(1);
        }

        return personaRepository.save(persona).getId();
    }

    @Override
    public List<DyPersona> listPersonas(Long userId, String personaType) {
        if (personaType != null && !personaType.isEmpty()) {
            return personaRepository.findByOwnerIdAndPersonaTypeAndDeleted(userId, personaType, 0);
        }
        return personaRepository.findByOwnerIdAndDeleted(userId, 0);
    }

    @Override
    public DyPersona getPersona(Long id, Long userId) {
        DyPersona persona = personaRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "人设不存在"));
        if (!persona.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED, "无权限访问此人设");
        }
        return persona;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePersona(Long id, Long userId) {
        DyPersona persona = personaRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "人设不存在"));
        if (!persona.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED, "无权限删除此人设");
        }
        persona.setDeleted(1);
        personaRepository.save(persona);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefaultPersona(Long id, Long userId) {
        DyPersona persona = personaRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "人设不存在"));
        if (!persona.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED, "无权限设置此人设");
        }

        // 先取消其他默认人设
        personaRepository.clearDefaultByOwnerId(userId);

        // 设置当前为默认
        persona.setIsDefault(1);
        personaRepository.save(persona);
    }

    @Override
    public DyPersona getDefaultPersona(Long userId) {
        return personaRepository.findByOwnerIdAndIsDefaultAndDeleted(userId, 1, 0)
                .orElse(null);
    }

    @Override
    public List<DyPersona> getSystemTemplates() {
        // 系统模板使用 ownerId = 0
        return personaRepository.findByOwnerIdAndDeleted(0L, 0);
    }

    @Override
    public DyPersona getPersonaByAccountId(Long accountId, Long userId) {
        return personaRepository.findByAccountIdAndOwnerIdAndDeleted(accountId, userId, 0)
                .orElse(null);
    }
}
