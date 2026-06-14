package com.hcmute.careergraph.helper;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Chuẩn hóa tên/slug/mã tỉnh-thành Việt Nam cho job search.
 */
public final class VietnamProvinceUtils {

    private static final Map<String, String> NAME_TO_CODE = new ConcurrentHashMap<>();

    static {
        register("An Giang", "89");
        register("Bà Rịa - Vũng Tàu", "77");
        register("Bắc Giang", "24");
        register("Bắc Kạn", "06");
        register("Bạc Liêu", "95");
        register("Bắc Ninh", "27");
        register("Bến Tre", "83");
        register("Bình Định", "52");
        register("Bình Dương", "74");
        register("Bình Phước", "70");
        register("Bình Thuận", "60");
        register("Cà Mau", "96");
        register("Cần Thơ", "92");
        register("Cao Bằng", "04");
        register("Đà Nẵng", "48");
        register("Đắk Lắk", "66");
        register("Đắk Nông", "67");
        register("Điện Biên", "11");
        register("Đồng Nai", "75");
        register("Đồng Tháp", "87");
        register("Gia Lai", "64");
        register("Hà Giang", "02");
        register("Hà Nam", "35");
        register("Hà Nội", "01");
        register("Hà Tĩnh", "42");
        register("Hải Dương", "30");
        register("Hải Phòng", "31");
        register("Hậu Giang", "93");
        register("Hòa Bình", "17");
        register("Hưng Yên", "33");
        register("Khánh Hòa", "56");
        register("Kiên Giang", "91");
        register("Kon Tum", "62");
        register("Lai Châu", "12");
        register("Lâm Đồng", "68");
        register("Lạng Sơn", "20");
        register("Lào Cai", "10");
        register("Long An", "80");
        register("Nam Định", "36");
        register("Nghệ An", "40");
        register("Ninh Bình", "37");
        register("Ninh Thuận", "58");
        register("Phú Thọ", "25");
        register("Phú Yên", "54");
        register("Quảng Bình", "44");
        register("Quảng Nam", "49");
        register("Quảng Ngãi", "51");
        register("Quảng Ninh", "22");
        register("Quảng Trị", "45");
        register("Sóc Trăng", "94");
        register("Sơn La", "14");
        register("Tây Ninh", "72");
        register("Thái Bình", "34");
        register("Thái Nguyên", "19");
        register("Thanh Hóa", "38");
        register("Thành phố Hồ Chí Minh", "79");
        register("Hồ Chí Minh", "79");
        register("Thừa Thiên Huế", "46");
        register("Huế", "46");
        register("Tiền Giang", "82");
        register("Trà Vinh", "84");
        register("Tuyên Quang", "08");
        register("Vĩnh Long", "86");
        register("Vĩnh Phúc", "26");
        register("Yên Bái", "15");
    }

    private VietnamProvinceUtils() {
    }

    private static void register(String name, String code) {
        NAME_TO_CODE.put(normalizeProvinceName(name).toLowerCase(Locale.ROOT), code);
    }

    public static String normalizeProvinceName(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replaceAll("^(Thành phố|Tỉnh|TP\\.?)\\s+", "").trim();
    }

    public static String toProvinceSlug(String raw) {
        String normalized = normalizeProvinceName(raw);
        if (normalized.isEmpty()) {
            return "";
        }

        String ascii = Normalizer.normalize(normalized, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT);

        return ascii.replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
    }

    public static String resolveProvinceCode(String provinceCode, String location, String provinceSlug) {
        if (provinceCode != null && !provinceCode.isBlank()) {
            return provinceCode.trim();
        }
        if (location != null && !location.isBlank()) {
            String code = NAME_TO_CODE.get(normalizeProvinceName(location).toLowerCase(Locale.ROOT));
            if (code != null) {
                return code;
            }
        }
        if (provinceSlug != null && !provinceSlug.isBlank()) {
            for (Map.Entry<String, String> entry : NAME_TO_CODE.entrySet()) {
                if (toProvinceSlug(entry.getKey()).equals(provinceSlug)) {
                    return entry.getValue();
                }
            }
        }
        return "";
    }

    public static String slugFromStateName(String stateName) {
        return toProvinceSlug(stateName);
    }

    public static String codeFromStateName(String stateName) {
        if (stateName == null || stateName.isBlank()) {
            return "";
        }
        return NAME_TO_CODE.getOrDefault(
                normalizeProvinceName(stateName).toLowerCase(Locale.ROOT),
                "");
    }
}
