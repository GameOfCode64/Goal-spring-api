package com.tracker.backend.security;

import com.tracker.backend.entity.OAuthIdentity;
import com.tracker.backend.entity.User;
import com.tracker.backend.repository.OAuthIdentityRepository;
import com.tracker.backend.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Runs once, right after Google confirms the login. Looks up the
 * OAuthIdentity by (provider, providerUserId); creates a new User +
 * OAuthIdentity on first login. Issues our own JWT and redirects to a
 * client-owned URL carrying the token - the client never sees Google's
 * own access/refresh tokens directly.
 */
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {


    private final UserRepository userRepository;
    private final OAuthIdentityRepository oAuthIdentityRepository;
    private final JwtService jwtService;

    // Where to send the user after login, with the JWT attached as a query
    // param. For Electron/React Native this is typically a custom URL
    // scheme (deep link) the app registers itself to handle.
    @Value("${spring.security.oauth2.redirect-uri}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication
    ) throws IOException, ServletException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String providerUserId = oAuth2User.getName(); // Google's stable user ID (the "sub" claim)
        String email = oAuth2User.getAttribute("email");
        String displayName = oAuth2User.getAttribute("name");
        String avatarUrl = oAuth2User.getAttribute("picture");

        User user = oAuthIdentityRepository.findByProviderAndProviderUserId("GOOGLE", providerUserId)
                .map(OAuthIdentity::getUser)
                .orElseGet(() -> createNewUser(providerUserId, email, displayName, avatarUrl));

        String jwt = jwtService.generateToken(user.getId(), user.getEmail());

        String targetUrl = redirectUri + "?token=" + jwt;
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    private User createNewUser(String providerUserId, String email, String displayName, String avatarUrl) {
        User newUser = User.builder()
                .email(email)
                .displayName(displayName != null ? displayName : email)
                .avatarUrl(avatarUrl)
                .build();
        newUser = userRepository.save(newUser);

        OAuthIdentity identity = OAuthIdentity.builder()
                .user(newUser)
                .provider("GOOGLE")
                .providerUserId(providerUserId)
                .build();
        oAuthIdentityRepository.save(identity);

        return newUser;
    }
}