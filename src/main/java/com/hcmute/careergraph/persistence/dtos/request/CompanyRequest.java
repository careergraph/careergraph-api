package com.hcmute.careergraph.persistence.dtos.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CompanyRequest {

    @NotBlank(message = "Tagname is required")
    private String tagname;

    private String avatar;

    private String cover;

    private String size;

    private String website;

    private String ceoName;

    private Integer noOfMembers;

    private Integer yearFounded;
}
