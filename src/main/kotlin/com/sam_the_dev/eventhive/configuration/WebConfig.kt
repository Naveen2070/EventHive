package com.sam_the_dev.eventhive.configuration

import org.springframework.context.annotation.Configuration
import org.springframework.data.web.config.EnableSpringDataWebSupport
import org.springframework.retry.annotation.EnableRetry

@Configuration
@EnableRetry
@EnableSpringDataWebSupport(
    pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO
)
class WebConfig
