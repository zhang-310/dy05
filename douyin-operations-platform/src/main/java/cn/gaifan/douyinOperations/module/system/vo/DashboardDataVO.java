package cn.gaifan.douyinOperations.module.system.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 仪表板数据 VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardDataVO {

    private String title;               // 标题

    private Object data;                // 数据内容

    private String type;                // 数据类型（overview/chart/table/status）

    private Long timestamp;             // 时间戳
}
