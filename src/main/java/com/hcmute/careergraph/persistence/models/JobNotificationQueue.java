package com.hcmute.careergraph.persistence.models;

import com.hcmute.careergraph.enums.job.SendType;
import com.hcmute.careergraph.enums.job.StatusSend;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Table(
        uniqueConstraints = @UniqueConstraint(columnNames = {"userId", "jobId"})
)
public class JobNotificationQueue extends BaseEntity{


    private String userId;
    private String jobId;

    @Enumerated(EnumType.STRING)
    private SendType sendType;

    @Enumerated(EnumType.STRING)
    private StatusSend statusSend;





}
