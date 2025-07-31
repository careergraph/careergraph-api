package com.hcmute.careergraph.dtos.request.movie;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MovieCreationRequest {

    private String title;

    private String description;

    private int releaseYear;

    private LocalDate releaseDate;

    private int duration;

    private String language;

    private String country;

    private List<String> genreName;
}
