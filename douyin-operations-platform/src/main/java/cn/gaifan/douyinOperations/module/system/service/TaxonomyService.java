package cn.gaifan.douyinOperations.module.system.service;

import java.util.List;
import java.util.Map;

/**
 * 全站 taxonomy（A-2）：平台预置 + 可扩展租户节点。
 */
public interface TaxonomyService {

    /** 登录用户可读；合并 ownerId=0 与当前用户 ownerId（若有私有节点） */
    List<Map<String, Object>> list(Long userOwnerId, String moduleScope, Long parentId);

    /** 仅 admin；保存平台节点时 ownerId 固定 0 */
    Long save(Long operatorUserId, boolean admin, Map<String, Object> body);

    /** 仅 admin；逻辑删除 */
    void delete(Long operatorUserId, boolean admin, Long id);

    /** 将 taxonomy code 解析为可与 {@code sv_material.tags} 做 OR 匹配的短语（code + name） */
    List<String> resolveMatchTokens(Long userOwnerId, String moduleScope, List<String> taxonomyCodes);
}
