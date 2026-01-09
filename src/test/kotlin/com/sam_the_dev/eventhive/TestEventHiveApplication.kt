package com.sam_the_dev.eventhive

import org.springframework.boot.fromApplication
import org.springframework.boot.with


fun main(args: Array<String>) {
	fromApplication<EventHiveApplication>().with(TestcontainersConfiguration::class).run(*args)
}
