package com.hcmute.careergraph.dtos.request.movie;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MovieCreationNodeRequest {

    private String title;

    private List<String> genres;
}
