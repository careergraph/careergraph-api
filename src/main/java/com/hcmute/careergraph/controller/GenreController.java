package com.hcmute.careergraph.controller;

import com.hcmute.careergraph.dtos.request.genre.GenreCreationNodeRequest;
import com.hcmute.careergraph.dtos.response.genre.GenreCreationNodeResponse;
import com.hcmute.careergraph.dtos.response.genre.GenreNodeResponse;
import com.hcmute.careergraph.services.impl.GenreService;
import com.hcmute.careergraph.helper.ApiResponse;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/genres")
@CrossOrigin(origins = "*")
public class GenreController {

    @Autowired
    private GenreService genreService;

    @PostMapping("/create")
    public ApiResponse<GenreCreationNodeResponse> createGenreNode(@RequestBody GenreCreationNodeRequest request)
            throws BadRequestException {

        if (request.getName() == null || request.getName().isEmpty()) {
            throw new BadRequestException("Name invalid");
        }

        GenreCreationNodeResponse result = genreService.saveGenreNode(request.getName());
        return ApiResponse.<GenreCreationNodeResponse>builder()
                .code(200)
                .message("Completed API create node genre")
                .data(result)
                .build();
    }

    @PostMapping("/link-node")
    public ApiResponse<Void> linkMovieToGenre(@RequestParam("movieId") String movieId,
                                                                   @RequestParam("genreId") String genreId)
            throws BadRequestException {

        if (movieId == null || genreId == null
        || movieId.isEmpty() || genreId.isEmpty()) {
            throw new BadRequestException("Request invalid");
        }

        genreService.linkMovieToGenre(Long.parseLong(movieId), Long.parseLong(genreId));
        return ApiResponse.<Void>builder()
                .code(200)
                .message("Completed API link node genre to movie")
                .build();
    }

    @GetMapping("/get-all")
    public ApiResponse<List<GenreNodeResponse>> getFriends() {

        List<GenreNodeResponse> result = genreService.findAllGenreNodes();

        return ApiResponse.<List<GenreNodeResponse>>builder()
                .code(200)
                .message("Completed API get all genre nodes")
                .data(result)
                .build();
    }
}
