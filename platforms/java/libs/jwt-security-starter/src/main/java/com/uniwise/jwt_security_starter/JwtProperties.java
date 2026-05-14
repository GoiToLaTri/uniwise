package com.uniwise.jwt_security_starter;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private String publicKeyPath;
    private String tokenHeader = "X-Token";
    private boolean enabled = true;
}
