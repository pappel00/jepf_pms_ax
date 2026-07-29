package com.platform.module.code.config;

import com.platform.module.tenant.config.ModuleTenantAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * module-code 모듈의 Spring Boot 자동 구성 진입점.
 * 이 모듈을 의존성에 추가하는 것만으로 CodeGroup/CodeDetail 엔티티, 리포지토리, 서비스,
 * 컨트롤러가 자동으로 활성화된다.
 */
@AutoConfiguration
@AutoConfigureAfter(ModuleTenantAutoConfiguration.class)
@EntityScan(basePackages = "com.platform.module.code.domain")
@EnableJpaRepositories(basePackages = "com.platform.module.code.repository")
@ComponentScan(basePackages = "com.platform.module.code")
public class ModuleCodeAutoConfiguration {
}
