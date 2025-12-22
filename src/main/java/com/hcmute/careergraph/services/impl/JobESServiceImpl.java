package com.hcmute.careergraph.services.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.hcmute.careergraph.enums.common.PartyType;
import com.hcmute.careergraph.persistence.documents.JobES;
import com.hcmute.careergraph.persistence.dtos.request.JobFilterRequest;
import com.hcmute.careergraph.repositories.JobESRepository;
import com.hcmute.careergraph.services.JobESService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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
                                            .fields("title^10","jobCategory^5","state")
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
                                                                    "jobCategory^5",
                                                                    "state^1"
//                                                                    ,"description"
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

    @Override
    public SearchResponse<JobES> knnSearch(
            float[] queryVector,
            JobFilterRequest filter,
            String partyId,
            Pageable pageable,
            PartyType type
    ) {
        try {
            return client.search(s -> s
                            .index("jobs_es")
                            .from((int) pageable.getOffset())
                            .size(pageable.getPageSize())
                            .knn(knn -> knn
                                    .field("embedding")
                                    .queryVector(toFloatList(queryVector))
                                    .k(100)
                                    .numCandidates(300)
                                    .filter(f -> f.bool(b -> {

                                        /* ===== STATUS ===== */
                                        if (filter.getStatuses() != null && !filter.getStatuses().isEmpty()) {
                                            b.filter(q -> q.terms(t -> t
                                                    .field("status")
                                                    .terms(v -> v.value(
                                                            filter.getStatuses().stream()
                                                                    .map(s1 -> FieldValue.of(s1.name()))
                                                                    .toList()
                                                    ))
                                            ));
                                        }

                                        /* ===== JOB CATEGORY ===== */
                                        if (filter.getJobCategories() != null && !filter.getJobCategories().isEmpty()) {
                                            b.filter(q -> q.terms(t -> t
                                                    .field("jobCategory.keyword")
                                                    .terms(v -> v.value(
                                                            filter.getJobCategories().stream()
                                                                    .map(c -> FieldValue.of(c.name()))
                                                                    .toList()
                                                    ))
                                            ));
                                        }

                                        /* ===== EMPLOYMENT TYPE ===== */
                                        if (filter.getEmploymentTypes() != null && !filter.getEmploymentTypes().isEmpty()) {
                                            b.filter(q -> q.terms(t -> t
                                                    .field("employmentType")
                                                    .terms(v -> v.value(
                                                            filter.getEmploymentTypes().stream()
                                                                    .map(e -> FieldValue.of(e.name()))
                                                                    .toList()
                                                    ))
                                            ));
                                        }

                                        /* ===== EXPERIENCE ===== */
                                        if (filter.getExperienceLevels() != null && !filter.getExperienceLevels().isEmpty()) {
                                            b.filter(q -> q.terms(t -> t
                                                    .field("experienceLevel")
                                                    .terms(v -> v.value(
                                                            filter.getExperienceLevels().stream()
                                                                    .map(e -> FieldValue.of(e.name()))
                                                                    .toList()
                                                    ))
                                            ));
                                        }

                                        /* ===== EDUCATION ===== */
                                        if (filter.getEducationTypes() != null && !filter.getEducationTypes().isEmpty()) {
                                            b.filter(q -> q.terms(t -> t
                                                    .field("education")
                                                    .terms(v -> v.value(
                                                            filter.getEducationTypes().stream()
                                                                    .map(e -> FieldValue.of(e.name()))
                                                                    .toList()
                                                    ))
                                            ));
                                        }

                                        /* ===== CITY ===== */
                                        if (filter.getCity() != null && !filter.getCity().isEmpty()) {
                                            b.filter(q -> q.term(t -> t
                                                    .field("city")
                                                    .value(filter.getCity())
                                            ));
                                        }

                                        /* ===== COMPANY ===== */
                                        if (type == PartyType.COMPANY && partyId != null) {
                                            b.filter(q -> q.term(t -> t
                                                    .field("companyId")
                                                    .value(partyId)
                                            ));
                                        }

                                        return b;
                                    }))
                            ),
                    JobES.class);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }




    @Override
    public SearchResponse<JobES> knnSearch(float[] queryVector, int k) {
        try {
            SearchResponse<JobES> response = client.search( s -> s
                            .index("jobs_es")
                            .knn(knn -> knn
                                    .field("embedding")
                                    .queryVector(toFloatList(queryVector))
                                    .k(k)
                                    .numCandidates(100)

                            ),
                    JobES .class
            );

            return response;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
//    @Override
//    public SearchResponse<JobES> knnSearch(float[] queryVector, int k) {
//        try {
//            SearchResponse<JobES> response = client.search( s -> s
//                            .index("jobs_es")
//                            .knn(knn -> knn
//                                    .field("embedding")
//                                    .queryVector(toFloatList(queryVector))
//                                    .k(k)
//                                    .numCandidates(100)
////                                    .filter(f-> f
////                                            .bool(b -> b
////                                                    .must(m -> m.term( t -> t
////                                                            .field("isPublic")
////                                                            .value(true))
////                                                    )
////                                                    .must(m -> m.term( t -> t
////                                                            .field("isDraft")
////                                                            .value(false)
////                                                    ))
////                                            )
////                                    )
//
//                            ),
//                    JobES .class
//            );
//
//            return response;
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            return null;
//        }
//    }
    private List<Float> toFloatList(float [] vector){
        List<Float> list = new ArrayList<>();
        for(float v : vector)
            list.add(v);

        return list;
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
