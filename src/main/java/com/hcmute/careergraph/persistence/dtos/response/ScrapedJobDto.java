package com.hcmute.careergraph.persistence.dtos.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ScrapedJobDto {
    private String url;
    @JsonProperty("chuc_danh") private String title;
    @JsonProperty("muc_luong") private String salaryRange;
    @JsonProperty("mo_ta") private List<String> responsibilities;
    @JsonProperty("yeu_cau") private List<String> qualifications;
    @JsonProperty("quyen_loi") private List<String> benefits;
    @JsonProperty("ky_nang") private List<String> requiredSkills;
    @JsonProperty("min_experience") private Integer minExperience;
    @JsonProperty("max_experience") private Integer maxExperience;
    @JsonProperty("cong_ty") private String companyName;
    @JsonProperty("quy_mo_cong_ty") private String companySize;
    @JsonProperty("dia_chi_cong_ty") private String companyAddress;
    @JsonProperty("thong_tin_chung") private Map<String, Object> generalInfo;
    @JsonProperty("khu_vuc_tuyen") private String city;
    @JsonProperty("han_nop_ho_so_epoch") private Long expiryDateEpoch;
}
