package com.hcmute.careergraph.dtos.request.user;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserCreationRequest {

    private String fullname;


    private String username;

    private String password;

    private LocalDate dob;

    private String email;

    private String phone;

    private String country;

    private String city;

    private String district;

    private String special;
}
