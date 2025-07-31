package com.hcmute.careergraph.dtos.response.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LogoutResponse {

    private String token;

    private Date expiryDate;
}
