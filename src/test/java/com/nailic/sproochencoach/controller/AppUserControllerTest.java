package com.nailic.sproochencoach.controller;

import com.nailic.sproochencoach.dto.RequestUserDto;
import com.nailic.sproochencoach.dto.ResponseUserDto;
import com.nailic.sproochencoach.model.AppUser;
import com.nailic.sproochencoach.service.AiQuotaService;
import com.nailic.sproochencoach.service.AppUserService;
import com.nailic.sproochencoach.service.AuthenticatedUser;
import com.nailic.sproochencoach.service.ClientIpResolver;
import com.nailic.sproochencoach.service.EmailAndOtpService;
import com.nailic.sproochencoach.service.GoogleLoginService;
import com.nailic.sproochencoach.service.JwtCookieService;
import com.nailic.sproochencoach.service.LuxembourgLocationService;
import com.nailic.sproochencoach.service.PasswordResetService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AppUserControllerTest {
    @Test
    void updateMeUsesAuthenticatedUserId() throws Exception {
        AppUserService appUserService = mock(AppUserService.class);
        AppUser authenticatedUser = new AppUser();
        authenticatedUser.setId(42);

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(authenticatedUser);

        ResponseUserDto responseUser = new ResponseUserDto();
        responseUser.setId(42);
        responseUser.setFirstName("Sam");
        when(appUserService.updateUser(eq(42), any(RequestUserDto.class))).thenReturn(responseUser);

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new AppUserController(
                        appUserService,
                        mock(EmailAndOtpService.class),
                        mock(LuxembourgLocationService.class),
                        mock(AiQuotaService.class),
                        mock(PasswordResetService.class),
                        mock(JwtCookieService.class),
                        mock(ClientIpResolver.class),
                        mock(GoogleLoginService.class)
                ))
                .build();

        mockMvc.perform(put("/api/users/me")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Sam"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Current user updated successfully"))
                .andExpect(jsonPath("$.data.id").value(42))
                .andExpect(jsonPath("$.data.firstName").value("Sam"));

        verify(appUserService).updateUser(eq(42), any(RequestUserDto.class));
    }

    @Test
    void sendOtpUsesForwardedClientIp() throws Exception {
        EmailAndOtpService emailAndOtpService = mock(EmailAndOtpService.class);
        ClientIpResolver clientIpResolver = mock(ClientIpResolver.class);
        when(clientIpResolver.resolve(any())).thenReturn("203.0.113.10");
        doNothing().when(emailAndOtpService).sendEmailAndSaveOtp("learner@example.com", "203.0.113.10");

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new AppUserController(
                        mock(AppUserService.class),
                        emailAndOtpService,
                        mock(LuxembourgLocationService.class),
                        mock(AiQuotaService.class),
                        mock(PasswordResetService.class),
                        mock(JwtCookieService.class),
                        clientIpResolver,
                        mock(GoogleLoginService.class)
                ))
                .build();

        mockMvc.perform(post("/api/users/sendOtp")
                        .header("X-Forwarded-For", "203.0.113.10, 10.0.0.5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "learner@example.com"
                                }
                                """))
                .andExpect(status().isOk());

        verify(emailAndOtpService).sendEmailAndSaveOtp("learner@example.com", "203.0.113.10");
    }

    @Test
    void loginSetsHttpOnlyAccessTokenCookie() throws Exception {
        AppUserService appUserService = mock(AppUserService.class);
        JwtCookieService jwtCookieService = mock(JwtCookieService.class);
        ClientIpResolver clientIpResolver = mock(ClientIpResolver.class);
        ResponseUserDto authenticatedUser = new ResponseUserDto();

        when(clientIpResolver.resolve(any())).thenReturn("127.0.0.1");
        when(appUserService.login(any(RequestUserDto.class), eq("127.0.0.1")))
                .thenReturn(new AuthenticatedUser(authenticatedUser, "jwt-token"));
        when(jwtCookieService.accessTokenCookie("jwt-token"))
                .thenReturn(ResponseCookie.from("access_token", "jwt-token")
                        .httpOnly(true)
                        .path("/")
                        .sameSite("Lax")
                        .build());

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new AppUserController(
                        appUserService,
                        mock(EmailAndOtpService.class),
                        mock(LuxembourgLocationService.class),
                        mock(AiQuotaService.class),
                        mock(PasswordResetService.class),
                        jwtCookieService,
                        clientIpResolver,
                        mock(GoogleLoginService.class)
                ))
                .build();

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "learner@example.com",
                                  "password": "password"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.jwt").doesNotExist())
                .andExpect(result -> assertThat(result.getResponse().getHeader(HttpHeaders.SET_COOKIE))
                        .contains("access_token=jwt-token", "HttpOnly", "SameSite=Lax"));
    }

    @Test
    void googleLoginSetsHttpOnlyAccessTokenCookie() throws Exception {
        GoogleLoginService googleLoginService = mock(GoogleLoginService.class);
        JwtCookieService jwtCookieService = mock(JwtCookieService.class);
        ClientIpResolver clientIpResolver = mock(ClientIpResolver.class);
        ResponseUserDto authenticatedUser = new ResponseUserDto();

        when(clientIpResolver.resolve(any())).thenReturn("127.0.0.1");
        when(googleLoginService.login("google-id-token", "127.0.0.1"))
                .thenReturn(new AuthenticatedUser(authenticatedUser, "jwt-token"));
        when(jwtCookieService.accessTokenCookie("jwt-token"))
                .thenReturn(ResponseCookie.from("access_token", "jwt-token")
                        .httpOnly(true)
                        .path("/")
                        .sameSite("Lax")
                        .build());

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new AppUserController(
                        mock(AppUserService.class),
                        mock(EmailAndOtpService.class),
                        mock(LuxembourgLocationService.class),
                        mock(AiQuotaService.class),
                        mock(PasswordResetService.class),
                        jwtCookieService,
                        clientIpResolver,
                        googleLoginService
                ))
                .build();

        mockMvc.perform(post("/api/users/google-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idToken": "google-id-token"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Google login successful"))
                .andExpect(jsonPath("$.data.jwt").doesNotExist())
                .andExpect(result -> assertThat(result.getResponse().getHeader(HttpHeaders.SET_COOKIE))
                        .contains("access_token=jwt-token", "HttpOnly", "SameSite=Lax"));
    }

    @Test
    void logoutClearsAccessTokenCookie() throws Exception {
        JwtCookieService jwtCookieService = mock(JwtCookieService.class);
        when(jwtCookieService.clearAccessTokenCookie())
                .thenReturn(ResponseCookie.from("access_token", "")
                        .httpOnly(true)
                        .path("/")
                        .maxAge(0)
                        .sameSite("Lax")
                        .build());

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new AppUserController(
                        mock(AppUserService.class),
                        mock(EmailAndOtpService.class),
                        mock(LuxembourgLocationService.class),
                        mock(AiQuotaService.class),
                        mock(PasswordResetService.class),
                        jwtCookieService,
                        mock(ClientIpResolver.class),
                        mock(GoogleLoginService.class)
                ))
                .build();

        mockMvc.perform(post("/api/users/logout"))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(result.getResponse().getHeader(HttpHeaders.SET_COOKIE))
                        .contains("access_token=", "Max-Age=0", "HttpOnly"));
    }
}
