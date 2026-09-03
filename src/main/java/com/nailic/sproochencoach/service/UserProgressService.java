package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.dto.CompleteExerciseRequest;
import com.nailic.sproochencoach.dto.ExerciseRequestDto;
import com.nailic.sproochencoach.dto.ExerciseAttemptDto;
import com.nailic.sproochencoach.dto.ProgressDashboardDto;
import com.nailic.sproochencoach.dto.SkillProgressDto;
import com.nailic.sproochencoach.exceptions.BadRequestException;
import com.nailic.sproochencoach.exceptions.UserNotFoundException;
import com.nailic.sproochencoach.model.AppUser;
import com.nailic.sproochencoach.model.ExerciseAttempt;
import com.nailic.sproochencoach.model.ExerciseAttemptStatus;
import com.nailic.sproochencoach.repository.AppUserRepo;
import com.nailic.sproochencoach.repository.ExerciseAttemptRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class UserProgressService {
    private final LoggedInUser loggedInUser;
    private final ExerciseAttemptRepo exerciseAttemptRepo;
    private final UserLoginDayService userLoginDayService;
    private final AppUserRepo appUserRepo;

    public UserProgressService(
            LoggedInUser loggedInUser,
            ExerciseAttemptRepo exerciseAttemptRepo,
            UserLoginDayService userLoginDayService,
            AppUserRepo appUserRepo
    ) {
        this.loggedInUser = loggedInUser;
        this.exerciseAttemptRepo = exerciseAttemptRepo;
        this.userLoginDayService = userLoginDayService;
        this.appUserRepo = appUserRepo;
    }

    @Transactional
    public Long recordGeneratedExercise(String exerciseType, ExerciseRequestDto request) {
        AppUser user = loggedInUser.get();

        ExerciseAttempt attempt = new ExerciseAttempt();
        attempt.setUser(user);
        attempt.setExerciseType(exerciseType);
        attempt.setExerciseName(buildExerciseName(request));
        attempt.setLevel(request.getLevel());
        attempt.setTopic(request.getTopic());
        attempt.setAnswerType(request.getType());

        return exerciseAttemptRepo.save(attempt).getId();
    }

    @Transactional
    public void recordEvaluation(String exerciseType, String exerciseName, double ratingOverall) {
        recordEvaluation(exerciseType, exerciseName, ratingOverall, null);
    }

    @Transactional
    public void recordEvaluation(String exerciseType, String exerciseName, double ratingOverall, Long attemptId) {
        recordEvaluation(exerciseType, exerciseName, ratingOverall, attemptId, null);
    }

    @Transactional
    public void recordEvaluation(
            String exerciseType,
            String exerciseName,
            double ratingOverall,
            Long attemptId,
            String learnerAnswer
    ) {
        ExerciseAttempt attempt = attemptId == null
                ? newEvaluationAttempt(exerciseType, exerciseName)
                : currentUserAttempt(attemptId);

        attempt.setStatus(ExerciseAttemptStatus.EVALUATED);
        attempt.setAverageRatingOverall(ratingOverall);
        if (StringUtils.hasText(learnerAnswer)) {
            attempt.setLearnerAnswer(cleanAnswer(learnerAnswer));
        }
        attempt.setEvaluatedAt(LocalDateTime.now());
        if (attempt.getCompletedAt() == null) {
            attempt.setCompletedAt(attempt.getEvaluatedAt());
        }

        exerciseAttemptRepo.save(attempt);
    }

    @Transactional
    public ExerciseAttemptDto completeCurrentUserExercise(Long attemptId, CompleteExerciseRequest request) {
        ExerciseAttempt attempt = currentUserAttempt(attemptId);

        if (attempt.getStatus() == ExerciseAttemptStatus.EVALUATED) {
            throw new BadRequestException("Evaluated exercises are already completed");
        }

        attempt.setStatus(ExerciseAttemptStatus.COMPLETED);
        attempt.setCompletedAt(LocalDateTime.now());
        attempt.setLearnerAnswer(request == null ? null : cleanAnswer(request.getLearnerAnswer()));

        return toDto(exerciseAttemptRepo.save(attempt));
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
        List<ExerciseAttempt> attempts = exerciseAttemptRepo.findAllByUser_IdOrderByIdDesc(user.getId());
        UserLoginDayService.LoginStreakSummary loginStreakSummary =
                userLoginDayService.getLoginStreakSummary(user.getId());

        ProgressDashboardDto dashboard = new ProgressDashboardDto();
        dashboard.setUserId(user.getId());
        dashboard.setUsername(user.getUsername());
        dashboard.setEmail(user.getEmail());
        dashboard.setLoggedInDays(loginStreakSummary.loggedInDays());
        dashboard.setCurrentStreakDays(loginStreakSummary.currentStreakDays());
        dashboard.setLastLoginDate(loginStreakSummary.lastLoginDate());
        dashboard.setTotalActivities(attempts.size());
        dashboard.setCompletedActivities(completedCount(attempts));
        dashboard.setEvaluatedActivities(evaluatedCount(attempts));
        dashboard.setAverageRatingOverall(averageRating(attempts));
        dashboard.setLatestExerciseName(latestExerciseName(attempts));
        dashboard.setSkillProgress(skillProgress(attempts));

        return dashboard;
    }

    private ExerciseAttempt newEvaluationAttempt(String exerciseType, String exerciseName) {
        AppUser user = loggedInUser.get();

        ExerciseAttempt attempt = new ExerciseAttempt();
        attempt.setUser(user);
        attempt.setExerciseType(exerciseType);
        attempt.setExerciseName(exerciseName);
        attempt.setGeneratedAt(LocalDateTime.now());

        return attempt;
    }

    private String buildExerciseName(ExerciseRequestDto request) {
        String level = request.getLevel() == null ? "UNKNOWN_LEVEL" : request.getLevel();
        String topic = request.getTopic() == null ? "UNKNOWN_TOPIC" : request.getTopic();
        String type = request.getType() == null ? "GENERAL" : request.getType();

        return level + " " + topic + " " + type;
    }

    private List<SkillProgressDto> skillProgress(List<ExerciseAttempt> attempts) {
        return attempts.stream()
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

    private SkillProgressDto buildSkillProgress(String exerciseType, List<ExerciseAttempt> attempts) {
        SkillProgressDto skillProgress = new SkillProgressDto();
        skillProgress.setExerciseType(exerciseType);
        skillProgress.setTotalActivities(attempts.size());
        skillProgress.setCompletedActivities(completedCount(attempts));
        skillProgress.setEvaluatedActivities(evaluatedCount(attempts));
        skillProgress.setAverageRatingOverall(averageRating(attempts));
        skillProgress.setLatestExerciseName(latestExerciseName(attempts));

        return skillProgress;
    }

    private int completedCount(List<ExerciseAttempt> attempts) {
        return (int) attempts.stream()
                .map(ExerciseAttempt::getStatus)
                .filter(status -> status == ExerciseAttemptStatus.COMPLETED || status == ExerciseAttemptStatus.EVALUATED)
                .count();
    }

    private int evaluatedCount(List<ExerciseAttempt> attempts) {
        return (int) attempts.stream()
                .map(ExerciseAttempt::getAverageRatingOverall)
                .filter(Objects::nonNull)
                .count();
    }

    private Double averageRating(List<ExerciseAttempt> attempts) {
        DoubleSummaryStatistics statistics = attempts.stream()
                .map(ExerciseAttempt::getAverageRatingOverall)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .summaryStatistics();

        if (statistics.getCount() == 0) {
            return null;
        }

        return statistics.getAverage();
    }

    private String latestExerciseName(List<ExerciseAttempt> attempts) {
        return attempts.stream()
                .map(ExerciseAttempt::getExerciseName)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(null);
    }

    public ExerciseAttemptDto toDto(ExerciseAttempt attempt) {
        return new ExerciseAttemptDto(
                attempt.getId(),
                attempt.getExerciseType(),
                attempt.getExerciseName(),
                attempt.getStatus(),
                attempt.getLevel(),
                attempt.getTopic(),
                attempt.getAnswerType(),
                attempt.getLearnerAnswer(),
                attempt.getAverageRatingOverall(),
                attempt.getGeneratedAt(),
                attempt.getCompletedAt(),
                attempt.getEvaluatedAt()
        );
    }

    private ExerciseAttempt currentUserAttempt(Long attemptId) {
        AppUser user = loggedInUser.get();
        ExerciseAttempt attempt = exerciseAttemptRepo.findById(attemptId)
                .orElseThrow(() -> new BadRequestException("Exercise attempt not found"));

        if (!attempt.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("Exercise attempt does not belong to the current user");
        }

        return attempt;
    }

    private String cleanAnswer(String answer) {
        if (!StringUtils.hasText(answer)) {
            return null;
        }

        return answer.strip();
    }

    private String safeExerciseType(ExerciseAttempt attempt) {
        if (StringUtils.hasText(attempt.getExerciseType())) {
            return attempt.getExerciseType();
        }

        return "UNKNOWN";
    }
}
