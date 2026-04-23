package cn.gaifan.douyinOperations.module.script.service;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.script.vo.*;

import java.util.List;

/**
 * 用户自定义违规词服务
 */
public interface UserViolationWordService {

    PageResultVO<UserViolationWordVO> search(UserViolationWordSearchVO vo);

    UserViolationWordVO getById(Long id);

    long save(UserViolationWordSaveVO vo);

    void delete(Long id);

    List<UserViolationWordVO> listActiveByUserId(Long userId);
}
