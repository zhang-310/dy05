package cn.gaifan.douyinOperations.module.dashboard.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * Dashboard 业务区：当前进行中场次摘要（用于在播横幅）
 */
@Data
public class BusinessOngoingSessionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String liveTitle;
    private Integer status;
}
