package com.platform.module.user.config;

import com.platform.module.tenant.config.ModuleTenantAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * module-user 모듈의 Spring Boot 자동 구성 진입점.
 * module-tenant(Company) 이후에 적용되어야 한다 (AuthService가 CompanyRepository를 사용).
 */
@AutoConfiguration
@AutoConfigureAfter(ModuleTenantAutoConfiguration.class)
@EntityScan(basePackages = "com.platform.module.user.domain")
@EnableJpaRepositories(basePackages = "com.platform.module.user.repository")
@ComponentScan(basePackages = "com.platform.module.user")
public class ModuleUserAutoConfiguration {
}
