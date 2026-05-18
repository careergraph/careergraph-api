package com.hcmute.careergraph.enums.job;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum JobCategory {

    ENGINEER("Ky thuat", "Xay dung va doi moi cac giai phap ky thuat.", "Engineer"),
    BUSINESS("Kinh doanh", "Lap ke hoach chien luoc va thuc day tang truong.", "Business"),
    ART_MUSIC("Nghe thuat va Am nhac", "Sang tao qua nghe thuat thi giac, bieu dien va am nhac.", "Art & Music"),
    ADMINISTRATION("Hanh chinh", "Ho tro van hanh hang ngay voi to chuc va phoi hop.", "Administration"),
    SALES("Ban hang", "Quang ba san pham, xay dung quan he va dat doanh thu.", "Sales"),
    EDUCATION("Giao duc", "Huong dan va truyen cam hung cho nguoi hoc.", "Education"),
    CUSTOMER_SERVICE("Cham soc khach hang", "Ho tro khach hang va dam bao su hai long.", "Customer Service"),
    MANUFACTURING("San xuat", "San xuat hang hoa hieu qua voi kiem soat chat luong.", "Manufacturing"),

    TECHNOLOGY("Cong nghe", "Phat trien phan mem, ha tang, du lieu va san pham so.", "Technology"),
    MARKETING("Marketing", "Xay dung thuong hieu, trien khai chien dich va toi uu chuyen doi.", "Marketing"),
    FINANCE("Tai chinh", "Quan tri tai chinh, ke toan va phan tich dau tu.", "Finance"),
    HEALTHCARE("Y te", "Cham soc suc khoe, van hanh dich vu y te va ho tro benh nhan.", "Healthcare"),
    HUMAN_RESOURCES("Nhan su", "Thu hut nhan tai, phat trien to chuc va quan ly hieu suat.", "Human Resources"),
    DESIGN("Thiet ke", "Thiet ke trai nghiem, giao dien va nhan dien thuong hieu.", "Design");

    private final String displayName;
    private final String description;
    private final String englishLabel;

    JobCategory(String displayName, String description, String englishLabel) {
        this.displayName = displayName;
        this.description = description;
        this.englishLabel = englishLabel;
    }

    @JsonValue
    public String getEnglishLabel() {
        return englishLabel;
    }

    public String getCode() {
        return name();
    }

    @JsonCreator
    public static JobCategory fromValue(String value) {
        if (value == null) {
            return null;
        }
        try {
            return JobCategory.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            for (JobCategory category : JobCategory.values()) {
                if (category.englishLabel.equalsIgnoreCase(value)) {
                    return category;
                }
            }
            throw new IllegalArgumentException("Invalid JobCategory: " + value);
        }
    }
}

