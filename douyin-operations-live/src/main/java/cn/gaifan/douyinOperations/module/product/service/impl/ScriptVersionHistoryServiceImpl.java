package cn.gaifan.douyinOperations.module.product.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.product.entity.DyProduct;
import cn.gaifan.douyinOperations.module.product.entity.DyProductScript;
import cn.gaifan.douyinOperations.module.product.entity.ScriptVersionHistory;
import cn.gaifan.douyinOperations.module.product.repository.DyProductRepository;
import cn.gaifan.douyinOperations.module.product.repository.DyProductScriptRepository;
import cn.gaifan.douyinOperations.module.product.repository.ScriptVersionHistoryRepository;
import cn.gaifan.douyinOperations.module.product.service.ScriptVersionHistoryService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ScriptVersionHistoryServiceImpl implements ScriptVersionHistoryService {

    @Resource
    private ScriptVersionHistoryRepository historyRepository;

    @Resource
    private DyProductScriptRepository scriptRepository;

    @Resource
    private DyProductRepository productRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveHistory(DyProductScript script, Long userId) {
        if (script == null || script.getScriptContent() == null) return;
        ScriptVersionHistory h = new ScriptVersionHistory();
        h.setScriptId(script.getId());
        h.setProductId(script.getProductId());
        h.setScriptType(script.getScriptType());
        h.setStyle(script.getStyle());
        h.setVersion(script.getVersion() != null ? script.getVersion() : 1);
        h.setScriptContent(script.getScriptContent());
        h.setPersonaId(script.getPersonaId());
        h.setDuration(script.getDuration());
        h.setCreatedBy(userId);
        historyRepository.save(h);
    }

    @Override
    public List<ScriptVersionHistory> listByScriptId(Long scriptId) {
        if (scriptId == null || scriptId <= 0) return List.of();
        return historyRepository.findByScriptIdOrderByCreateTimeDesc(scriptId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DyProductScript rollbackToVersion(Long historyId, Long userId) {
        ScriptVersionHistory h = historyRepository.findById(historyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "版本历史不存在"));
        DyProductScript script = scriptRepository.findById(h.getScriptId())
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "话术不存在"));
        DyProduct product = productRepository.findById(script.getProductId())
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "产品不存在"));
        if (!product.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权限操作此产品");
        }
        script.setScriptContent(h.getScriptContent());
        script.setPersonaId(h.getPersonaId());
        script.setDuration(h.getDuration());
        return scriptRepository.save(script);
    }
}
