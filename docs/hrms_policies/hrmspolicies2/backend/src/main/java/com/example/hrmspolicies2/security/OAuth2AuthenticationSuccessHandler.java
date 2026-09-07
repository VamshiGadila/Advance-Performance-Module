package com.example.hrmspolicies2.security;

import com.example.hrmspolicies2.entity.User;
import com.example.hrmspolicies2.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2AuthenticationSuccessHandler.class);

    @Value("${app.oauth2.authorized-redirect-uri:http://localhost:3001/login}")
    private String redirectUri;

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public OAuth2AuthenticationSuccessHandler(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
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

        if (principal instanceof CustomOAuth2User customUser) {
            user = customUser.getUser();
        } else if (principal instanceof OAuth2User oAuth2User) {
            String email = oAuth2User.getAttribute("email");
            String name = oAuth2User.getAttribute("name");

            if (email != null && !email.isBlank()) {
                String cleanEmail = email.trim().toLowerCase();
                Optional<User> existingUserOpt = userRepository.findByEmail(cleanEmail);

                if (existingUserOpt.isPresent()) {
                    user = existingUserOpt.get();
                    log.info("HRMS Policies: Found existing user for email '{}' with Role '{}'", cleanEmail, user.getRole());
                } else {
                    log.info("HRMS Policies: User '{}' not found. Auto-provisioning new account via Google OAuth", cleanEmail);
                    String assignedRole = cleanEmail.contains("admin") ? "ADMIN" : "USER";

                    User newUser = User.builder()
                            .name(name != null && !name.isBlank() ? name.trim() : "Google User")
                            .email(cleanEmail)
                            .password("{noop}OAUTH2_NO_PASSWORD_" + UUID.randomUUID())
                            .role(assignedRole)
                            .build();

                    user = userRepository.save(newUser);
                    log.info("HRMS Policies: Provisioned new account ID={}, Role={}", user.getId(), user.getRole());
                }
            }
        }

        if (user != null) {
            log.info("HRMS Policies: Google OAuth2 login successful for User ID: {}, Email: {}, Role: {}",
                    user.getId(), user.getEmail(), user.getRole());

            String token = jwtService.generateToken(user.getEmail(), user.getRole());

            String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                    .queryParam("oauth_success", "true")
                    .queryParam("token", token)
                    .queryParam("role", user.getRole())
                    .queryParam("email", user.getEmail())
                    .queryParam("name", user.getName())
                    .queryParam("userId", user.getId())
                    .build()
                    .toUriString();

            getRedirectStrategy().sendRedirect(request, response, targetUrl);
        } else {
            log.error("HRMS Policies Google OAuth2 login failed: Could not determine user from principal: {}", principal);
            String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                    .queryParam("oauth_error", "Unable to retrieve Google account details. Please try again.")
                    .build()
                    .toUriString();
            getRedirectStrategy().sendRedirect(request, response, targetUrl);
        }
    }
}
