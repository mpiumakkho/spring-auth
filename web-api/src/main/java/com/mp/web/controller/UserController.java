package com.mp.web.controller;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import com.mp.web.model.User;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Provides REST APIs for user and role management.
 * Works as a bridge between frontend and core API.
 */
@Slf4j
@RestController
@RequestMapping("/apis/user")
public class UserController {

    @Value("${core.api.url}")
    private String coreApiServer;

    private final RestTemplate restTemplate;

    public UserController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @PostMapping("/find-by-id")
    public ResponseEntity<?> getUserById(@RequestBody Map<String, String> request, HttpServletResponse response, HttpSession session) {
        try {
            return restTemplate.postForEntity(coreApiServer + "/api/users/find-by-id", 
                "{\"userId\":\"" + request.get("userId") + "\"}", Object.class);
        } catch (RestClientResponseException e) {
            // log.error("Error calling core-api: " + e.getMessage());
            return ResponseEntity.status(e.getStatusCode()).body("Core API Error: " + e.getMessage());
        }
    }

    @PostMapping("/find-by-username")
    public ResponseEntity<?> getUserByUsername(@RequestBody Map<String, String> request, HttpServletResponse response, HttpSession session) {
        try {
            return restTemplate.postForEntity(coreApiServer + "/api/users/find-by-username", 
                "{\"username\":\"" + request.get("username") + "\"}", Object.class);
        } catch (RestClientResponseException e) {
            // log.error("Error calling core-api: " + e.getMessage());
            return ResponseEntity.status(e.getStatusCode()).body("Core API Error: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllUsers(HttpServletResponse response, HttpSession session) {
        try {
            return restTemplate.getForEntity(coreApiServer + "/api/users", Object.class);
        } catch (RestClientResponseException e) {
            // log.error("Error calling core-api: " + e.getMessage());
            return ResponseEntity.status(e.getStatusCode()).body("Core API Error: " + e.getMessage());
        }
    }

    @PostMapping("/create")
    public ResponseEntity<?> createUser(@RequestBody User user, HttpServletResponse response, HttpSession session) {
        try {
            return restTemplate.postForEntity(coreApiServer + "/api/users/create", user, Object.class);
        } catch (RestClientResponseException e) {
            // log.error("Error calling core-api: " + e.getMessage());
            return ResponseEntity.status(e.getStatusCode()).body("Core API Error: " + e.getMessage());
        }
    }

    @PutMapping("/update")
    public ResponseEntity<?> updateUser(@RequestBody User user, HttpServletResponse response, HttpSession session) {
        try {
            return restTemplate.exchange(coreApiServer + "/api/users/update", org.springframework.http.HttpMethod.PUT, 
                new org.springframework.http.HttpEntity<>(user), Object.class);
        } catch (RestClientResponseException e) {
            // log.error("Error calling core-api: " + e.getMessage());
            return ResponseEntity.status(e.getStatusCode()).body("Core API Error: " + e.getMessage());
        }
    }

    @PostMapping("/delete")
    public ResponseEntity<?> deleteUser(@RequestBody Map<String, String> request, HttpServletResponse response, HttpSession session) {
        try {
            return restTemplate.postForEntity(coreApiServer + "/api/users/delete", 
                "{\"userId\":\"" + request.get("userId") + "\"}", Object.class);
        } catch (RestClientResponseException e) {
            // log.error("Error calling core-api: " + e.getMessage());
            return ResponseEntity.status(e.getStatusCode()).body("Core API Error: " + e.getMessage());
        }
    }

    @PostMapping("/assign-role")
    public ResponseEntity<?> assignRoleToUser(@RequestBody Map<String, String> request, HttpServletResponse response, HttpSession session) {
        try {
            String jsonBody = "{\"userId\":\"" + request.get("userId") + "\",\"roleId\":\"" + request.get("roleId") + "\"}";
            return restTemplate.postForEntity(coreApiServer + "/api/users/assign-role", jsonBody, Object.class);
        } catch (RestClientResponseException e) {
            // log.error("Error calling core-api: " + e.getMessage());
            return ResponseEntity.status(e.getStatusCode()).body("Core API Error: " + e.getMessage());
        }
    }

    @PostMapping("/remove-role")
    public ResponseEntity<?> removeRoleFromUser(@RequestBody Map<String, String> request, HttpServletResponse response, HttpSession session) {
        try {
            String jsonBody = "{\"userId\":\"" + request.get("userId") + "\",\"roleId\":\"" + request.get("roleId") + "\"}";
            return restTemplate.postForEntity(coreApiServer + "/api/users/remove-role", jsonBody, Object.class);
        } catch (RestClientResponseException e) {
            // log.error("Error calling core-api: " + e.getMessage());
            return ResponseEntity.status(e.getStatusCode()).body("Core API Error: " + e.getMessage());
        }
    }

    @PostMapping("/update-status")
    public ResponseEntity<?> updateUserStatus(@RequestBody Map<String, String> request, HttpServletResponse response, HttpSession session) {
        try {
            String jsonBody = "{\"userId\":\"" + request.get("userId") + "\",\"status\":\"" + request.get("status") + "\"}";
            return restTemplate.postForEntity(coreApiServer + "/api/users/update-status", jsonBody, Object.class);
        } catch (RestClientResponseException e) {
            // log.error("Error calling core-api: " + e.getMessage());
            return ResponseEntity.status(e.getStatusCode()).body("Core API Error: " + e.getMessage());
        }
    }

    // role management endpoint

    @GetMapping("/roles")
    public ResponseEntity<?> getAllRoles(HttpServletResponse response, HttpSession session) {
        try {
            return restTemplate.getForEntity(coreApiServer + "/api/roles", Object.class);
        } catch (RestClientResponseException e) {
            // log.error("Error calling core-api: " + e.getMessage());
            return ResponseEntity.status(e.getStatusCode()).body("Core API Error: " + e.getMessage());
        }
    }

    @PostMapping("/roles/find-by-id")
    public ResponseEntity<?> getRoleById(@RequestBody Map<String, String> request, HttpServletResponse response, HttpSession session) {
        try {
            String jsonBody = "{\"roleId\":\"" + request.get("roleId") + "\"}";
            return restTemplate.postForEntity(coreApiServer + "/api/roles/find-by-id", jsonBody, Object.class);
        } catch (RestClientResponseException e) {
            // log.error("Error calling core-api: " + e.getMessage());
            return ResponseEntity.status(e.getStatusCode()).body("Core API Error: " + e.getMessage());
        }
    }

    @PostMapping("/roles/find-by-name")
    public ResponseEntity<?> getRoleByName(@RequestBody Map<String, String> request, HttpServletResponse response, HttpSession session) {
        try {
            String jsonBody = "{\"name\":\"" + request.get("name") + "\"}";
            return restTemplate.postForEntity(coreApiServer + "/api/roles/find-by-name", jsonBody, Object.class);
        } catch (RestClientResponseException e) {
            // log.error("Error calling core-api: " + e.getMessage());
            return ResponseEntity.status(e.getStatusCode()).body("Core API Error: " + e.getMessage());
        }
    }

    @PostMapping("/roles/create")
    public ResponseEntity<?> createRole(@RequestBody Object role, HttpServletResponse response, HttpSession session) {
        try {
            return restTemplate.postForEntity(coreApiServer + "/api/roles/create", role, Object.class);
        } catch (RestClientResponseException e) {
            // log.error("Error calling core-api: " + e.getMessage());
            return ResponseEntity.status(e.getStatusCode()).body("Core API Error: " + e.getMessage());
        }
    }

    @PutMapping("/roles/update")
    public ResponseEntity<?> updateRole(@RequestBody Object role, HttpServletResponse response, HttpSession session) {
        try {
            return restTemplate.exchange(coreApiServer + "/api/roles/update", org.springframework.http.HttpMethod.PUT,
                new org.springframework.http.HttpEntity<>(role), Object.class);
        } catch (RestClientResponseException e) {
            // log.error("Error calling core-api: " + e.getMessage());
            return ResponseEntity.status(e.getStatusCode()).body("Core API Error: " + e.getMessage());
        }
    }

    @PostMapping("/roles/delete")
    public ResponseEntity<?> deleteRole(@RequestBody Map<String, String> request, HttpServletResponse response, HttpSession session) {
        try {
            String jsonBody = "{\"roleId\":\"" + request.get("roleId") + "\"}";
            return restTemplate.postForEntity(coreApiServer + "/api/roles/delete", jsonBody, Object.class);
        } catch (RestClientResponseException e) {
            // log.error("Error calling core-api: " + e.getMessage());
            return ResponseEntity.status(e.getStatusCode()).body("Core API Error: " + e.getMessage());
        }
    }

    @PostMapping("/roles/assign-permission")
    public ResponseEntity<?> assignPermissionToRole(@RequestBody Map<String, String> request, HttpServletResponse response, HttpSession session) {
        try {
            String jsonBody = "{\"roleId\":\"" + request.get("roleId") + "\",\"permissionId\":\"" + request.get("permissionId") + "\"}";
            return restTemplate.postForEntity(coreApiServer + "/api/roles/assign-permission", jsonBody, Object.class);
        } catch (RestClientResponseException e) {
            // log.error("Error calling core-api: " + e.getMessage());
            return ResponseEntity.status(e.getStatusCode()).body("Core API Error: " + e.getMessage());
        }
    }

    @PostMapping("/roles/remove-permission")
    public ResponseEntity<?> removePermissionFromRole(@RequestBody Map<String, String> request, HttpServletResponse response, HttpSession session) {
        try {
            String jsonBody = "{\"roleId\":\"" + request.get("roleId") + "\",\"permissionId\":\"" + request.get("permissionId") + "\"}";
            return restTemplate.postForEntity(coreApiServer + "/api/roles/remove-permission", jsonBody, Object.class);
        } catch (RestClientResponseException e) {
            // log.error("Error calling core-api: " + e.getMessage());
            return ResponseEntity.status(e.getStatusCode()).body("Core API Error: " + e.getMessage());
        }
    }

    @PostMapping("/roles/get-permissions")
    public ResponseEntity<?> getRolePermissions(@RequestBody Map<String, String> request, HttpServletResponse response, HttpSession session) {
        try {
            String jsonBody = "{\"roleId\":\"" + request.get("roleId") + "\"}";
            return restTemplate.postForEntity(coreApiServer + "/api/roles/get-permissions", jsonBody, Object.class);
        } catch (RestClientResponseException e) {
            // log.error("Error calling core-api: " + e.getMessage());
            return ResponseEntity.status(e.getStatusCode()).body("Core API Error: " + e.getMessage());
        }
    }

    // permission management endpoint
    
    @GetMapping("/permissions")
    public ResponseEntity<?> getAllPermissions(HttpServletResponse response, HttpSession session) {
        try {
            return restTemplate.getForEntity(coreApiServer + "/api/permissions", Object.class);
        } catch (RestClientResponseException e) {
            // log.error("Error calling core-api: " + e.getMessage());
            return ResponseEntity.status(e.getStatusCode()).body("Core API Error: " + e.getMessage());
        }
    }

    @PostMapping("/permissions/find-by-id")
    public ResponseEntity<?> getPermissionById(@RequestBody Map<String, String> request, HttpServletResponse response, HttpSession session) {
        try {
            String jsonBody = "{\"permissionId\":\"" + request.get("permissionId") + "\"}";
            return restTemplate.postForEntity(coreApiServer + "/api/permissions/find-by-id", jsonBody, Object.class);
        } catch (RestClientResponseException e) {
            // log.error("Error calling core-api: " + e.getMessage());
            return ResponseEntity.status(e.getStatusCode()).body("Core API Error: " + e.getMessage());
        }
    }

    @PostMapping("/permissions/find-by-name")
    public ResponseEntity<?> getPermissionByName(@RequestBody Map<String, String> request, HttpServletResponse response, HttpSession session) {
        try {
            String jsonBody = "{\"name\":\"" + request.get("name") + "\"}";
            return restTemplate.postForEntity(coreApiServer + "/api/permissions/find-by-name", jsonBody, Object.class);
        } catch (RestClientResponseException e) {
            // log.error("Error calling core-api: " + e.getMessage());
            return ResponseEntity.status(e.getStatusCode()).body("Core API Error: " + e.getMessage());
        }
    }

    @PostMapping("/permissions/find-by-resource")
    public ResponseEntity<?> getPermissionsByResource(@RequestBody Map<String, String> request, HttpServletResponse response, HttpSession session) {
        try {
            String jsonBody = "{\"resource\":\"" + request.get("resource") + "\"}";
            return restTemplate.postForEntity(coreApiServer + "/api/permissions/find-by-resource", jsonBody, Object.class);
        } catch (RestClientResponseException e) {
            // log.error("Error calling core-api: " + e.getMessage());
            return ResponseEntity.status(e.getStatusCode()).body("Core API Error: " + e.getMessage());
        }
    }

    @PostMapping("/permissions/find-by-resource-and-action")
    public ResponseEntity<?> getPermissionsByResourceAndAction(@RequestBody Map<String, String> request, HttpServletResponse response, HttpSession session) {
        try {
            String jsonBody = "{\"resource\":\"" + request.get("resource") + "\",\"action\":\"" + request.get("action") + "\"}";
            return restTemplate.postForEntity(coreApiServer + "/api/permissions/find-by-resource-and-action", jsonBody, Object.class);
        } catch (RestClientResponseException e) {
            // log.error("Error calling core-api: " + e.getMessage());
            return ResponseEntity.status(e.getStatusCode()).body("Core API Error: " + e.getMessage());
        }
    }

    @PostMapping("/permissions/create")
    public ResponseEntity<?> createPermission(@RequestBody Object permission, HttpServletResponse response, HttpSession session) {
        try {
            return restTemplate.postForEntity(coreApiServer + "/api/permissions/create", permission, Object.class);
        } catch (RestClientResponseException e) {
            // log.error("Error calling core-api: " + e.getMessage());
            return ResponseEntity.status(e.getStatusCode()).body("Core API Error: " + e.getMessage());
        }
    }

    @PutMapping("/permissions/update")
    public ResponseEntity<?> updatePermission(@RequestBody Object permission, HttpServletResponse response, HttpSession session) {
        try {
            return restTemplate.exchange(coreApiServer + "/api/permissions/update", org.springframework.http.HttpMethod.PUT,
                new org.springframework.http.HttpEntity<>(permission), Object.class);
        } catch (RestClientResponseException e) {
            // log.error("Error calling core-api: " + e.getMessage());
            return ResponseEntity.status(e.getStatusCode()).body("Core API Error: " + e.getMessage());
        }
    }

    @PostMapping("/permissions/delete")
    public ResponseEntity<?> deletePermission(@RequestBody Map<String, String> request, HttpServletResponse response, HttpSession session) {
        try {
            String jsonBody = "{\"permissionId\":\"" + request.get("permissionId") + "\"}";
            return restTemplate.postForEntity(coreApiServer + "/api/permissions/delete", jsonBody, Object.class);
        } catch (RestClientResponseException e) {
            // log.error("Error calling core-api: " + e.getMessage());
            return ResponseEntity.status(e.getStatusCode()).body("Core API Error: " + e.getMessage());
        }
    }

    // health check
    @GetMapping("/health")
    public ResponseEntity<?> health(HttpServletResponse response, HttpSession session) {
        try {
            return restTemplate.getForEntity(coreApiServer + "/api/health", String.class);
        } catch (RestClientResponseException e) {
            // log.error("Core API not available: " + e.getMessage());
            return ResponseEntity.ok("Web API: OK, Core API: DOWN");
        }
    }
} 