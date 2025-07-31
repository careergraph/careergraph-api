package com.hcmute.careergraph.controller;

import com.hcmute.careergraph.dtos.request.movie.MovieCreationNodeRequest;
import com.hcmute.careergraph.dtos.request.movie.MovieUpdateNodeRequest;
import com.hcmute.careergraph.dtos.response.movie.MovieCreationNodeResponse;
import com.hcmute.careergraph.dtos.response.movie.MovieNodeResponse;
import com.hcmute.careergraph.entities.graph.Movie;
import com.hcmute.careergraph.helper.ApiResponse;
import com.hcmute.careergraph.helper.PageResponse;
import com.hcmute.careergraph.services.impl.MovieService;
import com.hcmute.careergraph.utils.MathUtil;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/movies")
@CrossOrigin(origins = "*")
public class MovieController {

    @Autowired
    private MovieService movieService;

    @PostMapping("/create")
    public ApiResponse<MovieCreationNodeResponse> createMovieNode(@RequestBody MovieCreationNodeRequest request)
            throws BadRequestException {

        if (request.getTitle() == null || request.getTitle().isEmpty()
            || request.getGenres() == null || request.getGenres().isEmpty()) {
            throw new BadRequestException("Request invalid");
        }

        MovieCreationNodeResponse result = movieService.saveMovieNode(request.getTitle(), request.getGenres());
        return ApiResponse.<MovieCreationNodeResponse>builder()
                .code(200)
                .message("Completed API create node movie")
                .data(result)
                .build();
    }

    @GetMapping("/related")
    public ApiResponse<PageResponse<MovieNodeResponse>> findRelatedMoviesByTitlePattern(@RequestParam("title") String title,
                                                                                        @RequestParam("page") String page,
                                                                                        @RequestParam("size") String size)
            throws BadRequestException, ChangeSetPersister.NotFoundException {

        if (title == null || title.isEmpty()
            || page == null || page.isEmpty()
            || size == null || size.isEmpty()) {
            throw new BadRequestException("Request invalid");
        }

        int pageNo = Integer.parseInt(page);
        int pageSize = Integer.parseInt(size);

        int pageAdjust = (pageNo < 0) ? 0 : pageNo;

        PageResponse<MovieNodeResponse> result =
                movieService.findRelatedMoviesByTitlePattern(title, pageAdjust, pageSize);

        return ApiResponse.<PageResponse<MovieNodeResponse>>builder()
                .code(200)
                .message("Completed API find related movies by title pattern")
                .data(result)
                .build();
    }

    @GetMapping("/all")
    ApiResponse<Page<Movie>> findAllMoviesNode(@RequestParam("page") String page,
                                               @RequestParam("size") String size)
            throws BadRequestException {

        if (page.isEmpty() || size.isEmpty()
        || page == null || size == null) {

            throw new BadRequestException("Request invalid");
        }

        int pageNo = Integer.parseInt(page);
        int pageSize = Integer.parseInt(size);
        int adjustedPageNo = (pageNo > 0) ? pageNo - 1 : 0;

        return ApiResponse.<Page<Movie>>builder()
                .code(200)
                .message("Completed API find all movies node")
                .data(movieService.findAllMovieNodes(adjustedPageNo, pageSize))
                .build();
    }

    @GetMapping("/friend-followed")
    ApiResponse<PageResponse<MovieNodeResponse>> findMoviesByFollowOfFriendAndLiked(@RequestParam("username") String username,
                                                                 @RequestParam("page") String page,
                                                                 @RequestParam("size") String size) throws BadRequestException {
        if (page == null || page.isEmpty()
        || size == null || size.isEmpty()
        || username == null || username.isEmpty()) {
            throw new BadRequestException("Request invalid");
        }

        if (!MathUtil.isInteger(page) || !MathUtil.isInteger(size)) {
            throw new BadRequestException("Page or size invalid");
        }

        int pageNo = Integer.parseInt(page);
        int pageSize = Integer.parseInt(size);

        int adjustedPageNo = (pageNo > 0) ? pageNo - 1 : 0;

        PageResponse<MovieNodeResponse> result =
                movieService.findMoviesByFollowOfFriendAndLiked(username, adjustedPageNo, pageSize);

        return ApiResponse.<PageResponse<MovieNodeResponse>>builder()
                .code(200)
                .message("Completed API find friend movie nodes")
                .data(result)
                .build();
    }

    @DeleteMapping("/delete")
    public ApiResponse<Void> deleteMovieNode(@RequestParam("title") String title)
            throws BadRequestException {
        if (title == null || title.isEmpty()) {
            throw new BadRequestException("Request invalid");
        }

        boolean isDeleted = movieService.deleteMovieNode(title);
        return ApiResponse.<Void>builder()
                .code(200)
                .message("Completed API delete movie node with result is " +
                        (isDeleted ? "success" : "fail"))
                .build();
    }

    @PostMapping("/update")
    public ApiResponse<MovieNodeResponse> updateMovieNode(@RequestBody MovieUpdateNodeRequest request)
            throws BadRequestException {
        if (request == null) {
            throw new BadRequestException("Request invalid");
        }

        MovieNodeResponse updatedMovie = movieService.updateMovieNode(request);
        return ApiResponse.<MovieNodeResponse>builder()
                .code(200)
                .message("Completed API update movie node")
                .data(updatedMovie)
                .build();
    }
}
