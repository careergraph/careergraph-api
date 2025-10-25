package com.hcmute.careergraph.persistence.dtos.request;

import lombok.Data;

public record AddressRequest() {

    @Data
    public static class AddressUpdate{
        private String province;
        private String district;
    }
}
