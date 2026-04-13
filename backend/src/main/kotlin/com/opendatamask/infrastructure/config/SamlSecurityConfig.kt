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
// This configuration is activated only when:
//   1. The `spring-security-saml2-service-provider` library is on the classpath, AND
//   2. At least one relying-party registration is configured under the property
//      prefix `spring.security.saml2.relyingparty.registration`.
//
// To enable SAML SSO:
//   1. Add the Shibboleth repository and the SAML SP dependency to build.gradle.kts:
//        repositories {
//            maven { url = uri("https://build.shibboleth.net/nexus/content/repositories/releases/") }
//        }
//        dependencies {
//            implementation("org.springframework.security:spring-security-saml2-service-provider")
//        }
//   2. Configure the IdP in application.yml:
//        spring.security.saml2.relyingparty.registration.default.assertingparty.metadata-uri: <url>
@Configuration
@ConditionalOnClass(name = ["org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository"])
@ConditionalOnProperty(prefix = "spring.security.saml2.relyingparty.registration", name = ["default.assertingparty.metadata-uri"])
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
