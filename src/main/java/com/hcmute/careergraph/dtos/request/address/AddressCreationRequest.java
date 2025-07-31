package com.hcmute.careergraph.dtos.request.address;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AddressCreationRequest {

    private String country;

    private String city;

    private String district;

    private String special;
}
