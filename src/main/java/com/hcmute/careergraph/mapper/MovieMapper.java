package com.hcmute.careergraph.mapper;

import com.hcmute.careergraph.dtos.request.movie.MovieCreationRequest;
import com.hcmute.careergraph.dtos.response.movie.MovieCreationResponse;
import com.hcmute.careergraph.entities.mysql.Movie;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface MovieMapper {

    @Mapping(target = "slug", ignore = true)
    Movie toMovie(MovieCreationRequest request);

    @Mapping(target = "genreName", ignore = true)
    MovieCreationResponse toMovieResponse(Movie movie);
}
