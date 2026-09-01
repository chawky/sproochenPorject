package com.nailic.sproochencoach.service;

import org.springframework.core.io.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class PromptFileService {
    private static final Logger log = LoggerFactory.getLogger(PromptFileService.class);

    public String read(Resource resource) {
        try {
            String content = resource.getContentAsString(StandardCharsets.UTF_8).strip();
            return content;
        } catch (IOException exception) {
            log.error("Failed to load prompt resource {}", resource, exception);
            throw new IllegalStateException("Failed to load prompt file: " + resource, exception);
        }
    }
}
