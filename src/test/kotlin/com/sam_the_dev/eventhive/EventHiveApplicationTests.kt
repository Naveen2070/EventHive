package com.sam_the_dev.eventhive

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import

@Import(TestcontainersConfiguration::class)
@SpringBootTest
class EventHiveApplicationTests {

	@Test
	fun contextLoads() {
	}

}
