package com.mp.core.security;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Activates Hibernate's "tenantFilter" on the current Session before any service
 * method runs, scoped to the active TenantContext. SUPER_ADMIN tokens skip the
 * filter entirely.
 *
 * The filter is enabled idempotently — if a service method calls into another
 * service method, both invocations re-enable with the same tenantId. We do NOT
 * disable in a finally block, since that would tear down the filter while the
 * outer caller still needs it. The session is closed by Spring at request end.
 */
@Aspect
@Component
public class TenantFilterAspect {

    @PersistenceContext
    private EntityManager entityManager;

    @Around("execution(public * com.mp.core.service..*.*(..))")
    public Object enableTenantFilter(ProceedingJoinPoint pjp) throws Throwable {
        String tenantId = TenantContext.get();
        if (tenantId != null && !TenantContext.isSuperAdmin()) {
            try {
                Session session = entityManager.unwrap(Session.class);
                Filter existing = session.getEnabledFilter("tenantFilter");
                if (existing == null) {
                    session.enableFilter("tenantFilter").setParameter("tenantId", tenantId);
                }
            } catch (IllegalStateException e) {
                // No active session yet — Spring will open one when the @Transactional
                // method begins. The aspect re-runs on the next inner call once a session exists.
            }
        }
        return pjp.proceed();
    }
}
