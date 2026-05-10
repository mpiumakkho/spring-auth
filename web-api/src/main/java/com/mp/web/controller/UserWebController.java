package com.mp.web.controller;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.mp.web.dto.RoleDto;
import com.mp.web.dto.UserDto;
import com.mp.web.mapper.DtoMapper;
import com.mp.web.service.UserWebService;
import com.mp.web.service.RoleWebService;

/**
 * Manages all user-related web pages and forms.
 * Lets staff create, view, update and delete system users.
 * Uses dedicated services with comprehensive error handling.
 */
@Slf4j
@Controller
@RequestMapping("/users")
public class UserWebController {    private static final String CURRENT_PATH = "/users";

    private final UserWebService userWebService;
    private final RoleWebService roleWebService;

    public UserWebController(UserWebService userWebService, RoleWebService roleWebService) {
        this.userWebService = userWebService;
        this.roleWebService = roleWebService;
    }

    @GetMapping
    public String listUsers(Model model) {
        List<UserDto> users = userWebService.getAllUsers().stream()
            .map(DtoMapper::toUserDto)
            .collect(Collectors.toList());
        model.addAttribute("users", users);
        model.addAttribute("currentPath", CURRENT_PATH);
        return "users/list";
    }

    @GetMapping("/create")
    public String createUserForm(Model model) {
        return "users/create";
    }

    @PostMapping("/create")
    public String createUser(
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(defaultValue = "inactive") String status,
            RedirectAttributes redirectAttributes) {
        
        UserDto user = new UserDto();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(password);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setStatus(status);

        userWebService.createUser(DtoMapper.toMap(user));
        redirectAttributes.addFlashAttribute("success", "User created successfully!");
        return "redirect:/users";
    }

    @PostMapping("/view")
    public String viewUser(@RequestParam String userId, Model model) {
        UserDto user = DtoMapper.toUserDto(userWebService.getUserById(userId));
        model.addAttribute("user", user);
        return "users/detail";
    }

    @PostMapping("/edit-form")
    public String editUserForm(@RequestParam String userId, Model model) {
        UserDto user = DtoMapper.toUserDto(userWebService.getUserById(userId));
        model.addAttribute("user", user);
        return "users/edit";
    }

    @PostMapping("/update")
    public String updateUser(
            @RequestParam String userId,
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam String status,
            @RequestParam(required = false) String password,
            RedirectAttributes redirectAttributes) {
        
        UserDto user = new UserDto();
        user.setUserId(userId);
        user.setUsername(username);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setStatus(status);
        
        if (password != null && !password.trim().isEmpty()) {
            user.setPassword(password);
        }

        userWebService.updateUser(userId, DtoMapper.toMap(user));
        redirectAttributes.addFlashAttribute("success", "User updated successfully!");
        return "redirect:/users";
    }

    @PostMapping("/delete")
    public String deleteUser(@RequestParam String userId, RedirectAttributes redirectAttributes) {
        userWebService.deleteUser(userId);
        redirectAttributes.addFlashAttribute("success", "User deleted successfully!");
        return "redirect:/users";
    }

    @PostMapping("/roles-form")
    public String manageUserRoles(@RequestParam String userId, Model model) {
        UserDto user = DtoMapper.toUserDto(userWebService.getUserById(userId));
        List<RoleDto> allRoles = roleWebService.getAllRoles().stream()
            .map(DtoMapper::toRoleDto)
            .collect(Collectors.toList());
        
        model.addAttribute("user", user);
        model.addAttribute("allRoles", allRoles);
        return "users/roles";
    }

    @PostMapping("/assign-role")
    public String assignRoleToUser(
            @RequestParam String userId,
            @RequestParam String roleId,
            RedirectAttributes redirectAttributes) {
        
        userWebService.assignRole(userId, roleId);
        redirectAttributes.addFlashAttribute("success", "Role assigned successfully!");
        return "redirect:/users";
    }

    @PostMapping("/remove-role")
    public String removeRoleFromUser(
            @RequestParam String userId,
            @RequestParam String roleId,
            RedirectAttributes redirectAttributes) {
        
        userWebService.removeRole(userId, roleId);
        redirectAttributes.addFlashAttribute("success", "Role removed successfully!");
        return "redirect:/users";
    }
} 