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

import com.mp.web.dto.PermissionDto;
import com.mp.web.dto.RoleDto;
import com.mp.web.mapper.DtoMapper;
import com.mp.web.service.RoleWebService;
import com.mp.web.service.PermissionWebService;

/**
 * Manages all role-related web pages and forms.
 * Lets staff create, view, update and delete system roles.
 * Also handles role-permission assignments.
 * Uses dedicated services with comprehensive error handling.
 */
@Slf4j
@Controller
@RequestMapping("/roles")
public class RoleWebController {    private static final String CURRENT_PATH = "/roles";

    private final RoleWebService roleWebService;
    private final PermissionWebService permissionWebService;

    public RoleWebController(RoleWebService roleWebService, PermissionWebService permissionWebService) {
        this.roleWebService = roleWebService;
        this.permissionWebService = permissionWebService;
    }

    @GetMapping
    public String listRoles(Model model) {
        List<RoleDto> roles = roleWebService.getAllRoles().stream()
            .map(DtoMapper::toRoleDto)
            .collect(Collectors.toList());
        model.addAttribute("roles", roles);
        model.addAttribute("currentPath", CURRENT_PATH);
        return "roles/list";
    }

    @GetMapping("/create")
    public String createRoleForm(Model model) {
        model.addAttribute("currentPath", CURRENT_PATH);
        return "roles/create";
    }

    @PostMapping("/create")
    public String createRole(
            @RequestParam String name,
            @RequestParam(required = false) String description,
            RedirectAttributes redirectAttributes) {
        
        RoleDto role = new RoleDto();
        role.setName(name);
        role.setDescription(description);

        roleWebService.createRole(DtoMapper.toMap(role));
        redirectAttributes.addFlashAttribute("success", "Role created successfully!");
        return "redirect:/roles";
    }

    @PostMapping("/view")
    public String viewRole(@RequestParam String roleId, Model model) {
        RoleDto role = DtoMapper.toRoleDto(roleWebService.getRoleById(roleId));
        model.addAttribute("role", role);
        return "roles/detail";
    }

    @PostMapping("/edit-form")
    public String editRoleForm(@RequestParam String roleId, Model model) {
        RoleDto role = DtoMapper.toRoleDto(roleWebService.getRoleById(roleId));
        model.addAttribute("role", role);
        return "roles/edit";
    }

    @PostMapping("/update")
    public String updateRole(
            @RequestParam String roleId,
            @RequestParam String name,
            @RequestParam(required = false) String description,
            RedirectAttributes redirectAttributes) {
        
        RoleDto role = new RoleDto();
        role.setRoleId(roleId);
        role.setName(name);
        role.setDescription(description);

        roleWebService.updateRole(roleId, DtoMapper.toMap(role));
        redirectAttributes.addFlashAttribute("success", "Role updated successfully!");
        return "redirect:/roles";
    }

    @PostMapping("/delete")
    public String deleteRole(@RequestParam String roleId, RedirectAttributes redirectAttributes) {
        roleWebService.deleteRole(roleId);
        redirectAttributes.addFlashAttribute("success", "Role deleted successfully!");
        return "redirect:/roles";
    }

    @PostMapping("/permissions-form")
    public String manageRolePermissions(@RequestParam String roleId, Model model) {
        RoleDto role = DtoMapper.toRoleDto(roleWebService.getRoleById(roleId));
        List<PermissionDto> rolePermissions = roleWebService.getRolePermissions(roleId).stream()
            .map(DtoMapper::toPermissionDto)
            .collect(Collectors.toList());
        List<PermissionDto> allPermissions = permissionWebService.getAllPermissions().stream()
            .map(DtoMapper::toPermissionDto)
            .collect(Collectors.toList());

        model.addAttribute("role", role);
        model.addAttribute("rolePermissions", rolePermissions);
        model.addAttribute("allPermissions", allPermissions);
        return "roles/permissions";
    }

    @PostMapping("/assign-permission")
    public String assignPermissionToRole(
            @RequestParam String roleId,
            @RequestParam String permissionId,
            RedirectAttributes redirectAttributes) {
        
        roleWebService.assignPermission(roleId, permissionId);
        redirectAttributes.addFlashAttribute("success", "Permission assigned successfully!");
        return "redirect:/roles";
    }

    @PostMapping("/remove-permission")
    public String removePermissionFromRole(
            @RequestParam String roleId,
            @RequestParam String permissionId,
            RedirectAttributes redirectAttributes) {
        
        roleWebService.removePermission(roleId, permissionId);
        redirectAttributes.addFlashAttribute("success", "Permission removed successfully!");
        return "redirect:/roles";
    }
} 