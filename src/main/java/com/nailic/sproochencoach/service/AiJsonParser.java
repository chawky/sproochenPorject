package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.exceptions.AiProviderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class AiJsonParser {
    private static final Logger log = LoggerFactory.getLogger(AiJsonParser.class);

    private final ObjectMapper objectMapper;

    public AiJsonParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public <T> T parseObject(String content, Class<T> responseType, String responseName) {
        if (!StringUtils.hasText(content)) {
            log.error("AI provider returned blank {} JSON", responseName);
            throw invalidJson(responseName);
        }

        try {
            return objectMapper.readValue(content, responseType);
        } catch (JacksonException directParseException) {
            String jsonObject = extractFirstJsonObject(content);

            if (jsonObject == null || jsonObject.equals(content)) {
                log.error("Failed to parse AI provider {} JSON. contentLength={}, jacksonMessage={}", responseName, content.length(), directParseException.getMessage());
                throw invalidJson(responseName);
            }

            try {
                log.debug("Extracted JSON object from AI provider {} response. originalLength={}, extractedLength={}", responseName, content.length(), jsonObject.length());
                return objectMapper.readValue(jsonObject, responseType);
            } catch (JacksonException extractedParseException) {
                log.error("Failed to parse extracted AI provider {} JSON. contentLength={}, extractedLength={}, jacksonMessage={}", responseName, content.length(), jsonObject.length(), extractedParseException.getMessage());
                throw invalidJson(responseName);
            }
        }
    }

    private String extractFirstJsonObject(String content) {
        int start = content.indexOf('{');
        if (start < 0) {
            return null;
        }

        boolean insideString = false;
        boolean escaping = false;
        int depth = 0;

        for (int index = start; index < content.length(); index++) {
            char character = content.charAt(index);

            if (escaping) {
                escaping = false;
                continue;
            }

            if (insideString && character == '\\') {
                escaping = true;
                continue;
            }

            if (character == '"') {
                insideString = !insideString;
                continue;
            }

            if (insideString) {
                continue;
            }

            if (character == '{') {
                depth++;
            } else if (character == '}') {
                depth--;
                if (depth == 0) {
                    return content.substring(start, index + 1);
                }
            }
        }

        return null;
    }

    private AiProviderException invalidJson(String responseName) {
        return new AiProviderException(
                HttpStatus.BAD_GATEWAY.value(),
                "AI provider returned invalid " + responseName + " JSON"
        );
    }
}
