package com.mp.web.controller;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.mp.web.utils.ViewAttributeUtils;

import jakarta.servlet.http.HttpSession;

/**
 * Handles user login and logout.
 * Works with core API for session management.
 */
@Controller
@RequestMapping("/auth")
public class AuthController {
    @Value("${core.api.url}")
    private String coreApiUrl;

    @Autowired
    private RestTemplate restTemplate;

    /**
     * Shows login page.
     * Goes to dashboard if user is logged in.
     *
     * @param session User session
     * @param model View data
     * @return Login page or dashboard redirect
     */
    @GetMapping("/login")
    public String loginForm(HttpSession session, Model model) {
        if (session.getAttribute("userId") != null) {
            return "redirect:/dashboard";
        }
        ViewAttributeUtils.addCommonAttributes(model, "/login");
        return "auth/login";
    }

    /**
     * Logs out user.
     * Cleans up both local and API sessions.
     *
     * @param session User session to end
     * @param redirectAttributes Messages for next page
     * @return Goes to home page
     */
    @PostMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes redirectAttributes) {
        try {
            String token = (String) session.getAttribute("sessionToken");
            if (token != null) {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                Map<String, String> req = Map.of("token", token);
                HttpEntity<Map<String, String>> entity = new HttpEntity<>(req, headers);
                
                try {
                    restTemplate.postForEntity(coreApiUrl + "/api/sessions/logout", entity, String.class);
                } catch (Exception e) {
                    // log.warn("Error invalidating session in core-api: {}", e.getMessage());
                }
            }
            
            session.invalidate();
            redirectAttributes.addFlashAttribute("success", "Logged out successfully");
            
        } catch (Exception e) {
            // log.error("Error during logout: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Error occurred during logout");
        }
        
        return "redirect:/";
    }
} 