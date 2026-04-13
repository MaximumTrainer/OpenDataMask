package com.opendatamask.infrastructure.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.csrf.CookieCsrfTokenRepository
import org.springframework.security.web.util.matcher.AntPathRequestMatcher

// SAML2 Single Sign-On security filter chain.
//
// This configuration is activated only when BOTH conditions are true:
//   1. The `spring-security-saml2-service-provider` library is on the classpath, AND
//   2. The env var SAML_IDP_METADATA_URI is set to a non-empty value, which binds
//      `spring.security.saml2.relyingparty.registration.default.assertingparty.metadata-uri`.
//      The property is NOT defined in application.yml with an empty default, so it is
//      absent from the Spring Environment unless the env var is explicitly provided.
//      This ensures `@ConditionalOnProperty` does not activate on empty/blank values.
//
// To enable SAML SSO:
//   1. Add the Shibboleth repository and the SAML SP dependency to build.gradle.kts:
//        repositories {
//            maven { url = uri("https://build.shibboleth.net/nexus/content/repositories/releases/") }
//        }
//        dependencies {
//            implementation("org.springframework.security:spring-security-saml2-service-provider")
//        }
//   2. Set the required environment variables (see application.yml comments):
//        SAML_IDP_METADATA_URI=https://idp.example.com/metadata
//        SAML_SP_ENTITY_ID=https://your-app.example.com
//        SAML_SP_PRIVATE_KEY=classpath:saml/sp-private-key.pem
//        SAML_SP_CERTIFICATE=classpath:saml/sp-certificate.pem
//        SAML_IDP_SSO_URL=https://idp.example.com/sso
@Configuration
@ConditionalOnClass(name = ["org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository"])
@ConditionalOnProperty(
    name = ["spring.security.saml2.relyingparty.registration.default.assertingparty.metadata-uri"],
    matchIfMissing = false
)
class SamlSecurityConfig(
    private val corsConfig: CorsConfig
) {

    // SAML2 web security filter chain (order=2): intercepts browser routes before the
    // fallback webSecurityFilterChain (order=3) in SecurityConfig.
    // CSRF protection is enabled via CookieCsrfTokenRepository so the SPA can read the
    // XSRF-TOKEN cookie and send it back in an X-XSRF-TOKEN header on mutating requests.
    @Bean
    @Order(2)
    fun samlSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .cors { it.configurationSource(corsConfig.corsConfigurationSource()) }
            .csrf { it.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()) }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(
                        AntPathRequestMatcher("/saml2/**"),
                        AntPathRequestMatcher("/login/**"),
                        AntPathRequestMatcher("/error")
                    ).permitAll()
                    .anyRequest().authenticated()
            }
            .saml2Login(Customizer.withDefaults())
            .saml2Logout(Customizer.withDefaults())

        return http.build()
    }
}
