package com.mp.web.controller;

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
import com.mp.web.mapper.DtoMapper;
import com.mp.web.service.PermissionWebService;

import lombok.extern.slf4j.Slf4j;

/**
 * Manages all permission-related web pages and forms.
 * Lets staff create, view, update and delete system permissions.
 * Permissions define what actions can be performed on resources.
 * Uses dedicated services with comprehensive error handling.
 */
@Slf4j
@Controller
@RequestMapping("/permissions")
public class PermissionWebController {
    private static final String CURRENT_PATH = "/permissions";

    private final PermissionWebService permissionWebService;

    public PermissionWebController(PermissionWebService permissionWebService) {
        this.permissionWebService = permissionWebService;
    }

    @GetMapping
    public String listPermissions(Model model) {
        List<PermissionDto> permissions = permissionWebService.getAllPermissions().stream()
            .map(DtoMapper::toPermissionDto)
            .collect(Collectors.toList());
        model.addAttribute("permissions", permissions);
        model.addAttribute("currentPath", CURRENT_PATH);
        return "permissions/list";
    }

    @GetMapping("/create")
    public String createPermissionForm(Model model) {
        model.addAttribute("currentPath", CURRENT_PATH);
        return "permissions/create";
    }

    @PostMapping("/create")
    public String createPermission(
            @RequestParam String name,
            @RequestParam String resource,
            @RequestParam String action,
            @RequestParam(required = false) String description,
            RedirectAttributes redirectAttributes) {
        
        PermissionDto permission = new PermissionDto();
        permission.setName(name);
        permission.setResource(resource);
        permission.setAction(action);
        permission.setDescription(description);

        permissionWebService.createPermission(DtoMapper.toMap(permission));
        redirectAttributes.addFlashAttribute("success", "Permission created successfully!");
        return "redirect:/permissions";
    }

    @PostMapping("/view")
    public String viewPermission(@RequestParam String permissionId, Model model) {
        PermissionDto permission = DtoMapper.toPermissionDto(permissionWebService.getPermissionById(permissionId));
        model.addAttribute("permission", permission);
        return "permissions/detail";
    }

    @PostMapping("/edit-form")
    public String editPermissionForm(@RequestParam String permissionId, Model model) {
        PermissionDto permission = DtoMapper.toPermissionDto(permissionWebService.getPermissionById(permissionId));
        model.addAttribute("permission", permission);
        return "permissions/edit";
    }

    @PostMapping("/update")
    public String updatePermission(
            @RequestParam String permissionId,
            @RequestParam String name,
            @RequestParam String resource,
            @RequestParam String action,
            @RequestParam(required = false) String description,
            RedirectAttributes redirectAttributes) {
        
        PermissionDto permission = new PermissionDto();
        permission.setPermissionId(permissionId);
        permission.setName(name);
        permission.setResource(resource);
        permission.setAction(action);
        permission.setDescription(description);

        permissionWebService.updatePermission(permissionId, DtoMapper.toMap(permission));
        redirectAttributes.addFlashAttribute("success", "Permission updated successfully!");
        return "redirect:/permissions";
    }

    @PostMapping("/delete")
    public String deletePermission(@RequestParam String permissionId, RedirectAttributes redirectAttributes) {
        permissionWebService.deletePermission(permissionId);
        redirectAttributes.addFlashAttribute("success", "Permission deleted successfully!");
        return "redirect:/permissions";
    }

    @GetMapping("/search")
    public String searchPermissions(
            @RequestParam(required = false) String resource,
            @RequestParam(required = false) String action,
            Model model) {
        
        List<PermissionDto> searchResults;

        if (resource != null && !resource.trim().isEmpty() && action != null && !action.trim().isEmpty()) {
            searchResults = permissionWebService.findByResourceAndAction(resource.trim(), action.trim()).stream()
                .map(DtoMapper::toPermissionDto)
                .collect(Collectors.toList());
        } else if (resource != null && !resource.trim().isEmpty()) {
            searchResults = permissionWebService.findByResource(resource.trim()).stream()
                .map(DtoMapper::toPermissionDto)
                .collect(Collectors.toList());
        } else {
            searchResults = permissionWebService.getAllPermissions().stream()
                .map(DtoMapper::toPermissionDto)
                .collect(Collectors.toList());
        }

        model.addAttribute("permissions", searchResults);
        model.addAttribute("searchResource", resource);
        model.addAttribute("searchAction", action);
        return "permissions/search";
    }
} 