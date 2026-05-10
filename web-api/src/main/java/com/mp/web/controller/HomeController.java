package com.mp.web.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.mp.web.utils.ViewAttributeUtils;

import jakarta.servlet.http.HttpSession;

/**
 * Now that user/role/permission CRUD lives in the SPA (served via the BFF),
 * Thymeleaf only renders the public login landing. Everything authenticated
 * is the SPA's job — `/dashboard` simply redirects there.
 */
@Controller
public class HomeController {

    @Value("${bff.spa.url:http://localhost:5173}")
    private String spaUrl;

    @GetMapping({"/", "/login"})
    public String home(HttpSession session, Model model) {
        if (session.getAttribute("userId") != null) {
            return "redirect:" + spaUrl + "/";
        }
        ViewAttributeUtils.addCommonAttributes(model, "/login");
        return "auth/login";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "redirect:" + spaUrl + "/";
    }
}
