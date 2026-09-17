package com.nailic.sproochencoach.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ListeningPromptContractTest {
    @Test
    void listeningTaskUsesLuxembourgishWithEnglishTranslation() throws Exception {
        String prompt = Files.readString(Path.of("src/main/resources/prompts/listening-generation.txt"));

        assertThat(prompt)
                .contains("\"hint\" must contain the learner-facing Luxembourgish comprehension question")
                .contains("\"hintTranslation\" must contain the English translation of \"hint\"")
                .doesNotContain("\"hintTranslation\" must exactly match \"hint\"");
    }
}
