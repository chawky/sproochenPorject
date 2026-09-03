package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.dto.ExerciseRequestDto;
import com.nailic.sproochencoach.dto.ProgressDashboardDto;
import com.nailic.sproochencoach.dto.SkillProgressDto;
import com.nailic.sproochencoach.exceptions.UserNotFoundException;
import com.nailic.sproochencoach.model.AppUser;
import com.nailic.sproochencoach.model.UserProgress;
import com.nailic.sproochencoach.repository.AppUserRepo;
import com.nailic.sproochencoach.repository.UserProgressRepo;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class UserProgressService {
    private final LoggedInUser loggedInUser;
    private final UserProgressRepo userProgressRepo;
    private final UserLoginDayService userLoginDayService;
    private final AppUserRepo appUserRepo;

    public UserProgressService(
            LoggedInUser loggedInUser,
            UserProgressRepo userProgressRepo,
            UserLoginDayService userLoginDayService,
            AppUserRepo appUserRepo
    ) {
        this.loggedInUser = loggedInUser;
        this.userProgressRepo = userProgressRepo;
        this.userLoginDayService = userLoginDayService;
        this.appUserRepo = appUserRepo;
    }

    public void recordGeneratedExercise(String exerciseType, ExerciseRequestDto request) {
        saveProgress(exerciseType, buildExerciseName(request), null);
    }

    public void recordEvaluation(String exerciseType, String exerciseName, double ratingOverall) {
        saveProgress(exerciseType, exerciseName, ratingOverall);
    }

    public ProgressDashboardDto getCurrentUserProgress() {
        AppUser user = loggedInUser.get();
        return buildProgressDashboard(user);
    }

    public ProgressDashboardDto getUserProgress(Integer userId) {
        AppUser user = appUserRepo.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        return buildProgressDashboard(user);
    }

    private ProgressDashboardDto buildProgressDashboard(AppUser user) {
        List<UserProgress> progressRecords = userProgressRepo.findAllByUser_IdOrderByIdDesc(user.getId());
        UserLoginDayService.LoginStreakSummary loginStreakSummary =
                userLoginDayService.getLoginStreakSummary(user.getId());

        ProgressDashboardDto dashboard = new ProgressDashboardDto();
        dashboard.setUserId(user.getId());
        dashboard.setUsername(user.getUsername());
        dashboard.setEmail(user.getEmail());
        dashboard.setLoggedInDays(loginStreakSummary.loggedInDays());
        dashboard.setCurrentStreakDays(loginStreakSummary.currentStreakDays());
        dashboard.setLastLoginDate(loginStreakSummary.lastLoginDate());
        dashboard.setTotalActivities(progressRecords.size());
        dashboard.setEvaluatedActivities(evaluatedCount(progressRecords));
        dashboard.setAverageRatingOverall(averageRating(progressRecords));
        dashboard.setLatestExerciseName(latestExerciseName(progressRecords));
        dashboard.setSkillProgress(skillProgress(progressRecords));

        return dashboard;
    }

    private void saveProgress(String exerciseType, String exerciseName, Double ratingOverall) {
        AppUser user = loggedInUser.get();

        UserProgress progress = new UserProgress();
        progress.setUser(user);
        progress.setExerciseType(exerciseType);
        progress.setExerciseName(exerciseName);
        progress.setAverageRatingOverall(ratingOverall);

        userProgressRepo.save(progress);
    }

    private String buildExerciseName(ExerciseRequestDto request) {
        String level = request.getLevel() == null ? "UNKNOWN_LEVEL" : request.getLevel().name();
        String topic = request.getTopic() == null ? "UNKNOWN_TOPIC" : request.getTopic().name();
        String type = request.getType() == null ? "GENERAL" : request.getType().name();

        return level + " " + topic + " " + type;
    }

    private List<SkillProgressDto> skillProgress(List<UserProgress> progressRecords) {
        return progressRecords.stream()
                .collect(Collectors.groupingBy(
                        this::safeExerciseType,
                        TreeMap::new,
                        Collectors.toList()
                ))
                .entrySet()
                .stream()
                .map(entry -> buildSkillProgress(entry.getKey(), entry.getValue()))
                .toList();
    }

    private SkillProgressDto buildSkillProgress(String exerciseType, List<UserProgress> progressRecords) {
        SkillProgressDto skillProgress = new SkillProgressDto();
        skillProgress.setExerciseType(exerciseType);
        skillProgress.setTotalActivities(progressRecords.size());
        skillProgress.setEvaluatedActivities(evaluatedCount(progressRecords));
        skillProgress.setAverageRatingOverall(averageRating(progressRecords));
        skillProgress.setLatestExerciseName(latestExerciseName(progressRecords));

        return skillProgress;
    }

    private int evaluatedCount(List<UserProgress> progressRecords) {
        return (int) progressRecords.stream()
                .map(UserProgress::getAverageRatingOverall)
                .filter(Objects::nonNull)
                .count();
    }

    private Double averageRating(List<UserProgress> progressRecords) {
        DoubleSummaryStatistics statistics = progressRecords.stream()
                .map(UserProgress::getAverageRatingOverall)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .summaryStatistics();

        if (statistics.getCount() == 0) {
            return null;
        }

        return statistics.getAverage();
    }

    private String latestExerciseName(List<UserProgress> progressRecords) {
        return progressRecords.stream()
                .map(UserProgress::getExerciseName)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(null);
    }

    private String safeExerciseType(UserProgress progress) {
        if (StringUtils.hasText(progress.getExerciseType())) {
            return progress.getExerciseType();
        }

        return "UNKNOWN";
    }
}
