package com.uniwise.jwt_security_starter;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private String publicKeyPath;
    private String tokenHeader = "X-Auth-Token";
    private String issuer = "uniwise-gateway";
    private String audience = "uniwise-services";
    private boolean enabled = true;
}
