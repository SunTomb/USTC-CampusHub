package com.campushub.ops;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Transactional
class OperationsAnalyticsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsOverviewAnalytics() throws Exception {
        mockMvc.perform(get("/api/admin/ops/analytics/overview")
                        .param("startDate", "2020-01-01")
                        .param("endDate", "2026-05-22"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.newUsers").exists())
                .andExpect(jsonPath("$.data.cards").isArray());
    }

    @Test
    void rejectsInvalidDateRange() throws Exception {
        mockMvc.perform(get("/api/admin/ops/analytics/overview")
                        .param("startDate", "2026-05-22")
                        .param("endDate", "2026-05-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("结束日期不能早于开始日期"));
    }

    @Test
    void returnsAnalyticsSmokeResponses() throws Exception {
        mockMvc.perform(get("/api/admin/ops/analytics/funnels")
                        .param("startDate", "2020-01-01")
                        .param("endDate", "2026-05-22"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.funnels").isArray());

        mockMvc.perform(get("/api/admin/ops/analytics/zones")
                        .param("startDate", "2020-01-01")
                        .param("endDate", "2026-05-22"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.taskOriginZones").isArray())
                .andExpect(jsonPath("$.data.projectAdZones").isArray());

        mockMvc.perform(get("/api/admin/ops/analytics/fees")
                        .param("startDate", "2020-01-01")
                        .param("endDate", "2026-05-22"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.serviceFeeCount").exists())
                .andExpect(jsonPath("$.data.roleApplicationCount").exists());
    }

    @Test
    void returnsCsvExportWithDownloadHeaders() throws Exception {
        mockMvc.perform(get("/api/admin/ops/exports/goods.csv")
                        .param("startDate", "2020-01-01")
                        .param("endDate", "2026-05-22"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.valueOf("text/csv")))
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(content().string(containsString("联系方式开放")));

        mockMvc.perform(get("/api/admin/ops/exports/fees.csv")
                        .param("startDate", "2020-01-01")
                        .param("endDate", "2026-05-22"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.valueOf("text/csv")))
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(content().string(containsString("分类")));
    }
}
