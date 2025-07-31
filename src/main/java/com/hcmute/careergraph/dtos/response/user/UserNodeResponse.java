package com.hcmute.careergraph.dtos.response.user;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserNodeResponse {

    private Long id;

    private String username;
}
