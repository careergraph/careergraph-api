package com.hcmute.careergraph.helper;


import com.hcmute.careergraph.persistence.models.Job;

import java.util.List;

public class JobMailTemplateBuilder {

    public static String build(List<Job> jobs) {

        StringBuilder sb = new StringBuilder("""
        <html><body>
        <h2>🔥 Việc làm phù hợp với bạn</h2>
        """);

        for (Job j : jobs) {
            sb.append("""
            <div style="border:1px solid #ddd;padding:12px;margin-bottom:10px">
                <h3>%s</h3>
                <p><b>Công ty:</b> %s</p>
                <p><b>Lương:</b> %s</p>
                <p><b>Địa điểm:</b> %s</p>
            </div>
            """.formatted(
                    j.getTitle(),
                    j.getCompany().getName(),
                    j.getSalaryRange(),
                    j.getState()
            ));
        }

        sb.append("</body></html>");
        return sb.toString();
    }
}
