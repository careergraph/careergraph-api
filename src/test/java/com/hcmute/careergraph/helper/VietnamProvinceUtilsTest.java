package com.hcmute.careergraph.helper;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VietnamProvinceUtilsTest {

    @Test
    void toProvinceSlug_normalizesVietnameseNames() {
        assertEquals("tuyen-quang", VietnamProvinceUtils.toProvinceSlug("Tỉnh Tuyên Quang"));
        assertEquals("ha-noi", VietnamProvinceUtils.toProvinceSlug("Thành phố Hà Nội"));
        assertEquals("ho-chi-minh", VietnamProvinceUtils.toProvinceSlug("Thành phố Hồ Chí Minh"));
    }

    @Test
    void codeFromStateName_resolvesKnownProvince() {
        assertEquals("08", VietnamProvinceUtils.codeFromStateName("Tỉnh Tuyên Quang"));
        assertEquals("01", VietnamProvinceUtils.codeFromStateName("Thành phố Hà Nội"));
    }

    @Test
    void slugFromStateName_matchesClientSlug() {
        assertEquals(
                VietnamProvinceUtils.toProvinceSlug("Tuyên Quang"),
                VietnamProvinceUtils.slugFromStateName("Tỉnh Tuyên Quang"));
    }

    @Test
    void resolveProvinceCode_prefersExplicitCode() {
        assertEquals(
                "08",
                VietnamProvinceUtils.resolveProvinceCode("08", "Hà Nội", "ha-noi"));
    }

    @Test
    void resolveProvinceCode_fallsBackToLocationName() {
        assertEquals(
                "08",
                VietnamProvinceUtils.resolveProvinceCode(null, "Tuyên Quang", null));
    }

    @Test
    void resolveProvinceCode_fallsBackToSlug() {
        assertEquals(
                "01",
                VietnamProvinceUtils.resolveProvinceCode(null, null, "ha-noi"));
    }
}
