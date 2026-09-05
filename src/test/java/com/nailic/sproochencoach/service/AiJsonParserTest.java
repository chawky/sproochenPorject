package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.dto.GeneratedExerciseDto;
import com.nailic.sproochencoach.exceptions.AiProviderException;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiJsonParserTest {
    private final AiJsonParser parser = new AiJsonParser(new ObjectMapper());

    @Test
    void parsesStrictJsonObject() {
        GeneratedExerciseDto exercise = parser.parseObject(
                """
                        {
                          "question": "Wéi heeschs du?",
                          "type": "SHORT_ANSWER",
                          "options": [],
                          "expectedAnswer": "Ech heesche Sam.",
                          "hint": "Sot däin Numm."
                        }
                        """,
                GeneratedExerciseDto.class,
                "exercise"
        );

        assertThat(exercise.getQuestion()).isEqualTo("Wéi heeschs du?");
        assertThat(exercise.getOptions()).isEmpty();
    }

    @Test
    void extractsJsonObjectFromProviderWrapperText() {
        GeneratedExerciseDto exercise = parser.parseObject(
                """
                        ```json
                        {
                          "question": "Wou wunns du?",
                          "type": "SHORT_ANSWER",
                          "options": [],
                          "expectedAnswer": "Ech wunnen zu Esch.",
                          "hint": "Sot deng Stad."
                        }
                        ```
                        """,
                GeneratedExerciseDto.class,
                "exercise"
        );

        assertThat(exercise.getQuestion()).isEqualTo("Wou wunns du?");
    }

    @Test
    void keepsBracesInsideStringsWhileExtracting() {
        GeneratedExerciseDto exercise = parser.parseObject(
                """
                        Here is the JSON:
                        {
                          "question": "Fëll d'Lück: Ech {wunnen} zu Lëtzebuerg.",
                          "type": "FILL_IN_THE_BLANK",
                          "options": [],
                          "expectedAnswer": "wunnen",
                          "hint": "Kuck op d'Verb."
                        }
                        """,
                GeneratedExerciseDto.class,
                "exercise"
        );

        assertThat(exercise.getQuestion()).contains("{wunnen}");
    }

    @Test
    void throwsProviderExceptionForInvalidJson() {
        assertThatThrownBy(() -> parser.parseObject("not json", GeneratedExerciseDto.class, "exercise"))
                .isInstanceOf(AiProviderException.class)
                .hasMessage("AI provider returned invalid exercise JSON");
    }
}
