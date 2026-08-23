package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.dto.ExerciseRequestDto;
import com.nailic.sproochencoach.dto.GeneratedImageDto;
import com.nailic.sproochencoach.dto.SpeakingEvaluation;
import com.nailic.sproochencoach.exceptions.AiProviderException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class ImageDescriptionService {

    private static final Logger log = LoggerFactory.getLogger(ImageDescriptionService.class);

    @Value("${ai.prompts.image-generation}")
    private Resource imageGenerationPromptResource;

    @Value("${ai.prompts.image-description-evaluation}")
    private Resource imageDescriptionEvaluationPromptResource;

    private final PromptFileService promptFileService;
    private final AiChatClient aiChatClient;
    private final AiImageClient aiImageClient;
    private final SpeakingService speakingService;
    private final ObjectMapper objectMapper;
    private String imageDescriptionPrompt;
    private String imageDescriptionEvaluationPrompt;

    public ImageDescriptionService(
            AiChatClient aiChatClient,
            AiImageClient aiImageClient,
            PromptFileService promptFileService,
            SpeakingService speakingService,
            ObjectMapper objectMapper
    ) {
        this.promptFileService = promptFileService;
        this.aiChatClient = aiChatClient;
        this.aiImageClient = aiImageClient;
        this.speakingService = speakingService;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void loadPromptFiles() {
        imageDescriptionPrompt = promptFileService.read(imageGenerationPromptResource);
        imageDescriptionEvaluationPrompt = promptFileService.read(imageDescriptionEvaluationPromptResource);
        log.debug(
                "Image description prompts loaded. generationCharacters={}, evaluationCharacters={}",
                imageDescriptionPrompt.length(),
                imageDescriptionEvaluationPrompt.length()
        );
    }


    public GeneratedImageDto generateImage(ExerciseRequestDto request) {
        log.debug(
                "Generating image description exercise. level={}, topic={}",
                request.getLevel(),
                request.getTopic()
        );

        String promptInstruction = imageDescriptionPrompt.formatted(
                request.getLevel(),
                request.getTopic()
        );

        String imageDescription = aiChatClient.complete(
                promptInstruction,
                "image description prompt"
        );

        GeneratedImageDto generatedImageDto = new GeneratedImageDto();
        generatedImageDto.setImage(aiImageClient.generateImage(imageDescription));
        generatedImageDto.setImageDescription(imageDescription);
        return generatedImageDto;
    }

    public SpeakingEvaluation generateEvaluation(MultipartFile audio, String imageDescription) {
        log.debug(
                "Generating image description evaluation. audioName={}, audioSize={}, imageDescriptionLength={}",
                audio.getOriginalFilename(),
                audio.getSize(),
                imageDescription == null ? 0 : imageDescription.length()
        );

        String transcription = speakingService.transcribeAudio(audio);

        String content = aiChatClient.complete(
                imageDescriptionEvaluationPrompt.formatted(imageDescription, transcription),
                "image description evaluation"
        );

        try {
            SpeakingEvaluation evaluation = objectMapper.readValue(
                    content,
                    SpeakingEvaluation.class
            );
            log.debug(
                    "Image description evaluation parsed successfully. score={}, corrections={}",
                    evaluation.getScore(),
                    evaluation.getCorrections() == null ? 0 : evaluation.getCorrections().size()
            );
            return evaluation;
        } catch (JacksonException exception) {
            log.error(
                    "Failed to parse AI provider image description evaluation JSON. jacksonMessage={}, aiReturned={}",
                    exception.getMessage(),
                    content,
                    exception
            );

            throw new AiProviderException(
                    HttpStatus.BAD_GATEWAY.value(),
                    "AI provider returned invalid image description evaluation JSON"
            );
        }
    }
}
