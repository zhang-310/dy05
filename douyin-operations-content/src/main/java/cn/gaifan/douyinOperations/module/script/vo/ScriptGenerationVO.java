package cn.gaifan.douyinOperations.module.script.vo;

import java.util.List;

public class ScriptGenerationVO {
    private Long id;
    private List<ScriptVariantVO> variants;
    private Long generationTime;

    public ScriptGenerationVO() {}

    public ScriptGenerationVO(Long id, List<ScriptVariantVO> variants, Long generationTime) {
        this.id = id;
        this.variants = variants;
        this.generationTime = generationTime;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public List<ScriptVariantVO> getVariants() { return variants; }
    public void setVariants(List<ScriptVariantVO> variants) { this.variants = variants; }

    public Long getGenerationTime() { return generationTime; }
    public void setGenerationTime(Long generationTime) { this.generationTime = generationTime; }
}
