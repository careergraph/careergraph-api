package com.hcmute.careergraph.dtos.response.movie;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MovieNodeResponse {

    private Long id;

    private String title;

    private int totalView;

    private int totalStart;

    private double averageRating;

    private boolean isTrending;
}
