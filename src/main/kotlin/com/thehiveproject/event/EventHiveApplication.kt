package com.thehiveproject.event

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class EventHiveApplication

fun main(args: Array<String>) {
	runApplication<EventHiveApplication>(*args)
}
