package cn.gaifan.douyinOperations.common.vo;

import lombok.Data;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.io.Serializable;

/**
 * 基础查询数据传输对象
 * 用于封装查询请求中的通用参数，包括分页、排序等基础查询条件
 *
 * @author gaifan
 */
@Data
public class BasicQueryDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 每页最大记录数限制
     */
    private static final int MAX_ROWS = 1000;

    /**
     * 默认构造函数
     * 使用默认值初始化分页和排序参数
     */
    public BasicQueryDto() {
        // 使用默认值，无需验证
    }

    /**
     * 带参数的构造函数
     *
     * @param page      当前页数，从0开始
     * @param rows      每页显示的记录数
     * @param sortName  排序字段名
     * @param sortOrder 排序方式，asc（升序）或 desc（降序）
     */
    public BasicQueryDto(Integer page, Integer rows, String sortName, String sortOrder) {
        this.page = page;
        this.rows = rows;
        this.sortName = sortName;
        this.sortOrder = sortOrder;
        // 构造函数中直接调用验证，确保对象创建时参数就被修正
        validateParams();
    }

    /**
     * 分页:当前页数
     * 默认值为 0，表示第一页
     */
    @Min(value = 0, message = "页码不能小于0")
    private Integer page = 0;

    /**
     * 分页:每页显示的记录数
     * 默认值为 30 条
     */
    @Min(value = 1, message = "每页记录数不能小于1")
    @Max(value = 1000, message = "每页记录数不能超过1000")
    private Integer rows = 30;

    /**
     * 排序字段名
     * 默认按照 id 字段排序
     * 只允许字母、数字和下划线，防止 SQL 注入
     */
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "排序字段名格式不正确，只能包含字母、数字和下划线")
    private String sortName = "id";

    /**
     * 排序方式
     * 可选值：asc（升序）、desc（降序）
     * 默认值为 desc（降序）
     */
    @Pattern(regexp = "^(?i)(asc|desc)$", message = "排序方式只能是asc或desc")
    private String sortOrder = "desc";

    /**
     * 是否查询随机数据
     * true：返回随机排序的数据
     * false 或 null：按照 sortName 和 sortOrder 指定的规则排序
     */
    private Boolean random;

    /**
     * 验证并修正参数
     * 确保所有参数在合理范围内
     * 此方法会在构造函数中自动调用，也可以在需要时手动调用
     */
    public void validateParams() {
        // 验证并修正页码
        if (page == null || page < 0) {
            page = 0;
        }

        // 验证并修正每页记录数
        if (rows == null || rows < 1) {
            rows = 30;
        } else if (rows > MAX_ROWS) {
            rows = MAX_ROWS;
        }

        // 验证并修正排序字段名（trim 处理空字符串，如果不符合格式，使用默认值）
        if (sortName == null || sortName.trim().isEmpty()) {
            sortName = "id";
        } else {
            String trimmedName = sortName.trim();
            if (trimmedName.matches("^[a-zA-Z0-9_]+$")) {
                sortName = trimmedName;
            } else {
                sortName = "id";
            }
        }

        // 验证并修正排序方式（trim 并统一转换为小写）
        if (sortOrder == null || sortOrder.trim().isEmpty()) {
            sortOrder = "desc";
        } else {
            String trimmedOrder = sortOrder.trim().toLowerCase();
            if ("asc".equals(trimmedOrder) || "desc".equals(trimmedOrder)) {
                sortOrder = trimmedOrder;
            } else {
                sortOrder = "desc";
            }
        }
    }

    /**
     * 获取数据库查询的偏移量（offset）
     * 用于分页查询，计算需要跳过的记录数
     *
     * @return 偏移量，即需要跳过的记录数
     */
    public Integer getOffset() {
        if (page == null || rows == null) {
            return 0;
        }
        return page * rows;
    }

    /**
     * 判断是否需要随机排序
     *
     * @return true 如果需要随机排序，false 否则
     */
    public boolean isRandom() {
        return random != null && random;
    }

}
