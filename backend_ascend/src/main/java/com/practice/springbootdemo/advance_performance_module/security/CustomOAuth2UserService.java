package com.practice.springbootdemo.advance_performance_module.security;

import com.practice.springbootdemo.advance_performance_module.entity.User;
import com.practice.springbootdemo.advance_performance_module.service.OAuth2AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final OAuth2AuthService oAuth2AuthService;

    public CustomOAuth2UserService(OAuth2AuthService oAuth2AuthService) {
        this.oAuth2AuthService = oAuth2AuthService;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        try {
            String email = oAuth2User.getAttribute("email");
            String name = oAuth2User.getAttribute("name");
            String sub = oAuth2User.getAttribute("sub");

            if (email == null || email.isBlank()) {
                throw new OAuth2AuthenticationException(new OAuth2Error("missing_email"), "Email not provided by Google");
            }

            User user = oAuth2AuthService.processOAuth2User(email, name, sub);
            return new CustomOAuth2User(user, oAuth2User.getAttributes());
        } catch (Exception ex) {
            log.error("Error processing Google OAuth2 user: {}", ex.getMessage(), ex);
            throw new OAuth2AuthenticationException(new OAuth2Error("oauth2_processing_error"), ex.getMessage());
        }
    }
}