package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.dto.ExerciseRequestDto;
import com.nailic.sproochencoach.dto.SpeakingDto;
import com.nailic.sproochencoach.dto.SpeakingEvaluation;
import com.nailic.sproochencoach.dto.TtsRequest;
import com.nailic.sproochencoach.exceptions.OpenRouterError;
import com.nailic.sproochencoach.model.AIRoleEnum;
import com.nailic.sproochencoach.model.AiBody;
import com.nailic.sproochencoach.model.MessageBody;
import com.nailic.sproochencoach.model.OpenRouterResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Service
public class SpeakingService {
    @Value("${ai.openrouter.free.model}")
    private String openrouterFreeModel;

    @Value("${ai.openrouter.paid.model}")
    private String openrouterPaidModel;

    @Value("${ai.completion.uri}")
    private String completionURI;

    @Value("${ai.system.content}")
    private String systemContent;
    @Value("${ai.groq.base-url}")
    private String grokURL;
    @Value("${ai.openrouter.base-url}")
    private String baseUrl;
    @Value("${ai.elevenlabs.voice-id}")
    private String voiceId;
    private final RestClient openRouterRestClient;
    private final RestClient groqRestClient;
    private final ObjectMapper objectMapper;
    private final RestClient ttsRestClient;

    public SpeakingService(
            @Qualifier("openRouterRestClient") RestClient openRouterRestClient,
            @Qualifier("groqRestClient") RestClient groqRestClient,
            @Qualifier("ttsRestClient") RestClient ttsRestClient,
            ObjectMapper objectMapper
    ) {
        this.openRouterRestClient = openRouterRestClient;
        this.groqRestClient = groqRestClient;
        this.objectMapper = objectMapper;
        this.ttsRestClient = ttsRestClient;
    }

