package com.hcmute.careergraph.enums;

import lombok.Getter;

@Getter
public enum JobCategory {

    ENGINEER("Kỹ thuật", "Xây dựng và đổi mới các giải pháp trong các lĩnh vực kỹ thuật khác nhau."),
    BUSINESS("Kinh doanh", "Lập kế hoạch chiến lược, quản lý nguồn lực và thúc đẩy tăng trưởng tổ chức."),
    ART_MUSIC("Nghệ thuật & Âm nhạc", "Thể hiện sự sáng tạo thông qua nghệ thuật thị giác, biểu diễn và âm nhạc."),
    ADMINISTRATION("Hành chính", "Hỗ trợ hoạt động hàng ngày với tổ chức và phối hợp."),
    SALES("Bán hàng", "Quảng bá sản phẩm, xây dựng mối quan hệ và đạt được doanh thu."),
    EDUCATION("Giáo dục", "Hướng dẫn và truyền cảm hứng cho người học ở các độ tuổi khác nhau."),
    CUSTOMER_SERVICE("Chăm sóc khách hàng", "Hỗ trợ khách hàng, giải quyết vấn đề và đảm bảo sự hài lòng của khách hàng."),
    MANUFACTURING("Sản xuất", "Sản xuất hàng hóa hiệu quả với kiểm soát chất lượng và đổi mới.");

    private final String displayName;
    private final String description;

    JobCategory(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
}
