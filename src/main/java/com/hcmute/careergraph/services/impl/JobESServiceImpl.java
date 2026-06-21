package com.hcmute.careergraph.services.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionBoostMode;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionScoreMode;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.hcmute.careergraph.enums.common.PartyType;
import com.hcmute.careergraph.helper.JobLocationFilterSupport;
import com.hcmute.careergraph.persistence.documents.JobES;
import com.hcmute.careergraph.persistence.dtos.request.JobFilterRequest;
import com.hcmute.careergraph.services.EmbedService;
import com.hcmute.careergraph.services.JobESService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobESServiceImpl implements JobESService {

        private final ElasticsearchClient client;

        private final EmbedService embedService;

        @Override
        public SearchResponse<JobES> searchJobsByNavtiveAndFuzzy(String key, Pageable pageable) {
                try {
                        String normalizedKey = normalizeSearchQuery(key);
                        boolean allowFuzzy = shouldUseFuzzy(normalizedKey);
                        SearchResponse<JobES> response = client.search(s -> s
                                        .index("jobs_es")
                                        .query(q -> q
                                                        .bool(b -> b
                                                                        // Search theo text
                                                                        .must(m -> m
                                                                                        .multiMatch(mm -> {
                                                                                                mm.query(normalizedKey)
                                                                                                                .fields(
                                                                                                                                "title^10",
                                                                                                                                "jobCategory^5",
                                                                                                                                "description^4",
                                                                                                                                "qualifications^5",
                                                                                                                                "minimumQualifications^5",
                                                                                                                                "responsibilities^4",
                                                                                                                                "skills^6",
                                                                                                                                "state^2",
                                                                                                                                "city^1")
                                                                                                                .operator(Operator.Or)
                                                                                                                .minimumShouldMatch("30%")
                                                                                                                .type(TextQueryType.MostFields);
                                                                                                if (allowFuzzy) {
                                                                                                        mm.fuzziness("AUTO");
                                                                                                }
                                                                                                return mm;
                                                                                        }))
                                                                        .filter(f -> f.term(t -> t.field("jobSearchable").value(true)))

                                                        ))
                                        .from((int) pageable.getOffset())
                                        .size(pageable.getPageSize()),
                                        JobES.class);

                        return response;
                } catch (Exception e) {
                        log.error("Error in searchJobsByNavtiveAndFuzzy: {}", e.getMessage(), e);
                        return null;
                }
        }

        private String normalizeSearchQuery(String key) {
                if (key == null) {
                        return "";
                }
                return key.replaceAll("\\s+", " ").trim();
        }

        private boolean shouldUseFuzzy(String query) {
                if (query == null || query.length() > 256) {
                        return false;
                }
                return query.split("\\s+").length <= 12;
        }

        /**
         * Filter-only search: match_all + post filter, sắp xếp theo createdAt desc.
         * Dùng khi user chưa nhập keyword nhưng có chọn filter.
         */
        @Override
        public SearchResponse<JobES> filterOnlySearch(
                        JobFilterRequest filter,
                        String partyId,
                        Pageable pageable,
                        PartyType type) {
                try {
                        return client.search(s -> s
                                        .index("jobs_es")
                                        .from((int) pageable.getOffset())
                                        .size(pageable.getPageSize())
                                        .sort(so -> so.field(f -> f.field("createdAt").order(co.elastic.clients.elasticsearch._types.SortOrder.Desc)))
                                        .query(q -> q.matchAll(m -> m))
                                        .postFilter(pf -> pf
                                                        .bool(b -> {
                                                                // Mặc định chỉ hiện ACTIVE
                                                                if (filter.getStatuses() != null
                                                                                && !filter.getStatuses().isEmpty()) {
                                                                        b.filter(fq -> fq.terms(t -> t
                                                                                        .field("status")
                                                                                        .terms(v -> v.value(
                                                                                                        filter.getStatuses()
                                                                                                                        .stream()
                                                                                                                        .map(st -> FieldValue
                                                                                                                                        .of(st.name()))
                                                                                                                        .toList()))));
                                                                }

                                                                if (filter.getJobCategories() != null
                                                                                && !filter.getJobCategories()
                                                                                                .isEmpty()) {
                                                                        b.filter(fq -> fq.terms(t -> t
                                                                                        .field("jobCategory.keyword")
                                                                                        .terms(v -> v.value(
                                                                                                        filter.getJobCategories()
                                                                                                                        .stream()
                                                                                                                        .map(c -> FieldValue
                                                                                                                                        .of(c.name()))
                                                                                                                        .toList()))));
                                                                }

                                                                if (filter.getEmploymentTypes() != null
                                                                                && !filter.getEmploymentTypes()
                                                                                                .isEmpty()) {
                                                                        b.filter(fq -> fq.terms(t -> t
                                                                                        .field("employmentType")
                                                                                        .terms(v -> v.value(
                                                                                                        filter.getEmploymentTypes()
                                                                                                                        .stream()
                                                                                                                        .map(e -> FieldValue
                                                                                                                                        .of(e.name()))
                                                                                                                        .toList()))));
                                                                }

                                                                if (filter.getExperienceLevels() != null
                                                                                && !filter.getExperienceLevels()
                                                                                                .isEmpty()) {
                                                                        b.filter(fq -> fq.terms(t -> t
                                                                                        .field("experienceLevel")
                                                                                        .terms(v -> v.value(
                                                                                                        filter.getExperienceLevels()
                                                                                                                        .stream()
                                                                                                                        .map(e -> FieldValue
                                                                                                                                        .of(e.name()))
                                                                                                                        .toList()))));
                                                                }

                                                                if (filter.getEducationTypes() != null
                                                                                && !filter.getEducationTypes()
                                                                                                .isEmpty()) {
                                                                        b.filter(fq -> fq.terms(t -> t
                                                                                        .field("education")
                                                                                        .terms(v -> v.value(
                                                                                                        filter.getEducationTypes()
                                                                                                                        .stream()
                                                                                                                        .map(e -> FieldValue
                                                                                                                                        .of(e.name()))
                                                                                                                        .toList()))));
                                                                }

                                                                JobLocationFilterSupport.applyLocationFilter(b, filter);

                                                                if (type != PartyType.COMPANY) {
                                                                        b.filter(fq -> fq.term(t -> t
                                                                                        .field("jobSearchable")
                                                                                        .value(true)));
                                                                }

                                                                if (type == PartyType.COMPANY && partyId != null) {
                                                                        b.filter(fq -> fq.term(t -> t
                                                                                        .field("companyId")
                                                                                        .value(partyId)));
                                                                }

                                                                return b;
                                                        })),
                                        JobES.class);
                } catch (Exception e) {
                        log.error("Error in filterOnlySearch: {}", e.getMessage());
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

                                                                /* ===== LOCATION ===== */
                                                                JobLocationFilterSupport.applyLocationFilter(b, filter);

                                                                if (type != PartyType.COMPANY) {
                                                                        b.filter(q -> q.term(t -> t
                                                                                        .field("jobSearchable")
                                                                                        .value(true)));
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

        /**
         * Hybrid Search: Kết hợp BM25 Text Search + KNN Embedding Search + Exact
         * Filters
         * (FREE VERSION - Basic License, không dùng RRF)
         * 
         * Kiến trúc:
         * 1. KNN Query: Đặt trong bool.should (KHÔNG dùng top-level knn)
         * 2. BM25 Text Search: multi_match với fuzzy, phrase_prefix, cross_fields
         * 3. Hybrid Score: ES tự động cộng scores từ các should clauses
         * 4. Post Filter: Lọc chính xác sau khi scoring
         * 
         * Mapping required cho embedding field:
         * {
         * "embedding": {
         * "type": "dense_vector",
         * "dims": 384,
         * "index": true,
         * "similarity": "cosine"
         * }
         * }
         */
        @Override
        public SearchResponse<JobES> knnSearch(
                        String keyword,
                        JobFilterRequest filter,
                        String partyId,
                        Pageable pageable,
                        PartyType type) {
                try {
                        String normalizedKeyword = normalizeSearchQuery(keyword);
                        boolean allowFuzzy = shouldUseFuzzy(normalizedKeyword);
                        float[] queryVector = embedService.embed(normalizedKeyword);

                        return client.search(s -> s
                                        .index("jobs_es")
                                        .from((int) pageable.getOffset())
                                        .size(pageable.getPageSize())

                                        /* ===== HYBRID QUERY: KNN + BM25 trong bool.should ===== */
                                        .query(q -> q
                                                        .bool(b -> b
                                                                        /*
                                                                         * ===== 1. KNN SEMANTIC SEARCH =====
                                                                         * Đặt trong should, boost 0.5 để cân bằng với
                                                                         * BM25
                                                                         */
                                                                        .should(sh -> sh
                                                                                        .knn(knn -> knn
                                                                                                        .field("embedding")
                                                                                                        .queryVector(toFloatList(
                                                                                                                        queryVector))
                                                                                                        .numCandidates(300)
                                                                                                        .boost(50.0f)))

                                                                        /*
                                                                         * ===== 2. BM25 TEXT SEARCH =====
                                                                         * BestFields: Tìm term match tốt nhất trong 1
                                                                         * field
                                                                         */
                                                                        .should(sh -> sh
                                                                                        .multiMatch(mm -> {
                                                                                                mm.query(normalizedKeyword)
                                                                                                                .fields(
                                                                                                                                "title^10",
                                                                                                                                "jobCategory^5",
                                                                                                                                "description^4",
                                                                                                                                "qualifications^5",
                                                                                                                                "minimumQualifications^5",
                                                                                                                                "responsibilities^4",
                                                                                                                                "skills^6",
                                                                                                                                "state^2",
                                                                                                                                "city^1")
                                                                                                                .type(TextQueryType.BestFields)
                                                                                                                .operator(Operator.Or)
                                                                                                                .minimumShouldMatch(
                                                                                                                                "50%")
                                                                                                                .boost(1.0f);
                                                                                                if (allowFuzzy) {
                                                                                                        mm.fuzziness("AUTO");
                                                                                                }
                                                                                                return mm;
                                                                                        }))

                                                                        /*
                                                                         * ===== 3. PHRASE PREFIX =====
                                                                         * Boost cao cho exact phrase match
                                                                         */
                                                                        .should(sh -> sh
                                                                                        .multiMatch(mm -> mm
                                                                                                        .query(normalizedKeyword)
                                                                                                        .fields(
                                                                                                                        "title^10",
                                                                                                                        "jobCategory^5",
                                                                                                                        "skills^5",
                                                                                                                        "qualifications^4")
                                                                                                        .type(TextQueryType.PhrasePrefix)
                                                                                                        .boost(0.3f)))

                                                                        /*
                                                                         * ===== 4. CROSS FIELDS =====
                                                                         * Match terms across multiple fields
                                                                         */
                                                                        .should(sh -> sh
                                                                                        .multiMatch(mm -> mm
                                                                                                        .query(normalizedKeyword)
                                                                                                        .fields(
                                                                                                                        "title^10",
                                                                                                                        "jobCategory^5",
                                                                                                                        "description^4",
                                                                                                                        "qualifications^5",
                                                                                                                        "minimumQualifications^5",
                                                                                                                        "responsibilities^4",
                                                                                                                        "skills^6",
                                                                                                                        "state^2",
                                                                                                                        "city^1")
                                                                                                        .type(TextQueryType.CrossFields)
                                                                                                        .operator(Operator.Or)
                                                                                                        .minimumShouldMatch(
                                                                                                                        "50%")
                                                                                                        .boost(0.1f)))

                                                                        // Ít nhất 1 should clause phải match
                                                                        .minimumShouldMatch("1")))

                                        /* ===== 5. POST FILTER (Exact Filtering) ===== */
                                        .postFilter(pf -> pf
                                                        .bool(b -> {
                                                                /* ===== STATUS ===== */
                                                                if (filter.getStatuses() != null
                                                                                && !filter.getStatuses().isEmpty()) {
                                                                        b.filter(fq -> fq.terms(t -> t
                                                                                        .field("status")
                                                                                        .terms(v -> v.value(
                                                                                                        filter.getStatuses()
                                                                                                                        .stream()
                                                                                                                        .map(st -> FieldValue
                                                                                                                                        .of(st.name()))
                                                                                                                        .toList()))));
                                                                }

                                                                /* ===== JOB CATEGORY ===== */
                                                                if (filter.getJobCategories() != null
                                                                                && !filter.getJobCategories()
                                                                                                .isEmpty()) {
                                                                        b.filter(fq -> fq.terms(t -> t
                                                                                        .field("jobCategory.keyword")
                                                                                        .terms(v -> v.value(
                                                                                                        filter.getJobCategories()
                                                                                                                        .stream()
                                                                                                                        .map(c -> FieldValue
                                                                                                                                        .of(c.name()))
                                                                                                                        .toList()))));
                                                                }

                                                                /* ===== EMPLOYMENT TYPE ===== */
                                                                if (filter.getEmploymentTypes() != null
                                                                                && !filter.getEmploymentTypes()
                                                                                                .isEmpty()) {
                                                                        b.filter(fq -> fq.terms(t -> t
                                                                                        .field("employmentType")
                                                                                        .terms(v -> v.value(
                                                                                                        filter.getEmploymentTypes()
                                                                                                                        .stream()
                                                                                                                        .map(e -> FieldValue
                                                                                                                                        .of(e.name()))
                                                                                                                        .toList()))));
                                                                }

                                                                /* ===== EXPERIENCE LEVEL ===== */
                                                                if (filter.getExperienceLevels() != null
                                                                                && !filter.getExperienceLevels()
                                                                                                .isEmpty()) {
                                                                        b.filter(fq -> fq.terms(t -> t
                                                                                        .field("experienceLevel")
                                                                                        .terms(v -> v.value(
                                                                                                        filter.getExperienceLevels()
                                                                                                                        .stream()
                                                                                                                        .map(e -> FieldValue
                                                                                                                                        .of(e.name()))
                                                                                                                        .toList()))));
                                                                }

                                                                /* ===== EDUCATION ===== */
                                                                if (filter.getEducationTypes() != null
                                                                                && !filter.getEducationTypes()
                                                                                                .isEmpty()) {
                                                                        b.filter(fq -> fq.terms(t -> t
                                                                                        .field("education")
                                                                                        .terms(v -> v.value(
                                                                                                        filter.getEducationTypes()
                                                                                                                        .stream()
                                                                                                                        .map(e -> FieldValue
                                                                                                                                        .of(e.name()))
                                                                                                                        .toList()))));
                                                                }

                                                                /* ===== LOCATION ===== */
                                                                JobLocationFilterSupport.applyLocationFilter(b, filter);

                                                                /* ===== COMPANY ===== */
                                                                if (type == PartyType.COMPANY && partyId != null) {
                                                                        b.filter(fq -> fq.term(t -> t
                                                                                        .field("companyId")
                                                                                        .value(partyId)));
                                                                }

                                                                if (type != PartyType.COMPANY) {
                                                                        b.filter(fq -> fq.term(t -> t
                                                                                        .field("jobSearchable")
                                                                                        .value(true)));
                                                                }

                                                                return b;
                                                        })),
                                        JobES.class);

                } catch (Exception e) {
                        log.error("Error in hybrid knnSearch: {}", e.getMessage());
                        e.printStackTrace();
                        throw new RuntimeException("Failed to execute hybrid search", e);
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
                        String normalizedKeyword = normalizeSearchQuery(keyword);
                        boolean allowFuzzy = shouldUseFuzzy(normalizedKeyword);
                        float[] queryVector = embedService.embed(normalizedKeyword);

                        return client.search(s -> s
                                        .index("jobs_es")
                                        .query(q -> q
                                                        .functionScore(fs -> fs
                                                                        .query(base -> base
                                                                                        .bool(b -> {
                                                                                                // Text matching với
                                                                                                // relevance
                                                                                                b.should(sh -> sh
                                                                                                                .knn(knn -> knn
                                                                                                                                .field("embedding")
                                                                                                                                .queryVector(toFloatList(queryVector))
                                                                                                                                .numCandidates(200)
                                                                                                                                .boost(0.8f)));

                                                                                                b.should(m -> m
                                                                                                                .multiMatch(mm -> {
                                                                                                                        mm.query(normalizedKeyword)
                                                                                                                                        .fields(
                                                                                                                                                        "title^10",
                                                                                                                                                        "jobCategory^5",
                                                                                                                                                        "description^4",
                                                                                                                                                        "qualifications^5",
                                                                                                                                                        "minimumQualifications^5",
                                                                                                                                                        "responsibilities^4",
                                                                                                                                                        "skills^6",
                                                                                                                                                        "state^2",
                                                                                                                                                        "city^1")
                                                                                                                                        .type(TextQueryType.MostFields);
                                                                                                                        if (allowFuzzy) {
                                                                                                                                mm.fuzziness("AUTO");
                                                                                                                        }
                                                                                                                        return mm;
                                                                                                                }));

                                                                                                // Filter: CHỈ search
                                                                                                // trong danh sách job
                                                                                                // mới đăng
                                                                                                b.minimumShouldMatch("1");

                                                                                                b.filter(f -> f
                                                                                                                .ids(ids -> ids
                                                                                                                                .values(newlyPostedJobIds)));

                                                                                                // Filter: Chỉ job còn
                                                                                                // ACTIVE
                                                                                                b.filter(f -> f
                                                                                                                .term(t -> t
                                                                                                                                .field("status")
                                                                                                                                .value("ACTIVE")));
                                                                                                b.filter(f -> f
                                                                                                                .term(t -> t
                                                                                                                                .field("jobSearchable")
                                                                                                                                .value(true)));

                                                                                                // Exclude job IDs đã
                                                                                                // // gửi
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

        private List<Float> toFloatList(float[] vector) {
                List<Float> list = new ArrayList<>();
                for (float v : vector)
                        list.add(v);

                return list;
        }
}
