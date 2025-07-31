package com.hcmute.careergraph.mapper;


import com.hcmute.careergraph.dtos.request.address.AddressCreationRequest;
import com.hcmute.careergraph.dtos.response.address.AddressResponse;
import com.hcmute.careergraph.entities.mysql.Address;
import org.mapstruct.Mapper;

@Mapper
public interface AddressMapper {

    Address toAddress(AddressCreationRequest request);

    AddressResponse toAddressResponse(Address address);
}