    public SpeakingDto generateSpeakingPrompt(ExerciseRequestDto exerciseRequestDto) {

        AiBody aiBody = new AiBody();
        aiBody.setModel(openrouterFreeModel);

        MessageBody systemMessageBody = new MessageBody();
        systemMessageBody.setRole(AIRoleEnum.SYSTEM);
        systemMessageBody.setContent(systemContent);

        MessageBody userMessageBody = new MessageBody();
        userMessageBody.setRole(AIRoleEnum.USER);
        userMessageBody.setContent("""
                Generate exactly ONE speaking prompt
                for level %s
                about the topic %s.
                
                Requirements:
                - The question MUST be written in authentic Luxembourgish (Lëtzebuergesch).
                - Use standard Luxembourgish spelling and grammar.
                - Never invent Luxembourgish words.
                - Never imitate Luxembourgish by modifying German, French, Dutch, or other languages.
                - Keep the question appropriate for the requested CEFR level.
                - Make the prompt noticeably different each time.
                - Vary vocabulary, sentence structure, verbs, time expressions, and situations.
                - Avoid always using the most obvious examples for the topic.
                
                Return ONLY strict valid JSON.
                
                Use exactly this structure:
                {
                  "question": "...",
                  "type": "%s",
                  "options": [],
                  "expectedAnswer": "...",
                  "hint": "...",
                  "questionTranslation": "..."
                }
                
                JSON rules:
                - Return only the JSON object.
                - Do not include markdown.
                - Do not use ```json code fences.
                - Do not include comments such as // or /* */.
                - Do not include trailing commas.
                - The JSON MUST be syntactically valid and parsable.
                - Every field except the last one MUST end with a comma.
                - Before responding, internally verify the JSON syntax.
                - Every property name must be inside double quotes.
                - "questionTranslation" is required and must contain the English translation of "question".
                - Do not include any text before or after the JSON.
                """
                .formatted(
                        exerciseRequestDto.getLevel(),
                        exerciseRequestDto.getTopic(),
                        exerciseRequestDto.getType()
                ));

        aiBody.setMessages(
                List.of(systemMessageBody, userMessageBody)
        );

        OpenRouterResponse openRouterResponse = openRouterRestClient.post()
                .uri(completionURI)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(aiBody)
                .retrieve()
                .onStatus(
                        HttpStatusCode::isError,
                        (request, response) -> {

                            OpenRouterResponse errorResponse =
                                    objectMapper.readValue(
                                            response.getBody(),
                                            OpenRouterResponse.class
                                    );

                            if (errorResponse.getError() == null) {
                                throw new OpenRouterError(
                                        response.getStatusCode().value(),
                                        "OpenRouter returned an unknown error"
                                );
                            }

                            throw new OpenRouterError(
                                    errorResponse.getError().getCode(),
                                    errorResponse.getError().getMessage()
                            );
                        }
                )
                .body(OpenRouterResponse.class);

        if (openRouterResponse == null
                || openRouterResponse.getChoices() == null
                || openRouterResponse.getChoices().isEmpty()
                || openRouterResponse.getChoices().get(0).getMessage() == null
                || openRouterResponse.getChoices().get(0).getMessage().getContent() == null) {

            throw new OpenRouterError(
                    HttpStatus.BAD_GATEWAY.value(),
                    "OpenRouter returned an invalid response"
            );
        }

        String content = openRouterResponse
                .getChoices()
                .get(0)
                .getMessage()
                .getContent();


        System.out.println("===== OPENROUTER EVALUATION =====");
        System.out.println(content);
        try {
            SpeakingDto result = objectMapper.readValue(
                    content,
                    SpeakingDto.class
            );
            TtsRequest ttsRequest = new TtsRequest(
                    result.getQuestion(),
                    "eleven_multilingual_v2"
            );

            byte[] audio = ttsRestClient.post()
                    .uri("/text-to-speech/" + voiceId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.valueOf("audio/mpeg"))
                    .body(ttsRequest)
                    .retrieve()
                    .body(byte[].class);
            result.setAudio(audio);
            return  result;
        } catch (JacksonException exception) {

            System.out.println("===== JSON PARSING ERROR =====");
            System.out.println("AI returned:");
            System.out.println(content);

            System.out.println("Jackson error:");
            System.out.println(exception.getMessage());

            throw new OpenRouterError(
                    HttpStatus.BAD_GATEWAY.value(),
                    "OpenRouter returned invalid evaluation JSON"
            );
        }
    }




