package com.uniwise.jwt_security_starter;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.io.ResourceLoader;

@AutoConfiguration
@EnableConfigurationProperties(JwtProperties.class)
@ComponentScan(basePackages = "com.uniwise.jwt_security_starter")
@ConditionalOnProperty(prefix = "jwt", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JwtSecurityAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public JwtService jwtService(JwtProperties jwtProperties, ResourceLoader resourceLoader) {
        return new JwtService(jwtProperties, resourceLoader);
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnMissingBean(name = "jwtAuthenticationFilterRegistration")
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtAuthenticationFilterRegistration(
            JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
