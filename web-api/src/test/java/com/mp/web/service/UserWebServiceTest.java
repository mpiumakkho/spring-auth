package com.mp.web.service;

import com.mp.web.exception.CoreApiClientException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserWebService
 */
@ExtendWith(MockitoExtension.class)
class UserWebServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private UserWebService userWebService;

    private final String coreApiUrl = "http://localhost:8091";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userWebService, "coreApiUrl", coreApiUrl);
    }

    @Test
    void getAllUsers_Success_ReturnsUserList() {
        // Arrange
        List<Map<String, Object>> mockUsers = List.of(
            Map.of("userId", "1", "username", "admin"),
            Map.of("userId", "2", "username", "user")
        );
        when(restTemplate.getForEntity(anyString(), eq(List.class)))
            .thenReturn(ResponseEntity.ok(mockUsers));

        // Act
        List<Map<String, Object>> result = userWebService.getAllUsers();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("admin", result.get(0).get("username"));
        verify(restTemplate, times(1)).getForEntity(anyString(), eq(List.class));
    }

    @Test
    void getAllUsers_HttpError_ThrowsCoreApiClientException() {
        // Arrange
        when(restTemplate.getForEntity(anyString(), eq(List.class)))
            .thenThrow(new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

        // Act & Assert
        CoreApiClientException exception = assertThrows(
            CoreApiClientException.class,
            () -> userWebService.getAllUsers()
        );
        
        assertNotNull(exception.getStatusCode());
        assertEquals("/users", exception.getRedirectPath());
    }

    @Test
    void getUserById_Success_ReturnsUser() {
        // Arrange
        Map<String, Object> mockUser = Map.of("userId", "1", "username", "admin");
        when(restTemplate.getForEntity(anyString(), eq(Map.class)))
            .thenReturn(ResponseEntity.ok(mockUser));

        // Act
        Map<String, Object> result = userWebService.getUserById("1");

        // Assert
        assertNotNull(result);
        assertEquals("admin", result.get("username"));
    }

    @Test
    void getUserById_NotFound_ThrowsCoreApiClientException() {
        // Arrange
        when(restTemplate.getForEntity(anyString(), eq(Map.class)))
            .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        // Act & Assert
        CoreApiClientException exception = assertThrows(
            CoreApiClientException.class,
            () -> userWebService.getUserById("999")
        );
        
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("RESOURCE_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void createUser_Success_ReturnsCreatedUser() {
        // Arrange
        Map<String, Object> userForm = Map.of("username", "newuser", "email", "new@example.com");
        Map<String, Object> mockResponse = Map.of("userId", "3", "username", "newuser");
        
        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
            .thenReturn(ResponseEntity.ok(mockResponse));

        // Act
        Map<String, Object> result = userWebService.createUser(userForm);

        // Assert
        assertNotNull(result);
        assertEquals("newuser", result.get("username"));
    }

    @Test
    void createUser_Conflict_ThrowsCoreApiClientException() {
        // Arrange
        Map<String, Object> userForm = Map.of("username", "existing", "email", "existing@example.com");
        
        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
            .thenThrow(new HttpClientErrorException(HttpStatus.CONFLICT));

        // Act & Assert
        CoreApiClientException exception = assertThrows(
            CoreApiClientException.class,
            () -> userWebService.createUser(userForm)
        );
        
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("DUPLICATE_RESOURCE", exception.getErrorCode());
    }

    @Test
    void createUser_BadRequest_ThrowsCoreApiClientException() {
        // Arrange
        Map<String, Object> userForm = Map.of("username", "");
        
        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
            .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

        // Act & Assert
        CoreApiClientException exception = assertThrows(
            CoreApiClientException.class,
            () -> userWebService.createUser(userForm)
        );
        
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("VALIDATION_ERROR", exception.getErrorCode());
    }

    @Test
    void deleteUser_Success_NoException() {
        // Arrange
        doNothing().when(restTemplate).delete(anyString());

        // Act & Assert
        assertDoesNotThrow(() -> userWebService.deleteUser("1"));
        verify(restTemplate, times(1)).delete(anyString());
    }

    @Test
    void deleteUser_NotFound_ThrowsCoreApiClientException() {
        // Arrange
        doThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND))
            .when(restTemplate).delete(anyString());

        // Act & Assert
        CoreApiClientException exception = assertThrows(
            CoreApiClientException.class,
            () -> userWebService.deleteUser("999")
        );
        
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }
}
