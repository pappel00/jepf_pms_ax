package com.platform.framework.security.config;

import com.platform.framework.security.tenant.TenantIdentifierResolver;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MAD 4200 core/tenant: 별도 DataSource 라우팅 없이, 공유 스키마 + tenant_id 컬럼 방식으로
 * 멀티테넌시를 구현한다 (Hibernate @TenantId 파티션 전략).
 */
@Configuration
public class TenantHibernateConfig {

    @Bean
    public CurrentTenantIdentifierResolver<String> tenantIdentifierResolver() {
        return new TenantIdentifierResolver();
    }

    @Bean
    public HibernatePropertiesCustomizer tenantIdentifierResolverCustomizer(CurrentTenantIdentifierResolver<String> resolver) {
        return properties -> properties.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, resolver);
    }
}
