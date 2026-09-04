package com.nailic.sproochencoach.controller;

import com.nailic.sproochencoach.dto.ExerciseRequestDto;
import com.nailic.sproochencoach.exceptions.AiQuotaExceededException;
import com.nailic.sproochencoach.exceptions.GlobalExceptionHandler;
import com.nailic.sproochencoach.service.ExerciseService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ExerciseControllerQuotaTest {
    @Test
    void returnsTooManyRequestsWhenQuotaIsExceeded() throws Exception {
        ExerciseService exerciseService = mock(ExerciseService.class);
        when(exerciseService.generateExercise(any(ExerciseRequestDto.class)))
                .thenThrow(new AiQuotaExceededException("Daily AI limit reached for chat"));

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new ExerciseController(exerciseService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(post("/api/exercises/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "level": "A1",
                                  "topic": "FAMILY",
                                  "type": "MULTIPLE_CHOICE"
                                }
                                """))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Daily AI limit reached for chat"));
    }
}
