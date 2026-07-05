package com.intern.trustai.security;


import jakarta.persistence.EntityManager;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class TenantAspect {

    private final EntityManager entityManager;

    public TenantAspect(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Before("execution(* com.intern.trustai.service..*(..)) || execution(* com.intern.trustai.repository..*(..))")
    public void enableTenantFilter(){
        String tenanytId= TenantContext.getCurrentTenant();
        if(tenanytId!=null){
            Session session = entityManager.unwrap(Session.class);
            session.enableFilter("tenantFilter").setParameter("tenantId", tenanytId);
        }

    }

}
