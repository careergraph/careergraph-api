package com.hcmute.careergraph.persistence.dtos.request;

import com.hcmute.careergraph.enums.application.ApplicationStage;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ApplicationStageUpdateRequest {

        @NotNull(message = "Stage is not null")
        private ApplicationStage stage;

        private String note;

        private String changeBy;
}
