package com.mp.web.controller;

import com.mp.web.service.UserWebService;
import com.mp.web.service.RoleWebService;
import com.mp.web.service.PermissionWebService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.Model;
import org.springframework.ui.ConcurrentModel;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for HomeController
 */
@ExtendWith(MockitoExtension.class)
class HomeControllerTest {

    @Mock
    private UserWebService userWebService;

    @Mock
    private RoleWebService roleWebService;

    @Mock
    private PermissionWebService permissionWebService;

    @InjectMocks
    private HomeController homeController;

    private MockHttpSession session;
    private Model model;

    @BeforeEach
    void setUp() {
        session = new MockHttpSession();
        model = new ConcurrentModel();
    }

    @Test
    void home_NotLoggedIn_ReturnsLoginView() {
        // Act
        String view = homeController.home(session, model);

        // Assert
        assertEquals("auth/login", view);
    }

    @Test
    void home_LoggedIn_RedirectsToDashboard() {
        // Arrange
        session.setAttribute("userId", "1");
        session.setAttribute("username", "admin");

        // Act
        String view = homeController.home(session, model);

        // Assert
        assertEquals("redirect:/dashboard", view);
    }

    @Test
    void dashboard_NotLoggedIn_RedirectsToLogin() {
        // Act
        String view = homeController.dashboard(session, model);

        // Assert
        assertEquals("redirect:/", view);
    }

    @Test
    void dashboard_LoggedIn_LoadsStatsSuccessfully() {
        // Arrange
        session.setAttribute("userId", "1");
        session.setAttribute("username", "admin");

        List<Map<String, Object>> mockUsers = List.of(
            Map.of("userId", "1"),
            Map.of("userId", "2")
        );
        List<Map<String, Object>> mockRoles = List.of(
            Map.of("roleId", "1"),
            Map.of("roleId", "2"),
            Map.of("roleId", "3")
        );
        List<Map<String, Object>> mockPermissions = List.of(
            Map.of("permissionId", "1"),
            Map.of("permissionId", "2"),
            Map.of("permissionId", "3"),
            Map.of("permissionId", "4")
        );

        when(userWebService.getAllUsers()).thenReturn(mockUsers);
        when(roleWebService.getAllRoles()).thenReturn(mockRoles);
        when(permissionWebService.getAllPermissions()).thenReturn(mockPermissions);

        // Act
        String view = homeController.dashboard(session, model);

        // Assert
        assertEquals("dashboard/index", view);
        
        Map<String, Object> stats = (Map<String, Object>) model.getAttribute("stats");
        assertNotNull(stats);
        assertEquals(2, stats.get("totalUsers"));
        assertEquals(3, stats.get("totalRoles"));
        assertEquals(4, stats.get("totalPermissions"));

        verify(userWebService, times(1)).getAllUsers();
        verify(roleWebService, times(1)).getAllRoles();
        verify(permissionWebService, times(1)).getAllPermissions();
    }

    @Test
    void dashboard_UserServiceFails_UsesFallbackValue() {
        // Arrange
        session.setAttribute("userId", "1");

        when(userWebService.getAllUsers()).thenThrow(new RuntimeException("Service unavailable"));
        when(roleWebService.getAllRoles()).thenReturn(List.of(Map.of("roleId", "1")));
        when(permissionWebService.getAllPermissions()).thenReturn(List.of(Map.of("permissionId", "1")));

        // Act
        String view = homeController.dashboard(session, model);

        // Assert
        assertEquals("dashboard/index", view);
        
        Map<String, Object> stats = (Map<String, Object>) model.getAttribute("stats");
        assertNotNull(stats);
        assertEquals(0, stats.get("totalUsers")); // Fallback value
        assertEquals(1, stats.get("totalRoles"));
        assertEquals(1, stats.get("totalPermissions"));
    }

    @Test
    void dashboard_AllServicesFail_UsesAllFallbackValues() {
        // Arrange
        session.setAttribute("userId", "1");

        when(userWebService.getAllUsers()).thenThrow(new RuntimeException("Service unavailable"));
        when(roleWebService.getAllRoles()).thenThrow(new RuntimeException("Service unavailable"));
        when(permissionWebService.getAllPermissions()).thenThrow(new RuntimeException("Service unavailable"));

        // Act
        String view = homeController.dashboard(session, model);

        // Assert
        assertEquals("dashboard/index", view);
        
        Map<String, Object> stats = (Map<String, Object>) model.getAttribute("stats");
        assertNotNull(stats);
        assertEquals(0, stats.get("totalUsers"));
        assertEquals(0, stats.get("totalRoles"));
        assertEquals(0, stats.get("totalPermissions"));
    }
}
