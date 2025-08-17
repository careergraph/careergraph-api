package com.hcmute.careergraph.entities.base;

import com.hcmute.careergraph.constant.Constant;
import lombok.Data;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.Transient;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;

import java.time.Instant;

@Data
@SuperBuilder
public abstract class BaseGraph implements UUIDEntity {

    @Id
    @GeneratedValue
    private String id;

    private String uuid;
    private Long createDate;

    @CreatedBy
    private String createdBy;

    @Override
    public String getUUID() {
        if (uuid == null) {
            return this.id;
        }
        return this.uuid;
    }

    @Override
    public void setUUID(String uuid) {
        this.id = uuid;
    }

    @Transient
    private void init() {
        Instant timestamp = Instant.now();
        timestamp.atZone(Constant.TIMEZONE);
        createDate = timestamp.getEpochSecond();

        if (this.uuid == null) {
            this.uuid = generateUUID();
        }
    }
}
