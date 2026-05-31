package cn.gaifan.douyinOperations.contract.role;

/**
 * 内部运营 6 角色定义
 *
 * vision: SaaS 多租户 → 内部运营工具，6 种运营角色
 */
public enum OperationRole {

    ADMIN("admin", "管理员", "系统配置、人员管理、数据总览"),
    OPERATIONS("operations", "运营人员", "场次策划、选品审核、话术审核、数据复盘"),
    ANCHOR("anchor", "主播", "直播执行、人设对话、话术规划、短视频创作"),
    PHOTOGRAPHER("photographer", "摄影师", "短视频拍摄、素材制作"),
    DIRECTOR("director", "中控人员", "直播中控台、实时数据、话术提词"),
    SELECTOR("selector", "选品人员", "商品筛选、竞品分析、选品提报");

    final String code;
    final String displayName;
    final String responsibility;

    OperationRole(String code, String displayName, String responsibility) {
        this.code = code;
        this.displayName = displayName;
        this.responsibility = responsibility;
    }

    public String code() { return code; }
    public String displayName() { return displayName; }
    public String responsibility() { return responsibility; }

    public static OperationRole fromCode(String code) {
        for (OperationRole r : values()) {
            if (r.code.equals(code)) return r;
        }
        return ADMIN;
    }
}
