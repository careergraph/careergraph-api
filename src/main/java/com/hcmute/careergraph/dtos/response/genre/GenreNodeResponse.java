package com.hcmute.careergraph.dtos.response.genre;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GenreNodeResponse {

    private Long id;

    private String name;

    private List<String> movies;
}
