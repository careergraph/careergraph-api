package com.hcmute.careergraph.persistence.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AddressDto {

    private String addressId;

    private String name;

    private String country;

    private String province;

    private String district;

    private String ward;

    private Boolean isPrimary;

    private String partyId;
}
