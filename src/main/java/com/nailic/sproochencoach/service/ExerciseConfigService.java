package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.dto.AdminExerciseConfigDto;
import com.nailic.sproochencoach.dto.AdminExerciseTypeConfigRequest;
import com.nailic.sproochencoach.dto.AdminExerciseTypeOptionDto;
import com.nailic.sproochencoach.dto.AdminLevelConfigRequest;
import com.nailic.sproochencoach.dto.AdminLevelOptionDto;
import com.nailic.sproochencoach.dto.AdminTopicConfigRequest;
import com.nailic.sproochencoach.dto.AdminTopicOptionDto;
import com.nailic.sproochencoach.dto.ExerciseRequestDto;
import com.nailic.sproochencoach.exceptions.BadRequestException;
import com.nailic.sproochencoach.model.ExerciseLevelConfig;
import com.nailic.sproochencoach.model.ExerciseTopicConfig;
import com.nailic.sproochencoach.model.ExerciseTypeConfig;
import com.nailic.sproochencoach.repository.ExerciseLevelConfigRepo;
import com.nailic.sproochencoach.repository.ExerciseTopicConfigRepo;
import com.nailic.sproochencoach.repository.ExerciseTypeConfigRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ExerciseConfigService {
    private final ExerciseLevelConfigRepo levelRepo;
    private final ExerciseTopicConfigRepo topicRepo;
    private final ExerciseTypeConfigRepo typeRepo;
    private final AdminAuditService adminAuditService;
    private final LoggedInUser loggedInUser;

    @Transactional(readOnly = true)
    public AdminExerciseConfigDto getConfig() {
        AdminExerciseConfigDto config = new AdminExerciseConfigDto();
        config.setEditable(true);
        config.setLevels(levelRepo.findAllByOrderByCodeAsc().stream().map(this::toLevelDto).toList());
        config.setTopics(topicRepo.findAllByOrderByLevelCodeAscCodeAsc().stream().map(this::toTopicDto).toList());
        config.setExerciseTypes(typeRepo.findAllByOrderByCodeAsc().stream().map(this::toTypeDto).toList());
        return config;
    }

    @Transactional
    public AdminLevelOptionDto createLevel(AdminLevelConfigRequest request) {
        String code = normalizeCode(request.getCode());
        if (levelRepo.existsByCode(code)) {
            throw new BadRequestException("Exercise level already exists");
        }

        ExerciseLevelConfig level = new ExerciseLevelConfig();
        level.setCode(code);
        requireLevelCreate(request);
        applyLevelRequest(level, request);
        ExerciseLevelConfig savedLevel = levelRepo.save(level);
        recordConfigAudit("EXERCISE_LEVEL_CREATED", "EXERCISE_LEVEL", savedLevel.getCode(), null, levelState(savedLevel));
        return toLevelDto(savedLevel);
    }

    @Transactional
    public AdminLevelOptionDto updateLevel(String code, AdminLevelConfigRequest request) {
        ExerciseLevelConfig level = levelByCode(code);
        String oldValue = levelState(level);
        applyLevelRequest(level, request);
        ExerciseLevelConfig savedLevel = levelRepo.save(level);
        recordConfigAudit("EXERCISE_LEVEL_UPDATED", "EXERCISE_LEVEL", savedLevel.getCode(), oldValue, levelState(savedLevel));
        return toLevelDto(savedLevel);
    }

    @Transactional
    public void deleteLevel(String code) {
        ExerciseLevelConfig level = levelByCode(code);
        if (topicRepo.existsByLevelCode(level.getCode())) {
            throw new BadRequestException("Delete or move topics before deleting this level");
        }

        String oldValue = levelState(level);
        levelRepo.delete(level);
        recordConfigAudit("EXERCISE_LEVEL_DELETED", "EXERCISE_LEVEL", level.getCode(), oldValue, null);
    }

    @Transactional
    public AdminTopicOptionDto createTopic(AdminTopicConfigRequest request) {
        String code = normalizeCode(request.getCode());
        if (topicRepo.existsByCode(code)) {
            throw new BadRequestException("Exercise topic already exists");
        }

        ExerciseTopicConfig topic = new ExerciseTopicConfig();
        topic.setCode(code);
        requireTopicCreate(request);
        applyTopicRequest(topic, request);
        ExerciseTopicConfig savedTopic = topicRepo.save(topic);
        recordConfigAudit("EXERCISE_TOPIC_CREATED", "EXERCISE_TOPIC", savedTopic.getCode(), null, topicState(savedTopic));
        return toTopicDto(savedTopic);
    }

    @Transactional
    public AdminTopicOptionDto updateTopic(String code, AdminTopicConfigRequest request) {
        ExerciseTopicConfig topic = topicByCode(code);
        String oldValue = topicState(topic);
        applyTopicRequest(topic, request);
        ExerciseTopicConfig savedTopic = topicRepo.save(topic);
        recordConfigAudit("EXERCISE_TOPIC_UPDATED", "EXERCISE_TOPIC", savedTopic.getCode(), oldValue, topicState(savedTopic));
        return toTopicDto(savedTopic);
    }

    @Transactional
    public void deleteTopic(String code) {
        ExerciseTopicConfig topic = topicByCode(code);
        String oldValue = topicState(topic);
        topicRepo.delete(topic);
        recordConfigAudit("EXERCISE_TOPIC_DELETED", "EXERCISE_TOPIC", topic.getCode(), oldValue, null);
    }

    @Transactional
    public AdminExerciseTypeOptionDto createType(AdminExerciseTypeConfigRequest request) {
        String code = normalizeCode(request.getCode());
        if (typeRepo.existsByCode(code)) {
            throw new BadRequestException("Exercise type already exists");
        }

        ExerciseTypeConfig type = new ExerciseTypeConfig();
        type.setCode(code);
        requireTypeCreate(request);
        applyTypeRequest(type, request);
        ExerciseTypeConfig savedType = typeRepo.save(type);
        recordConfigAudit("EXERCISE_TYPE_CREATED", "EXERCISE_TYPE", savedType.getCode(), null, typeState(savedType));
        return toTypeDto(savedType);
    }

    @Transactional
    public AdminExerciseTypeOptionDto updateType(String code, AdminExerciseTypeConfigRequest request) {
        ExerciseTypeConfig type = typeByCode(code);
        String oldValue = typeState(type);
        applyTypeRequest(type, request);
        ExerciseTypeConfig savedType = typeRepo.save(type);
        recordConfigAudit("EXERCISE_TYPE_UPDATED", "EXERCISE_TYPE", savedType.getCode(), oldValue, typeState(savedType));
        return toTypeDto(savedType);
    }

    @Transactional
    public void deleteType(String code) {
        ExerciseTypeConfig type = typeByCode(code);
        String oldValue = typeState(type);
        typeRepo.delete(type);
        recordConfigAudit("EXERCISE_TYPE_DELETED", "EXERCISE_TYPE", type.getCode(), oldValue, null);
    }

    @Transactional(readOnly = true)
    public void validateRequest(ExerciseRequestDto request) {
        ExerciseLevelConfig level = levelByCode(request.getLevel());
        ExerciseTopicConfig topic = topicByCode(request.getTopic());
        ExerciseTypeConfig type = typeByCode(request.getType());

        if (!level.isEnabled()) {
            throw new BadRequestException("Exercise level is disabled");
        }

        if (!topic.isEnabled()) {
            throw new BadRequestException("Exercise topic is disabled");
        }

        if (!type.isEnabled()) {
            throw new BadRequestException("Exercise type is disabled");
        }

        if (!topic.getLevelCode().equals(level.getCode())) {
            throw new BadRequestException("Exercise topic does not belong to the selected level");
        }
    }

    @Transactional(readOnly = true)
    public ExerciseRequestDto normalizedRequest(ExerciseRequestDto request) {
        validateRequest(request);

        ExerciseRequestDto normalizedRequest = new ExerciseRequestDto();
        normalizedRequest.setLevel(normalizeCode(request.getLevel()));
        normalizedRequest.setTopic(normalizeCode(request.getTopic()));
        normalizedRequest.setType(normalizeCode(request.getType()));
        return normalizedRequest;
    }

    @Transactional(readOnly = true)
    public String topicLabel(String code) {
        return topicByCode(code).getLabel();
    }

    private void applyLevelRequest(ExerciseLevelConfig level, AdminLevelConfigRequest request) {
        if (request.getLabel() != null) {
            level.setLabel(cleanText(request.getLabel(), "Level label is required"));
        }

        if (request.getDescription() != null) {
            level.setDescription(cleanText(request.getDescription(), "Level description is required"));
        }

        if (request.getEnabled() != null) {
            level.setEnabled(request.getEnabled());
        }
    }

    private void requireLevelCreate(AdminLevelConfigRequest request) {
        cleanText(request.getLabel(), "Level label is required");
        cleanText(request.getDescription(), "Level description is required");
    }

    private void requireTopicCreate(AdminTopicConfigRequest request) {
        cleanText(request.getLabel(), "Topic label is required");
        normalizeCode(request.getLevelCode());
    }

    private void requireTypeCreate(AdminExerciseTypeConfigRequest request) {
        cleanText(request.getLabel(), "Exercise type label is required");
    }

    private void applyTopicRequest(ExerciseTopicConfig topic, AdminTopicConfigRequest request) {
        if (request.getLevelCode() != null) {
            String levelCode = normalizeCode(request.getLevelCode());
            levelByCode(levelCode);
            topic.setLevelCode(levelCode);
        }

        if (request.getLabel() != null) {
            topic.setLabel(cleanText(request.getLabel(), "Topic label is required"));
        }

        if (request.getEnabled() != null) {
            topic.setEnabled(request.getEnabled());
        }
    }

    private void applyTypeRequest(ExerciseTypeConfig type, AdminExerciseTypeConfigRequest request) {
        if (request.getLabel() != null) {
            type.setLabel(cleanText(request.getLabel(), "Exercise type label is required"));
        }

        if (request.getEnabled() != null) {
            type.setEnabled(request.getEnabled());
        }
    }

    private ExerciseLevelConfig levelByCode(String code) {
        return levelRepo.findByCode(normalizeCode(code))
                .orElseThrow(() -> new BadRequestException("Exercise level not found"));
    }

    private ExerciseTopicConfig topicByCode(String code) {
        return topicRepo.findByCode(normalizeCode(code))
                .orElseThrow(() -> new BadRequestException("Exercise topic not found"));
    }

    private ExerciseTypeConfig typeByCode(String code) {
        return typeRepo.findByCode(normalizeCode(code))
                .orElseThrow(() -> new BadRequestException("Exercise type not found"));
    }

    private AdminLevelOptionDto toLevelDto(ExerciseLevelConfig level) {
        return new AdminLevelOptionDto(level.getCode(), level.getLabel(), level.getDescription(), level.isEnabled());
    }

    private AdminTopicOptionDto toTopicDto(ExerciseTopicConfig topic) {
        return new AdminTopicOptionDto(topic.getCode(), topic.getLabel(), topic.getLevelCode(), topic.isEnabled());
    }

    private AdminExerciseTypeOptionDto toTypeDto(ExerciseTypeConfig type) {
        return new AdminExerciseTypeOptionDto(type.getCode(), type.getLabel(), type.isEnabled());
    }

    private String normalizeCode(String code) {
        return cleanText(code, "Code is required")
                .trim()
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(Locale.ROOT);
    }

    private String cleanText(String text, String message) {
        if (!StringUtils.hasText(text)) {
            throw new BadRequestException(message);
        }

        return text.strip();
    }

    private void recordConfigAudit(String action, String targetType, String targetId, String oldValue, String newValue) {
        adminAuditService.recordAction(
                loggedInUser.getId(),
                null,
                targetType,
                targetId,
                action,
                valueOrEmpty(oldValue),
                valueOrEmpty(newValue),
                null
        );
    }

    private String levelState(ExerciseLevelConfig level) {
        return "label=" + level.getLabel()
                + ", description=" + level.getDescription()
                + ", enabled=" + level.isEnabled();
    }

    private String topicState(ExerciseTopicConfig topic) {
        return "label=" + topic.getLabel()
                + ", levelCode=" + topic.getLevelCode()
                + ", enabled=" + topic.isEnabled();
    }

    private String typeState(ExerciseTypeConfig type) {
        return "label=" + type.getLabel()
                + ", enabled=" + type.isEnabled();
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
