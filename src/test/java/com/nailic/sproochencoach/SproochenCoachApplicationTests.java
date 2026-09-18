package com.nailic.sproochencoach;

import com.nailic.sproochencoach.service.SupportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockPart;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SproochenCoachApplicationTests {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private SupportService supportService;

  @Test
  void contextLoads() {
  }

  @Test
  void openApiDocsArePublic() throws Exception {
    mockMvc.perform(get("/v3/api-docs"))
        .andExpect(status().isOk());
  }

  @Test
  void cookieLogoutRequiresCsrfToken() throws Exception {
    mockMvc.perform(post("/api/users/logout"))
        .andExpect(status().isForbidden());

    mockMvc.perform(post("/api/users/logout").with(csrf()))
        .andExpect(status().isOk());
  }

  @Test
  void supportEndpointIsPublicWithoutCsrfToken() throws Exception {
    mockMvc.perform(multipart("/api/support")
            .part(textPart("email", "user@example.com"))
            .part(textPart("subject", "Subscription problem"))
            .part(textPart("message", "My subscription page is showing the wrong status.")))
        .andExpect(status().isOk());

    verify(supportService).sendSupportRequest(
        any(),
        any(),
        any(),
        any(),
        any()
    );
  }

  @Test
  void supportEndpointDocumentsMultipartRequestBody() throws Exception {
    mockMvc.perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paths['/api/support'].post.requestBody").exists())
        .andExpect(jsonPath("$.paths['/api/support'].post.parameters").doesNotExist());
  }

  private MockPart textPart(String name, String value) {
    return new MockPart(name, value.getBytes(StandardCharsets.UTF_8));
  }

}
