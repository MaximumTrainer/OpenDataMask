package com.opendatamask.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate

@Configuration
class WebClientConfig {
    @Bean
    fun restTemplate(): RestTemplate = RestTemplate()
}
