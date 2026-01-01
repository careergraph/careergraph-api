package com.hcmute.careergraph.services;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.hcmute.careergraph.enums.common.PartyType;
import com.hcmute.careergraph.persistence.documents.JobES;
import com.hcmute.careergraph.persistence.dtos.request.JobFilterRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface JobESService {
        Page<JobES> searchPosts(String key, Pageable pageable);

        SearchResponse<JobES> searchJobsByNavtive(String key, Pageable pageable);

        SearchResponse<JobES> searchJobsByNavtiveAndFuzzy(String key, Pageable pageable);

        SearchResponse<JobES> knnSearch(float[] queryVector, int k);

        SearchResponse<JobES> knnSearch(
                        float[] vector,
                        JobFilterRequest filter,
                        String partyId,
                        Pageable pageable,
                        PartyType type);

        SearchResponse<JobES> searchRecommendJobs(
                        String keyword,
                        Pageable pageable);

        /**
         * Search jobs cho Daily Digest với decay function và filters
         */
        SearchResponse<JobES> searchRecommendJobs(
                        String keyword,
                        Pageable pageable,
                        List<String> excludeJobIds,
                        int maxAgeDays);

        /**
         * Search jobs cho Daily Digest với filter chỉ từ danh sách job mới đăng.
         * 
         * @param keyword           Từ khóa tìm kiếm (từ candidate profile)
         * @param pageable          Pagination
         * @param newlyPostedJobIds Danh sách job IDs mới đăng (filter IN)
         * @param excludeJobIds     Danh sách job IDs cần loại (đã gửi)
         */
        SearchResponse<JobES> searchRecommendJobsFromNewlyPosted(
                        String keyword,
                        Pageable pageable,
                        List<String> newlyPostedJobIds,
                        List<String> excludeJobIds);
}
