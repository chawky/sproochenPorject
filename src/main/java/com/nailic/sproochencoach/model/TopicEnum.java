package com.nailic.sproochencoach.model;

import lombok.Getter;

@Getter
public enum TopicEnum {
    // A1
    INTRODUCTION("Introduction & Personal Information", LevelEnum.A1),
    FAMILY("Family & Relationships", LevelEnum.A1),
    HOME("Home & Housing", LevelEnum.A1),
    FOOD_AND_DRINK("Food & Drink", LevelEnum.A1),
    TIME_AND_DATES("Time, Dates & Appointments", LevelEnum.A1),
    DAILY_ROUTINE("Daily Routine", LevelEnum.A1),

    // A2
    WORK("Work & Profession", LevelEnum.A2),
    SHOPPING("Shopping", LevelEnum.A2),
    CLOTHES("Clothes", LevelEnum.A2),
    HEALTH("Health", LevelEnum.A2),
    SPORTS("Sports & Fitness", LevelEnum.A2),
    HOBBIES("Hobbies & Free Time", LevelEnum.A2),
    TRANSPORT("Transport", LevelEnum.A2),
    TRAVEL("Travel & Holidays", LevelEnum.A2),
    WEATHER("Weather", LevelEnum.A2),
    NATURE("Nature & Environment", LevelEnum.A2),
    CITY_AND_PLACES("City & Places", LevelEnum.A2),
    LUXEMBOURG("Life in Luxembourg", LevelEnum.A2),
    FRIENDS_AND_SOCIAL_LIFE("Friends & Social Life", LevelEnum.A2),
    EVENTS_AND_CELEBRATIONS("Events & Celebrations", LevelEnum.A2),
    PAST_EXPERIENCES("Past Experiences", LevelEnum.A2),
    OPINIONS_AND_PREFERENCES("Opinions & Preferences", LevelEnum.A2),

    // B1
    EDUCATION("Education & Training", LevelEnum.B1),
    PUBLIC_SERVICES("Public Services", LevelEnum.B1),
    MEDIA_AND_TECHNOLOGY("Media & Technology", LevelEnum.B1),
    FUTURE_PLANS("Future Plans", LevelEnum.B1);

    private final String label;
    public final LevelEnum levelEnum ;

    TopicEnum(String label, LevelEnum levelEnum) {
        this.label = label;
        this.levelEnum = levelEnum;
    }

}
