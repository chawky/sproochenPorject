package com.nailic.sproochencoach.dto;

import com.nailic.sproochencoach.constants.AppConstants;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

class TtsRequestTest {
    @Test
    void serializesElevenV3WithLuxembourgishLanguageCode() throws Exception {
        TtsRequest request = new TtsRequest("Moien", AppConstants.Models.ELEVEN_V3, "lb");

        String json = new ObjectMapper().writeValueAsString(request);

        assertThat(json).contains(
                "\"model_id\":\"eleven_v3\"",
                "\"language_code\":\"lb\""
        );
    }
}
