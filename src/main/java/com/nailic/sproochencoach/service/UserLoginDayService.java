package com.nailic.sproochencoach.service;

import com.nailic.sproochencoach.model.AppUser;
import com.nailic.sproochencoach.model.UserLoginDay;
import com.nailic.sproochencoach.repository.UserLoginDayRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
public class UserLoginDayService {
    private static final Logger log = LoggerFactory.getLogger(UserLoginDayService.class);
    private static final ZoneId STREAK_ZONE = ZoneId.of("Europe/Luxembourg");

    private final UserLoginDayRepo userLoginDayRepo;

    public UserLoginDayService(UserLoginDayRepo userLoginDayRepo) {
        this.userLoginDayRepo = userLoginDayRepo;
    }

    @Transactional
    public void recordLogin(AppUser user) {
        LocalDate today = today();

        if (userLoginDayRepo.existsByUser_IdAndLoginDate(user.getId(), today)) {
            log.debug("Login day already recorded. userId={}, loginDate={}", user.getId(), today);
            return;
        }

        UserLoginDay loginDay = new UserLoginDay();
        loginDay.setUser(user);
        loginDay.setLoginDate(today);

        userLoginDayRepo.save(loginDay);

        log.debug("Login day recorded. userId={}, loginDate={}", user.getId(), today);
    }

    public LoginStreakSummary getLoginStreakSummary(Integer userId) {
        List<LocalDate> loginDates = userLoginDayRepo.findAllByUser_IdOrderByLoginDateDesc(userId)
                .stream()
                .map(UserLoginDay::getLoginDate)
                .toList();

        return new LoginStreakSummary(
                loginDates.size(),
                currentStreak(loginDates, today()),
                loginDates.isEmpty() ? null : loginDates.get(0)
        );
    }

    private int currentStreak(List<LocalDate> loginDatesDescending, LocalDate today) {
        if (loginDatesDescending.isEmpty()) {
            return 0;
        }

        LocalDate expectedDate = today;
        LocalDate latestLoginDate = loginDatesDescending.get(0);

        if (latestLoginDate.equals(today.minusDays(1))) {
            expectedDate = latestLoginDate;
        } else if (!latestLoginDate.equals(today)) {
            return 0;
        }

        int streak = 0;
        for (LocalDate loginDate : loginDatesDescending) {
            if (!loginDate.equals(expectedDate)) {
                break;
            }

            streak++;
            expectedDate = expectedDate.minusDays(1);
        }

        return streak;
    }

    private LocalDate today() {
        return LocalDate.now(STREAK_ZONE);
    }

    public record LoginStreakSummary(
            int loggedInDays,
            int currentStreakDays,
            LocalDate lastLoginDate
    ) {
    }
}
