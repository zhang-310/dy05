package cn.gaifan.douyinOperations.common.vo;

import jakarta.validation.constraints.NotNull;

/**
 * 通用 ID 请求 — 替代 Map<String, Object> body
 */
public class IdRequest {
    @NotNull
    private Long id;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
}
