package com.hcmute.careergraph.utils;

import com.hcmute.careergraph.dtos.response.movie.MovieNodeResponse;
import com.hcmute.careergraph.dtos.response.user.UserResponse;
import com.hcmute.careergraph.helper.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

public class PageUtil {

    public static PageResponse<MovieNodeResponse> convertToMoviePageResponse(Page<MovieNodeResponse> page) {
        return PageResponse.<MovieNodeResponse>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }


    public static Page<MovieNodeResponse> convertToMoviePage(PageResponse<MovieNodeResponse> pageResponse) {
        Pageable pageable = PageRequest.of(pageResponse.getPage(), pageResponse.getSize());
        return new PageImpl<>(
                pageResponse.getContent(),
                pageable,
                pageResponse.getTotalElements()
        );
    }

    public static PageResponse<UserResponse> convertToUserPageResponse(Page<UserResponse> page) {
        return PageResponse.<UserResponse>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }

    public static Page<UserResponse> convertToUserPage(PageResponse<UserResponse> pageResponse) {
        Pageable pageable = PageRequest.of(pageResponse.getPage(), pageResponse.getSize());
        return new PageImpl<>(
                pageResponse.getContent(),
                pageable,
                pageResponse.getTotalElements()
        );
    }
}
