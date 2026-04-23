package cn.gaifan.douyinOperations.module.abtest.vo;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
public class AbExperimentSaveVO {
    private Long id;
    @NotNull(message = "用户 ID 不能为空")
    private Long ownerId;
    @NotBlank(message = "实验名称不能为空")
    private String name;
    private String description;
    @NotBlank(message = "实验类型不能为空")
    private String experimentType;
    private Integer status;
    private String conclusion;
    /** script_style 实验时：目标实体类型 product / live_session */
    private String targetEntityType;
    /** script_style 实验时：目标实体 ID（产品 ID 或直播场次 ID） */
    private Long targetEntityId;
    private List<AbVariantSaveVO> variants;
}
