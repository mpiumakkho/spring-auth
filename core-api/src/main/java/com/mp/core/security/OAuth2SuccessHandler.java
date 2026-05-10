package com.mp.core.security;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.mp.core.entity.RefreshToken;
import com.mp.core.entity.User;
import com.mp.core.service.OAuthLinkService;
import com.mp.core.service.RefreshTokenService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * After Spring's OAuth2Login finishes, we hand control back to the BFF (or to
 * the SPA directly when no BFF is in front). The handler:
 *   1. Reads the verified provider userinfo from OAuth2User
 *   2. Resolves/creates the matching internal User via OAuthLinkService
 *   3. Mints our own JWT + refresh token
 *   4. Redirects to the configured success URL with token + refreshToken
 *      as query params; the BFF rewrites them into httpOnly cookies.
 */
@Slf4j
@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final OAuthLinkService linkService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Value("${app.security.oauth2.success-redirect:http://localhost:8081/bff/auth/oauth2-callback}")
    private String successRedirect;

    public OAuth2SuccessHandler(
            OAuthLinkService linkService,
            JwtService jwtService,
            RefreshTokenService refreshTokenService) {
        this.linkService = linkService;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        if (!(authentication instanceof OAuth2AuthenticationToken oauthToken)) {
            super.onAuthenticationSuccess(request, response, authentication);
            return;
        }
        OAuth2User principal = oauthToken.getPrincipal();
        String provider = oauthToken.getAuthorizedClientRegistrationId();
        String providerUid = pickProviderUid(provider, principal);
        String email = principal.getAttribute("email");
        String name = principal.getAttribute("name");
        String picture = picture(provider, principal);

        String firstName = null;
        String lastName = null;
        if (name != null) {
            String[] parts = name.split(" ", 2);
            firstName = parts[0];
            if (parts.length > 1) lastName = parts[1];
        }

        User user = linkService.resolveOrCreate(provider, providerUid, email, firstName, lastName, picture);
        String jwt = jwtService.generate(user);
        RefreshToken refresh = refreshTokenService.issue(user.getUserId());

        log.info("OAuth2 login success: provider={}, userId={}", provider, user.getUserId());
        String url = successRedirect
                + "?token=" + URLEncoder.encode(jwt, StandardCharsets.UTF_8)
                + "&refreshToken=" + URLEncoder.encode(refresh.getToken(), StandardCharsets.UTF_8);
        getRedirectStrategy().sendRedirect(request, response, url);
    }

    private String pickProviderUid(String provider, OAuth2User principal) {
        // Different providers expose the stable user id under different attributes.
        return switch (provider) {
            case "google" -> stringAttr(principal, "sub");
            case "github" -> stringAttr(principal, "id");
            case "azure", "microsoft" -> stringAttr(principal, "oid");
            default -> stringAttr(principal, "sub");
        };
    }

    private String picture(String provider, OAuth2User principal) {
        return switch (provider) {
            case "github" -> stringAttr(principal, "avatar_url");
            default -> stringAttr(principal, "picture");
        };
    }

    private String stringAttr(OAuth2User principal, String key) {
        Object v = principal.getAttribute(key);
        return v == null ? null : v.toString();
    }
}
