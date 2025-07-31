package com.hcmute.careergraph.dtos.response.address;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AddressResponse {

    private String country;

    private String city;

    private String district;

    private String special;
}
