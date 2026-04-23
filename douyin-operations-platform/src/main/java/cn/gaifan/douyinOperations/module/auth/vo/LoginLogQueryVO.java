package cn.gaifan.douyinOperations.module.auth.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 登录记录查询入参
 * 管理员：userId 为空时查全部，有值时查指定用户
 * 普通用户：仅能查自己的（后端强制用当前用户ID）
 */
@Data
public class LoginLogQueryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户ID，管理员可选；普通用户忽略此字段 */
    private Long userId;

    private int page = 0;
    private int size = 20;
}
