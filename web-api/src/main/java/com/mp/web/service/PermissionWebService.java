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
 * Service layer for Permission operations via core-api
 * Handles REST calls and translates exceptions to user-friendly messages
 */
@Slf4j
@Service
public class PermissionWebService {    
    @Autowired
    private RestTemplate restTemplate;
    
    @Value("${core.api.url}")
    private String coreApiUrl;

    /**
     * Get all permissions from core-api
     */
    public List<Map<String, Object>> getAllPermissions() {
        try {
            ResponseEntity<List> response = restTemplate.getForEntity(
                coreApiUrl + "/api/permissions",
                List.class
            );
            return response.getBody();
            
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            log.error("Failed to get permissions: {}", ex.getMessage());
            throw new CoreApiClientException(
                "ไม่สามารถดึงข้อมูล permissions ได้",
                HttpStatus.resolve(ex.getStatusCode().value()),
                "/permissions",
                ex
            );
        }
    }

    /**
     * Get permission by ID
     */
    public Map<String, Object> getPermissionById(String permissionId) {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                coreApiUrl + "/api/permissions/" + permissionId,
                Map.class
            );
            return response.getBody();
            
        } catch (HttpClientErrorException.NotFound ex) {
            throw new CoreApiClientException(
                "ไม่พบ permission ที่ต้องการ",
                HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND",
                "/permissions"
            );
            
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            throw new CoreApiClientException(
                "ไม่สามารถดึงข้อมูล permission ได้",
                HttpStatus.resolve(ex.getStatusCode().value()),
                "/permissions",
                ex
            );
        }
    }

    /**
     * Create new permission via core-api
     */
    public Map<String, Object> createPermission(Map<String, Object> permissionForm) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(permissionForm, headers);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(
                coreApiUrl + "/api/permissions",
                request,
                Map.class
            );
            
            log.info("Permission created successfully");
            return response.getBody();
            
        } catch (HttpClientErrorException.BadRequest ex) {
            log.warn("Validation error creating permission: {}", ex.getResponseBodyAsString());
            throw new CoreApiClientException(
                "ข้อมูลไม่ถูกต้อง กรุณาตรวจสอบอีกครั้ง",
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "/permissions/create"
            );
            
        } catch (HttpClientErrorException.Conflict ex) {
            log.warn("Duplicate permission: {}", ex.getResponseBodyAsString());
            throw new CoreApiClientException(
                "Permission นี้มีอยู่แล้ว",
                HttpStatus.CONFLICT,
                "DUPLICATE_RESOURCE",
                "/permissions/create"
            );
            
        } catch (HttpClientErrorException.Forbidden ex) {
            throw new CoreApiClientException(
                "คุณไม่มีสิทธิ์สร้าง permission",
                HttpStatus.FORBIDDEN,
                "INSUFFICIENT_PERMISSION",
                "/permissions"
            );
            
        } catch (HttpClientErrorException ex) {
            log.error("Client error creating permission: {}", ex.getMessage());
            throw new CoreApiClientException(
                "ไม่สามารถสร้าง permission ได้ กรุณาลองใหม่อีกครั้ง",
                HttpStatus.resolve(ex.getStatusCode().value()),
                "/permissions/create",
                ex
            );
            
        } catch (HttpServerErrorException ex) {
            log.error("Server error creating permission: {}", ex.getMessage());
            throw new CoreApiClientException(
                "เกิดข้อผิดพลาดจากระบบ กรุณาลองใหม่อีกครั้ง",
                HttpStatus.resolve(ex.getStatusCode().value()),
                "/permissions/create",
                ex
            );
        }
    }

    /**
     * Update existing permission
     */
    public Map<String, Object> updatePermission(String permissionId, Map<String, Object> permissionForm) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(permissionForm, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                coreApiUrl + "/api/permissions/" + permissionId,
                HttpMethod.PUT,
                request,
                Map.class
            );
            
            log.info("Permission {} updated successfully", permissionId);
            return response.getBody();
            
        } catch (HttpClientErrorException.NotFound ex) {
            throw new CoreApiClientException(
                "ไม่พบ permission ที่ต้องการแก้ไข",
                HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND",
                "/permissions"
            );
            
        } catch (HttpClientErrorException.Conflict ex) {
            throw new CoreApiClientException(
                "Permission นี้มีอยู่แล้ว",
                HttpStatus.CONFLICT,
                "DUPLICATE_RESOURCE",
                "/permissions/" + permissionId + "/edit"
            );
            
        } catch (HttpClientErrorException.Forbidden ex) {
            throw new CoreApiClientException(
                "คุณไม่มีสิทธิ์แก้ไข permission นี้",
                HttpStatus.FORBIDDEN,
                "INSUFFICIENT_PERMISSION",
                "/permissions"
            );
            
        } catch (HttpClientErrorException ex) {
            log.error("Client error updating permission {}: {}", permissionId, ex.getMessage());
            throw new CoreApiClientException(
                "ไม่สามารถแก้ไขข้อมูลได้ กรุณาตรวจสอบอีกครั้ง",
                HttpStatus.resolve(ex.getStatusCode().value()),
                "/permissions/" + permissionId + "/edit",
                ex
            );
            
        } catch (HttpServerErrorException ex) {
            log.error("Server error updating permission {}: {}", permissionId, ex.getMessage());
            throw new CoreApiClientException(
                "เกิดข้อผิดพลาดจากระบบ กรุณาลองใหม่อีกครั้ง",
                HttpStatus.resolve(ex.getStatusCode().value()),
                "/permissions/" + permissionId + "/edit",
                ex
            );
        }
    }

    /**
     * Delete permission by ID
     */
    public void deletePermission(String permissionId) {
        try {
            restTemplate.delete(coreApiUrl + "/api/permissions/" + permissionId);
            log.info("Permission {} deleted successfully", permissionId);
            
        } catch (HttpClientErrorException.NotFound ex) {
            throw new CoreApiClientException(
                "ไม่พบ permission ที่ต้องการลบ",
                HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND",
                "/permissions"
            );
            
        } catch (HttpClientErrorException.Conflict ex) {
            throw new CoreApiClientException(
                "ไม่สามารถลบ permission นี้ได้ เนื่องจากมีข้อมูลที่เกี่ยวข้อง",
                HttpStatus.CONFLICT,
                "BUSINESS_VALIDATION_ERROR",
                "/permissions"
            );
            
        } catch (HttpClientErrorException.Forbidden ex) {
            throw new CoreApiClientException(
                "คุณไม่มีสิทธิ์ลบ permission นี้",
                HttpStatus.FORBIDDEN,
                "INSUFFICIENT_PERMISSION",
                "/permissions"
            );
            
        } catch (HttpClientErrorException ex) {
            log.error("Client error deleting permission {}: {}", permissionId, ex.getMessage());
            throw new CoreApiClientException(
                "ไม่สามารถลบ permission ได้",
                HttpStatus.resolve(ex.getStatusCode().value()),
                "/permissions",
                ex
            );
            
        } catch (HttpServerErrorException ex) {
            log.error("Server error deleting permission {}: {}", permissionId, ex.getMessage());
            throw new CoreApiClientException(
                "เกิดข้อผิดพลาดจากระบบ กรุณาลองใหม่อีกครั้ง",
                HttpStatus.resolve(ex.getStatusCode().value()),
                "/permissions",
                ex
            );
        }
    }

    /**
     * Find permissions by resource
     */
    public List<Map<String, Object>> findByResource(String resource) {
        try {
            ResponseEntity<List> response = restTemplate.getForEntity(
                coreApiUrl + "/api/permissions/search?resource=" + resource,
                List.class
            );
            return response.getBody();
            
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            log.error("Failed to search permissions: {}", ex.getMessage());
            throw new CoreApiClientException(
                "ไม่สามารถค้นหา permissions ได้",
                HttpStatus.resolve(ex.getStatusCode().value()),
                "/permissions",
                ex
            );
        }
    }

    /**
     * Find permissions by resource and action
     */
    public List<Map<String, Object>> findByResourceAndAction(String resource, String action) {
        try {
            ResponseEntity<List> response = restTemplate.getForEntity(
                coreApiUrl + "/api/permissions/search?resource=" + resource + "&action=" + action,
                List.class
            );
            return response.getBody();
            
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            log.error("Failed to search permissions: {}", ex.getMessage());
            throw new CoreApiClientException(
                "ไม่สามารถค้นหา permissions ได้",
                HttpStatus.resolve(ex.getStatusCode().value()),
                "/permissions",
                ex
            );
        }
    }
}
