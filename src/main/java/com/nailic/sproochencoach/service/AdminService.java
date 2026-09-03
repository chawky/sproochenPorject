package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.dto.AdminExerciseConfigDto;
import com.nailic.sproochencoach.dto.AdminExerciseTypeOptionDto;
import com.nailic.sproochencoach.dto.AdminLevelOptionDto;
import com.nailic.sproochencoach.dto.AdminTopicOptionDto;
import com.nailic.sproochencoach.dto.AdminAuditLogDto;
import com.nailic.sproochencoach.dto.AdminAiUsageDto;
import com.nailic.sproochencoach.dto.AdminAiUsageSummaryDto;
import com.nailic.sproochencoach.dto.AdminExerciseTypeConfigRequest;
import com.nailic.sproochencoach.dto.AdminUserDetailDto;
import com.nailic.sproochencoach.dto.AdminUserProgressDto;
import com.nailic.sproochencoach.dto.AdminUserStatusUpdateRequest;
import com.nailic.sproochencoach.dto.AdminLevelConfigRequest;
import com.nailic.sproochencoach.dto.AdminTopicConfigRequest;
import com.nailic.sproochencoach.dto.PageResponseDto;
import com.nailic.sproochencoach.dto.ProgressDashboardDto;
import com.nailic.sproochencoach.dto.ResponseUserDto;
import com.nailic.sproochencoach.exceptions.BadRequestException;
import com.nailic.sproochencoach.exceptions.UserNotFoundException;
import com.nailic.sproochencoach.model.AppUser;
import com.nailic.sproochencoach.model.SubscriptionPlan;
import com.nailic.sproochencoach.model.ExerciseAttempt;
import com.nailic.sproochencoach.repository.AppUserRepo;
import com.nailic.sproochencoach.repository.ExerciseAttemptRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final AppUserService appUserService;
    private final AppUserRepo appUserRepo;
    private final ExerciseAttemptRepo exerciseAttemptRepo;
    private final UserProgressService userProgressService;
    private final LoggedInUser loggedInUser;
    private final SubscriptionAccessService subscriptionAccessService;
    private final AiUsageService aiUsageService;
    private final AdminAuditService adminAuditService;
    private final ExerciseConfigService exerciseConfigService;

    @Transactional(readOnly = true)
    public PageResponseDto<ResponseUserDto> getUsers(
            String search,
            Boolean subscribed,
            Boolean adminDisabled,
            int page,
            int size
    ) {
        int safePage = safePage(page);
        int safeSize = safeSize(size);
        String normalizedSearch = normalizedSearch(search);
        List<ResponseUserDto> filteredUsers = appUserRepo.findAll()
                .stream()
                .sorted(Comparator.comparing(AppUser::getId).reversed())
                .filter(user -> matchesSearch(user, normalizedSearch))
                .filter(user -> matchesAdminDisabled(user, adminDisabled))
                .filter(user -> matchesSubscribed(user, subscribed))
                .map(appUserService::toResponseUserDto)
                .toList();
        int totalItems = filteredUsers.size();
        int fromIndex = pageStartIndex(safePage, safeSize, totalItems);
        int toIndex = Math.min(fromIndex + safeSize, totalItems);

        return new PageResponseDto<>(
                filteredUsers.subList(fromIndex, toIndex),
                safePage,
                safeSize,
                totalItems,
                totalPages(totalItems, safeSize)
        );
    }

    public AdminUserDetailDto getUser(Integer id) {
        ResponseUserDto user = appUserService.findById(id);
        ProgressDashboardDto progress = userProgressService.getUserProgress(id);
        AdminAiUsageSummaryDto aiUsage = aiUsageService.getUserAiUsageSummary(id);

        return new AdminUserDetailDto(user, progress, aiUsage);
    }

    @Transactional
    public AdminUserDetailDto updateUserStatus(Integer id, AdminUserStatusUpdateRequest request) {
        boolean adminDisabled = Boolean.TRUE.equals(request.getAdminDisabled());

        if (adminDisabled && id.equals(loggedInUser.getId())) {
            throw new BadRequestException("Admins cannot disable their own account");
        }

        AppUser user = appUserRepo.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        boolean oldAdminDisabled = user.isAdminDisabled();

        user.setAdminDisabled(adminDisabled);
        appUserRepo.save(user);
        if (oldAdminDisabled != adminDisabled) {
            adminAuditService.recordUserStatusChange(
                    loggedInUser.getId(),
                    id,
                    oldAdminDisabled,
                    adminDisabled,
                    request.getReason()
            );
        }

        return getUser(id);
    }

    public PageResponseDto<AdminAuditLogDto> getAuditLogs(
            Integer actorUserId,
            Integer targetUserId,
            String targetType,
            String targetId,
            String action,
            int page,
            int size
    ) {
        return adminAuditService.getAuditLogs(actorUserId, targetUserId, targetType, targetId, action, page, size);
    }

    public PageResponseDto<AdminUserProgressDto> getUserProgress(Integer userId, int page, int size) {
        appUserService.findById(userId);
        int safePage = safePage(page);
        int safeSize = safeSize(size);
        Page<ExerciseAttempt> progressPage = exerciseAttemptRepo.findByUser_Id(
                userId,
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "id"))
        );

        List<AdminUserProgressDto> progress = progressPage.getContent()
                .stream()
                .map(this::toAdminUserProgressDto)
                .toList();

        return new PageResponseDto<>(
                progress,
                progressPage.getNumber(),
                progressPage.getSize(),
                progressPage.getTotalElements(),
                progressPage.getTotalPages()
        );
    }

    public AdminAiUsageSummaryDto getUserAiUsageSummary(Integer userId) {
        appUserService.findById(userId);

        return aiUsageService.getUserAiUsageSummary(userId);
    }

    public PageResponseDto<AdminAiUsageDto> getUserAiUsage(
            Integer userId,
            int page,
            int size,
            String provider,
            String model,
            LocalDateTime from,
            LocalDateTime to
    ) {
        appUserService.findById(userId);

        return aiUsageService.getUserAiUsage(userId, page, size, provider, model, from, to);
    }

    public AdminExerciseConfigDto getExerciseConfig() {
        return exerciseConfigService.getConfig();
    }

    public AdminLevelOptionDto createLevel(AdminLevelConfigRequest request) {
        return exerciseConfigService.createLevel(request);
    }

    public AdminLevelOptionDto updateLevel(String code, AdminLevelConfigRequest request) {
        return exerciseConfigService.updateLevel(code, request);
    }

    public void deleteLevel(String code) {
        exerciseConfigService.deleteLevel(code);
    }

    public AdminTopicOptionDto createTopic(AdminTopicConfigRequest request) {
        return exerciseConfigService.createTopic(request);
    }

    public AdminTopicOptionDto updateTopic(String code, AdminTopicConfigRequest request) {
        return exerciseConfigService.updateTopic(code, request);
    }

    public void deleteTopic(String code) {
        exerciseConfigService.deleteTopic(code);
    }

    public AdminExerciseTypeOptionDto createType(AdminExerciseTypeConfigRequest request) {
        return exerciseConfigService.createType(request);
    }

    public AdminExerciseTypeOptionDto updateType(String code, AdminExerciseTypeConfigRequest request) {
        return exerciseConfigService.updateType(code, request);
    }

    public void deleteType(String code) {
        exerciseConfigService.deleteType(code);
    }

    private AdminUserProgressDto toAdminUserProgressDto(ExerciseAttempt progress) {
        return new AdminUserProgressDto(
                progress.getId(),
                progress.getExerciseType(),
                progress.getExerciseName(),
                progress.getStatus().name(),
                progress.getLevel(),
                progress.getTopic(),
                progress.getAnswerType(),
                progress.getAverageRatingOverall(),
                progress.getGeneratedAt(),
                progress.getCompletedAt(),
                progress.getEvaluatedAt()
        );
    }

    private String normalizedSearch(String search) {
        if (!StringUtils.hasText(search)) {
            return null;
        }

        return search.trim().toLowerCase(Locale.ROOT);
    }

    private int safePage(int page) {
        return Math.max(page, 0);
    }

    private int safeSize(int size) {
        int minimumSize = Math.max(size, 1);
        return Math.min(minimumSize, 100);
    }

    private boolean matchesSearch(AppUser user, String search) {
        if (search == null) {
            return true;
        }

        return containsSearch(user.getUsername(), search)
                || containsSearch(user.getEmail(), search)
                || containsSearch(user.getFirstName(), search)
                || containsSearch(user.getLastName(), search);
    }

    private boolean matchesAdminDisabled(AppUser user, Boolean adminDisabled) {
        return adminDisabled == null || user.isAdminDisabled() == adminDisabled;
    }

    private boolean matchesSubscribed(AppUser user, Boolean subscribed) {
        if (subscribed == null) {
            return true;
        }

        return subscribed == hasSubscriptionAccess(user.getSubscriptionPlan());
    }

    private boolean containsSearch(String value, String search) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(search);
    }

    private boolean hasSubscriptionAccess(SubscriptionPlan subscriptionPlan) {
        return subscriptionPlan != null
                && subscriptionAccessService.hasSubscriptionAccess(subscriptionPlan.getSubscriptionStatus());
    }

    private int pageStartIndex(int page, int size, int totalItems) {
        long startIndex = (long) page * size;
        if (startIndex >= totalItems) {
            return totalItems;
        }

        return (int) startIndex;
    }

    private int totalPages(int totalItems, int size) {
        if (totalItems == 0) {
            return 0;
        }

        return (int) Math.ceil((double) totalItems / size);
    }
}
