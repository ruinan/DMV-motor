package com.dmvmotor.api.content.infrastructure;

import com.dmvmotor.api.content.domain.Choice;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QuestionRepositoryTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void parseChoices_validJson_returnsList() {
        String json = "[{\"key\":\"A\",\"text\":\"Stop\"},{\"key\":\"B\",\"text\":\"Go\"}]";

        List<Choice> choices = QuestionRepository.parseChoices(objectMapper, json);

        assertEquals(2, choices.size());
        assertEquals("A", choices.get(0).key());
        assertEquals("Stop", choices.get(0).text());
    }

    @Test
    void parseChoices_invalidJson_throwsIllegalState() {
        String json = "not-valid-json";

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> QuestionRepository.parseChoices(objectMapper, json));

        assertTrue(ex.getMessage().contains("Failed to parse choices_payload"));
    }
}
