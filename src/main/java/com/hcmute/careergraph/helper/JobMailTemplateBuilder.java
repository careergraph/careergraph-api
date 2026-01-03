package com.hcmute.careergraph.helper;

import com.hcmute.careergraph.persistence.models.Job;

import java.util.List;

public class JobMailTemplateBuilder {

    public static String build(List<Job> jobs, String baseUrl) {

        StringBuilder sb = new StringBuilder("""
        <html>
        <body style="font-family:Arial,sans-serif">
            <h2 style="color:#ff5722">🔥 Việc làm phù hợp với bạn</h2>
        """);

        for (Job j : jobs) {

            String jobLink = baseUrl + "/jobs/" + j.getId();

            sb.append("""
            <div style="border:1px solid #ddd;padding:12px;margin-bottom:12px">
                <h3>%s</h3>
                <p><b>Công ty:</b> %s</p>
                <p><b>Lương:</b> %s</p>
                <p><b>Địa điểm:</b> %s</p>

                <a href="%s"
                   target="_blank"
                   style="
                       display:inline-block;
                       margin-top:8px;
                       padding:8px 14px;
                       background:#1976d2;
                       color:#fff;
                       text-decoration:none;
                       border-radius:4px">
                   🔎 Xem chi tiết
                </a>
            </div>
            """.formatted(
                    j.getTitle(),
                    j.getCompany().getName(),
                    j.getSalaryRange(),
                    j.getState(),
                    jobLink
            ));
        }

        sb.append("""
            <hr/>
            <p style="font-size:12px;color:#888">
                Bạn nhận email này vì đã bật thông báo việc làm.
                <a href="%s/settings/notification">Tắt thông báo</a>
            </p>
        </body>
        </html>
        """.formatted(baseUrl));

        return sb.toString();
    }
}
