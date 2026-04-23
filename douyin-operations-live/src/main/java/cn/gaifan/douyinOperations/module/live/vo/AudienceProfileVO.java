package cn.gaifan.douyinOperations.module.live.vo;

import lombok.Data;
import java.util.List;

@Data
public class AudienceProfileVO {
    private String ageRange;          // "18-25", "25-35", "35-50"
    private String gender;            // "female", "male", "mixed"
    private String city;              // "一二线", "三四线", "全线"
    private String purchasePower;     // "high", "medium", "low"
    private List<String> interests;   // ["护肤", "彩妆", "时尚"]
}
