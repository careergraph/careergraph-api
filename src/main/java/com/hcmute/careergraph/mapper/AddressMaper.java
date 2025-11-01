package com.hcmute.careergraph.mapper;

import com.hcmute.careergraph.persistence.dtos.response.AddressResponse;
import com.hcmute.careergraph.persistence.models.Address;
import com.hcmute.careergraph.persistence.models.BaseEntity;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class AddressMaper {
    public AddressResponse toResponse(Address address) {
        if(address == null) {
            return AddressResponse.builder().build();
        }
        return AddressResponse.builder()
                .addressId(address.getId())
                .name(address.getName())
                .ward(address.getWard())
                .province(address.getProvince())
                .district(address.getDistrict())
                .country(address.getCountry())
                .isPrimary(address.getIsPrimary())
                .addressType(address.getAddressType())
                .build();

    }

    public Set<AddressResponse> toResponses(Set<Address> addresses) {
        if(addresses == null) { return new HashSet<>(); }
        return addresses.stream()
                .filter(BaseEntity::getActive)
                .map(this::toResponse)
                .collect(Collectors.toSet());

    }
}
