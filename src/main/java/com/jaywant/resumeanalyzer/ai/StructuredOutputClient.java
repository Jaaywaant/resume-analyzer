package com.jaywant.resumeanalyzer.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

/**
 * Calls the LLM, converts the response into a typed object, validates it,
 * and retries once when JSON parsing or schema validation fails.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StructuredOutputClient {

    private final ChatClient.Builder chatClientBuilder;

    public <T> T generate(String prompt, Class<T> type, Consumer<String> rawJsonValidator, Consumer<T> entityValidator) {
        BeanOutputConverter<T> converter = new BeanOutputConverter<>(type);
        try {
            return attempt(prompt, converter, type, rawJsonValidator, entityValidator);
        }
        catch (StructuredOutputException firstFailure) {
            log.warn("Structured output attempt failed for {}; retrying once: {}", type.getSimpleName(), firstFailure.getMessage());
            String retryPrompt = prompt
                    + "\n\nPREVIOUS RESPONSE WAS INVALID: "
                    + firstFailure.getMessage()
                    + "\nReturn ONLY corrected valid JSON that matches the schema. No markdown fences, no commentary.";
            try {
                return attempt(retryPrompt, converter, type, rawJsonValidator, entityValidator);
            }
            catch (StructuredOutputException secondFailure) {
                throw new StructuredOutputException(
                        "Could not get valid structured output for "
                                + type.getSimpleName()
                                + " after retry. Last error: "
                                + secondFailure.getMessage(),
                        secondFailure);
            }
        }
    }

    private <T> T attempt(
            String prompt,
            BeanOutputConverter<T> converter,
            Class<T> type,
            Consumer<String> rawJsonValidator,
            Consumer<T> entityValidator) {
        String content;
        try {
            content = chatClientBuilder.build()
                    .prompt(prompt)
                    .call()
                    .content();
        }
        catch (Exception ex) {
            throw new StructuredOutputException("LLM call failed: " + ex.getMessage(), ex);
        }

        if (content == null || content.isBlank()) {
            throw new StructuredOutputException("LLM returned an empty response");
        }

        String json = extractJson(content);
        rawJsonValidator.accept(json);

        T parsed;
        try {
            parsed = converter.convert(json);
        }
        catch (Exception ex) {
            throw new StructuredOutputException(
                    "Failed to map LLM JSON to " + type.getSimpleName() + ": " + ex.getMessage(),
                    ex);
        }

        if (parsed == null) {
            throw new StructuredOutputException("LLM JSON mapped to null " + type.getSimpleName());
        }

        entityValidator.accept(parsed);
        return parsed;
    }

    /**
     * Strips optional markdown code fences some models wrap around JSON.
     */
    static String extractJson(String content) {
        String trimmed = content.strip();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline > 0) {
                trimmed = trimmed.substring(firstNewline + 1);
            }
            int fence = trimmed.lastIndexOf("```");
            if (fence >= 0) {
                trimmed = trimmed.substring(0, fence);
            }
            trimmed = trimmed.strip();
        }

        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return trimmed;
    }
}
