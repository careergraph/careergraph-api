package com.hcmute.careergraph.services.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.Script;
import co.elastic.clients.elasticsearch._types.ScriptLanguage;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionBoostMode;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionScoreMode;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.json.JsonData;
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
                                                                        .fields("title^10", "jobCategory^5", "state")))
                                        .from((int) pageable.getOffset())
                                        .size(pageable.getPageSize()),
                                        JobES.class);
                        return response;

                } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                }
        }

        @Override
        public SearchResponse<JobES> searchJobsByNavtiveAndFuzzy(String key, Pageable pageable) {
                try {
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
                                                                                                                        "state^1")
                                                                                                        .fuzziness("AUTO")
                                                                                                        .type(TextQueryType.MostFields)))

                                                        ))
                                        .from((int) pageable.getOffset())
                                        .size(pageable.getPageSize()),
                                        JobES.class);

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
                        PartyType type) {
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
                                                                if (filter.getStatuses() != null
                                                                                && !filter.getStatuses().isEmpty()) {
                                                                        b.filter(q -> q.terms(t -> t
                                                                                        .field("status")
                                                                                        .terms(v -> v.value(
                                                                                                        filter.getStatuses()
                                                                                                                        .stream()
                                                                                                                        .map(s1 -> FieldValue
                                                                                                                                        .of(s1.name()))
                                                                                                                        .toList()))));
                                                                }

                                                                /* ===== JOB CATEGORY ===== */
                                                                if (filter.getJobCategories() != null && !filter
                                                                                .getJobCategories().isEmpty()) {
                                                                        b.filter(q -> q.terms(t -> t
                                                                                        .field("jobCategory.keyword")
                                                                                        .terms(v -> v.value(
                                                                                                        filter.getJobCategories()
                                                                                                                        .stream()
                                                                                                                        .map(c -> FieldValue
                                                                                                                                        .of(c.name()))
                                                                                                                        .toList()))));
                                                                }

                                                                /* ===== EMPLOYMENT TYPE ===== */
                                                                if (filter.getEmploymentTypes() != null && !filter
                                                                                .getEmploymentTypes().isEmpty()) {
                                                                        b.filter(q -> q.terms(t -> t
                                                                                        .field("employmentType")
                                                                                        .terms(v -> v.value(
                                                                                                        filter.getEmploymentTypes()
                                                                                                                        .stream()
                                                                                                                        .map(e -> FieldValue
                                                                                                                                        .of(e.name()))
                                                                                                                        .toList()))));
                                                                }

                                                                /* ===== EXPERIENCE ===== */
                                                                if (filter.getExperienceLevels() != null && !filter
                                                                                .getExperienceLevels().isEmpty()) {
                                                                        b.filter(q -> q.terms(t -> t
                                                                                        .field("experienceLevel")
                                                                                        .terms(v -> v.value(
                                                                                                        filter.getExperienceLevels()
                                                                                                                        .stream()
                                                                                                                        .map(e -> FieldValue
                                                                                                                                        .of(e.name()))
                                                                                                                        .toList()))));
                                                                }

                                                                /* ===== EDUCATION ===== */
                                                                if (filter.getEducationTypes() != null && !filter
                                                                                .getEducationTypes().isEmpty()) {
                                                                        b.filter(q -> q.terms(t -> t
                                                                                        .field("education")
                                                                                        .terms(v -> v.value(
                                                                                                        filter.getEducationTypes()
                                                                                                                        .stream()
                                                                                                                        .map(e -> FieldValue
                                                                                                                                        .of(e.name()))
                                                                                                                        .toList()))));
                                                                }

                                                                /* ===== CITY ===== */
                                                                if (filter.getCity() != null
                                                                                && !filter.getCity().isEmpty()) {
                                                                        b.filter(q -> q.term(t -> t
                                                                                        .field("city")
                                                                                        .value(filter.getCity())));
                                                                }

                                                                /* ===== COMPANY ===== */
                                                                if (type == PartyType.COMPANY && partyId != null) {
                                                                        b.filter(q -> q.term(t -> t
                                                                                        .field("companyId")
                                                                                        .value(partyId)));
                                                                }

                                                                return b;
                                                        }))),
                                        JobES.class);

                } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                }
        }

        public SearchResponse<JobES> searchRecommendJobs(
                        String keyword,
                        Pageable pageable) {
                return searchRecommendJobs(keyword, pageable, null, 7);
        }

        /**
         * Search jobs cho Daily Digest với:
         * - Text matching (relevance)
         * - Decay function boost job mới
         * - Filter range ngày (optional)
         * - Exclude job IDs đã gửi
         *
         * @param keyword       Từ khóa tìm kiếm (desiredPosition + industry +
         *                      locations)
         * @param pageable      Pagination
         * @param excludeJobIds Job IDs cần loại trừ (đã gửi trước đó)
         * @param maxAgeDays    Chỉ lấy job trong X ngày gần đây (0 = không filter)
         */
        public SearchResponse<JobES> searchRecommendJobs(
                        String keyword,
                        Pageable pageable,
                        List<String> excludeJobIds,
                        int maxAgeDays) {
                try {
                        LocalDate cutoffDate = maxAgeDays > 0
                                        ? LocalDate.now().minusDays(maxAgeDays)
                                        : null;

                        return client.search(s -> s
                                        .index("jobs_es")
                                        .query(q -> q
                                                        .functionScore(fs -> fs
                                                                        .query(base -> base
                                                                                        .bool(b -> {
                                                                                                // Text matching với
                                                                                                // relevance
                                                                                                b.must(m -> m
                                                                                                                .multiMatch(mm -> mm
                                                                                                                                .query(keyword)
                                                                                                                                .fields(
                                                                                                                                                "title^10",
                                                                                                                                                "jobCategory^5",
                                                                                                                                                "state^2",
                                                                                                                                                "city^1")
                                                                                                                                .fuzziness("AUTO")
                                                                                                                                .type(TextQueryType.MostFields)));

                                                                                                // Filter: Chỉ job còn
                                                                                                // ACTIVE
                                                                                                b.filter(f -> f
                                                                                                                .term(t -> t
                                                                                                                                .field("status")
                                                                                                                                .value("ACTIVE")));

                                                                                                // Filter: Job trong X
                                                                                                // ngày gần đây
                                                                                                if (cutoffDate != null) {
                                                                                                        b.filter(f -> f
                                                                                                                        .range(r -> r
                                                                                                                                        .date(d -> d
                                                                                                                                                        .field("createdAt")
                                                                                                                                                        .gte(cutoffDate.toString()))));
                                                                                                }

                                                                                                // Exclude job IDs đã
                                                                                                // gửi
                                                                                                if (excludeJobIds != null
                                                                                                                && !excludeJobIds
                                                                                                                                .isEmpty()) {
                                                                                                        b.mustNot(mn -> mn
                                                                                                                        .ids(ids -> ids
                                                                                                                                        .values(excludeJobIds)));
                                                                                                }

                                                                                                return b;
                                                                                        }))
                                                                        // Decay function: Boost job mới, giảm dần theo
                                                                        // thời gian
                                                                        .functions(f -> f
                                                                                        .scriptScore(ss -> ss
                                                                                                        .script(script -> script
                                                                                                                        .source("""
                                                                                                                                            // Decay factor: job mới hơn = score cao hơn
                                                                                                                                            // decay_rate = 0.1 nghĩa là sau 10 ngày score giảm ~63%
                                                                                                                                            long created = doc['createdAt'].value.toEpochMilli();
                                                                                                                                            long now = new Date().getTime();
                                                                                                                                            double days = (now - created) / 86400000.0;
                                                                                                                                            if (days < 0) days = 0;

                                                                                                                                            // Gaussian decay: e^(-days^2 / (2 * scale^2))
                                                                                                                                            // scale = 7 days: sau 7 ngày score giảm ~60%
                                                                                                                                            double scale = 7.0;
                                                                                                                                            double decayScore = Math.exp(-Math.pow(days, 2) / (2 * Math.pow(scale, 2)));

                                                                                                                                            // Đảm bảo score >= 0.1 để không loại bỏ hoàn toàn job cũ
                                                                                                                                            return Math.max(decayScore, 0.1);
                                                                                                                                        """))))
                                                                        // Multiply: final_score = text_relevance *
                                                                        // decay_score
                                                                        .boostMode(FunctionBoostMode.Multiply)
                                                                        .scoreMode(FunctionScoreMode.Multiply)))
                                        .from((int) pageable.getOffset())
                                        .size(pageable.getPageSize()),
                                        JobES.class);
                } catch (Exception e) {
                        log.error("Error searching recommend jobs: {}", e.getMessage());
                        e.printStackTrace();
                        return null;
                }
        }

        /**
         * Search jobs cho Daily Digest với filter CHỈ từ danh sách job mới đăng.
         * Đây là method chính cho Daily Digest pipeline.
         *
         * @param keyword           Từ khóa tìm kiếm (từ candidate profile)
         * @param pageable          Pagination
         * @param newlyPostedJobIds Danh sách job IDs mới đăng (filter IN - chỉ search
         *                          trong này)
         * @param excludeJobIds     Danh sách job IDs cần loại (đã gửi trước đó)
         */
        @Override
        public SearchResponse<JobES> searchRecommendJobsFromNewlyPosted(
                        String keyword,
                        Pageable pageable,
                        List<String> newlyPostedJobIds,
                        List<String> excludeJobIds) {
                try {
                        // Nếu không có job mới → trả về null
                        if (newlyPostedJobIds == null || newlyPostedJobIds.isEmpty()) {
                                log.debug("No newly posted jobs to search");
                                return null;
                        }

                        return client.search(s -> s
                                        .index("jobs_es")
                                        .query(q -> q
                                                        .functionScore(fs -> fs
                                                                        .query(base -> base
                                                                                        .bool(b -> {
                                                                                                // Text matching với
                                                                                                // relevance
                                                                                                b.must(m -> m
                                                                                                                .multiMatch(mm -> mm
                                                                                                                                .query(keyword)
                                                                                                                                .fields(
                                                                                                                                                "title^10",
                                                                                                                                                "jobCategory^5",
                                                                                                                                                "state^2",
                                                                                                                                                "city^1")
                                                                                                                                .fuzziness("AUTO")
                                                                                                                                .type(TextQueryType.MostFields)));

                                                                                                // Filter: CHỈ search
                                                                                                // trong danh sách job
                                                                                                // mới đăng
                                                                                                b.filter(f -> f
                                                                                                                .ids(ids -> ids
                                                                                                                                .values(newlyPostedJobIds)));

                                                                                                // Filter: Chỉ job còn
                                                                                                // ACTIVE
                                                                                                b.filter(f -> f
                                                                                                                .term(t -> t
                                                                                                                                .field("status")
                                                                                                                                .value("ACTIVE")));

                                                                                                // Exclude job IDs đã
                                                                                            //                                                                                                // gửi
                                                                                            if (excludeJobIds != null
                                                                                                    && !excludeJobIds
                                                                                                                                .isEmpty()) {
                                                                                                        b.mustNot(mn -> mn
                                                                                                                        .ids(ids -> ids
                                                                                                                                        .values(excludeJobIds)));
                                                                                                }

                                                                                                return b;
                                                                                        }))
                                                                        // Decay function: Boost job mới, giảm dần theo
                                                                        // thời gian
                                                                        .functions(f -> f
                                                                                        .scriptScore(ss -> ss
                                                                                                        .script(script -> script
                                                                                                                        .source("""
                                                                                                                                            long created = doc['createdAt'].value.toEpochMilli();
                                                                                                                                            long now = new Date().getTime();
                                                                                                                                            double days = (now - created) / 86400000.0;
                                                                                                                                            if (days < 0) days = 0;
                                                                                                                                            double scale = 7.0;
                                                                                                                                            double decayScore = Math.exp(-Math.pow(days, 2) / (2 * Math.pow(scale, 2)));
                                                                                                                                            return Math.max(decayScore, 0.1);
                                                                                                                                        """))))
                                                                        .boostMode(FunctionBoostMode.Multiply)
                                                                        .scoreMode(FunctionScoreMode.Multiply)))
                                        .from((int) pageable.getOffset())
                                        .size(pageable.getPageSize()),
                                        JobES.class);
                } catch (Exception e) {
                        log.error("Error searching recommend jobs from newly posted: {}", e.getMessage());
                        e.printStackTrace();
                        return null;
                }
        }

        @Override
        public SearchResponse<JobES> knnSearch(float[] queryVector, int k) {
                try {
                        SearchResponse<JobES> response = client.search(s -> s
                                        .index("jobs_es")
                                        .knn(knn -> knn
                                                        .field("embedding")
                                                        .queryVector(toFloatList(queryVector))
                                                        .k(k)
                                                        .numCandidates(100)

                                        ),
                                        JobES.class);

                        return response;

                } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                }
        }

        private List<Float> toFloatList(float[] vector) {
                List<Float> list = new ArrayList<>();
                for (float v : vector)
                        list.add(v);

                return list;
        }
}
