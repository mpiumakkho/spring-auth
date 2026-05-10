package com.mp.web.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.AfterEach;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class HomeControllerTest {

    private static final String SPA_URL = "http://localhost:5173";

    private HomeController homeController;
    private MockHttpSession session;
    private Model model;

    @BeforeEach
    void setUp() {
        homeController = new HomeController();
        ReflectionTestUtils.setField(homeController, "spaUrl", SPA_URL);
        session = new MockHttpSession();
        model = new ConcurrentModel();
    }

    @AfterEach
    void tearDown() {
        session.invalidate();
    }

    @Test
    void home_notLoggedIn_returnsLoginView() {
        assertEquals("auth/login", homeController.home(session, model));
    }

    @Test
    void home_loggedIn_redirectsToSpa() {
        session.setAttribute("userId", "1");
        session.setAttribute("username", "admin");
        assertEquals("redirect:" + SPA_URL + "/", homeController.home(session, model));
    }

    @Test
    void dashboard_alwaysRedirectsToSpa() {
        assertEquals("redirect:" + SPA_URL + "/", homeController.dashboard());
    }
}
