package com.hcmute.careergraph.services.impl;

import com.hcmute.careergraph.dtos.response.genre.GenreCreationNodeResponse;
import com.hcmute.careergraph.dtos.response.genre.GenreNodeResponse;
import com.hcmute.careergraph.entities.graph.Genre;
import com.hcmute.careergraph.entities.graph.Movie;
import com.hcmute.careergraph.repository.graph.GenreNodeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class GenreService {

    @Autowired
    private GenreNodeRepository genreNodeRepository;

    public GenreCreationNodeResponse saveGenreNode(String label) {

        // Check existed label
        // ...

        Genre genre = Genre.builder()
                .name(label)
                .isPopular(false)
                .movies(new ArrayList<>())
                .build();

        Genre newGenre = genreNodeRepository.save(genre);
        return GenreCreationNodeResponse.builder()
                .id(newGenre.getId())
                .name(newGenre.getName())
                .build();
    }

    public List<GenreNodeResponse> findAllGenreNodes() {

        List<Genre> genres = genreNodeRepository.findAll();
        return genres.stream().map(genre -> {

            List<String> movies = genre.getMovies().stream()
                    .map(Movie::getTitle)
                    .toList();

            return GenreNodeResponse.builder()
                    .id(genre.getId())
                    .name(genre.getName())
                    .movies(movies)
                    .build();
        }).toList();
    }

    public void linkMovieToGenre(Long movieId, Long genreId) {
        genreNodeRepository.linkGenreToMovie(genreId, movieId);
    }
}
