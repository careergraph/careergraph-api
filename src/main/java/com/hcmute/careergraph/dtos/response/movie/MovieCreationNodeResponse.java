package com.hcmute.careergraph.dtos.response.movie;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MovieCreationNodeResponse {

    private Long id;

    private String title;

    private List<String> genres;
}
