package com.hcmute.careergraph.dtos.response.user;

import com.hcmute.careergraph.dtos.response.address.AddressResponse;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserResponse {

    private String fullname;

    private String email;

    private String username;

    private String phone;

    private int age;

    private AddressResponse address;
}
