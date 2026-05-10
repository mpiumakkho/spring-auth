package com.mp.web.exception;

import lombok.extern.slf4j.Slf4j;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Global exception handler for web controllers.
 * Handles both UI-specific and core-api errors.
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    /**
     * Handle errors from core-api calls
     * Redirect back with user-friendly message
     */
    @ExceptionHandler(CoreApiClientException.class)
    public String handleCoreApiError(
            CoreApiClientException ex,
            RedirectAttributes redirectAttributes
    ) {
        log.error("Core API error [{}]: {}", ex.getStatusCode(), ex.getMessage(), ex);
        
        // Translate technical error to user-friendly message
        String userMessage = translateErrorMessage(ex);
        
        redirectAttributes.addFlashAttribute("error", userMessage);
        if (ex.getErrorCode() != null) {
            redirectAttributes.addFlashAttribute("errorCode", ex.getErrorCode());
        }
        
        return "redirect:" + ex.getRedirectPath();
    }

    /**
     * Handle session expiration
     * Redirect to login with message
     */
    @ExceptionHandler(SessionExpiredException.class)
    public String handleSessionExpired(
            SessionExpiredException ex,
            RedirectAttributes redirectAttributes
    ) {
        log.warn("Session expired: {}", ex.getMessage());
        
        redirectAttributes.addFlashAttribute("info", ex.getMessage());
        return "redirect:" + ex.getLoginPath();
    }

    /**
     * Handle validation errors
     * Return to form with field errors
     */
    @ExceptionHandler(ValidationException.class)
    public String handleValidationError(
            ValidationException ex,
            RedirectAttributes redirectAttributes
    ) {
        log.warn("Validation error: {}", ex.getMessage());
        
        redirectAttributes.addFlashAttribute("error", ex.getMessage());
        if (ex.hasFieldErrors()) {
            redirectAttributes.addFlashAttribute("fieldErrors", ex.getFieldErrors());
        }
        
        return "redirect:" + ex.getRedirectPath();
    }

    /**
     * Handle WebApiException (existing)
     * Show error on current page
     */
    @ExceptionHandler(WebApiException.class)
    public String handleWebApiError(WebApiException ex, Model model) {
        log.error("Web API error: {}", ex.getMessage(), ex);
        
        model.addAttribute("error", ex.getMessage());
        model.addAttribute("currentPath", ex.getCurrentPath());
        
        return ex.getViewName();
    }

    /**
     * Handle FormSubmissionException (existing)
     * Redirect back with flash message
     */
    @ExceptionHandler(FormSubmissionException.class)
    public String handleFormError(
            FormSubmissionException ex,
            RedirectAttributes redirectAttributes
    ) {
        log.error("Form submission error: {}", ex.getMessage(), ex);
        
        redirectAttributes.addFlashAttribute("error", ex.getMessage());
        return "redirect:" + ex.getRedirectPath();
    }

    /**
     * Handle unexpected errors
     * Show generic error page
     */
    @ExceptionHandler(Exception.class)
    public String handleUnexpectedError(Exception ex, Model model) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        
        model.addAttribute("error", "เกิดข้อผิดพลาดที่ไม่คาดคิด กรุณาลองใหม่อีกครั้ง");
        model.addAttribute("supportContact", "support@example.com");
        
        return "error";
    }

    /**
     * Translate technical error messages to user-friendly Thai messages
     */
    private String translateErrorMessage(CoreApiClientException ex) {
        String errorCode = ex.getErrorCode();
        
        // Map error codes to user-friendly messages
        if ("RESOURCE_NOT_FOUND".equals(errorCode)) {
            return "ไม่พบข้อมูลที่ค้นหา";
        } else if ("DUPLICATE_RESOURCE".equals(errorCode)) {
            return "ข้อมูลนี้มีอยู่ในระบบแล้ว";
        } else if ("BUSINESS_VALIDATION_ERROR".equals(errorCode)) {
            return "ข้อมูลไม่ถูกต้อง กรุณาตรวจสอบอีกครั้ง";
        } else if ("INSUFFICIENT_PERMISSION".equals(errorCode)) {
            return "คุณไม่มีสิทธิ์เข้าถึงข้อมูลนี้";
        } else if ("EXTERNAL_SERVICE_ERROR".equals(errorCode)) {
            return "ไม่สามารถเชื่อมต่อบริการภายนอกได้";
        } else if ("VALIDATION_ERROR".equals(errorCode)) {
            return "กรุณากรอกข้อมูลให้ถูกต้องและครบถ้วน";
        }
        
        // Default messages based on HTTP status
        if (ex.isClientError()) {
            return "ข้อมูลไม่ถูกต้อง กรุณาตรวจสอบและลองใหม่อีกครั้ง";
        } else if (ex.isServerError()) {
            return "เกิดข้อผิดพลาดจากระบบ กรุณาติดต่อผู้ดูแลระบบ";
        }
        
        // Return original message if no translation available
        return ex.getMessage();
    }
}
 