    public SpeakingEvaluation generateEvaluation(MultipartFile audio) {
        String transcription = transcribeAudio(audio);

        AiBody aiBody = new AiBody();
        aiBody.setModel(openrouterFreeModel);

        MessageBody systemMessageBody = new MessageBody();
        systemMessageBody.setRole(AIRoleEnum.SYSTEM);
        systemMessageBody.setContent(systemContent);

        MessageBody userMessageBody = new MessageBody();
        userMessageBody.setRole(AIRoleEnum.USER);
        userMessageBody.setContent("""
                Evaluate the following Luxembourgish speaking answer.
                
                Transcript:
                "%s"
                
                Evaluate:
                - Luxembourgish grammar
                - vocabulary
                - sentence structure
                - whether the answer is understandable
                - overall language quality
                
                Return ONLY strict valid JSON.
                
                Use exactly this structure:
                {
                  "transcript": "...",
                  "score": 0,
                  "feedback": "...",
                  "corrections": []
                }
                
                Evaluation rules:
                - "score" must be an integer from 0 to 100.
                - "transcript" must contain exactly the transcript supplied above.
                - "feedback" should briefly explain what was done well and what should improve MUST always be written in English.
                - "corrections" must contain corrected Luxembourgish sentences when mistakes exist.
                - If there are no corrections, return an empty array.
                - Use authentic Luxembourgish spelling and grammar.
                - Never invent Luxembourgish words.
                - Do not penalize the user for transcription mistakes unless the transcript clearly indicates a language mistake.
                - If there are no corrections, return an empty array.
                - Use authentic Luxembourgish spelling and grammar for corrections.
                JSON rules:
                - Return only the JSON object.
                - Do not include markdown.
                - Do not use ```json code fences.
                - The JSON MUST be syntactically valid and parsable.
                - Every field except the last one MUST end with a comma.
                - Before responding, internally verify the JSON syntax.
                - Do not include comments such as // or /* */.
                - Do not include trailing commas.
                - Every property name must be inside double quotes.
                - Do not include any text before or after the JSON.
                """
                .formatted(transcription));

        aiBody.setMessages(
                List.of(systemMessageBody, userMessageBody)
        );

        OpenRouterResponse openRouterResponse = openRouterRestClient.post()
                .uri(completionURI)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(aiBody)
                .retrieve()
                .onStatus(
                        HttpStatusCode::isError,
                        (request, response) -> {

                            OpenRouterResponse errorResponse =
                                    objectMapper.readValue(
                                            response.getBody(),
                                            OpenRouterResponse.class
                                    );

                            if (errorResponse.getError() == null) {
                                throw new OpenRouterError(
                                        response.getStatusCode().value(),
                                        "OpenRouter returned an unknown error"
                                );
                            }

                            throw new OpenRouterError(
                                    errorResponse.getError().getCode(),
                                    errorResponse.getError().getMessage()
                            );
                        }
                )
                .body(OpenRouterResponse.class);

        if (openRouterResponse == null
                || openRouterResponse.getChoices() == null
                || openRouterResponse.getChoices().isEmpty()
                || openRouterResponse.getChoices().get(0).getMessage() == null
                || openRouterResponse.getChoices().get(0).getMessage().getContent() == null) {

            throw new OpenRouterError(
                    HttpStatus.BAD_GATEWAY.value(),
                    "OpenRouter returned an invalid response"
            );
        }

        String content = openRouterResponse
                .getChoices()
                .get(0)
                .getMessage()
                .getContent();

        try {
            System.out.println("===== OPENROUTER SPEAKING EVALUATION =====");
            System.out.println(content);
            return objectMapper.readValue(
                    content,
                    SpeakingEvaluation.class
            );
        } catch (JacksonException exception) {
            System.out.println("===== EVALUATION JSON PARSING ERROR =====");
            System.out.println(content);
            System.out.println(exception.getMessage());

            throw new OpenRouterError(
                    HttpStatus.BAD_GATEWAY.value(),
                    "OpenRouter returned invalid evaluation JSON"
            );
        }
    }

    //    private String transcribeAudio(MultipartFile audio) {
//
//        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
//
//        body.add("file", audio.getResource());
//        body.add("model", "whisper-large-v3");
//        body.add("response_format", "text");
//
//        return groqRestClient.post()
//                .uri("/audio/transcriptions")
//                .contentType(MediaType.MULTIPART_FORM_DATA)
//                .body(body)
//                .retrieve()
//                .body(String.class);
//    }
    private String transcribeAudio(MultipartFile audio) {

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        body.add("file", audio.getResource());
        body.add("model", "whisper-large-v3");
        body.add("response_format", "text");
        body.add("language", "lb");
        body.add(
                "prompt",
                "The speaker is speaking Luxembourgish (Lëtzebuergesch). " +
                        "Transcribe exactly what is spoken in Luxembourgish. " +
                        "Do not translate into English or German. " +
                        "Use standard Luxembourgish spelling."+
                        "This audio is Luxembourgish (Lëtzebuergesch). " +
                        "Transcribe it in Luxembourgish and do not translate it."
        );
        try {
            String transcription = groqRestClient.post()
                    .uri("/audio/transcriptions")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            System.out.println("===== GROQ TRANSCRIPTION =====");
            System.out.println(transcription);

            return transcription;

        } catch (Exception exception) {
            System.out.println("===== GROQ ERROR =====");
            System.out.println(exception.getMessage());
            exception.printStackTrace();

            throw exception;
        }
    }
}
