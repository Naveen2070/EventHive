package com.sam_the_dev.eventhive

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class EventHiveApplication

fun main(args: Array<String>) {
	runApplication<EventHiveApplication>(*args)
}
