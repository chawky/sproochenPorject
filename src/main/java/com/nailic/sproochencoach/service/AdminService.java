package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.dto.AdminExerciseConfigDto;
import com.nailic.sproochencoach.dto.AdminExerciseTypeOptionDto;
import com.nailic.sproochencoach.dto.AdminLevelOptionDto;
import com.nailic.sproochencoach.dto.AdminTopicOptionDto;
import com.nailic.sproochencoach.dto.AdminAiUsageDto;
import com.nailic.sproochencoach.dto.AdminAiUsageSummaryDto;
import com.nailic.sproochencoach.dto.AdminUserDetailDto;
import com.nailic.sproochencoach.dto.AdminUserProgressDto;
import com.nailic.sproochencoach.dto.AdminUserStatusUpdateRequest;
import com.nailic.sproochencoach.dto.PageResponseDto;
import com.nailic.sproochencoach.dto.ProgressDashboardDto;
import com.nailic.sproochencoach.dto.ResponseUserDto;
import com.nailic.sproochencoach.exceptions.BadRequestException;
import com.nailic.sproochencoach.exceptions.UserNotFoundException;
import com.nailic.sproochencoach.model.AppUser;
import com.nailic.sproochencoach.model.ExerciseTypeEnum;
import com.nailic.sproochencoach.model.LevelEnum;
import com.nailic.sproochencoach.model.SubscriptionPlan;
import com.nailic.sproochencoach.model.TopicEnum;
import com.nailic.sproochencoach.model.UserProgress;
import com.nailic.sproochencoach.repository.AppUserRepo;
import com.nailic.sproochencoach.repository.UserProgressRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final AppUserService appUserService;
    private final AppUserRepo appUserRepo;
    private final UserProgressRepo userProgressRepo;
    private final UserProgressService userProgressService;
    private final LoggedInUser loggedInUser;
    private final SubscriptionAccessService subscriptionAccessService;
    private final AiUsageService aiUsageService;

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

    public AdminUserDetailDto updateUserStatus(Integer id, AdminUserStatusUpdateRequest request) {
        boolean adminDisabled = Boolean.TRUE.equals(request.getAdminDisabled());

        if (adminDisabled && id.equals(loggedInUser.getId())) {
            throw new BadRequestException("Admins cannot disable their own account");
        }

        AppUser user = appUserRepo.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        user.setAdminDisabled(adminDisabled);
        appUserRepo.save(user);

        return getUser(id);
    }

    public PageResponseDto<AdminUserProgressDto> getUserProgress(Integer userId, int page, int size) {
        appUserService.findById(userId);
        int safePage = safePage(page);
        int safeSize = safeSize(size);
        Page<UserProgress> progressPage = userProgressRepo.findByUser_Id(
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

    public PageResponseDto<AdminAiUsageDto> getUserAiUsage(Integer userId, int page, int size) {
        appUserService.findById(userId);

        return aiUsageService.getUserAiUsage(userId, page, size);
    }

    public AdminExerciseConfigDto getExerciseConfig() {
        AdminExerciseConfigDto config = new AdminExerciseConfigDto();
        config.setEditable(false);
        config.setLevels(levelOptions());
        config.setTopics(topicOptions());
        config.setExerciseTypes(exerciseTypeOptions());

        return config;
    }

    private AdminUserProgressDto toAdminUserProgressDto(UserProgress progress) {
        return new AdminUserProgressDto(
                progress.getId(),
                progress.getExerciseType(),
                progress.getExerciseName(),
                progress.getAverageRatingOverall()
        );
    }

    private List<AdminLevelOptionDto> levelOptions() {
        return Arrays.stream(LevelEnum.values())
                .map(level -> new AdminLevelOptionDto(
                        level.name(),
                        level.getLabel(),
                        level.getDescription(),
                        true
                ))
                .toList();
    }

    private List<AdminTopicOptionDto> topicOptions() {
        return Arrays.stream(TopicEnum.values())
                .map(topic -> new AdminTopicOptionDto(
                        topic.name(),
                        topic.getLabel(),
                        topic.getLevelEnum(),
                        true
                ))
                .toList();
    }

    private List<AdminExerciseTypeOptionDto> exerciseTypeOptions() {
        return Arrays.stream(ExerciseTypeEnum.values())
                .map(type -> new AdminExerciseTypeOptionDto(type.name(), true))
                .toList();
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
