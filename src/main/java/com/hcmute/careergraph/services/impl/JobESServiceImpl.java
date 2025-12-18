package com.hcmute.careergraph.services.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.hcmute.careergraph.persistence.documents.JobES;
import com.hcmute.careergraph.repositories.JobESRepository;
import com.hcmute.careergraph.services.JobESService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobESServiceImpl implements JobESService {

    private final JobESRepository jobESRepository;


    private final ElasticsearchClient client;
    @Override
    public Page<JobES> searchPosts(String key, Pageable pageable) {
        return jobESRepository.search(key, pageable);
    }

    @Override
    public SearchResponse<JobES> searchJobsByNavtive(String key, Pageable pageable) {
        try {
            // Thực hiện search
            SearchResponse<JobES> response = client.search(s -> s
                            .index("jobs_es")
                            .query(q -> q
                                    .multiMatch(mm -> mm
                                            .query(key)
                                            .fields("title^10","description^5","state")
                                    )
                            )
                            .from((int) pageable.getOffset())
                            .size(pageable.getPageSize()),
                    JobES.class
            );
            return response;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public SearchResponse<JobES> searchJobsByNavtiveAndFuzzy(String key, Pageable pageable) {
        try {
            LocalDate today = LocalDate.now();

            SearchResponse<JobES> response = client.search(s -> s
                            .index("jobs_es")
                            .query(q -> q
                                    .bool(b -> b
                                            // Search theo text
                                            .must(m -> m
                                                    .multiMatch(mm -> mm
                                                            .query(key)
                                                            .fields(
                                                                    "title^10",
                                                                    "description^5",
                                                                    "state^1"
                                                            )
                                                            .fuzziness("AUTO")
                                                            .type(TextQueryType.MostFields)
                                                    )
                                            )
                                            // Filter job chưa hết hạn
//                                            .filter(f -> f
//                                                    .range(r -> r
//                                                            .date(d -> d
//                                                                    .field("expiredDate")
//                                                                    .gte(today.toString()) // yyyy-MM-dd
//                                                            )
//                                                    )
//                                            )
                                    )
                            )
                            .from((int) pageable.getOffset())
                            .size(pageable.getPageSize()),
                    JobES.class
            );

            return response;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}

//
//@Override
//public SearchResponse<PostES> searchPostsByNavtiveAndFuzzy(String key, Pageable pageable) {
//    try {
//        // Thực hiện search
//        SearchResponse<PostES> response = client.search(s -> s
//                        .index("posts")
//                        .query(q -> q
//                                .multiMatch(mm -> mm
//                                        .query(key)
//                                        .fields("title^5","description^1","shortDescription^1")
//                                        .fuzziness("AUTO")
//                                        .type(TextQueryType.MostFields)
//                                )
//                        )
//                        .from((int) pageable.getOffset())
//                        .size(pageable.getPageSize()),
//                PostES.class
//        );
//        return response;
//
//    } catch (Exception e) {
//        e.printStackTrace();
//        return null;
//    }
//}
