package com.hcmute.careergraph.helper;

import com.hcmute.careergraph.persistence.dtos.request.JobFilterRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JobLocationFilterSupportTest {

    @Test
    void hasLocationFilter_detectsProvinceSlug() {
        JobFilterRequest filter = new JobFilterRequest();
        filter.setProvinceSlug("tuyen-quang");

        assertTrue(JobLocationFilterSupport.hasLocationFilter(filter));
    }

    @Test
    void hasLocationFilter_detectsLocationName() {
        JobFilterRequest filter = new JobFilterRequest();
        filter.setLocation("Hà Nội");

        assertTrue(JobLocationFilterSupport.hasLocationFilter(filter));
    }

    @Test
    void hasLocationFilter_returnsFalseWhenEmpty() {
        assertFalse(JobLocationFilterSupport.hasLocationFilter(new JobFilterRequest()));
        assertFalse(JobLocationFilterSupport.hasLocationFilter(null));
    }

    @Test
    void jobFilterRequest_acceptsCityAliasAsLocation() throws Exception {
        String json = """
                {
                  "city": "Tuyên Quang",
                  "provinceSlug": "tuyen-quang",
                  "provinceCode": "08"
                }
                """;

        JobFilterRequest filter = new com.fasterxml.jackson.databind.ObjectMapper()
                .readValue(json, JobFilterRequest.class);

        assertEquals("Tuyên Quang", filter.getLocation());
        assertEquals("tuyen-quang", filter.getProvinceSlug());
        assertEquals("08", filter.getProvinceCode());
    }
}
