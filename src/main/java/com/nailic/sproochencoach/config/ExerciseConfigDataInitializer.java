package com.nailic.sproochencoach.config;

import com.nailic.sproochencoach.model.ExerciseLevelConfig;
import com.nailic.sproochencoach.model.ExerciseTopicConfig;
import com.nailic.sproochencoach.model.ExerciseTypeConfig;
import com.nailic.sproochencoach.repository.ExerciseLevelConfigRepo;
import com.nailic.sproochencoach.repository.ExerciseTopicConfigRepo;
import com.nailic.sproochencoach.repository.ExerciseTypeConfigRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ExerciseConfigDataInitializer implements ApplicationRunner {
    private final ExerciseLevelConfigRepo levelRepo;
    private final ExerciseTopicConfigRepo topicRepo;
    private final ExerciseTypeConfigRepo typeRepo;

    @Override
    public void run(ApplicationArguments args) {
        seedLevels();
        seedTopics();
        seedTypes();
    }

    private void seedLevels() {
        List<LevelSeed> levels = List.of(
                new LevelSeed("A1", "Beginner", "Basic words, expressions and simple sentences"),
                new LevelSeed("A2", "Elementary", "Target level for Sproochentest speaking"),
                new LevelSeed("B1", "Intermediate", "Target level for Sproochentest listening")
        );

        levels.forEach(level -> {
            if (!levelRepo.existsByCode(level.code())) {
                ExerciseLevelConfig config = new ExerciseLevelConfig();
                config.setCode(level.code());
                config.setLabel(level.label());
                config.setDescription(level.description());
                levelRepo.save(config);
            }
        });
    }

    private void seedTopics() {
        List<TopicSeed> topics = List.of(
                new TopicSeed("INTRODUCTION", "Introduction & Personal Information", "A1"),
                new TopicSeed("FAMILY", "Family & Relationships", "A1"),
                new TopicSeed("HOME", "Home & Housing", "A1"),
                new TopicSeed("FOOD_AND_DRINK", "Food & Drink", "A1"),
                new TopicSeed("TIME_AND_DATES", "Time, Dates & Appointments", "A1"),
                new TopicSeed("DAILY_ROUTINE", "Daily Routine", "A1"),
                new TopicSeed("WORK", "Work & Profession", "A2"),
                new TopicSeed("SHOPPING", "Shopping", "A2"),
                new TopicSeed("CLOTHES", "Clothes", "A2"),
                new TopicSeed("HEALTH", "Health", "A2"),
                new TopicSeed("SPORTS", "Sports & Fitness", "A2"),
                new TopicSeed("HOBBIES", "Hobbies & Free Time", "A2"),
                new TopicSeed("TRANSPORT", "Transport", "A2"),
                new TopicSeed("TRAVEL", "Travel & Holidays", "A2"),
                new TopicSeed("WEATHER", "Weather", "A2"),
                new TopicSeed("NATURE", "Nature & Environment", "A2"),
                new TopicSeed("CITY_AND_PLACES", "City & Places", "A2"),
                new TopicSeed("LUXEMBOURG", "Life in Luxembourg", "A2"),
                new TopicSeed("FRIENDS_AND_SOCIAL_LIFE", "Friends & Social Life", "A2"),
                new TopicSeed("EVENTS_AND_CELEBRATIONS", "Events & Celebrations", "A2"),
                new TopicSeed("PAST_EXPERIENCES", "Past Experiences", "A2"),
                new TopicSeed("OPINIONS_AND_PREFERENCES", "Opinions & Preferences", "A2"),
                new TopicSeed("EDUCATION", "Education & Training", "B1"),
                new TopicSeed("PUBLIC_SERVICES", "Public Services", "B1"),
                new TopicSeed("MEDIA_AND_TECHNOLOGY", "Media & Technology", "B1"),
                new TopicSeed("FUTURE_PLANS", "Future Plans", "B1")
        );

        topics.forEach(topic -> {
            if (!topicRepo.existsByCode(topic.code())) {
                ExerciseTopicConfig config = new ExerciseTopicConfig();
                config.setCode(topic.code());
                config.setLabel(topic.label());
                config.setLevelCode(topic.levelCode());
                topicRepo.save(config);
            }
        });
    }

    private void seedTypes() {
        List<ExerciseTypeSeed> types = List.of(
                new ExerciseTypeSeed("TRANSLATION", "Translation"),
                new ExerciseTypeSeed("MULTIPLE_CHOICE", "Multiple choice"),
                new ExerciseTypeSeed("FILL_IN_THE_BLANK", "Fill in the blank"),
                new ExerciseTypeSeed("SHORT_ANSWER", "Short answer")
        );

        types.forEach(type -> {
            if (!typeRepo.existsByCode(type.code())) {
                ExerciseTypeConfig config = new ExerciseTypeConfig();
                config.setCode(type.code());
                config.setLabel(type.label());
                typeRepo.save(config);
            }
        });
    }

    private record LevelSeed(String code, String label, String description) {
    }

    private record TopicSeed(String code, String label, String levelCode) {
    }

    private record ExerciseTypeSeed(String code, String label) {
    }
}
