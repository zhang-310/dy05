package cn.gaifan.douyinOperations.module.auth.vo;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.Serializable;

/**
 * 资源新增/编辑 VO
 */
@Data
public class AuthResourceSaveVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    @NotBlank(message = "资源类型不能为空")
    @Size(max = 16)
    private String resourceType;
    @NotBlank(message = "资源编码不能为空")
    @Size(max = 256)
    private String resourceCode;
    @Size(max = 16)
    private String requestMethod;
    @Size(max = 64)
    private String module;
    @Size(max = 128)
    private String resourceName;
    private Long parentId = 0L;
    private Integer sortOrder = 0;
}
