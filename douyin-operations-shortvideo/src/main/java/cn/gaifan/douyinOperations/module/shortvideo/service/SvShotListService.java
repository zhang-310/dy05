package cn.gaifan.douyinOperations.module.shortvideo.service;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvShotListVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvShotSaveVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvShotVO;

import java.util.List;

/**
 * 分镜列表 Service
 */
public interface SvShotListService {

    SvShotListVO get(Long id, Long ownerId);

    /** 分镜列表分页（按创建时间倒序） */
    PageResultVO<SvShotListVO> list(Long ownerId, Integer page, Integer rows);

    /** 根据脚本 ID 获取最新分镜列表 */
    SvShotListVO getLatestByScriptId(Long scriptId, Long ownerId);

    Long save(SvShotSaveVO vo, Long ownerId);

    /** AI 生成分镜，返回分镜列表 */
    List<SvShotVO> generate(Long scriptId, String scriptContent, Integer shotCount, String style, Long ownerId);

    /** AI 生成分镜，返回分镜列表及新建的 shotListId（当 scriptId 不为空时） */
    GenerateResult generateWithResult(Long scriptId, String scriptContent, Integer shotCount, String style, Long ownerId);

    record GenerateResult(List<SvShotVO> shots, Long shotListId) {}
}
