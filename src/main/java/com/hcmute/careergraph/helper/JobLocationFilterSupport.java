package com.hcmute.careergraph.helper;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.hcmute.careergraph.persistence.dtos.request.JobFilterRequest;

/**
 * Xây dựng filter địa điểm cho job search trên Elasticsearch.
 */
public final class JobLocationFilterSupport {

    private JobLocationFilterSupport() {
    }

    public static boolean hasLocationFilter(JobFilterRequest filter) {
        if (filter == null) {
            return false;
        }
        return isNotBlank(filter.getProvinceSlug())
                || isNotBlank(filter.getProvinceCode())
                || isNotBlank(filter.getLocation());
    }

    public static void applyLocationFilter(BoolQuery.Builder builder, JobFilterRequest filter) {
        if (!hasLocationFilter(filter)) {
            return;
        }

        String slug = trim(filter.getProvinceSlug());
        String location = trim(filter.getLocation());
        String provinceCode = trim(VietnamProvinceUtils.resolveProvinceCode(
                filter.getProvinceCode(),
                filter.getLocation(),
                filter.getProvinceSlug()));

        builder.filter(Query.of(q -> q.bool(bb -> {
            if (!slug.isEmpty()) {
                bb.should(s -> s.term(t -> t.field("provinceSlug").value(slug)));
            }
            if (!provinceCode.isEmpty()) {
                bb.should(s -> s.term(t -> t.field("provinceCode").value(provinceCode)));
            }
            if (!location.isEmpty()) {
                bb.should(s -> s.match(m -> m.field("state").query(location)));
                bb.should(s -> s.match(m -> m.field("city").query(location)));
            }
            bb.minimumShouldMatch("1");
            return bb;
        })));
    }

    private static boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
