package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.dto.AiQuotaFeatureStatusDto;
import com.nailic.sproochencoach.model.AppUser;
import com.nailic.sproochencoach.model.ExerciseAttempt;
import com.nailic.sproochencoach.model.ExerciseAttemptStatus;
import com.nailic.sproochencoach.repository.AppUserRepo;
import com.nailic.sproochencoach.repository.ExerciseAttemptRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExerciseRetryIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private AppUserRepo users;
    @Autowired private ExerciseAttemptRepo attempts;
    @MockitoBean private AiChatClient chat;
    @MockitoBean private AiImageClient images;
    @MockitoBean private AudioExerciseGenerationService audioGeneration;
    @MockitoBean private SpeechTranscriptionRouter transcription;
    @MockitoBean private AiRateLimitService rateLimit;
    private AppUser owner;

    @BeforeEach
    void createOwner() {
        owner = new AppUser();
        owner.setUsername("retry-" + System.nanoTime());
        owner.setEmail(owner.getUsername() + "@example.com");
        owner.setPassword("test");
        owner.setEnabled(true);
        owner = users.saveAndFlush(owner);
    }

    @ParameterizedTest
    @ValueSource(strings = {"SPEAKING", "IMAGE_DESCRIPTION"})
    void retriesPreserveHistoryAndQuotaAndCanBeEvaluated(String type) throws Exception {
        ExerciseAttempt original = evaluated(type);
        AiQuotaFeatureStatusDto before = quota(type);
        long retryId = retry(original.getId());
        assertThat(retryId).isNotEqualTo(original.getId());
        ExerciseAttempt fresh = attempts.findById(retryId).orElseThrow();
        assertThat(fresh.getRetryOfAttemptId()).isEqualTo(original.getId());
        assertThat(fresh.getUser().getId()).isEqualTo(owner.getId());
        assertThat(fresh).extracting("exerciseType", "exerciseName", "level", "topic", "answerType")
                .containsExactly(type, original.getExerciseName(), "A1", "FAMILY", "SHORT_ANSWER");
        assertThat(fresh.getStatus()).isEqualTo(ExerciseAttemptStatus.GENERATED);
        assertThat(fresh.getGeneratedAt()).isAfter(original.getGeneratedAt());
        assertThat(fresh).extracting("learnerAnswer", "averageRatingOverall", "evaluatedAt", "completedAt", "evaluationClaimedAt")
                .containsOnlyNulls();
        assertOriginalUnchanged(original);
        assertThat(quota(type)).usingRecursiveComparison().isEqualTo(before);
        verifyNoInteractions(chat, images, audioGeneration, transcription, rateLimit);

        when(transcription.transcribeForEvaluation(any(), any(), eq(retryId), eq(type))).thenReturn("Moien");
        when(chat.complete(anyString(), anyString())).thenReturn(
                "{\"transcript\":\"Moien\",\"score\":80,\"feedback\":\"Good\",\"corrections\":[]}");
        String endpoint = type.equals("SPEAKING") ? "/api/exercises/recording"
                : "/api/exercises/image-description/recording";
        mvc.perform(multipart(endpoint)
                        .file(new MockMultipartFile("audio", "recording.webm", "audio/webm", new byte[]{1}))
                        .param("attemptId", Long.toString(retryId)).param("imageDescription", "Same image")
                        .with(user(owner)).with(csrf()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.score").value(80));
        assertThat(attempts.findById(retryId).orElseThrow().getStatus()).isEqualTo(ExerciseAttemptStatus.EVALUATED);
        assertOriginalUnchanged(original);

        clearInvocations(chat, transcription);
        mvc.perform(multipart(endpoint)
                        .file(new MockMultipartFile("audio", "recording.webm", "audio/webm", new byte[]{1}))
                        .param("attemptId", original.getId().toString()).param("imageDescription", "Same image")
                        .with(user(owner)).with(csrf()))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(chat, transcription);

        long nextRetryId = retry(retryId);
        assertThat(attempts.findById(nextRetryId).orElseThrow().getRetryOfAttemptId()).isEqualTo(retryId);
        assertThat(quota(type)).usingRecursiveComparison().isEqualTo(before);
    }

    @Test
    void retryStillWorksWhenGenerationQuotaIsExhausted() throws Exception {
        ExerciseAttempt original = evaluated("IMAGE_DESCRIPTION");
        for (int i = 0; i < 4; i++) evaluated("IMAGE_DESCRIPTION");
        assertThat(quota("IMAGE_DESCRIPTION").getRemaining()).isZero();
        retry(original.getId());
        assertThat(quota("IMAGE_DESCRIPTION").getUsed()).isEqualTo(5);
        verifyNoInteractions(chat, images, audioGeneration, transcription, rateLimit);
    }

    @ParameterizedTest
    @ValueSource(strings = {"LISTENING", "TEXT_EXERCISE", "VOCABULARY"})
    void rejectsUnsupportedTypes(String type) throws Exception {
        reject(evaluated(type).getId());
    }

    @ParameterizedTest
    @EnumSource(value = ExerciseAttemptStatus.class, names = {"GENERATED", "EVALUATING", "COMPLETED"})
    void rejectsNonEvaluatedStatuses(ExerciseAttemptStatus state) throws Exception {
        ExerciseAttempt original = evaluated("SPEAKING");
        original.setStatus(state);
        attempts.saveAndFlush(original);
        reject(original.getId());
    }

    @Test
    void rejectsMissingAndForeignAttempts() throws Exception {
        reject(Long.MAX_VALUE);
        ExerciseAttempt foreign = evaluated("SPEAKING");
        createOwner();
        reject(foreign.getId());
    }

    @Test
    void cookieRequestRequiresCsrf() throws Exception {
        ExerciseAttempt original = evaluated("SPEAKING");
        mvc.perform(post("/api/exercises/{id}/retry", original.getId()).with(user(owner)))
                .andExpect(status().isForbidden());
        verifyNoInteractions(chat, images, audioGeneration, transcription, rateLimit);
    }

    private void reject(long id) throws Exception {
        long count = attempts.count();
        mvc.perform(post("/api/exercises/{id}/retry", id).with(user(owner)).with(csrf()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false));
        assertThat(attempts.count()).isEqualTo(count);
        verifyNoInteractions(chat, images, audioGeneration, transcription, rateLimit);
    }

    private long retry(long id) throws Exception {
        String response = mvc.perform(post("/api/exercises/{id}/retry", id).with(user(owner)).with(csrf()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.retryOfAttemptId").value(id))
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(response).path("data").path("attemptId").asLong();
    }

    private AiQuotaFeatureStatusDto quota(String type) throws Exception {
        String response = mvc.perform(get("/api/users/me/ai-quota").with(user(owner)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        for (var feature : mapper.readTree(response).path("data").path("features")) {
            if (feature.path("feature").asText().equals(type)) {
                return mapper.treeToValue(feature, AiQuotaFeatureStatusDto.class);
            }
        }
        throw new AssertionError("Missing quota feature " + type);
    }

    private ExerciseAttempt evaluated(String type) {
        ExerciseAttempt attempt = new ExerciseAttempt();
        attempt.setUser(owner);
        attempt.setExerciseType(type);
        attempt.setExerciseName("A1 FAMILY SHORT_ANSWER");
        attempt.setLevel("A1");
        attempt.setTopic("FAMILY");
        attempt.setAnswerType("SHORT_ANSWER");
        attempt.setStatus(ExerciseAttemptStatus.EVALUATED);
        attempt.setGeneratedAt(LocalDateTime.now().minusSeconds(1).withNano(0));
        attempt.setCompletedAt(LocalDateTime.now().minusSeconds(1).withNano(0));
        attempt.setEvaluatedAt(attempt.getCompletedAt());
        attempt.setLearnerAnswer("Original transcript");
        attempt.setAverageRatingOverall(65.0);
        return attempts.saveAndFlush(attempt);
    }

    private void assertOriginalUnchanged(ExerciseAttempt original) {
        assertThat(attempts.findById(original.getId()).orElseThrow())
                .usingRecursiveComparison().ignoringFields("user").isEqualTo(original);
    }
}
