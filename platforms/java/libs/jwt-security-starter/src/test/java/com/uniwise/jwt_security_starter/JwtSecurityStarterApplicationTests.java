package com.uniwise.jwt_security_starter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

class JwtSecurityStarterApplicationTests {

    private static final String PUBLIC_KEY_PATH = "classpath:keys/public.pem";
    private static final String ISSUER = "uniwise-gateway";
    private static final String AUDIENCE = "uniwise-services";

    private static KeyPair rsaKeyPair;
    private static byte[] publicKeyPemBytes;

    @BeforeAll
    static void initKeys() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        rsaKeyPair = keyPairGenerator.generateKeyPair();

        String base64Key = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8))
                .encodeToString(rsaKeyPair.getPublic().getEncoded());
        String pem = "-----BEGIN PUBLIC KEY-----\n" + base64Key + "\n-----END PUBLIC KEY-----\n";
        publicKeyPemBytes = pem.getBytes(StandardCharsets.UTF_8);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private JwtProperties defaultProperties() {
        JwtProperties properties = new JwtProperties();
        properties.setPublicKeyPath(PUBLIC_KEY_PATH);
        properties.setIssuer(ISSUER);
        properties.setAudience(AUDIENCE);
        properties.setTokenHeader("X-Auth-Token");
        return properties;
    }

    private ResourceLoader mockResourceLoader() {
        ResourceLoader resourceLoader = mock(ResourceLoader.class);
        when(resourceLoader.getResource(PUBLIC_KEY_PATH))
                .thenAnswer(invocation -> new ByteArrayResource(publicKeyPemBytes));
        return resourceLoader;
    }

    private String buildToken(String subject, String scope, String issuer, String audience, Instant expiration) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .subject(subject)
                .issuer(issuer)
                .audience().add(audience).and()
                .issuedAt(Date.from(now.minusSeconds(5)))
                .expiration(Date.from(expiration));
        if (scope != null) {
            builder.claim("scope", scope);
        }
        return builder.signWith(rsaKeyPair.getPrivate(), Jwts.SIG.RS256).compact();
    }

    @Test
    void cachedPublicKeyFieldIsVolatile() throws NoSuchFieldException {
        Field field = JwtService.class.getDeclaredField("cachedPublicKey");
        assertTrue(Modifier.isVolatile(field.getModifiers()));
    }

    @Test
    void extractClaimsVerifiesValidRs256TokenAndCachesPublicKey() {
        ResourceLoader resourceLoader = mockResourceLoader();
        JwtService jwtService = new JwtService(defaultProperties(), resourceLoader);
        String token = buildToken("acc-100", "ROLE_USER COURSE_READ", ISSUER, AUDIENCE,
                Instant.now().plusSeconds(300));

        Claims first = jwtService.extractClaims(token);
        Claims second = jwtService.extractClaims(token);

        assertEquals("acc-100", first.getSubject());
        assertEquals("ROLE_USER COURSE_READ", first.get("scope", String.class));
        assertEquals("acc-100", second.getSubject());
        verify(resourceLoader, times(1)).getResource(PUBLIC_KEY_PATH);
    }

    @Test
    void extractClaimsRejectsWrongIssuerAudienceExpiredAndNonRs256() {
        JwtService jwtService = new JwtService(defaultProperties(), mockResourceLoader());

        String wrongIssuer = buildToken("acc-1", "ROLE_USER", "other-issuer", AUDIENCE,
                Instant.now().plusSeconds(300));
        assertThrows(RuntimeException.class, () -> jwtService.extractClaims(wrongIssuer));

        String wrongAudience = buildToken("acc-1", "ROLE_USER", ISSUER, "other-audience",
                Instant.now().plusSeconds(300));
        assertThrows(RuntimeException.class, () -> jwtService.extractClaims(wrongAudience));

        String expired = buildToken("acc-1", "ROLE_USER", ISSUER, AUDIENCE,
                Instant.now().minusSeconds(60));
        assertThrows(RuntimeException.class, () -> jwtService.extractClaims(expired));

        String rs384Token = Jwts.builder()
                .subject("acc-1")
                .issuer(ISSUER)
                .audience().add(AUDIENCE).and()
                .expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(rsaKeyPair.getPrivate(), Jwts.SIG.RS384)
                .compact();
        assertThrows(RuntimeException.class, () -> jwtService.extractClaims(rs384Token));
    }

    @Test
    void jwtAuthenticationFilterSetsSecurityContextWithParsedAuthorities() throws Exception {
        JwtProperties properties = defaultProperties();
        JwtService jwtService = new JwtService(properties, mockResourceLoader());
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, properties);

        String token = buildToken("acc-42", "ROLE_ADMIN COURSE_WRITE", ISSUER, AUDIENCE,
                Instant.now().plusSeconds(300));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/courses");
        request.addHeader("X-Auth-Token", token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals("acc-42", authentication.getPrincipal());
        List<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        assertEquals(List.of("ROLE_ADMIN", "COURSE_WRITE"), authorities);
        assertNotNull(filterChain.getRequest());
    }

    @Test
    void jwtAuthenticationFilterSetsEmptyAuthoritiesWhenScopesClaimIsNull() throws Exception {
        JwtProperties properties = defaultProperties();
        JwtService jwtService = new JwtService(properties, mockResourceLoader());
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, properties);

        String tokenWithoutScope = buildToken("acc-no-scope", null, ISSUER, AUDIENCE,
                Instant.now().plusSeconds(300));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/profile/me");
        request.addHeader("X-Auth-Token", tokenWithoutScope);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals("acc-no-scope", authentication.getPrincipal());
        assertTrue(authentication.getAuthorities().isEmpty());
        assertNotNull(filterChain.getRequest());
    }

    @Test
    void jwtAuthenticationFilterClearsContextOnInvalidTokenAndPassesThroughWhenMissing() throws Exception {
        JwtProperties properties = defaultProperties();
        JwtService jwtService = new JwtService(properties, mockResourceLoader());
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, properties);

        // 1. Invalid token clears any pre-existing SecurityContext
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("stale-user", null, List.of()));
        MockHttpServletRequest invalidRequest = new MockHttpServletRequest("GET", "/api/v1/protected");
        invalidRequest.addHeader("X-Auth-Token", "not.a.valid.jwt");
        MockFilterChain invalidChain = new MockFilterChain();

        filter.doFilter(invalidRequest, new MockHttpServletResponse(), invalidChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(invalidChain.getRequest());

        // 2. Missing token leaves SecurityContext empty and continues chain
        MockHttpServletRequest anonymousRequest = new MockHttpServletRequest("GET", "/actuator/health");
        MockFilterChain anonymousChain = new MockFilterChain();

        filter.doFilter(anonymousRequest, new MockHttpServletResponse(), anonymousChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(anonymousChain.getRequest());
    }

    @Test
    void autoConfigurationDisablesServletFilterDoubleRegistration() {
        JwtProperties properties = defaultProperties();
        JwtService jwtService = new JwtService(properties, mockResourceLoader());
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, properties);
        JwtSecurityAutoConfiguration autoConfig = new JwtSecurityAutoConfiguration();

        FilterRegistrationBean<JwtAuthenticationFilter> registration = autoConfig
                .jwtAuthenticationFilterRegistration(filter);

        assertNotNull(registration);
        assertSame(filter, registration.getFilter());
        assertFalse(registration.isEnabled());
    }
}
