package com.hcmute.careergraph.services.impl;

import com.hcmute.careergraph.dtos.request.movie.MovieUpdateNodeRequest;
import com.hcmute.careergraph.dtos.response.movie.MovieCreationNodeResponse;
import com.hcmute.careergraph.dtos.response.movie.MovieNodeResponse;
import com.hcmute.careergraph.entities.graph.Movie;
import com.hcmute.careergraph.enums.EErrorCode;
import com.hcmute.careergraph.exception.AppException;
import com.hcmute.careergraph.helper.PageResponse;
import com.hcmute.careergraph.repository.graph.GenreNodeRepository;
import com.hcmute.careergraph.repository.graph.MovieNodeRepository;
import com.hcmute.careergraph.services.IRedisService;
import com.hcmute.careergraph.utils.PageUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Slf4j
public class MovieService {

    @Autowired
    private MovieNodeRepository movieNodeRepository;

    @Autowired
    private GenreNodeRepository genreNodeRepository;

    @Autowired
    IRedisService redisService;

    // Create
    public MovieCreationNodeResponse saveMovieNode(String label, List<String> genres) {

        if (movieNodeRepository.existsByTitle(label)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Resource existed");
        }

        Movie newMovie = movieNodeRepository.save(Movie.builder()
                .title(label)
                .totalView(0)
                .totalStart(0)
                .totalRating(0)
                .build());

        // Set relationship with genre
        List<Long> genreIds = genres.stream()
                .map(genre -> genreNodeRepository.findIdByName(genre))
                .toList();
        genreIds.forEach(genreId -> {
            genreNodeRepository.linkGenreToMovie(genreId, newMovie.getId());
        });

        return MovieCreationNodeResponse.builder()
                .id(newMovie.getId())
                .title(newMovie.getTitle())
                .build();
    }

    // Update
    public MovieNodeResponse updateMovieNode(MovieUpdateNodeRequest request) {

        if (!movieNodeRepository.existsByTitle(request.getTitle())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not existed");
        }

        // Check in MySQL
        // ...

        Movie movie = movieNodeRepository.updateMovieNode(request.getTitle(),
                        request.getTotalStart(), request.getTotalView(), request.getTotalRating())
                .orElseThrow(() -> new AppException(EErrorCode.UNSAVED_DATA));

        redisService.deleteObject("*movies*");

        return MovieNodeResponse.builder()
                .id(movie.getId())
                .title(movie.getTitle())
                .totalView(movie.getTotalView())
                .totalStart(movie.getTotalStart())
                .averageRating(movie.getAverageRating())
                .isTrending(movie.getIsTrending())
                .build();
    }

    // Delete
    public boolean deleteMovieNode(String title) {

        if (!movieNodeRepository.existsByTitle(title)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not existed");
        }

        movieNodeRepository.deleteMovieNode(title);

        // Delete mysql
        // boolean isDeleted = deleteMovie(title);

        // Clear cache
        redisService.deleteObject("*movies*");

        return true;
    }

    // Find all movie
    public Page<Movie> findAllMovieNodes(int pageNo, int pageSize) {

        Pageable pageable = PageRequest.of(pageNo, pageSize);

        Page<Movie> movies = movieNodeRepository.findAll(pageable);

        return movies;
    }

    public PageResponse<MovieNodeResponse> findMoviesByFollowOfFriendAndLiked(String username,
                                                                              int pageNo,
                                                                              int pageSize) {

        String cacheKey = "movies_friend_follow:" + pageNo + ":" + pageSize;

        PageResponse<MovieNodeResponse> cachedResult = redisService.getObject(cacheKey, PageResponse.class);
        if (cachedResult != null && cachedResult.getSize() != 0) {
            log.info("Cache hit for page: {} and size: {}", pageNo, pageSize);
            return cachedResult;
        }

        Pageable pageable = PageRequest.of(pageNo, pageSize);

        Page<Movie> movies =
                movieNodeRepository.findMoviesByFollowOfFriendAndLiked(username, pageable);

        return getMovieNodeResponsePageResponse(cacheKey, movies);
    }

    // Learn batch check to optimize performance
    public PageResponse<MovieNodeResponse> findRelatedMoviesByTitlePattern(String label,
                                                                           int pageNo,
                                                                           int pageSize)
            throws ChangeSetPersister.NotFoundException {

        String cacheKey = "movies_related:" + label + ":" + pageNo + ":" + pageSize;

        PageResponse<MovieNodeResponse> cachedResult = redisService.getObject(cacheKey, PageResponse.class);
        if (cachedResult != null && cachedResult.getSize() != 0) {
            return cachedResult;
        }

        Pageable pageable = PageRequest.of(pageNo, pageSize);
        Page<Movie> moviesRelated = movieNodeRepository.findRelatedMoviesByTitlePattern(label, pageable);

        if (moviesRelated == null) {
            throw new ChangeSetPersister.NotFoundException();
        }

        // Filter
        // moviesRelated.stream().filter(movie -> checkMovieExists(movie.getTitle()));

        return getMovieNodeResponsePageResponse(cacheKey, moviesRelated);

    }

    private PageResponse<MovieNodeResponse> getMovieNodeResponsePageResponse(String cacheKey,
                                                                             Page<Movie> moviesRelated) {

        Page<MovieNodeResponse> result = moviesRelated.map(movie -> MovieNodeResponse.builder()
                        .id(movie.getId())
                        .title(movie.getTitle())
                        .averageRating(movie.getAverageRating())
                        .isTrending(movie.getIsTrending())
                        .totalStart(movie.getTotalStart())
                        .totalView(movie.getTotalView())
                        .build());

        PageResponse<MovieNodeResponse> pageResponse = PageUtil.convertToMoviePageResponse(result);
        redisService.setObject(cacheKey, pageResponse, 600);

        return pageResponse;
    }

    // ================================== Call API ==================================

    /** Web client
    private Boolean checkMovieExists(String movieTitle) {
        try {
            ApiResponse<Boolean> response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/management-service/api/v1/movies/check-existed")
                            .queryParam("title", movieTitle)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiResponse<Boolean>>() {})
                    .block();

            return response != null && response.getData() != null && response.getData();
        } catch (Exception e) {
            log.error("Error checking movie existence for title: {}", movieTitle, e);
            return false;
        }
    }

    private Boolean deleteMovie(String movieTitle) {
        try {
            ApiResponse<Boolean> response = webClient.delete()
                    .uri(uriBuilder -> uriBuilder
                            .path("/management-service/api/v1/movies/delete")
                            .queryParam("title", movieTitle)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiResponse<Boolean>>() {})
                    .block();

            return response != null && response.getData() != null && response.getData();
        } catch (Exception e) {
            log.error("Error delete movie for title: {}", movieTitle, e);
            return false;
        }
    }
     */
}
