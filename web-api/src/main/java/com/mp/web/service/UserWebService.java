package com.mp.web.service;

import lombok.extern.slf4j.Slf4j;

import com.mp.web.exception.CoreApiClientException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Service layer for User operations via core-api
 * Handles REST calls and translates exceptions to user-friendly messages
 */
@Slf4j
@Service
public class UserWebService {    
    @Autowired
    private RestTemplate restTemplate;
    
    @Value("${core.api.url}")
    private String coreApiUrl;

    /**
     * Get all users from core-api
     */
    public List<Map<String, Object>> getAllUsers() {
        try {
            ResponseEntity<List> response = restTemplate.getForEntity(
                coreApiUrl + "/api/users",
                List.class
            );
            return response.getBody();
            
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            log.error("Failed to get users: {}", ex.getMessage());
            throw new CoreApiClientException(
                "ไม่สามารถดึงข้อมูลผู้ใช้ได้",
                HttpStatus.resolve(ex.getStatusCode().value()),
                "/users",
                ex
            );
        }
    }

    /**
     * Get user by ID
     */
    public Map<String, Object> getUserById(String userId) {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                coreApiUrl + "/api/users/" + userId,
                Map.class
            );
            return response.getBody();
            
        } catch (HttpClientErrorException.NotFound ex) {
            throw new CoreApiClientException(
                "ไม่พบผู้ใช้ที่ต้องการ",
                HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND",
                "/users"
            );
            
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            throw new CoreApiClientException(
                "ไม่สามารถดึงข้อมูลผู้ใช้ได้",
                HttpStatus.resolve(ex.getStatusCode().value()),
                "/users",
                ex
            );
        }
    }

    /**
     * Create new user via core-api
     */
    public Map<String, Object> createUser(Map<String, Object> userForm) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(userForm, headers);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(
                coreApiUrl + "/api/users",
                request,
                Map.class
            );
            
            log.info("User created successfully");
            return response.getBody();
            
        } catch (HttpClientErrorException.BadRequest ex) {
            // 400 - Validation error
            log.warn("Validation error creating user: {}", ex.getResponseBodyAsString());
            throw new CoreApiClientException(
                "ข้อมูลไม่ถูกต้อง กรุณาตรวจสอบอีกครั้ง",
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "/users/new"
            );
            
        } catch (HttpClientErrorException.Conflict ex) {
            // 409 - Duplicate resource
            log.warn("Duplicate user: {}", ex.getResponseBodyAsString());
            throw new CoreApiClientException(
                "ชื่อผู้ใช้หรืออีเมลนี้มีคนใช้แล้ว",
                HttpStatus.CONFLICT,
                "DUPLICATE_RESOURCE",
                "/users/new"
            );
            
        } catch (HttpClientErrorException.Forbidden ex) {
            // 403 - Access denied
            throw new CoreApiClientException(
                "คุณไม่มีสิทธิ์สร้างผู้ใช้",
                HttpStatus.FORBIDDEN,
                "INSUFFICIENT_PERMISSION",
                "/users"
            );
            
        } catch (HttpClientErrorException ex) {
            // Other 4xx errors
            log.error("Client error creating user: {}", ex.getMessage());
            throw new CoreApiClientException(
                "ไม่สามารถสร้างผู้ใช้ได้ กรุณาลองใหม่อีกครั้ง",
                HttpStatus.resolve(ex.getStatusCode().value()),
                "/users/new",
                ex
            );
            
        } catch (HttpServerErrorException ex) {
            // 5xx - Server errors
            log.error("Server error creating user: {}", ex.getMessage());
            throw new CoreApiClientException(
                "เกิดข้อผิดพลาดจากระบบ กรุณาลองใหม่อีกครั้ง",
                HttpStatus.resolve(ex.getStatusCode().value()),
                "/users/new",
                ex
            );
        }
    }

    /**
     * Update existing user
     */
    public Map<String, Object> updateUser(String userId, Map<String, Object> userForm) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(userForm, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                coreApiUrl + "/api/users/" + userId,
                HttpMethod.PUT,
                request,
                Map.class
            );
            
            log.info("User {} updated successfully", userId);
            return response.getBody();
            
        } catch (HttpClientErrorException.NotFound ex) {
            throw new CoreApiClientException(
                "ไม่พบผู้ใช้ที่ต้องการแก้ไข",
                HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND",
                "/users"
            );
            
        } catch (HttpClientErrorException.Conflict ex) {
            throw new CoreApiClientException(
                "ชื่อผู้ใช้นี้มีคนใช้แล้ว",
                HttpStatus.CONFLICT,
                "DUPLICATE_RESOURCE",
                "/users/" + userId + "/edit"
            );
            
        } catch (HttpClientErrorException.Forbidden ex) {
            throw new CoreApiClientException(
                "คุณไม่มีสิทธิ์แก้ไขผู้ใช้นี้",
                HttpStatus.FORBIDDEN,
                "INSUFFICIENT_PERMISSION",
                "/users"
            );
            
        } catch (HttpClientErrorException ex) {
            log.error("Client error updating user {}: {}", userId, ex.getMessage());
            throw new CoreApiClientException(
                "ไม่สามารถแก้ไขข้อมูลได้ กรุณาตรวจสอบอีกครั้ง",
                HttpStatus.resolve(ex.getStatusCode().value()),
                "/users/" + userId + "/edit",
                ex
            );
            
        } catch (HttpServerErrorException ex) {
            log.error("Server error updating user {}: {}", userId, ex.getMessage());
            throw new CoreApiClientException(
                "เกิดข้อผิดพลาดจากระบบ กรุณาลองใหม่อีกครั้ง",
                HttpStatus.resolve(ex.getStatusCode().value()),
                "/users/" + userId + "/edit",
                ex
            );
        }
    }

    /**
     * Delete user by ID
     */
    public void deleteUser(String userId) {
        try {
            restTemplate.delete(coreApiUrl + "/api/users/" + userId);
            log.info("User {} deleted successfully", userId);
            
        } catch (HttpClientErrorException.NotFound ex) {
            throw new CoreApiClientException(
                "ไม่พบผู้ใช้ที่ต้องการลบ",
                HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND",
                "/users"
            );
            
        } catch (HttpClientErrorException.Conflict ex) {
            throw new CoreApiClientException(
                "ไม่สามารถลบผู้ใช้นี้ได้ เนื่องจากมีข้อมูลที่เกี่ยวข้อง",
                HttpStatus.CONFLICT,
                "BUSINESS_VALIDATION_ERROR",
                "/users"
            );
            
        } catch (HttpClientErrorException.Forbidden ex) {
            throw new CoreApiClientException(
                "คุณไม่มีสิทธิ์ลบผู้ใช้นี้",
                HttpStatus.FORBIDDEN,
                "INSUFFICIENT_PERMISSION",
                "/users"
            );
            
        } catch (HttpClientErrorException ex) {
            log.error("Client error deleting user {}: {}", userId, ex.getMessage());
            throw new CoreApiClientException(
                "ไม่สามารถลบผู้ใช้ได้",
                HttpStatus.resolve(ex.getStatusCode().value()),
                "/users",
                ex
            );
            
        } catch (HttpServerErrorException ex) {
            log.error("Server error deleting user {}: {}", userId, ex.getMessage());
            throw new CoreApiClientException(
                "เกิดข้อผิดพลาดจากระบบ กรุณาลองใหม่อีกครั้ง",
                HttpStatus.resolve(ex.getStatusCode().value()),
                "/users",
                ex
            );
        }
    }

    /**
     * Activate user
     */
    public void activateUser(String userId) {
        try {
            restTemplate.put(coreApiUrl + "/api/users/" + userId + "/activate", null);
            log.info("User {} activated", userId);
            
        } catch (HttpClientErrorException.NotFound ex) {
            throw new CoreApiClientException(
                "ไม่พบผู้ใช้ที่ต้องการเปิดใช้งาน",
                HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND",
                "/users"
            );
            
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            throw new CoreApiClientException(
                "ไม่สามารถเปิดใช้งานผู้ใช้ได้",
                HttpStatus.resolve(ex.getStatusCode().value()),
                "/users",
                ex
            );
        }
    }

    /**
     * Deactivate user
     */
    public void deactivateUser(String userId) {
        try {
            restTemplate.put(coreApiUrl + "/api/users/" + userId + "/deactivate", null);
            log.info("User {} deactivated", userId);
            
        } catch (HttpClientErrorException.NotFound ex) {
            throw new CoreApiClientException(
                "ไม่พบผู้ใช้ที่ต้องการปิดใช้งาน",
                HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND",
                "/users"
            );
            
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            throw new CoreApiClientException(
                "ไม่สามารถปิดใช้งานผู้ใช้ได้",
                HttpStatus.resolve(ex.getStatusCode().value()),
                "/users",
                ex
            );
        }
    }

    /**
     * Assign role to user
     */
    public void assignRole(String userId, String roleId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, String> request = Map.of("roleId", roleId);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);
            
            restTemplate.postForEntity(
                coreApiUrl + "/api/users/" + userId + "/roles",
                entity,
                Void.class
            );
            
            log.info("Role {} assigned to user {}", roleId, userId);
            
        } catch (HttpClientErrorException.NotFound ex) {
            throw new CoreApiClientException(
                "ไม่พบผู้ใช้หรือบทบาทที่เลือก",
                HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND",
                "/users/" + userId
            );
            
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            throw new CoreApiClientException(
                "ไม่สามารถกำหนดบทบาทได้",
                HttpStatus.resolve(ex.getStatusCode().value()),
                "/users/" + userId,
                ex
            );
        }
    }

    /**
     * Remove role from user
     */
    public void removeRole(String userId, String roleId) {
        try {
            restTemplate.delete(
                coreApiUrl + "/api/users/" + userId + "/roles/" + roleId
            );
            
            log.info("Role {} removed from user {}", roleId, userId);
            
        } catch (HttpClientErrorException.NotFound ex) {
            throw new CoreApiClientException(
                "ไม่พบผู้ใช้หรือบทบาทที่เลือก",
                HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND",
                "/users/" + userId
            );
            
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            throw new CoreApiClientException(
                "ไม่สามารถลบบทบาทได้",
                HttpStatus.resolve(ex.getStatusCode().value()),
                "/users/" + userId,
                ex
            );
        }
    }
}
