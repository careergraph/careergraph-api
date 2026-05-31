package com.hcmute.careergraph.persistence.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hcmute.careergraph.enums.candidate.AddressType;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.Builder;
import lombok.experimental.SuperBuilder;
import com.hcmute.careergraph.enums.common.ConstDefault;

@Entity
@Table(name = "addresses")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@ToString(callSuper = true, exclude = {"party"})
@EqualsAndHashCode(callSuper = true, exclude = {"party"})
@JsonIgnoreProperties(ignoreUnknown = true)
public class Address extends BaseEntity {

    @Column(name = "name")
    @Builder.Default
    private String name = ConstDefault.EMPTY_STRING.getValue();

    @Column(name = "country")
    @Builder.Default
    private String country = ConstDefault.EMPTY_STRING.getValue();

    @Column(name = "province")
    @Builder.Default
    private String province = ConstDefault.EMPTY_STRING.getValue();

    @Column(name = "district")
    @Builder.Default
    private String district = ConstDefault.EMPTY_STRING.getValue();

    @Column(name = "ward")
    @Builder.Default
    private String ward = ConstDefault.EMPTY_STRING.getValue();

    @Column(name = "is_primary")
    private Boolean isPrimary;

    @Column(name = "address_type")
    @Enumerated(EnumType.STRING)
    private AddressType  addressType;

    // Quan hệ với Party
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_id", nullable = false)
    private Party party;
}