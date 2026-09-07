package com.example.hrmspolicies2.security;

import com.example.hrmspolicies2.entity.User;
import com.example.hrmspolicies2.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private static final Logger log = LoggerFactory.getLogger(CustomOAuth2UserService.class);

    private final UserRepository userRepository;

    public CustomOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        try {
            String email = oAuth2User.getAttribute("email");
            String name = oAuth2User.getAttribute("name");

            if (email == null || email.isBlank()) {
                throw new OAuth2AuthenticationException(new OAuth2Error("missing_email"), "Email not provided by Google");
            }

            String cleanEmail = email.trim().toLowerCase();
            log.info("HRMS Policies: Processing Google OAuth2 authentication for email: '{}'", cleanEmail);

            Optional<User> existingUserOpt = userRepository.findByEmail(cleanEmail);
            User user;

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

            return new CustomOAuth2User(user, oAuth2User.getAttributes());
        } catch (Exception ex) {
            log.error("Error processing Google OAuth2 user in HRMS Policies: {}", ex.getMessage(), ex);
            throw new OAuth2AuthenticationException(new OAuth2Error("oauth2_processing_error"), ex.getMessage());
        }
    }
}
