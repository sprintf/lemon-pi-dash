package com.normtronix.dash

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit4.SpringRunner
@SpringBootTest(classes = [AuthService::class])
@Import(TestFirestoreConfiguration::class)
@TestPropertySource(locations=["classpath:application.properties"])
class DashApplicationTests {

	@Test
	fun contextLoads() {
	}

}
