package com.platform.module.pms.config;

import com.platform.module.tenant.config.ModuleTenantAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * module-pms(프로젝트 관리 시스템) 모듈의 Spring Boot 자동 구성 진입점.
 * 이 모듈을 의존성에 추가하는 것만으로 PMS 엔티티·리포지토리가 활성화된다.
 */
@AutoConfiguration
@AutoConfigureAfter(ModuleTenantAutoConfiguration.class)
@EntityScan(basePackages = "com.platform.module.pms.domain")
@EnableJpaRepositories(basePackages = "com.platform.module.pms.repository")
@ComponentScan(basePackages = "com.platform.module.pms")
public class ModulePmsAutoConfiguration {
}