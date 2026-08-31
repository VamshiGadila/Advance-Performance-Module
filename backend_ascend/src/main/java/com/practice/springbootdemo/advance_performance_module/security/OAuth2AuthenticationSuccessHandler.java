package com.practice.springbootdemo.advance_performance_module.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.practice.springbootdemo.advance_performance_module.entity.User;
import com.practice.springbootdemo.advance_performance_module.service.OAuth2AuthService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import java.io.IOException;

@Slf4j
@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Value("${app.oauth2.authorized-redirect-uri:http://localhost:3000/login}")
    private String redirectUri;

    private final JwtService jwtService;
    private final OAuth2AuthService oAuth2AuthService;

    public OAuth2AuthenticationSuccessHandler(JwtService jwtService, OAuth2AuthService oAuth2AuthService) {
        this.jwtService = jwtService;
        this.oAuth2AuthService = oAuth2AuthService;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {
        if (response.isCommitted()) {
            log.debug("Response already committed. Unable to redirect.");
            return;
        }

        Object principal = authentication.getPrincipal();
        User user = null;

        if (principal instanceof CustomOAuth2User oAuth2User) {
            user = oAuth2User.getUser();
        } else if (principal instanceof OAuth2User oAuth2User) {
            String email = oAuth2User.getAttribute("email");
            String name = oAuth2User.getAttribute("name");
            String sub = oAuth2User.getAttribute("sub");

            log.info("Processing OAuth2 login for generic OAuth2/Oidc principal. Email: '{}', Name: '{}', Sub: '{}'",
                    email, name, sub);

            if (email != null && !email.isBlank()) {
                user = oAuth2AuthService.processOAuth2User(email, name, sub);
            }
        }

        if (user != null) {
            log.info("Google OAuth2 login successful for User ID: {}, Email: {}, Role: {}",
                    user.getId(), user.getEmail(), user.getRole());

            String token = jwtService.generate(user);

            String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                    .queryParam("oauth_success", "true")
                    .queryParam("token", token)
                    .queryParam("role", user.getRole().name())
                    .queryParam("email", user.getEmail())
                    .queryParam("name", user.getName())
                    .queryParam("userId", user.getId())
                    .queryParam("employeeCode", user.getEmployeeCode())
                    .build()
                    .toUriString();

            getRedirectStrategy().sendRedirect(request, response, targetUrl);
        } else {
            log.error("Google OAuth2 login failed: Could not determine user from principal: {}", principal);
            String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                    .queryParam("oauth_error", "Unable to retrieve Google account details. Please try again.")
                    .build()
                    .toUriString();
            getRedirectStrategy().sendRedirect(request, response, targetUrl);
        }
    }
}