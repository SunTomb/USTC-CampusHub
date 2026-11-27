package com.campushub.file;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campushub.auth.JwtTokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FileControllerSecurityIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenService tokens;

    @Test
    void onlyAdminCanListFileMetadata() throws Exception {
        mockMvc.perform(get("/api/files").header("Authorization", bearer(1L, "alice")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/files").header("Authorization", bearer(4L, "admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].originalName").isString())
                .andExpect(jsonPath("$..storagePath").doesNotHaveJsonPath());
    }

    @Test
    void strangerCannotReadFileMetadata() throws Exception {
        mockMvc.perform(get("/api/files/1").header("Authorization", bearer(2L, "bob")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("当前账号无权限执行此操作"));
    }

    @ParameterizedTest
    @CsvSource({"1,alice", "4,admin"})
    void uploaderAndAdminCanReadFileWithoutStoragePath(long actorId, String username) throws Exception {
        mockMvc.perform(get("/api/files/1").header("Authorization", bearer(actorId, username)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.storagePath").doesNotHaveJsonPath());
    }

    @ParameterizedTest
    @CsvSource({"GOODS,1", "GOODS,-1", "UNKNOWN,1"})
    void unauthorizedBindingReadsDoNotRevealTargetDetails(String type, long id) throws Exception {
        mockMvc.perform(get("/api/files/{type}/{id}/bindings", type, id)
                        .header("Authorization", bearer(2L, "bob")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("当前账号无权限执行此操作"));
    }

    @ParameterizedTest
    @CsvSource({"1,alice", "4,admin"})
    void targetOwnerAndAdminCanReadBindingsWithoutStoragePath(long actorId, String username) throws Exception {
        mockMvc.perform(get("/api/files/GOODS/1/bindings").header("Authorization", bearer(actorId, username)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].file.id").value(1))
                .andExpect(jsonPath("$..storagePath").doesNotHaveJsonPath());
    }

    private String bearer(long id, String username) {
        return "Bearer " + tokens.issueToken(id, username);
    }
}
