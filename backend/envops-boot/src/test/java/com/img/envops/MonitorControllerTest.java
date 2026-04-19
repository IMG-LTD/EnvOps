package com.img.envops;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@TestPropertySource(properties = {
    "envops.security.token-secret=test-only-envops-token-secret-12345",
    "envops.security.credential-protection-secret=test-only-envops-credential-protection-secret-12345"
})
class MonitorControllerTest {
  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  void createDetectTaskReturnsCreatedTaskAndGetDetectTasksShowsItFirst() throws Exception {
    String accessToken = login();

    mockMvc.perform(post("/api/monitor/detect-tasks")
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "taskName": "host-disk-threshold",
                  "hostId": 1,
                  "schedule": "every_5m"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.id").isNumber())
        .andExpect(jsonPath("$.data.taskName").value("host-disk-threshold"))
        .andExpect(jsonPath("$.data.hostId").value(1))
        .andExpect(jsonPath("$.data.target").value("host-prd-01"))
        .andExpect(jsonPath("$.data.schedule").value("every_5m"))
        .andExpect(jsonPath("$.data.lastResult").value("pending"))
        .andExpect(jsonPath("$.data.createdAt").exists());

    mockMvc.perform(get("/api/monitor/detect-tasks")
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.length()").value(greaterThanOrEqualTo(3)))
        .andExpect(jsonPath("$.data[0].taskName").value("host-disk-threshold"))
        .andExpect(jsonPath("$.data[0].hostId").value(1))
        .andExpect(jsonPath("$.data[0].target").value("host-prd-01"))
        .andExpect(jsonPath("$.data[0].schedule").value("every_5m"))
        .andExpect(jsonPath("$.data[0].lastResult").value("pending"));
  }

  @Test
  void executeDetectTaskUpdatesLastResultAndWritesLatestHostFact() throws Exception {
    String accessToken = login();

    mockMvc.perform(post("/api/monitor/detect-tasks/{id}/execute", 1)
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.id").value(1))
        .andExpect(jsonPath("$.data.hostId").value(1))
        .andExpect(jsonPath("$.data.lastResult").value("success"))
        .andExpect(jsonPath("$.data.lastRunAt").exists());

    mockMvc.perform(get("/api/monitor/hosts/{hostId}/facts/latest", 1)
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.hostId").value(1))
        .andExpect(jsonPath("$.data.hostName").value("host-prd-01"))
        .andExpect(jsonPath("$.data.kernelVersion").value("5.15.0-107-generic"))
        .andExpect(jsonPath("$.data.agentVersion").value("1.0.3"));
  }

  @Test
  void executeDetectTaskReturnsFailureWhenExecutorProbeFails() throws Exception {
    String accessToken = login();

    mockMvc.perform(post("/api/monitor/detect-tasks")
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "taskName": "sandbox-probe",
                  "hostId": 3,
                  "schedule": "manual"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"));

    mockMvc.perform(post("/api/monitor/detect-tasks/{id}/execute", 3)
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.code").value("500"))
        .andExpect(jsonPath("$.msg").value("Internal server error"));

    mockMvc.perform(get("/api/monitor/detect-tasks")
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data[0].id").value(3))
        .andExpect(jsonPath("$.data[0].lastResult").value("failed"))
        .andExpect(jsonPath("$.data[0].lastRunAt").exists());
  }

  @Test
  void getLatestHostFactReturnsNewestFactForHost() throws Exception {
    String accessToken = login();

    mockMvc.perform(get("/api/monitor/hosts/{hostId}/facts/latest", 1)
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.hostId").value(1))
        .andExpect(jsonPath("$.data.hostName").value("host-prd-01"))
        .andExpect(jsonPath("$.data.osName").value("Ubuntu 22.04.4 LTS"))
        .andExpect(jsonPath("$.data.kernelVersion").value("5.15.0-106-generic"))
        .andExpect(jsonPath("$.data.cpuCores").value(8))
        .andExpect(jsonPath("$.data.memoryMb").value(16384))
        .andExpect(jsonPath("$.data.agentVersion").value("1.0.1"))
        .andExpect(jsonPath("$.data.collectedAt").value("2026-04-16T08:45:00"));
  }

  private String login() throws Exception {
    MvcResult result = mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "userName": "envops-admin",
                  "password": "EnvOps@123"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.token", not(blankOrNullString())))
        .andReturn();

    JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    return data.path("token").asText();
  }
}
