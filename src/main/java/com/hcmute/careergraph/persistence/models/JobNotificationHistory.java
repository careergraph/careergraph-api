package com.hcmute.careergraph.persistence.models;

import com.hcmute.careergraph.enums.job.SendType;
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
        name="job_notification_history",
        uniqueConstraints = @UniqueConstraint(columnNames = {"userId", "jobId"})
)
public class JobNotificationHistory extends BaseEntity{


    private String userId;
    private String jobId;

    private LocalDateTime sentAt;

    @Enumerated(EnumType.STRING)
    private SendType sendType;
}
