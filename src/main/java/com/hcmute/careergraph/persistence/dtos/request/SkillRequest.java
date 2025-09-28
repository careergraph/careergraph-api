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
public class SkillRequest {

    @NotBlank(message = "Name is required")
    private String name;

    private String category;

    private String description;
}
