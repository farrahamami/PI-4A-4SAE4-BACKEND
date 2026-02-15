package com.esprit.microservice.adsservice.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Slf4j
public class SecurityUtils {

    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            log.error("[SecurityUtils] No authenticated user in security context");
            return null;
        }

        Object details = authentication.getDetails();
        
        if (details instanceof Long) {
            return (Long) details;
        }
        
        log.error("[SecurityUtils] User ID not found in authentication details. Details type: {}", 
                details != null ? details.getClass().getName() : "null");
        return null;
    }
}
