package com.normtronix.dash

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import

@SpringBootTest
@Import(TestFirestoreConfiguration::class)
class DashApplicationTests {

	@Test
	fun contextLoads() {
	}

}
