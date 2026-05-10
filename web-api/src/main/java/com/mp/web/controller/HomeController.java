package com.mp.web.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.mp.web.service.UserWebService;
import com.mp.web.service.RoleWebService;
import com.mp.web.service.PermissionWebService;
import com.mp.web.utils.ViewAttributeUtils;

import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;

/**
 * Main page and dashboard controller.
 * Shows system stats from core API using dedicated services.
 */
@Slf4j
@Controller
public class HomeController {

    private final UserWebService userWebService;
    private final RoleWebService roleWebService;
    private final PermissionWebService permissionWebService;

    public HomeController(
            UserWebService userWebService,
            RoleWebService roleWebService,
            PermissionWebService permissionWebService
    ) {
        this.userWebService = userWebService;
        this.roleWebService = roleWebService;
        this.permissionWebService = permissionWebService;
    }

    /**
     * Main page handler.
     * Shows login page or goes to dashboard.
     *
     * @param session User session
     * @param model View data
     * @return Login page or dashboard redirect
     */
    @GetMapping({"/", "/login"})
    public String home(HttpSession session, Model model) {
        if (session.getAttribute("userId") == null) {
            // LOG.info("[LOGIN PAGE] Guest accessed login page");
            ViewAttributeUtils.addCommonAttributes(model, "/login");
            return "auth/login";  // Maintain direct view rendering for UI compatibility
        }
        // LOG.info("[LOGIN PAGE] User {} accessed root and is already logged in", session.getAttribute("username"));
        return "redirect:/dashboard";
    }

    /**
     * Shows dashboard with system stats.
     * Gets user, role and permission counts using dedicated services.
     *
     * @param session User session
     * @param model View data
     * @return Dashboard page or login redirect
     */
    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        if (session.getAttribute("userId") == null) {
            return "redirect:/";
        }

        Map<String, Object> stats = new HashMap<>();
        
        // Get counts using dedicated services with automatic error handling
        try {
            stats.put("totalUsers", userWebService.getAllUsers().size());
        } catch (Exception e) {
            log.warn("Could not fetch users count: {}", e.getMessage());
            stats.put("totalUsers", 0);
        }

        try {
            stats.put("totalRoles", roleWebService.getAllRoles().size());
        } catch (Exception e) {
            log.warn("Could not fetch roles count: {}", e.getMessage());
            stats.put("totalRoles", 0);
        }

        try {
            stats.put("totalPermissions", permissionWebService.getAllPermissions().size());
        } catch (Exception e) {
            log.warn("Could not fetch permissions count: {}", e.getMessage());
            stats.put("totalPermissions", 0);
        }

        model.addAttribute("stats", stats);
        ViewAttributeUtils.addCommonAttributes(model, "/dashboard");

        return "dashboard/index";
    }
} 