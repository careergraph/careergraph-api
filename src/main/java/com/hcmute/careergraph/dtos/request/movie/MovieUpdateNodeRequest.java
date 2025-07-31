package com.hcmute.careergraph.dtos.request.movie;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MovieUpdateNodeRequest {

    private String title;

    private int totalView;

    private int totalRating;

    private int totalStart;
}
